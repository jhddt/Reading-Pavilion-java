import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { hasAnyRole } from '../auth/roles'
import { api } from '../lib/api'
import { essayStatusText } from '../lib/format'
import { normalizeOcrTextForDisplay } from '../lib/ocrText'
import type { Essay, ReviewRule, ReviewStatus } from '../types'

const progressStages = ['批改任务已创建', '错字修改处理中', '内容批改生成中', '批改结果整理完成']
const ESSAY_CHARS_PER_LINE = 30
const ESSAY_FIRST_LINE_INDENT = 2
const ESSAY_FIRST_ROW_CHARS = ESSAY_CHARS_PER_LINE - ESSAY_FIRST_LINE_INDENT

function normalizeEssayLineBreaks(text: string) {
  return text.replace(/\r\n?/g, '\n')
}

function splitEssayParagraphLines(text: string) {
  const normalized = normalizeEssayLineBreaks(text).trim()
  if (!normalized) return []
  const paragraphs: string[] = []
  let current = ''
  for (let i = 0; i < normalized.length; i += 1) {
    const ch = normalized[i]
    if (ch === '\n') {
      if (current) {
        paragraphs.push(current)
        current = ''
      }
      continue
    }
    current += ch
  }
  if (current) paragraphs.push(current)
  return paragraphs
}

function buildEssayRowsForParagraph(paragraph: string) {
  // 后端格式化文本可能已带段首空白（半角/全角），
  // 这里先去掉原始前导空白，避免与稿纸首行缩进叠加导致“缩进过深”。
  const normalizedParagraph = paragraph.replace(/^[\s\u3000]+/, '')
  const chars = Array.from(normalizedParagraph)
  if (!chars.length) return [] as string[][]
  const rows: string[][] = []
  const firstChars = chars.slice(0, ESSAY_FIRST_ROW_CHARS)
  rows.push([...(Array.from({ length: ESSAY_FIRST_LINE_INDENT }).fill('') as string[]), ...firstChars])
  for (let offset = ESSAY_FIRST_ROW_CHARS; offset < chars.length; offset += ESSAY_CHARS_PER_LINE) {
    rows.push(chars.slice(offset, offset + ESSAY_CHARS_PER_LINE))
  }
  return rows
}

function buildEssayTitleRow(title: string) {
  const chars = Array.from((title || '').trim() || '未命名作文')
  const visible = chars.slice(0, ESSAY_CHARS_PER_LINE)
  const pad = Math.max(0, ESSAY_CHARS_PER_LINE - visible.length)
  const padLeft = Math.floor(pad / 2)
  const padRight = pad - padLeft
  return {
    padLeft,
    chars: visible,
    padRight,
  }
}

export function EssayDetailPage() {
  const { token, user } = useAuth()
  const navigate = useNavigate()
  const { essayId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const [essay, setEssay] = useState<Essay | null>(null)
  const [rules, setRules] = useState<ReviewRule[]>([])
  const [selectedRuleId, setSelectedRuleId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [reviewing, setReviewing] = useState(false)
  const [reviewPending, setReviewPending] = useState(false)
  const [reviewConfigOpen, setReviewConfigOpen] = useState(false)
  const [reviewConfigMode, setReviewConfigMode] = useState<'preset' | 'custom'>('preset')
  const [customRuleName, setCustomRuleName] = useState('')
  const [customRequirement, setCustomRequirement] = useState('')
  const [creatingCustomRule, setCreatingCustomRule] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [activeReviewId, setActiveReviewId] = useState<number | null>(null)
  const [reviewStatus, setReviewStatus] = useState<ReviewStatus | null>(null)
  const [ocrStatus, setOcrStatus] = useState<'loading' | 'success' | 'error' | null>(null)
  const [ocrAccuracy, setOcrAccuracy] = useState<number | null>(null)
  const [ocrText, setOcrText] = useState('')
  const [editedText, setEditedText] = useState('')
  const [ocrImageUrl, setOcrImageUrl] = useState<string | null>(null)
  const timerRef = useRef<number | null>(null)
  const ocrTimerRef = useRef<number | null>(null)
  const canManualReview = hasAnyRole(user?.role, ['TEACHER', 'ADMIN'])

  const loadData = async () => {
    if (!essayId) return
    setError('')
    try {
      const [essayData, ruleData] = await Promise.all([
        api.get<Essay>(`/essay/${essayId}`, token),
        api.get<ReviewRule[]>('/review/rules?enabledOnly=true', token),
      ])
      setEssay(essayData)
      setRules(ruleData)
      setSelectedRuleId((prev) => prev ?? ruleData[0]?.ruleId ?? null)
      
      console.log('Essay data:', essayData)
      console.log('Submit type:', essayData.submitType, 'Status:', essayData.status)
      
      // 只有图片作文(submitType=1)才检查OCR状态
      if (essayData.submitType === 1) {
        console.log('Image essay detected, checking OCR status...')
        checkOcrStatus(essayData.id)
      } else {
        console.log('Not an image essay, skipping OCR check')
      }
      
      // 如果作文状态是"批改中"(status=2)或"已批改"(status=3)，自动加载最新的批改进度
      if (essayData.status === 2 || essayData.status === 3) {
        console.log('Essay is being reviewed or reviewed, loading review status...')
        await checkLatestReviewStatus(essayData.id)
      }
    } catch (err) {
      setError((err as Error).message || '加载失败')
    }
  }
  
  const checkLatestReviewStatus = async (essayId: number) => {
    try {
      console.log('=== Checking latest review status for essay:', essayId)
      
      // 获取该作文的最新批改记录
      const response = await api.get<any>(`/review/records?essayId=${essayId}&page=1&pageSize=1`, token)
      console.log('=== Review records API response:', response)
      
      // 检查返回的数据结构（可能是分页对象）
      const reviews = response?.records || response?.data || response
      console.log('=== Extracted reviews:', reviews)
      
      if (reviews && Array.isArray(reviews) && reviews.length > 0) {
        const latestReview = reviews[0]
        console.log('=== Latest review found:', latestReview)
        console.log('=== Latest review status:', latestReview.status)
        
        // 如果最新批改记录状态是处理中(0,1,2)，启动轮询
        if (latestReview.status >= 0 && latestReview.status <= 2) {
          console.log('=== Review is in progress, starting polling...')
          setActiveReviewId(latestReview.reviewId)
          setReviewPending(true)
          
          // 立即获取一次状态
          const status = await api.get<ReviewStatus>(`/review/status/${latestReview.reviewId}`, token)
          console.log('=== Review status from API:', status)
          setReviewStatus(status)
        } else if (latestReview.status === 3) {
          // 如果已经完成，也显示进度条（显示完成状态）
          console.log('=== Review is completed, showing progress bar...')
          setActiveReviewId(latestReview.reviewId)
          const status = await api.get<ReviewStatus>(`/review/status/${latestReview.reviewId}`, token)
          console.log('=== Review status from API:', status)
          setReviewStatus(status)
        } else {
          console.log('=== Review status is:', latestReview.status, '- not showing progress bar')
        }
      } else {
        console.log('=== No reviews found for this essay')
      }
    } catch (err) {
      console.error('=== Failed to check latest review status:', err)
    }
  }

  const checkOcrStatus = async (id: number) => {
    try {
      console.log('Fetching OCR data for essay:', id)
      const ocrData = await api.get<any>(`/ocr/essay/${id}`, token)
      console.log('OCR data received:', ocrData)
      
      if (ocrData && ocrData.ocrText) {
        const normalizedOcrText = normalizeOcrTextForDisplay(ocrData.ocrText)
        setOcrStatus('success')
        setOcrAccuracy(ocrData.accuracy || null)
        setOcrText(normalizedOcrText)
        setEditedText(normalizedOcrText)
        
        // 获取OCR对比图URL
        if (ocrData.ocrId) {
          try {
            const imageData = await api.get<any>(`/ocr/${ocrData.ocrId}/result-image`, token)
            if (imageData && imageData.imageUrl) {
              setOcrImageUrl(imageData.imageUrl)
              console.log('OCR image URL loaded:', imageData.imageUrl)
            }
          } catch (imgErr) {
            console.error('Failed to load OCR image:', imgErr)
          }
        }
        
        console.log('OCR status set to success')
      } else {
        // OCR还在处理中,启动轮询
        console.log('OCR not ready, starting polling...')
        setOcrStatus('loading')
        startOcrPolling(id)
      }
    } catch (err) {
      console.error('Error fetching OCR data:', err)
      // OCR接口出错，不显示OCR面板
      setOcrStatus(null)
    }
  }

  const startOcrPolling = (id: number) => {
    if (ocrTimerRef.current) return
    
    ocrTimerRef.current = window.setInterval(async () => {
      try {
        const ocrData = await api.get<any>(`/ocr/essay/${id}`, token)
        if (ocrData && ocrData.ocrText) {
          const normalizedOcrText = normalizeOcrTextForDisplay(ocrData.ocrText)
          setOcrStatus('success')
          setOcrAccuracy(ocrData.accuracy || null)
          setOcrText(normalizedOcrText)
          setEditedText(normalizedOcrText)
          if (ocrTimerRef.current) {
            window.clearInterval(ocrTimerRef.current)
            ocrTimerRef.current = null
          }
          await loadData()
        }
      } catch (err) {
        // 继续轮询
      }
    }, 2000)
  }

  const saveEditedOcrText = async () => {
    if (!essay || !editedText.trim()) return
    setSubmitting(true)
    setError('')
    try {
      // 更新作文内容
      await api.put<void>(`/essay/${essay.id}`, { 
        title: essay.title,
        originalContent: editedText 
      }, token)
      await loadData()
      setOcrStatus(null) // 隐藏OCR编辑区
    } catch (err) {
      setError((err as Error).message || '保存失败')
    } finally {
      setSubmitting(false)
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined)
    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current)
      if (ocrTimerRef.current) window.clearInterval(ocrTimerRef.current)
    }
  }, [essayId])

  useEffect(() => {
    if (!activeReviewId) return
    timerRef.current = window.setInterval(async () => {
      try {
        const status = await api.get<ReviewStatus>(`/review/status/${activeReviewId}`, token)
        setReviewStatus(status)
        if (status.status === 3) {
          // SUCCESS - 批改完成
          if (timerRef.current) window.clearInterval(timerRef.current)
          setReviewPending(false)
          await loadData()
          
          // 提示用户批改完成
          const confirmView = window.confirm('✅ 批改完成！是否立即查看批改结果？')
          if (confirmView) {
            navigate(`/reviews/${activeReviewId}`)
          }
          return
        }
        if (status.status === 4) {
          // FAIL - 批改失败
          if (timerRef.current) window.clearInterval(timerRef.current)
          setReviewPending(false)
          setError(status.errorMsg || '批改失败，请稍后重试')
          await loadData()
        }
      } catch {
        if (timerRef.current) window.clearInterval(timerRef.current)
        setReviewPending(false)
      }
    }, 2000)

    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current)
    }
  }, [activeReviewId, token])

  const content = useMemo(
    () => normalizeOcrTextForDisplay(essay?.finalContent || essay?.originalContent || ''),
    [essay],
  )
  const essayTitleRow = useMemo(() => buildEssayTitleRow(essay?.title || ''), [essay?.title])
  const paperParagraphRows = useMemo(
    () => splitEssayParagraphLines(content).map((paragraph) => buildEssayRowsForParagraph(paragraph)),
    [content],
  )

  const submitEssay = async () => {
    if (!essay) return
    setSubmitting(true)
    setError('')
    try {
      await api.put<void>(`/essay/${essay.id}/submit`, null, token)
      await loadData()
    } catch (err) {
      setError((err as Error).message || '提交失败')
    } finally {
      setSubmitting(false)
    }
  }

  const withdrawEssay = async () => {
    if (!essay) return
    if (!window.confirm(`确定撤回作文「${essay.title}」吗？`)) return
    try {
      await api.put<void>(`/essay/${essay.id}/withdraw`, null, token)
      await loadData()
    } catch (err) {
      setError((err as Error).message || '撤回失败')
    }
  }

  const deleteEssay = async () => {
    if (!essay) return
    if (!window.confirm(`确定删除草稿「${essay.title}」吗？`)) return
    setDeleting(true)
    try {
      await api.delete<void>(`/essay/${essay.id}`, token)
      navigate('/essays')
    } catch (err) {
      setError((err as Error).message || '删除失败')
    } finally {
      setDeleting(false)
    }
  }

  const launchReview = async (ruleId: number) => {
    if (!essay) return
    setReviewing(true)
    setReviewPending(true)
    setError('')
    try {
      const result = await api.post<{ reviewId: number }>(`/review/essay/${essay.id}?ruleId=${ruleId}`, null, token)
      setActiveReviewId(result.reviewId)
      setReviewStatus({ reviewId: result.reviewId, status: 1 })
    } catch (err) {
      setError((err as Error).message || '发起批改失败')
      setReviewPending(false)
    } finally {
      setReviewing(false)
    }
  }

  const confirmReviewConfig = async () => {
    if (!essay) return
    if (reviewConfigMode === 'preset') {
      if (!selectedRuleId) {
        setError('请先选择评分细则')
        return
      }
      setReviewConfigOpen(false)
      await launchReview(selectedRuleId)
      return
    }

    const name = customRuleName.trim() || `临时细则-${new Date().toLocaleString()}`
    const custom = customRequirement.trim()
    if (!custom) {
      setError('请填写自定义评分细则内容')
      return
    }

    setCreatingCustomRule(true)
    try {
      const created = await api.post<ReviewRule>(
        '/review/rules',
        {
          ruleName: name,
          customRequirement: custom,
          reviewType: '自定义临时批改',
          gradeLevel: '临时',
        },
        token,
      )
      if (!created.ruleId) throw new Error('创建自定义细则失败')
      setRules((prev) => [created, ...prev])
      setSelectedRuleId(created.ruleId)
      setReviewConfigOpen(false)
      await launchReview(created.ruleId)
    } catch (err) {
      setError((err as Error).message || '创建自定义细则失败')
    } finally {
      setCreatingCustomRule(false)
    }
  }

  if (!essay) {
    return <div className="empty-state">正在加载作文详情...</div>
  }

  return (
    <div className="page-grid">
      <section className="split-grid detail-grid">
        <div className="stack-list">
          {/* 提示信息 - 当前作文类型 */}
          <article className="panel">
            <div className="helper-text">
              <strong>作文类型：</strong>
              {essay.submitType === 0 ? '📝 文本作文' : essay.submitType === 1 ? '📷 图片作文（OCR识别）' : essay.submitType === 2 ? '📄 文档作文' : '未知类型'}
              {' | '}
              <strong>状态：</strong>
              {essay.status === 0 ? '草稿' : essay.status === 1 ? '已提交' : essay.status === 2 ? '批改中' : essay.status === 3 ? '已批改' : '未知'}
            </div>
          </article>

          {/* 调试按钮 - 测试OCR面板（仅图片作文） */}
          {essay.submitType === 1 && !ocrStatus ? (
            <article className="panel">
              <button 
                type="button" 
                className="secondary-button" 
                onClick={() => checkOcrStatus(essay.id)}
              >
                🔍 检查OCR识别状态
              </button>
            </article>
          ) : null}

          {/* OCR识别结果展示区域（仅图片作文） */}
          {essay.submitType === 1 && ocrStatus ? (
            <article className="panel ocr-result-panel">
              {ocrStatus === 'loading' ? (
                <div className="ocr-loading">
                  <div className="ocr-loading-spinner" />
                  <h4>正在识别图片中的文字...</h4>
                  <p>请稍候,识别完成后将自动显示结果</p>
                </div>
              ) : ocrStatus === 'success' ? (
                <div className="ocr-success">
                  <div className="ocr-header">
                    <h4>OCR识别结果</h4>
                    {ocrAccuracy !== null ? (
                      <div className="ocr-accuracy-box">
                        <span className="ocr-accuracy-label">识别准确率</span>
                        <div className="ocr-accuracy-bar-container">
                          <div 
                            className={`ocr-accuracy-bar ${ocrAccuracy < 80 ? 'ocr-accuracy-low' : ''}`}
                            style={{ width: `${ocrAccuracy}%` }}
                          />
                        </div>
                        <span className={`ocr-accuracy-value ${ocrAccuracy < 80 ? 'ocr-accuracy-warning' : ''}`}>
                          {ocrAccuracy.toFixed(1)}%
                        </span>
                      </div>
                    ) : null}
                  </div>
                  
                  {ocrAccuracy !== null && ocrAccuracy < 80 ? (
                    <div className="ocr-warning">
                      ⚠️ 当前识别质量一般,建议仔细核对识别结果
                    </div>
                  ) : null}
                  
                  <div className="ocr-content-grid">
                    <div className="ocr-text-section">
                      <label className="field">
                        <span>识别文本（可编辑修改）</span>
                        <textarea
                          value={editedText}
                          onChange={(e) => setEditedText(e.target.value)}
                          rows={20}
                          className="ocr-text-editor"
                          placeholder="OCR识别的文本将显示在这里..."
                        />
                      </label>
                      
                      <div className="action-row">
                        <button 
                          type="button" 
                          className="primary-button" 
                          onClick={saveEditedOcrText}
                          disabled={submitting || !editedText.trim()}
                        >
                          {submitting ? '保存中...' : '确认并保存'}
                        </button>
                        <button 
                          type="button" 
                          className="secondary-button" 
                          onClick={() => setEditedText(ocrText)}
                        >
                          恢复原始识别结果
                        </button>
                      </div>
                    </div>
                    
                    {ocrImageUrl ? (
                      <div className="ocr-image-section">
                        <label className="field">
                          <span>原图预览</span>
                        </label>
                        <div className="ocr-image-container">
                          <img src={ocrImageUrl} alt="OCR识别原图" className="ocr-preview-image" />
                        </div>
                      </div>
                    ) : null}
                  </div>
                </div>
              ) : ocrStatus === 'error' ? (
                <div className="ocr-error">
                  <p>❌ OCR识别失败,请重新上传图片或联系管理员</p>
                </div>
              ) : null}
            </article>
          ) : null}

          <article className="panel">
            <div className="action-row">
              {essay.status === 0 ? (
                <button type="button" className="primary-button" onClick={submitEssay} disabled={submitting}>
                  {submitting ? '提交中...' : '提交作文'}
                </button>
              ) : null}
              {essay.status === 1 ? (
                <button type="button" className="primary-button" onClick={() => setReviewConfigOpen(true)} disabled={reviewing}>
                  {reviewing ? '提交中...' : '开始批改'}
                </button>
              ) : null}
              {essay.status >= 2 ? (
                <button type="button" className="primary-button" onClick={() => navigate(`/reviews?essayId=${essay.id}`)}>
                  查看批改记录
                </button>
              ) : null}
              {essay.status === 1 ? (
                <button type="button" className="secondary-button" onClick={withdrawEssay}>
                  撤回作文
                </button>
              ) : null}
              {canManualReview && essay.status >= 1 ? (
                <button type="button" className="secondary-button" onClick={() => navigate(`/essays/${essay.id}/manual-review`)}>
                  手动批改
                </button>
              ) : null}
              {essay.status === 0 ? (
                <button type="button" className="danger-button" onClick={deleteEssay} disabled={deleting}>
                  {deleting ? '删除中...' : '删除草稿'}
                </button>
              ) : null}
              <span className="pill essay-detail-status-pill">{essayStatusText(essay.status)}</span>
            </div>
          </article>

          {reviewStatus ? (
            <article className="panel panel-blue">
              <p className="eyebrow">批改进度</p>
              <h3>
                {reviewStatus.status === 4 
                  ? '批改失败' 
                  : reviewStatus.status === 3 
                  ? '批改完成' 
                  : progressStages[reviewStatus.status] || '处理中'}
              </h3>
              <div className="progress-box">
                <div className="progress-bar" style={{ width: `${reviewStatus.status >= 3 ? 100 : Math.round((reviewStatus.status / 3) * 100)}%` }} />
              </div>
              <div className="stack-list compact-list">
                {progressStages.map((stage, index) => (
                  <div key={stage} className={`progress-row ${reviewStatus.status >= index ? 'progress-row-active' : ''}`}>
                    {stage}
                  </div>
                ))}
              </div>
              {reviewStatus.status === 3 ? (
                <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${activeReviewId}`)}>
                  查看结果详情
                </button>
              ) : null}
              {reviewStatus.status === 4 && reviewStatus.errorMsg ? (
                <p style={{ color: 'var(--danger)', marginTop: '8px' }}>{reviewStatus.errorMsg}</p>
              ) : null}
            </article>
          ) : null}
        </div>

        <article className="panel">
          <div className="paper-content">
            <div className="paper-title">正文内容</div>
            {paperParagraphRows.length ? (
              <div className="essay-detail-paper-grid" aria-label="作文正文稿纸">
                <div
                  className="essay-detail-paper-row essay-detail-paper-title-row"
                  style={{ gridTemplateColumns: `repeat(${ESSAY_CHARS_PER_LINE}, minmax(0, 1fr))` }}
                >
                  {Array.from({ length: essayTitleRow.padLeft }).map((_, idx) => (
                    <span key={`essay-title-pl-${idx}`} className="essay-detail-paper-cell essay-detail-paper-cell-indent" aria-hidden="true" />
                  ))}
                  {essayTitleRow.chars.map((char, idx) => (
                    <span key={`essay-title-char-${idx}`} className="essay-detail-paper-cell essay-detail-paper-cell-title">
                      {char}
                    </span>
                  ))}
                  {Array.from({ length: essayTitleRow.padRight }).map((_, idx) => (
                    <span key={`essay-title-pr-${idx}`} className="essay-detail-paper-cell essay-detail-paper-cell-indent" aria-hidden="true" />
                  ))}
                </div>
                {paperParagraphRows.map((rows, paragraphIndex) => (
                  <section key={`essay-p-${paragraphIndex}`} className="essay-detail-paper-paragraph">
                    {rows.map((row, rowIndex) => (
                      <div
                        key={`essay-r-${paragraphIndex}-${rowIndex}`}
                        className="essay-detail-paper-row"
                        style={{ gridTemplateColumns: `repeat(${ESSAY_CHARS_PER_LINE}, minmax(0, 1fr))` }}
                      >
                        {row.map((char, charIndex) => (
                          <span
                            key={`essay-c-${paragraphIndex}-${rowIndex}-${charIndex}`}
                            className={`essay-detail-paper-cell ${char ? '' : 'essay-detail-paper-cell-indent'}`}
                            aria-hidden={char ? undefined : true}
                          >
                            {char}
                          </span>
                        ))}
                        {row.length < ESSAY_CHARS_PER_LINE
                          ? Array.from({ length: ESSAY_CHARS_PER_LINE - row.length }).map((_, idx) => (
                              <span key={`essay-empty-${paragraphIndex}-${rowIndex}-${idx}`} className="essay-detail-paper-cell" aria-hidden="true" />
                            ))
                          : null}
                      </div>
                    ))}
                  </section>
                ))}
              </div>
            ) : (
              <pre>暂无内容</pre>
            )}
          </div>
        </article>

      </section>

      {error ? <div className="feedback error">{error}</div> : null}
      {searchParams.get('rereview') === '1' ? (
        <div className="helper-text">
          当前从“再次批改”进入，已经保留在作文详情页选择细则。你可以直接重新发起批改。
          <button type="button" className="text-button" onClick={() => setSearchParams({})}>
            关闭提示
          </button>
        </div>
      ) : null}
      {reviewConfigOpen ? (
        <div className="overlay" onClick={() => setReviewConfigOpen(false)}>
          <div className="modal-card essay-review-config-modal" onClick={(event) => event.stopPropagation()}>
            <div className="create-mode-tabs" role="tablist" aria-label="评分细则来源">
              <button
                type="button"
                className={`create-mode-tab ${reviewConfigMode === 'preset' ? 'create-mode-tab-active' : ''}`}
                onClick={() => setReviewConfigMode('preset')}
              >
                选择已有细则
              </button>
              <button
                type="button"
                className={`create-mode-tab ${reviewConfigMode === 'custom' ? 'create-mode-tab-active' : ''}`}
                onClick={() => setReviewConfigMode('custom')}
              >
                自定义细则
              </button>
            </div>

            {reviewConfigMode === 'preset' ? (
              <label className="field">
                <span>评分细则</span>
                <select value={selectedRuleId ?? ''} onChange={(event) => setSelectedRuleId(Number(event.target.value))}>
                  {rules.map((rule) => (
                    <option key={rule.ruleId} value={rule.ruleId}>
                      {rule.ruleName}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <div className="form-grid">
                <label className="field">
                  <span>细则名称（可选）</span>
                  <input value={customRuleName} onChange={(event) => setCustomRuleName(event.target.value)} placeholder="例如：本次作文临时细则" />
                </label>
                <label className="field">
                  <span>自定义评分细则</span>
                  <textarea
                    value={customRequirement}
                    onChange={(event) => setCustomRequirement(event.target.value)}
                    rows={5}
                    placeholder="请输入本次批改要求，例如：突出结构完整性、语言准确性、情感真实性等。"
                  />
                </label>
              </div>
            )}

            <div className="action-row">
              <button type="button" className="primary-button" onClick={confirmReviewConfig} disabled={reviewing || creatingCustomRule}>
                {creatingCustomRule ? '创建并提交中...' : reviewing ? '提交中...' : '确认开始批改'}
              </button>
              <button type="button" className="secondary-button" onClick={() => setReviewConfigOpen(false)}>
                取消
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { hasAnyRole } from '../auth/roles'
import { api } from '../lib/api'
import { essayStatusText } from '../lib/format'
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
  const chars = Array.from(paragraph)
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
  const timerRef = useRef<number | null>(null)
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
    } catch (err) {
      setError((err as Error).message || '加载失败')
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined)
    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current)
    }
  }, [essayId])

  useEffect(() => {
    if (!activeReviewId) return
    timerRef.current = window.setInterval(async () => {
      try {
        const status = await api.get<ReviewStatus>(`/review/status/${activeReviewId}`, token)
        setReviewStatus(status)
        if (status.status === 2) {
          if (timerRef.current) window.clearInterval(timerRef.current)
          setReviewPending(false)
          await loadData()
          navigate(`/reviews/${activeReviewId}`)
          return
        }
        if (status.status === 3 || status.status === 4) {
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

  const content = useMemo(() => essay?.finalContent || essay?.originalContent || '', [essay])
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
              <h3>{progressStages[Math.min(reviewStatus.status, progressStages.length - 1)] || '处理中'}</h3>
              <div className="progress-box">
                <div className="progress-bar" style={{ width: `${reviewStatus.status >= 2 ? 100 : 65}%` }} />
              </div>
              <div className="stack-list compact-list">
                {progressStages.map((stage, index) => (
                  <div key={stage} className={`progress-row ${reviewStatus.status >= index ? 'progress-row-active' : ''}`}>
                    {stage}
                  </div>
                ))}
              </div>
              {reviewStatus.status === 2 ? (
                <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${activeReviewId}`)}>
                  查看结果详情
                </button>
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
      {reviewPending ? (
        <div className="overlay">
          <div className="modal-card essay-review-loading-modal">
            <div className="essay-review-loading-spinner" aria-hidden="true" />
            <h3>批改进行中</h3>
            <p>正在调用评分模型，请稍候，完成后将自动跳转到批注概览页面。</p>
          </div>
        </div>
      ) : null}
    </div>
  )
}

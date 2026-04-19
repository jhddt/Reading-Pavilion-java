import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { hasAnyRole } from '../auth/roles'
import { api } from '../lib/api'
import { essayStatusText, formatDateTime, submitTypeText } from '../lib/format'
import type { Essay, ReviewRule, ReviewStatus } from '../types'

const progressStages = ['批改任务已创建', '错字修改处理中', '内容批改生成中', '批改结果整理完成']

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
        if (status.status === 2 || status.status === 3 || status.status === 4) {
          if (timerRef.current) window.clearInterval(timerRef.current)
          await loadData()
        }
      } catch {
        if (timerRef.current) window.clearInterval(timerRef.current)
      }
    }, 2000)

    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current)
    }
  }, [activeReviewId, token])

  const content = useMemo(() => essay?.finalContent || essay?.originalContent || '', [essay])

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

  const startReview = async () => {
    if (!essay || !selectedRuleId) {
      setError('请先选择评分细则')
      return
    }
    setReviewing(true)
    setError('')
    try {
      const result = await api.post<{ reviewId: number }>(
        `/review/essay/${essay.id}?ruleId=${selectedRuleId}`,
        null,
        token,
      )
      setActiveReviewId(result.reviewId)
      setReviewStatus({ reviewId: result.reviewId, status: 1 })
    } catch (err) {
      setError((err as Error).message || '发起批改失败')
    } finally {
      setReviewing(false)
    }
  }

  if (!essay) {
    return <div className="empty-state">正在加载作文详情...</div>
  }

  return (
    <div className="page-grid">
      <section className="split-grid detail-grid">
        <article className="panel">
          <div className="section-heading">
            <div>
              <p className="eyebrow">作文详情</p>
              <h3>{essay.title || '未命名作文'}</h3>
            </div>
            <span className="pill">{essayStatusText(essay.status)}</span>
          </div>

          <div className="card-grid two-col">
            <article className="mini-card">
              <span>作文编号</span>
              <strong>{essay.id}</strong>
            </article>
            <article className="mini-card">
              <span>字数</span>
              <strong>{essay.wordCount || 0}</strong>
            </article>
            <article className="mini-card">
              <span>提交方式</span>
              <strong>{submitTypeText(essay.submitType)}</strong>
            </article>
            <article className="mini-card">
              <span>创建时间</span>
              <strong>{formatDateTime(essay.createTime)}</strong>
            </article>
          </div>

          <div className="paper-content">
            <div className="paper-title">正文内容</div>
            <pre>{content || '暂无内容'}</pre>
          </div>
        </article>

        <div className="stack-list">
          <article className="panel">
            <div className="section-heading">
              <div>
                <p className="eyebrow">操作区</p>
                <h3>{essayStatusText(essay.status)}</h3>
              </div>
            </div>

            {essay.status >= 1 ? (
              <label className="field">
                <span>评分细则</span>
                <select
                  value={selectedRuleId ?? ''}
                  onChange={(event) => setSelectedRuleId(Number(event.target.value))}
                >
                  {rules.map((rule) => (
                    <option key={rule.ruleId} value={rule.ruleId}>
                      {rule.ruleName}
                    </option>
                  ))}
                </select>
              </label>
            ) : null}

            <div className="action-row">
              {essay.status === 0 ? (
                <button type="button" className="primary-button" onClick={submitEssay} disabled={submitting}>
                  {submitting ? '提交中...' : '提交作文'}
                </button>
              ) : null}
              {essay.status === 1 ? (
                <button type="button" className="primary-button" onClick={startReview} disabled={reviewing}>
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
    </div>
  )
}

import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { hasAnyRole } from '../auth/roles'
import { api } from '../lib/api'
import { formatShortDateTime, reviewStatusText } from '../lib/format'
import type { PageData, ReviewRecord } from '../types'

export function ReviewsPage() {
  const { token, user } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [records, setRecords] = useState<ReviewRecord[]>([])
  const [page, setPage] = useState(1)
  const [status, setStatus] = useState<number | ''>('')
  const [reviewerType, setReviewerType] = useState<number | ''>('')
  const [hasMore, setHasMore] = useState(false)
  const [error, setError] = useState('')
  const canManualReview = hasAnyRole(user?.role, ['TEACHER', 'ADMIN'])

  useEffect(() => {
    const loadData = async () => {
      setError('')
      try {
        const params = new URLSearchParams({
          page: String(page),
          pageSize: '10',
        })
        if (status !== '') params.set('status', String(status))
        if (reviewerType !== '') params.set('reviewerType', String(reviewerType))
        if (searchParams.get('essayId')) params.set('essayId', searchParams.get('essayId') || '')
        const data = await api.get<PageData<ReviewRecord>>(`/review/records?${params.toString()}`, token)
        const list = data.records || data.rows || []
        setRecords(list)
        setHasMore(page * 10 < (data.total || 0))
      } catch (err) {
        setError((err as Error).message || '加载失败')
      }
    }

    loadData().catch(() => undefined)
  }, [page, reviewerType, searchParams, status, token])

  const removeReview = async (reviewId: number, essayId?: number) => {
    if (!window.confirm('确定删除这条批改记录吗？删除后可以重新批改该作文。')) return
    try {
      await api.delete<void>(`/review/record/${reviewId}`, token)
      setRecords((prev) => prev.filter((item) => item.reviewId !== reviewId))
      
      // 如果有essayId，提示用户可以重新批改
      if (essayId) {
        const shouldNavigate = window.confirm('批改记录已删除，作文已恢复为"已提交"状态。是否前往作文详情页重新批改？')
        if (shouldNavigate) {
          navigate(`/essays/${essayId}`)
        }
      }
    } catch (err) {
      setError((err as Error).message || '删除失败')
    }
  }

  return (
    <div className="page-grid">
      <section className="section-heading reviews-heading">
        <div>
          <p className="eyebrow">批改记录</p>
          <h3>查看每次批改的结果与状态</h3>
        </div>
        <div className="action-row reviews-heading-actions">
          <select value={status} onChange={(event) => setStatus(event.target.value === '' ? '' : Number(event.target.value))}>
            <option value="">全部状态</option>
            <option value="0">任务已创建</option>
            <option value="1">错字修改处理中</option>
            <option value="2">内容批改生成中</option>
            <option value="3">批改完成</option>
            <option value="4">批改失败</option>
          </select>
          <select
            value={reviewerType}
            onChange={(event) => setReviewerType(event.target.value === '' ? '' : Number(event.target.value))}
          >
            <option value="">全部评审者</option>
            <option value="0">AI</option>
            <option value="1">教师</option>
          </select>
        </div>
      </section>

      <section className="stack-list">
        {records.map((item) => (
          <article key={item.reviewId} className="list-row review-list-row">
            <div>
              <strong>{item.essayTitle || '未命名作文'}</strong>
              <p>
                第 {item.reviewVersion || 1} 次批改 · {item.ruleName || '未记录细则'} ·{' '}
                {formatShortDateTime(item.startTime)}
              </p>
            </div>
            <div className="list-row-actions">
              <span className="pill">{reviewStatusText(item.status)}</span>
              <span className="score-pill">
                {item.totalScore != null ? `${item.totalScore.toFixed(1)} 分` : '待评分'}
              </span>
              <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${item.reviewId}`)}>
                批注阅读
              </button>
              <button
                type="button"
                className="secondary-button"
                onClick={() => navigate(`/reviews/${item.reviewId}/summary`)}
              >
                结果总览
              </button>
              {canManualReview ? (
                <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${item.reviewId}/manual`)}>
                  手动批改
                </button>
              ) : null}
              <button type="button" className="danger-button" onClick={() => removeReview(item.reviewId, item.essayId)}>
                删除记录
              </button>
            </div>
          </article>
        ))}
        {!records.length ? <div className="empty-state">暂无批改记录</div> : null}
      </section>

      {error ? <div className="feedback error">{error}</div> : null}

      <div className="section-heading">
        <div />
        <div className="action-row">
          <button type="button" className="secondary-button" disabled={page === 1} onClick={() => setPage((prev) => prev - 1)}>
            上一页
          </button>
          <button type="button" className="secondary-button" disabled={!hasMore} onClick={() => setPage((prev) => prev + 1)}>
            下一页
          </button>
        </div>
      </div>
    </div>
  )
}

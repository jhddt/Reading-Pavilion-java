import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { ManualReviewWorkspace, type ManualAnnotation } from '../components/ManualReviewWorkspace'
import { api } from '../lib/api'
import type { Essay, ReviewDetail } from '../types'

export function ReviewManualPage() {
  const { token } = useAuth()
  const navigate = useNavigate()
  const { reviewId } = useParams()
  const [detail, setDetail] = useState<ReviewDetail | null>(null)
  const [essay, setEssay] = useState<Essay | null>(null)
  const [summary, setSummary] = useState('')
  const [annotations, setAnnotations] = useState<ManualAnnotation[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const loadData = async () => {
      if (!reviewId) return
      setError('')
      try {
        const reviewDetail = await api.get<ReviewDetail>(`/review/record/${reviewId}`, token)
        setDetail(reviewDetail)
        const essayDetail = await api.get<Essay>(`/essay/${reviewDetail.essayId}`, token)
        setEssay(essayDetail)
        const summaryText = reviewDetail.comments?.find((item) => item.commentType === 1)?.content || ''
        setSummary(summaryText)
        setAnnotations(
          (reviewDetail.comments || [])
            .filter(
              (item) =>
                (item.commentType === 2 || item.commentType === 3) &&
                item.startOffset != null &&
                item.endOffset != null &&
                item.relatedText,
            )
            .map((item, index) => ({
              id: `existing-${index}`,
              type: item.commentType === 2 ? 'suggestion' : 'revision',
              title: item.commentType === 2 ? '改进建议' : '修改意见',
              selectedText: item.relatedText || '',
              startOffset: item.startOffset || 0,
              endOffset: item.endOffset || 0,
              content: item.content || '',
            })),
        )
      } catch (err) {
        setError((err as Error).message || '加载失败')
      }
    }

    loadData().catch(() => undefined)
  }, [reviewId, token])

  const submitManualReview = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!detail) return
    setLoading(true)
    setError('')
    try {
      const created = await api.post<ReviewDetail>(
        '/review/teacher/manual',
        {
          essayId: detail.essayId,
          sourceReviewId: detail.reviewId,
          summary,
          annotations: annotations.map((item) => ({
            commentType: item.type === 'suggestion' ? 2 : 3,
            content: item.content,
            startOffset: item.startOffset,
            endOffset: item.endOffset,
            relatedText: item.selectedText,
          })),
        },
        token,
      )
      navigate(`/reviews/${created.reviewId}/summary`)
    } catch (err) {
      setError((err as Error).message || '提交失败')
    } finally {
      setLoading(false)
    }
  }

  if (!detail || !essay) {
    return <div className="empty-state">正在加载手动批改页...</div>
  }

  return (
    <div className="page-grid">
      <section className="panel create-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">教师手动批改</p>
            <h3>{detail.essayTitle || '未命名作文'}</h3>
          </div>
          <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${detail.reviewId}/summary`)}>
            返回详情
          </button>
        </div>

        <ManualReviewWorkspace
          contentText={essay.finalContent || essay.originalContent || ''}
          summary={summary}
          annotations={annotations}
          loading={loading}
          onSummaryChange={setSummary}
          onAnnotationsChange={setAnnotations}
          onSubmit={submitManualReview}
        />
      </section>

      {error ? <div className="feedback error">{error}</div> : null}
    </div>
  )
}

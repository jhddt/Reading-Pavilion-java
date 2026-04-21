import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { ManualReviewWorkspace, type ManualAnnotation } from '../components/ManualReviewWorkspace'
import { api } from '../lib/api'
import { normalizeOcrTextForDisplay } from '../lib/ocrText'
import type { Essay, ReviewDetail } from '../types'

export function EssayManualReviewPage() {
  const { token } = useAuth()
  const navigate = useNavigate()
  const { essayId } = useParams()
  const [essay, setEssay] = useState<Essay | null>(null)
  const [summary, setSummary] = useState('')
  const [annotations, setAnnotations] = useState<ManualAnnotation[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const loadData = async () => {
      if (!essayId) return
      setError('')
      try {
        const essayData = await api.get<Essay>(`/essay/${essayId}`, token)
        setEssay(essayData)
      } catch (err) {
        setError((err as Error).message || '加载失败')
      }
    }

    loadData().catch(() => undefined)
  }, [essayId, token])

  const submitManualReview = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!essay) return
    setLoading(true)
    setError('')
    try {
      const created = await api.post<ReviewDetail>(
        '/review/teacher/manual',
        {
          essayId: essay.id,
          summary,
          annotations: annotations.map((item) => ({
            commentType: 3,
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

  if (!essay) return <div className="empty-state">正在加载手动批改页...</div>

  return (
    <div className="page-grid">
      <section className="panel create-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">教师手动批改</p>
            <h3>{essay.title || '未命名作文'}</h3>
          </div>
          <button type="button" className="secondary-button" onClick={() => navigate(`/essays/${essay.id}`)}>
            返回作文
          </button>
        </div>

        <ManualReviewWorkspace
          contentText={normalizeOcrTextForDisplay(essay.finalContent || essay.originalContent || '')}
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

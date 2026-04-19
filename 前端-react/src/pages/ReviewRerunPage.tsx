import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'
import type { ReviewDetail } from '../types'

export function ReviewRerunPage() {
  const { token } = useAuth()
  const navigate = useNavigate()
  const { reviewId } = useParams()

  useEffect(() => {
    const jump = async () => {
      if (!reviewId) return
      try {
        const detail = await api.get<ReviewDetail>(`/review/record/${reviewId}`, token)
        navigate(`/essays/${detail.essayId}?rereview=1&preferredRuleId=${detail.ruleId || ''}`, {
          replace: true,
        })
      } catch {
        navigate('/reviews', { replace: true })
      }
    }

    jump().catch(() => navigate('/reviews', { replace: true }))
  }, [navigate, reviewId, token])

  return <div className="empty-state">正在跳转到再次批改页面...</div>
}

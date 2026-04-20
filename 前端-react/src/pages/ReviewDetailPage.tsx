import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { hasAnyRole } from '../auth/roles'
import { api } from '../lib/api'
import { formatDateTime, formatShortDateTime, reviewStatusText } from '../lib/format'
import { extractOverallSummaryLead } from '../lib/reviewSummaryText'
import type { ReviewDetail, ReviewRecord } from '../types'

function formatCommentContent(commentType: number | undefined, content: string) {
  if (!content) return ''
  const cleaned = commentType === 1 ? extractOverallSummaryLead(content) : content
  return cleaned.replace(/\n{3,}/g, '\n\n').trim()
}

function commentTitle(commentType: number | undefined) {
  if (commentType === 2) return '改进建议'
  if (commentType === 3) return '修改意见'
  if (commentType === 4) return '亮点赏析'
  if (commentType === 1) return ''
  return '其他评语'
}

function splitCommentItems(content: string) {
  const normalized = formatCommentContent(undefined, content).replace(/\r/g, '').trim()
  if (!normalized) return []

  const numberedMatches = normalized.match(/(?:^|\n)\s*(?:\d+[.、]|[-•])\s*[\s\S]*?(?=(?:\n\s*(?:\d+[.、]|[-•])\s*)|$)/g)
  if (numberedMatches?.length) {
    return numberedMatches
      .map((item) => item.replace(/^\s*(?:\d+[.、]|[-•])\s*/u, '').trim())
      .filter(Boolean)
  }

  return normalized
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

type RevisionRow = {
  original: string
  revised: string
  note: string
}

function parseRevisionRows(content: string): RevisionRow[] {
  const blocks = splitCommentItems(content)
  return blocks.map((item) => {
    const cleaned = item.replace(/\*\*/g, '').trim()
    const originalMatch = cleaned.match(/原句[：:]\s*([\s\S]*?)(?=\s*修改后[：:]|\s*[（(]修改说明|$)/u)
    const revisedMatch = cleaned.match(/修改后[：:]\s*([\s\S]*?)(?=\s*[（(]修改说明|$)/u)
    const noteMatch = cleaned.match(/[（(]修改说明[：:]\s*([\s\S]*?)[)）]\s*$/u)

    return {
      original: originalMatch?.[1]?.trim() || '-',
      revised: revisedMatch?.[1]?.trim() || '-',
      note: noteMatch?.[1]?.trim() || cleaned,
    }
  })
}

function correctionTypeLabel(errorType?: string) {
  const map: Record<string, string> = {
    spelling: '错别字',
    grammar: '语法问题',
    punctuation: '标点问题',
    word_choice: '用词问题',
    redundancy: '重复冗余',
    style: '表达风格',
  }
  return errorType ? map[errorType] || errorType : '纠错项'
}

function scoreLevel(score?: number | null) {
  if (score == null) return '暂无评分'
  if (score >= 90) return '表现突出'
  if (score >= 80) return '整体扎实'
  if (score >= 70) return '基础达标'
  if (score >= 60) return '仍可提升'
  return '需要重点改进'
}

export function ReviewDetailPage() {
  const { token, user } = useAuth()
  const navigate = useNavigate()
  const { reviewId } = useParams()
  const [detail, setDetail] = useState<ReviewDetail | null>(null)
  const [history, setHistory] = useState<ReviewRecord[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    const loadData = async () => {
      if (!reviewId) return
      setError('')
      try {
        const reviewDetail = await api.get<ReviewDetail>(`/review/record/${reviewId}`, token)
        setDetail(reviewDetail)
        if (reviewDetail.essayId) {
          const historyData = await api.get<ReviewRecord[]>(`/review/essay/${reviewDetail.essayId}/records`, token)
          setHistory(historyData)
        } else {
          setHistory([])
        }
      } catch (err) {
        setError((err as Error).message || '加载失败')
      }
    }

    loadData().catch(() => undefined)
  }, [reviewId, token])

  const canManualReview = hasAnyRole(user?.role, ['TEACHER', 'ADMIN'])

  if (!detail) {
    return <div className="empty-state">正在加载批改详情...</div>
  }

  const summaryComment = detail.comments?.find((comment) => comment.commentType === 1)
  const highlightsComment = detail.comments?.find((comment) => comment.commentType === 4)
  const suggestionComment = detail.comments?.find((comment) => comment.commentType === 2)
  const revisionComment = detail.comments?.find((comment) => comment.commentType === 3)
  const otherComments = detail.comments?.filter((comment) => ![1, 2, 3, 4].includes(comment.commentType ?? -1)) || []
  const totalScoreText = detail.totalScore != null ? detail.totalScore.toFixed(1) : '-'
  const highlightItems = highlightsComment ? splitCommentItems(highlightsComment.content) : []
  const suggestionItems = suggestionComment ? splitCommentItems(suggestionComment.content) : []
  const revisionRows = revisionComment ? parseRevisionRows(revisionComment.content) : []

  return (
    <div className="page-grid review-summary-page">
      <section className="panel review-summary-header">
        <div className="section-heading">
          <div>
            <p className="eyebrow">批改详情</p>
            <h3>{detail.essayTitle || '未命名作文'}</h3>
            <p className="review-summary-meta-text">
              第 {detail.reviewVersion || 1} 次批改 · {reviewStatusText(detail.status)} · {detail.ruleName || '未记录评分细则'}
            </p>
          </div>
          <div className="review-summary-actions">
            <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${detail.reviewId}`)}>
              批注阅读
            </button>
            <button type="button" className="secondary-button" onClick={() => navigate(`/essays/${detail.essayId}`)}>
              查看作文原文
            </button>
            {canManualReview ? (
              <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${detail.reviewId}/manual`)}>
                教师手动批改
              </button>
            ) : null}
            <button type="button" className="primary-button" onClick={() => navigate(`/reviews/${detail.reviewId}/rerun`)}>
              更换细则再次批改
            </button>
          </div>
        </div>

        <div className="review-summary-topcards">
          <article className="mini-card review-summary-topcard review-summary-topcard-score">
            <span>综合得分</span>
            <strong>{totalScoreText}</strong>
            <p>{scoreLevel(detail.totalScore)}</p>
          </article>
          <article className="mini-card review-summary-topcard">
            <span>评分细则</span>
            <strong>{detail.ruleName || '-'}</strong>
            <p>{detail.gradeLevel || '未设置学段'} / {detail.reviewType || '通用作文'}</p>
          </article>
          <article className="mini-card review-summary-topcard">
            <span>批改时间</span>
            <strong>{formatDateTime(detail.startTime)}</strong>
            <p>结束于 {formatDateTime(detail.endTime)}</p>
          </article>
          <article className="mini-card review-summary-topcard">
            <span>批改方式</span>
            <strong>{detail.reviewerType === 0 ? 'AI 自动批改' : '教师批改'}</strong>
            <p>模型版本：{detail.modelVersion || '-'}</p>
          </article>
        </div>

        {detail.topicRequirement || detail.customRequirement || detail.beautifyLevel ? (
          <div className="review-summary-rulepanel">
            <div className="review-summary-ruleline">
              <span>题干要求</span>
              <p>{detail.topicRequirement || '未单独设置题干要求'}</p>
            </div>
            <div className="review-summary-ruleline">
              <span>自定义要求</span>
              <p>{detail.customRequirement || '未设置自定义批改要求'}</p>
            </div>
            <div className="review-summary-ruleline">
              <span>润色等级</span>
              <p>{detail.beautifyLevel || '未设置'}</p>
            </div>
          </div>
        ) : null}
      </section>

      <section className="review-summary-main">
        <article className="panel review-summary-comments">
          <div className="review-section-head">
            <div>
              <p className="eyebrow">AI 评语</p>
              <h3>阅读反馈</h3>
            </div>
          </div>

          {summaryComment ? (
            <div className="review-summary-block">
              <div className="review-summary-block-title">总评</div>
              <p>{formatCommentContent(summaryComment.commentType, summaryComment.content)}</p>
            </div>
          ) : (
            <div className="empty-state">暂无总评内容</div>
          )}

          {highlightsComment ? (
            <div className="review-summary-block review-highlights-block">
              <div className="review-summary-block-title" style={{ color: '#10b981' }}>✨ {commentTitle(highlightsComment.commentType)}</div>
              <div className="review-table-wrap">
                <table className="review-content-table review-highlights-table">
                  <thead>
                    <tr>
                      <th className="review-table-index">序号</th>
                      <th>优秀之处</th>
                    </tr>
                  </thead>
                  <tbody>
                    {highlightItems.map((item, index) => (
                      <tr key={`highlight-${index}`} style={{ backgroundColor: '#f0fdf4' }}>
                        <td>{index + 1}</td>
                        <td style={{ color: '#065f46' }}>{item}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : null}

          <div className="review-comment-stack">
            {suggestionComment ? (
              <section className="review-summary-block">
                <div className="review-summary-block-title">{commentTitle(suggestionComment.commentType)}</div>
                <div className="review-table-wrap">
                  <table className="review-content-table">
                    <thead>
                      <tr>
                        <th className="review-table-index">序号</th>
                        <th>建议内容</th>
                      </tr>
                    </thead>
                    <tbody>
                      {suggestionItems.map((item, index) => (
                        <tr key={`suggestion-${index}`}>
                          <td>{index + 1}</td>
                          <td>{item}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            ) : null}

            {revisionComment ? (
              <section className="review-summary-block">
                <div className="review-summary-block-title">{commentTitle(revisionComment.commentType)}</div>
                <div className="review-table-wrap">
                  <table className="review-content-table">
                    <thead>
                      <tr>
                        <th className="review-table-index">序号</th>
                        <th>原句</th>
                        <th>修改后</th>
                        <th>说明</th>
                      </tr>
                    </thead>
                    <tbody>
                      {revisionRows.map((row, index) => (
                        <tr key={`revision-${index}`}>
                          <td>{index + 1}</td>
                          <td>{row.original}</td>
                          <td>{row.revised}</td>
                          <td>{row.note}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            ) : null}
          </div>

          {otherComments.length ? (
            <div className="stack-list">
              {otherComments.map((comment) => (
                <div key={comment.commentId} className="note-card review-extra-note">
                  <strong>{commentTitle(comment.commentType)}</strong>
                  <p>{formatCommentContent(comment.commentType, comment.content)}</p>
                </div>
              ))}
            </div>
          ) : null}
        </article>

        <aside className="review-summary-side">
          <article className="panel">
            <div className="review-section-head">
              <div>
                <p className="eyebrow">各维度得分</p>
                <h3>评分明细</h3>
              </div>
            </div>

            <div className="review-score-list">
              {detail.scores?.length ? (
                detail.scores.map((score) => {
                  const value = score.score ?? 0
                  const safePercent = Math.max(0, Math.min(100, value))
                  return (
                    <div key={score.dimensionId} className="review-score-row">
                      <div className="review-score-row-top">
                        <strong>{score.dimensionName}</strong>
                        <span>{score.score != null ? score.score.toFixed(2) : '-'}</span>
                      </div>
                      <div className="review-score-track">
                        <div className="review-score-fill" style={{ width: `${safePercent}%` }} />
                      </div>
                    </div>
                  )
                })
              ) : (
                <div className="empty-state">暂无维度得分</div>
              )}
            </div>
          </article>

          <article className="panel">
            <div className="review-section-head">
              <div>
                <p className="eyebrow">历史版本</p>
                <h3>批改记录</h3>
              </div>
            </div>

            <div className="review-history-list">
              {history.length ? (
                history.map((item) => {
                  const active = String(item.reviewId) === String(detail.reviewId)
                  return (
                    <button
                      type="button"
                      key={item.reviewId}
                      className={`review-history-card plain-button ${active ? 'is-active' : ''}`}
                      onClick={() => navigate(`/reviews/${item.reviewId}/summary`)}
                    >
                      <div className="review-history-card-head">
                        <strong>第 {item.reviewVersion || 1} 次</strong>
                        <span className="pill">{item.latestVersion ? '当前最新' : active ? '当前查看' : '查看详情'}</span>
                      </div>
                      <p>{item.ruleName || '未记录细则'}</p>
                      <small>
                        {reviewStatusText(item.status)} · {formatShortDateTime(item.startTime)} ·{' '}
                        {item.totalScore != null ? `${item.totalScore.toFixed(1)} 分` : '待评分'}
                      </small>
                    </button>
                  )
                })
              ) : (
                <div className="empty-state">暂无历史版本</div>
              )}
            </div>
          </article>

          <article className="panel review-correction-panel">
            <div className="review-section-head">
              <div>
                <p className="eyebrow">文本纠错</p>
                <h3>错字与表达修改</h3>
              </div>
            </div>

            <div className="review-correction-table-wrap">
              {detail.textCorrections?.length ? (
                <table className="review-correction-table">
                  <thead>
                    <tr>
                      <th>类型</th>
                      <th>位置</th>
                      <th>原文</th>
                      <th>建议修改</th>
                      <th>说明</th>
                    </tr>
                  </thead>
                  <tbody>
                    {detail.textCorrections.map((item, index) => (
                      <tr key={`${item.startOffset}-${index}`}>
                        <td>
                          <span className="annotation-tag annotation-tag-correction">{correctionTypeLabel(item.errorType)}</span>
                        </td>
                        <td>
                          {item.startOffset ?? '-'} - {item.endOffset ?? '-'}
                        </td>
                        <td className="review-correction-cell review-correction-cell-old">{item.originalText || '-'}</td>
                        <td className="review-correction-cell review-correction-cell-new">{item.correctedText || '-'}</td>
                        <td className="review-correction-cell">{item.suggestion || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <div className="empty-state">暂无纠错记录</div>
              )}
            </div>
          </article>
        </aside>
      </section>

      {error ? <div className="feedback error">{error}</div> : null}
    </div>
  )
}

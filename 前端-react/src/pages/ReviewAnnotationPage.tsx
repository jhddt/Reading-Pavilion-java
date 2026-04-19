import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { hasAnyRole } from '../auth/roles'
import { api } from '../lib/api'
import { formatDateTime } from '../lib/format'
import type { Essay, ReviewComment, ReviewDetail, TextCorrection } from '../types'

type Annotation =
  | {
      id: string
      type: 'correction'
      start: number
      end: number
      title: string
      content: string
      originalText?: string
      correctedText?: string
    }
  | {
      id: string
      type: 'suggestion' | 'revision'
      start: number
      end: number
      title: string
      content: string
      originalText?: string
    }

const CHARS_PER_LINE = 20

function commentTypeToAnnotationType(commentType?: number) {
  if (commentType === 2) return 'suggestion'
  return 'revision'
}

function normalizeLineBreaks(text: string) {
  return text.replace(/\r\n?/g, '\n')
}

function splitEssayParagraphs(text: string) {
  const normalized = normalizeLineBreaks(text).trim()
  if (!normalized) return []

  const paragraphs: Array<Array<{ char: string; index: number }>> = []
  let current: Array<{ char: string; index: number }> = []

  for (let index = 0; index < normalized.length; index += 1) {
    const char = normalized[index]

    if (char === '\n') {
      if (current.length) {
        paragraphs.push(current)
        current = []
      }
      continue
    }

    current.push({ char, index })
  }

  if (current.length) {
    paragraphs.push(current)
  }

  return paragraphs
}

function buildLooseSearchMap(text: string) {
  const chars: string[] = []
  const indices: number[] = []

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]
    if (!/\s/.test(char)) {
      chars.push(char)
      indices.push(index)
    }
  }

  return { text: chars.join(''), indices }
}

function resolveAnnotationRange(content: string, start: number, end: number, sourceText?: string) {
  const safeStart = Math.max(0, Math.min(start, content.length))
  const safeEnd = Math.max(safeStart, Math.min(end, content.length))
  const snippet = sourceText?.trim()

  if (!snippet) {
    return { start: safeStart, end: safeEnd }
  }

  const directCandidates = [
    { start: safeStart, end: safeEnd },
    { start: Math.max(0, safeStart - 1), end: Math.max(0, safeEnd - 1) },
    { start: safeStart, end: Math.min(content.length, safeEnd + 1) },
  ]

  for (const candidate of directCandidates) {
    if (content.slice(candidate.start, candidate.end) === snippet) {
      return candidate
    }
  }

  const windowStart = Math.max(0, safeStart - 160)
  const windowEnd = Math.min(content.length, safeEnd + 160 + snippet.length)
  const windowText = content.slice(windowStart, windowEnd)

  const exactIndex = windowText.indexOf(snippet)
  if (exactIndex >= 0) {
    return {
      start: windowStart + exactIndex,
      end: windowStart + exactIndex + snippet.length,
    }
  }

  const looseSnippet = snippet.replace(/\s+/g, '')
  if (!looseSnippet) {
    return { start: safeStart, end: safeEnd }
  }

  const looseWindow = buildLooseSearchMap(windowText)
  const looseIndex = looseWindow.text.indexOf(looseSnippet)

  if (looseIndex >= 0) {
    const resolvedStart = windowStart + looseWindow.indices[looseIndex]
    const resolvedEndIndex = looseWindow.indices[looseIndex + looseSnippet.length - 1]
    return {
      start: resolvedStart,
      end: windowStart + resolvedEndIndex + 1,
    }
  }

  return { start: safeStart, end: safeEnd }
}

function buildAnnotations(detail: ReviewDetail, content: string) {
  const annotations: Annotation[] = []

  ;(detail.textCorrections || []).forEach((item: TextCorrection, index) => {
    if (item.startOffset == null || item.endOffset == null) return
    const range = resolveAnnotationRange(content, item.startOffset, item.endOffset, item.originalText)
    annotations.push({
      id: `c-${index}`,
      type: 'correction',
      start: range.start,
      end: range.end,
      title: item.errorType || '纠错',
      content: item.suggestion || '文本纠错',
      originalText: item.originalText,
      correctedText: item.correctedText,
    })
  })

  ;(detail.comments || []).forEach((item: ReviewComment, index) => {
    if (item.startOffset == null || item.endOffset == null) return
    const range = resolveAnnotationRange(content, item.startOffset, item.endOffset, item.relatedText)
    annotations.push({
      id: `m-${index}`,
      type: commentTypeToAnnotationType(item.commentType),
      start: range.start,
      end: range.end,
      title: item.commentType === 2 ? '改进建议' : '修改意见',
      content: item.content,
      originalText: item.relatedText,
    })
  })

  return annotations.sort((a, b) => a.start - b.start)
}

export function ReviewAnnotationPage() {
  const { token, user } = useAuth()
  const navigate = useNavigate()
  const canManualReview = hasAnyRole(user?.role, ['TEACHER', 'ADMIN'])

  const { reviewId } = useParams()
  const [detail, setDetail] = useState<ReviewDetail | null>(null)
  const [essay, setEssay] = useState<Essay | null>(null)
  const [activeId, setActiveId] = useState<string | null>(null)
  const [floatingGroups, setFloatingGroups] = useState<
    Array<{ id: string; anchorTop: number; items: Annotation[] }>
  >([])
  const [floatingTopMap, setFloatingTopMap] = useState<Record<string, number>>({})
  const [floatingMinHeight, setFloatingMinHeight] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const paperRef = useRef<HTMLDivElement | null>(null)
  const inlineRefs = useRef<Record<string, HTMLSpanElement | null>>({})
  const groupRefs = useRef<Record<string, HTMLDivElement | null>>({})

  useEffect(() => {
    const loadData = async () => {
      if (!reviewId) return
      setLoading(true)
      setError('')
      try {
        const reviewDetail = await api.get<ReviewDetail>(`/review/record/${reviewId}`, token)
        setDetail(reviewDetail)
        const essayDetail = await api.get<Essay>(`/essay/${reviewDetail.essayId}`, token)
        setEssay(essayDetail)
      } catch (err) {
        setError((err as Error).message || '加载失败')
      } finally {
        setLoading(false)
      }
    }

    loadData().catch(() => undefined)
  }, [reviewId, token])

  const content = useMemo(() => normalizeLineBreaks(essay?.finalContent || essay?.originalContent || ''), [essay])
  const annotations = useMemo(() => (detail ? buildAnnotations(detail, content) : []), [content, detail])
  const paragraphs = useMemo(() => {
    const ranges = annotations.map((annotation) => ({
      annotation,
      start: Math.max(0, Math.min(annotation.start, content.length)),
      end: Math.max(0, Math.min(annotation.end, content.length)),
    }))

    const lines = splitEssayParagraphs(content)
    const result: Array<{
      text: string
      isTitle: boolean
      rows: Array<Array<{ char: string; index: number; annotation: Annotation | null }>>
    }> = []

    lines.forEach((line, lineIndex) => {
      const text = line.map((item) => item.char).join('')
      const trimmed = text.trim()
      const chars = line.map(({ char, index }) => {
        const matched = ranges.find((item) => index >= item.start && index < item.end)
        return {
          char,
          index,
          annotation: matched?.annotation ?? null,
        }
      })

      const rows: Array<Array<{ char: string; index: number; annotation: Annotation | null }>> = []
      for (let index = 0; index < chars.length; index += CHARS_PER_LINE) {
        rows.push(chars.slice(index, index + CHARS_PER_LINE))
      }

      result.push({
        text,
        isTitle: lineIndex === 0 && trimmed.length > 0 && trimmed.length <= 20,
        rows,
      })
    })

    return result
  }, [annotations, content])
  const annotationNumberMap = useMemo(
    () =>
      Object.fromEntries(annotations.map((annotation, index) => [annotation.id, index + 1])),
    [annotations],
  )
  const summaryComment = useMemo(
    () => detail?.comments?.find((comment) => comment.commentType === 1)?.content?.replace(/^【总评】\s*/u, '').trim() || '',
    [detail],
  )

  useEffect(() => {
    setActiveId((current) => (current && annotations.some((annotation) => annotation.id === current) ? current : null))
  }, [annotations])

  useEffect(() => {
    const rebuildFloatingGroups = () => {
      const paper = paperRef.current
      if (!paper || !annotations.length) {
        setFloatingGroups([])
        setFloatingTopMap({})
        setFloatingMinHeight(0)
        return
      }

      const paperRect = paper.getBoundingClientRect()
      const measured = annotations
        .map((annotation) => {
          const target = inlineRefs.current[annotation.id]
          if (!target) return null
          const rect = target.getBoundingClientRect()
          return {
            annotation,
            top: Math.max(0, rect.top - paperRect.top - 10),
          }
        })
        .filter(Boolean) as Array<{ annotation: Annotation; top: number }>

      const nextGroups: Array<{ id: string; anchorTop: number; items: Annotation[] }> = []
      const mergeThreshold = 58

      measured.forEach(({ annotation, top }) => {
        const prev = nextGroups[nextGroups.length - 1]
        if (prev && Math.abs(top - prev.anchorTop) <= mergeThreshold) {
          prev.items.push(annotation)
          prev.anchorTop = Math.min(prev.anchorTop, top)
          return
        }
        nextGroups.push({
          id: `group-${annotation.id}`,
          anchorTop: top,
          items: [annotation],
        })
      })

      setFloatingGroups(nextGroups)
      setFloatingMinHeight(paper.scrollHeight)
    }

    const raf = requestAnimationFrame(rebuildFloatingGroups)
    window.addEventListener('resize', rebuildFloatingGroups)
    return () => {
      cancelAnimationFrame(raf)
      window.removeEventListener('resize', rebuildFloatingGroups)
    }
  }, [annotations, content])

  useEffect(() => {
    if (!floatingGroups.length) {
      setFloatingTopMap({})
      return
    }

    const raf = requestAnimationFrame(() => {
      let cursor = 0
      const gap = 16
      const nextTopMap: Record<string, number> = {}

      floatingGroups.forEach((group) => {
        const groupHeight = groupRefs.current[group.id]?.offsetHeight || 0
        const top = Math.max(group.anchorTop, cursor)
        nextTopMap[group.id] = top
        cursor = top + groupHeight + gap
      })

      setFloatingTopMap(nextTopMap)
      setFloatingMinHeight((current) => Math.max(current, cursor))
    })

    return () => cancelAnimationFrame(raf)
  }, [floatingGroups])

  const focusAnnotation = (id: string) => {
    setActiveId(id)
    const target = inlineRefs.current[id]
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' })
    }
  }

  if (loading) {
    return <div className="empty-state">正在加载批注阅读页...</div>
  }

  if (error && (!detail || !essay)) {
    return <div className="feedback error">{error}</div>
  }

  if (!detail || !essay) {
    return <div className="empty-state">没有找到可阅读的批注内容</div>
  }

  return (
    <div className="page-grid annotation-reading-shell">
      <section className="annotation-reading-header">
        <div className="section-heading annotation-reading-heading">
          <div>
            <p className="eyebrow">作文批注稿</p>
            <h3>{detail.essayTitle || essay.title || '未命名作文'}</h3>
            <p className="annotation-reading-meta">
              {formatDateTime(detail.startTime)} · 总分 {detail.totalScore != null ? detail.totalScore.toFixed(1) : '-'} · 批注 {annotations.length} 条
            </p>
          </div>
          <div className="action-row annotation-reading-actions">
            <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${detail.reviewId}/summary`)}>
              查看总览
            </button>
            {canManualReview ? (
              <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${detail.reviewId}/manual`)}>
                教师手动批改
              </button>
            ) : null}
            <button type="button" className="primary-button" onClick={() => navigate(`/reviews/${detail.reviewId}/rerun`)}>
              继续批改
            </button>
          </div>
        </div>
      </section>

      <section className="annotation-layout">
        <article className="panel annotation-report-panel">
          <div className="annotation-report-grid">
            <div className="annotation-report-paper">
              <p className="eyebrow">作文正文</p>
              <h3>{detail.essayTitle || essay.title || '未命名作文'}</h3>
              <div className="annotation-paper-wrap" ref={paperRef}>
                <div className="annotation-paper annotation-paper-report">
                  {paragraphs.length ? (
                    paragraphs.map((paragraph, paragraphIndex) => (
                      <section
                        key={`paragraph-${paragraphIndex}`}
                        className={`annotation-paragraph ${paragraph.isTitle ? 'annotation-paragraph-title' : ''}`}
                        aria-label={paragraph.isTitle ? '作文标题' : `第 ${paragraphIndex + 1} 段`}
                      >
                        {paragraph.text.trim() ? (
                          paragraph.rows.map((row, rowIndex) => (
                            <div
                              key={`row-${paragraphIndex}-${rowIndex}`}
                              className={`annotation-row ${paragraph.isTitle ? 'annotation-row-title' : ''}`}
                              style={{ gridTemplateColumns: `repeat(${CHARS_PER_LINE}, var(--essay-cell-size))` }}
                            >
                              {row.map((cell, cellIndex) => {
                                const annotation = cell.annotation
                                if (!annotation) {
                                  return (
                                    <span key={`plain-${paragraphIndex}-${rowIndex}-${cellIndex}`} className="annotation-cell annotation-plain">
                                      {cell.char}
                                    </span>
                                  )
                                }

                                const annotationNumber = annotationNumberMap[annotation.id]
                                const showBadge = cell.index === annotation.start
                                const isWave = annotation.type === 'suggestion'

                                return (
                                  <span
                                    key={`${annotation.id}-${paragraphIndex}-${rowIndex}-${cellIndex}`}
                                    className={`annotation-cell annotation-inline annotation-inline-${annotation.type} ${
                                      activeId === annotation.id ? 'annotation-inline-active' : ''
                                    }`}
                                    ref={(node) => {
                                      if (!inlineRefs.current[annotation.id]) {
                                        inlineRefs.current[annotation.id] = node
                                      }
                                    }}
                                  >
                                    {showBadge ? (
                                      <span className={`annotation-inline-badge ${isWave ? 'annotation-inline-badge-wave' : 'annotation-inline-badge-line'}`}>
                                        {annotationNumber}
                                      </span>
                                    ) : null}
                                    <button
                                      type="button"
                                      className="annotation-char-trigger"
                                      onClick={(event) => {
                                        event.stopPropagation()
                                        focusAnnotation(annotation.id)
                                      }}
                                    >
                                      <span className="annotation-char-text">{cell.char}</span>
                                      {isWave ? (
                                        <span className="annotation-wave" aria-hidden="true">
                                          <span className="annotation-wave-segment" />
                                          <span className="annotation-wave-segment" />
                                          <span className="annotation-wave-segment" />
                                        </span>
                                      ) : (
                                        <span className="annotation-line" aria-hidden="true" />
                                      )}
                                    </button>
                                  </span>
                                )
                              })}
                              {!paragraph.isTitle && row.length < CHARS_PER_LINE
                                ? Array.from({ length: CHARS_PER_LINE - row.length }).map((_, emptyIndex) => (
                                    <span key={`empty-${paragraphIndex}-${rowIndex}-${emptyIndex}`} className="annotation-cell annotation-cell-empty" />
                                  ))
                                : null}
                            </div>
                          ))
                        ) : (
                          <span className="annotation-paragraph-empty" />
                        )}
                      </section>
                    ))
                  ) : (
                    <div className="empty-state annotation-empty">作文正文为空</div>
                  )}
                </div>
              </div>

              {summaryComment ? (
                <section className="annotation-summary-block">
                  <div className="annotation-summary-title">总评</div>
                  <p>{summaryComment}</p>
                </section>
              ) : null}
            </div>

            <aside className="annotation-report-notes">
              <p className="eyebrow">批注列表</p>
              <h3>{annotations.length ? '右侧浮动批注' : '暂无批注'}</h3>
              <div className="annotation-floating-layer" style={{ minHeight: `${floatingMinHeight || 240}px` }}>
                {floatingGroups.length ? (
                  floatingGroups.map((group) => (
                    <div
                      key={group.id}
                      ref={(node) => {
                        groupRefs.current[group.id] = node
                      }}
                      className="annotation-comment-group"
                      style={{ top: `${floatingTopMap[group.id] ?? group.anchorTop}px` }}
                    >
                      {group.items.map((annotation) => (
                        <button
                          type="button"
                          key={annotation.id}
                          className={`annotation-floating-comment annotation-floating-comment-${annotation.type} plain-button ${
                            activeId === annotation.id ? 'annotation-floating-comment-active' : ''
                          }`}
                          onClick={() => focusAnnotation(annotation.id)}
                        >
                          <span
                            className={`annotation-note-index ${
                              annotation.type === 'suggestion' ? 'annotation-note-index-wave' : 'annotation-note-index-line'
                            }`}
                          >
                            {annotationNumberMap[annotation.id]}
                          </span>
                          <span>{annotation.content}</span>
                        </button>
                      ))}
                    </div>
                  ))
                ) : (
                  <div className="empty-state annotation-empty">当前作文暂无可定位批注</div>
                )}
              </div>
            </aside>
          </div>
        </article>
      </section>

      {error ? <div className="feedback error">{error}</div> : null}
    </div>
  )
}

import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { hasAnyRole } from '../auth/roles'
import { api } from '../lib/api'
import { normalizeOcrTextForDisplay } from '../lib/ocrText'
import { extractOverallSummaryLead } from '../lib/reviewSummaryText'
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
      type: 'suggestion' | 'revision' | 'highlight'
      start: number
      end: number
      title: string
      content: string
      originalText?: string
    }

const CHARS_PER_LINE = 30
/** 正文段首行缩进格数（不占原文 offset，批注仍按原文索引匹配） */
const FIRST_LINE_INDENT_CELLS = 2
const BODY_FIRST_ROW_CHARS = CHARS_PER_LINE - FIRST_LINE_INDENT_CELLS

type AnnotationGridCell = {
  char: string
  index: number
  annotation: Annotation | null
  /** 版式缩进格，无对应原文 index */
  indent?: boolean
}

function buildParagraphRows(
  chars: Array<{ char: string; index: number; annotation: Annotation | null }>,
  isTitle: boolean,
): Array<Array<AnnotationGridCell>> {
  if (!chars.length) return []

  if (isTitle) {
    const rows: Array<Array<AnnotationGridCell>> = []
    for (let i = 0; i < chars.length; i += CHARS_PER_LINE) {
      rows.push(chars.slice(i, i + CHARS_PER_LINE).map((c) => ({ ...c })))
    }
    return rows
  }

  const rows: Array<Array<AnnotationGridCell>> = []
  const indentPrefix: AnnotationGridCell[] = Array.from({ length: FIRST_LINE_INDENT_CELLS }, () => ({
    char: '',
    index: -1,
    annotation: null,
    indent: true,
  }))

  const firstChunk = chars.slice(0, BODY_FIRST_ROW_CHARS)
  rows.push([...indentPrefix, ...firstChunk.map((c) => ({ ...c }))])

  for (let offset = BODY_FIRST_ROW_CHARS; offset < chars.length; offset += CHARS_PER_LINE) {
    rows.push(chars.slice(offset, offset + CHARS_PER_LINE).map((c) => ({ ...c })))
  }

  return rows
}

type PaperTitleRowSpec = { padLeft: number; chars: string[]; padRight: number }

function buildPaperTitleRowSpecs(rawTitle: string): PaperTitleRowSpec[] {
  const chars = Array.from(rawTitle.trim() || '未命名作文')
  if (!chars.length) {
    return [{ padLeft: CHARS_PER_LINE, chars: [], padRight: 0 }]
  }
  if (chars.length <= CHARS_PER_LINE) {
    const pad = CHARS_PER_LINE - chars.length
    const padLeft = Math.floor(pad / 2)
    return [{ padLeft, chars, padRight: pad - padLeft }]
  }
  const specs: PaperTitleRowSpec[] = []
  for (let i = 0; i < chars.length; i += CHARS_PER_LINE) {
    const chunk = chars.slice(i, i + CHARS_PER_LINE)
    specs.push({
      padLeft: 0,
      chars: chunk,
      padRight: CHARS_PER_LINE - chunk.length,
    })
  }
  return specs
}

function commentTypeToAnnotationType(commentType?: number) {
  if (commentType === 2) return 'suggestion'
  if (commentType === 4) return 'highlight'
  return 'revision'
}

function normalizeLineBreaks(text: string) {
  return text.replace(/\r\n?/g, '\n')
}

function extractHighlightEntries(raw: string): Array<{ quote: string; note: string }> {
  const text = normalizeLineBreaks(raw || '').trim()
  if (!text) return []

  // 先按序号分条（避免“裁切过多”把后续条目吞进去）
  const normalized = text.replace(/\s*(\d+[\.、]\s*)/g, '\n$1').trim()
  const parts = normalized
    .split(/\n\s*(?=\d+[\.、]\s*)/g)
    .map((s) => s.trim())
    .filter(Boolean)

  const entries: Array<{ quote: string; note: string }> = []
  const seen = new Set<string>()
  const quoteSingle = /\*\*[“"]([^”"]{3,})[”"]\*\*|[“"]([^”"]{3,})[”"]/u

  for (const part0 of parts.length ? parts : [normalized]) {
    const part = part0.replace(/^\d+[\.、]\s*/g, '').trim()
    const m = part.match(quoteSingle)
    if (!m) continue
    const quote = ((m[1] || m[2]) ?? '').trim()
    if (!quote || seen.has(quote)) continue
    const idx = m.index ?? -1
    const after = idx >= 0 ? part.slice(idx + m[0].length) : ''
    const note = after.replace(/^\s*[-—:：]\s*/g, '').trim()
    seen.add(quote)
    entries.push({ quote, note })
  }

  if (entries.length) return entries

  // 兜底：只提取引号句
  const quoteRegex = /[“"]([^”"]{3,})[”"]/g
  for (const match of text.matchAll(quoteRegex)) {
    const quote = (match[1] || '').trim()
    if (!quote || seen.has(quote)) continue
    seen.add(quote)
    entries.push({ quote, note: '' })
  }
  return entries
}

function extractSuggestionEntries(raw: string): string[] {
  const text = normalizeLineBreaks(raw || '').trim()
  if (!text) return []
  const normalized = text.replace(/\s*(\d+[\.、]\s*)/g, '\n$1').trim()
  const parts = normalized
    .split(/\n\s*(?=\d+[\.、]\s*)/g)
    .map((s) => s.trim())
    .filter(Boolean)
  // 去掉开头序号
  return parts.map((s) => s.replace(/^\d+[\.、]\s*/g, '').trim()).filter(Boolean)
}

function firstQuotedSnippet(text: string) {
  const m = text.match(/[“"]([^”"]{3,})[”"]/)
  return m?.[1]?.trim() || ''
}

/** 错字格：正字写在错字上方；每个字独立渲染为 span */
function correctionAboveChars(annotation: Annotation, cellIndex: number): string[] {
  if (annotation.type !== 'correction') return []
  const raw = annotation.correctedText?.trim()
  if (!raw) return []
  const spanLen = Math.max(1, annotation.end - annotation.start)
  const seq = Array.from(raw)
  const offset = cellIndex - annotation.start
  if (offset < 0 || offset >= spanLen) return []
  if (seq.length === spanLen) {
    const ch = seq[offset]
    return ch ? [ch] : []
  }
  if (cellIndex === annotation.start) {
    return seq
  }
  return []
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

function buildLooseSearchMapForQuote(text: string) {
  const chars: string[] = []
  const indices: number[] = []
  // 去除空白与常见标点，提高“引用句”匹配成功率
  const skip = /[\s，。,\.！？!?、：“”‘’（）()【】\[\]《》<>—\-…·]/u

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]
    if (!skip.test(char)) {
      chars.push(char)
      indices.push(index)
    }
  }

  return { text: chars.join(''), indices }
}

function resolveRangeBySnippet(content: string, snippet: string) {
  const trimmed = snippet.trim()
  if (!trimmed) return { start: 0, end: 0 }

  const exactIndex = content.indexOf(trimmed)
  if (exactIndex >= 0) {
    return { start: exactIndex, end: exactIndex + trimmed.length }
  }

  const looseSnippet = trimmed.replace(/\s+/g, '')
  if (!looseSnippet) return { start: 0, end: 0 }

  // 先用“仅去空白”的方式兜底
  const looseWindow = buildLooseSearchMap(content)
  let looseIndex = looseWindow.text.indexOf(looseSnippet)
  if (looseIndex >= 0) {
    const resolvedStart = looseWindow.indices[looseIndex]
    const resolvedEndIndex = looseWindow.indices[looseIndex + looseSnippet.length - 1]
    return { start: resolvedStart, end: resolvedEndIndex + 1 }
  }

  // 再用“去空白 + 去标点”的方式匹配引用句
  const quoteSnippet = trimmed.replace(/[\s，。,\.！？!?、：“”‘’（）()【】\[\]《》<>—\-…·]+/gu, '')
  if (!quoteSnippet) return { start: 0, end: 0 }
  const quoteWindow = buildLooseSearchMapForQuote(content)
  looseIndex = quoteWindow.text.indexOf(quoteSnippet)
  if (looseIndex >= 0) {
    const resolvedStart = quoteWindow.indices[looseIndex]
    const resolvedEndIndex = quoteWindow.indices[looseIndex + quoteSnippet.length - 1]
    return { start: resolvedStart, end: resolvedEndIndex + 1 }
  }

  return { start: 0, end: 0 }
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

  // 无起止坐标时（如亮点赏析逐条回贴），直接全篇检索
  if (safeStart === safeEnd) {
    const ranged = resolveRangeBySnippet(content, snippet)
    if (ranged.end > ranged.start) return ranged
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
    // 改进建议(2)只在右侧空白位置展示，不进入正文锚点
    if (item.commentType === 2) return
    const type = commentTypeToAnnotationType(item.commentType)
    if (item.startOffset != null && item.endOffset != null) {
      const range = resolveAnnotationRange(content, item.startOffset, item.endOffset, item.relatedText)
      annotations.push({
        id: `m-${index}`,
        type,
        start: range.start,
        end: range.end,
        title: item.commentType === 2 ? '改进建议' : item.commentType === 4 ? '亮点赏析' : '修改意见',
        content: item.content,
        originalText: item.relatedText,
      })
      return
    }

    // 亮点赏析常为整段无坐标文本：按条目拆分并逐条回贴到正文句子
    if (item.commentType === 4) {
      const entries = extractHighlightEntries(item.content || '')
      entries.forEach((entry, subIndex) => {
        const range = resolveAnnotationRange(content, 0, 0, entry.quote)
        if (range.end <= range.start) return
        annotations.push({
          id: `m-${index}-h-${subIndex}`,
          type: 'highlight',
          start: range.start,
          end: range.end,
          title: '亮点赏析',
          content: entry.note || `亮点句：${entry.quote}`,
          originalText: entry.quote,
        })
      })
    }
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
  const inlineRefs = useRef<Record<string, HTMLElement | null>>({})
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

  const content = useMemo(
    () => normalizeLineBreaks(normalizeOcrTextForDisplay(essay?.finalContent || essay?.originalContent || '')),
    [essay],
  )
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
      rows: Array<Array<AnnotationGridCell>>
      start: number
      end: number
    }> = []

    lines.forEach((line, lineIndex) => {
      const text = line.map((item) => item.char).join('')
      const trimmed = text.trim()
      const start = line.length ? Math.max(0, line[0].index) : 0
      const end = line.length ? Math.max(start, line[line.length - 1].index + 1) : start
      const chars = line.map(({ char, index }) => {
        const matched = ranges.find((item) => index >= item.start && index < item.end)
        return {
          char,
          index,
          annotation: matched?.annotation ?? null,
        }
      })

      const isTitle = lineIndex === 0 && trimmed.length > 0 && trimmed.length <= 20
      const rows = buildParagraphRows(chars, isTitle)

      result.push({
        text,
        isTitle,
        rows,
        start,
        end,
      })
    })

    return result
  }, [annotations, content])

  const paragraphSuggestions = useMemo(() => {
    const suggestions = (detail?.comments || []).filter((c) => c.commentType === 2 && c.content?.trim())
    if (!suggestions.length) return []

    const rows: Array<{ id: string; paragraphIndex: number; content: string }> = []
    suggestions.forEach((comment, commentIndex) => {
      const entries = extractSuggestionEntries(comment.content || '')
      const items = entries.length ? entries : [comment.content.trim()]
      items.forEach((entry, entryIndex) => {
        let range = { start: 0, end: 0 }
        if (comment.startOffset != null && comment.endOffset != null) {
          range = resolveAnnotationRange(content, comment.startOffset, comment.endOffset, comment.relatedText || entry)
        } else {
          const quoted = firstQuotedSnippet(entry)
          const snippet = quoted || entry.slice(0, 18)
          range = snippet ? resolveAnnotationRange(content, 0, 0, snippet) : { start: 0, end: 0 }
        }
        const pIdx = paragraphs.findIndex((p) => range.end > p.start && range.start < p.end)
        rows.push({
          id: `bulk-s-${commentIndex}-${entryIndex}`,
          paragraphIndex: pIdx >= 0 ? pIdx : 0,
          content: entry,
        })
      })
    })
    return rows
  }, [content, detail?.comments, paragraphs])

  const paragraphRefs = useRef<Record<number, HTMLElement | null>>({})
  const paragraphSuggestionRefs = useRef<Record<string, HTMLDivElement | null>>({})
  const [paragraphSuggestionTopMap, setParagraphSuggestionTopMap] = useState<Record<string, number>>({})

  useEffect(() => {
    const rebuild = () => {
      const paper = paperRef.current
      if (!paper || !paragraphSuggestions.length) {
        setParagraphSuggestionTopMap({})
        return
      }
      const paperRect = paper.getBoundingClientRect()

      const desired = paragraphSuggestions
        .map((s, order) => {
          const node = paragraphRefs.current[s.paragraphIndex]
          if (!node) return null
          const rect = node.getBoundingClientRect()
          return {
            ...s,
            order,
            desiredTop: Math.max(0, rect.top - paperRect.top - 10),
          }
        })
        .filter(Boolean) as Array<{ id: string; paragraphIndex: number; content: string; desiredTop: number; order: number }>

      // 固定占位块：亮点赏析/可定位批注（这些位置不能动）
      const fixedBlocks = floatingGroups
        .map((group) => {
          const top = floatingTopMap[group.id] ?? group.anchorTop
          const height = groupRefs.current[group.id]?.offsetHeight || 0
          return { top, bottom: top + height + 10 }
        })
        .filter((b) => b.bottom > b.top)
        .sort((a, b) => a.top - b.top)

      const overlaps = (top: number, height: number, block: { top: number; bottom: number }) => {
        const bottom = top + height
        return top < block.bottom && bottom > block.top
      }

      const nextAvailableTop = (startTop: number, height: number, occupied: Array<{ top: number; bottom: number }>) => {
        let top = startTop
        // 最多尝试若干次，避免死循环
        for (let i = 0; i < 50; i += 1) {
          const hit = occupied.find((b) => overlaps(top, height, b))
          if (!hit) return top
          top = hit.bottom
        }
        return top
      }

      const next: Record<string, number> = {}
      const occupied: Array<{ top: number; bottom: number }> = [...fixedBlocks]

      // 按序号从上到下放置；若目标位置在更下方，则从目标位置开始找空位
      desired
        .sort((a, b) => a.order - b.order)
        .forEach((s) => {
          const h = paragraphSuggestionRefs.current[s.id]?.offsetHeight || 0
          const height = Math.max(24, h) + 8
          // 不改变亮点赏析的前提下：从段落目标位置开始，向下找最近空白
          const top = nextAvailableTop(s.desiredTop, height, occupied)
          next[s.id] = top
          occupied.push({ top, bottom: top + height })
          occupied.sort((a, b) => a.top - b.top)
        })

      setParagraphSuggestionTopMap(next)
    }

    const raf = requestAnimationFrame(rebuild)
    window.addEventListener('resize', rebuild)
    return () => {
      cancelAnimationFrame(raf)
      window.removeEventListener('resize', rebuild)
    }
  }, [floatingGroups, floatingTopMap, paragraphSuggestions])
  const annotationNumberMap = useMemo(
    () =>
      Object.fromEntries(annotations.map((annotation, index) => [annotation.id, index + 1])),
    [annotations],
  )
  /** 右侧浮动区仅展示改进建议、亮点赏析（错字已在正文标出） */
  const sidebarFloatingAnnotations = useMemo(
    () => annotations.filter((a) => a.type === 'highlight'),
    [annotations],
  )
  const sidebarAnnotationNumberMap = useMemo(
    () =>
      Object.fromEntries(sidebarFloatingAnnotations.map((annotation, index) => [annotation.id, index + 1])),
    [sidebarFloatingAnnotations],
  )
  /** 亮点赏析已逐条回贴正文；侧栏不再展示整段长文本块 */
  const bulkSidebarComments = useMemo<ReviewComment[]>(() => {
    return []
  }, [])
  const bulkSidebarCount = bulkSidebarComments.length
  const summaryComment = useMemo(() => {
    const raw = detail?.comments?.find((comment) => comment.commentType === 1)?.content || ''
    return extractOverallSummaryLead(raw)
  }, [detail])

  const paperTitleRowSpecs = useMemo(() => {
    if (!detail || !essay) return []
    const title = (detail.essayTitle || essay.title || '未命名作文').trim() || '未命名作文'
    return buildPaperTitleRowSpecs(title)
  }, [detail, essay])

  useEffect(() => {
    setActiveId((current) => (current && annotations.some((annotation) => annotation.id === current) ? current : null))
  }, [annotations])

  useEffect(() => {
    const rebuildFloatingGroups = () => {
      const paper = paperRef.current
      if (!paper || !sidebarFloatingAnnotations.length) {
        setFloatingGroups([])
        setFloatingTopMap({})
        setFloatingMinHeight(0)
        return
      }

      const paperRect = paper.getBoundingClientRect()
      const measured = sidebarFloatingAnnotations
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
  }, [sidebarFloatingAnnotations, content])

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
    <>
      <div className="annotation-reading-toolbar action-row">
        <button type="button" className="primary-button" onClick={() => navigate(`/reviews/${detail.reviewId}/rerun`)}>
          继续批改
        </button>
        <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${detail.reviewId}/summary`)}>
          查看总览
        </button>
        {canManualReview ? (
          <button type="button" className="secondary-button" onClick={() => navigate(`/reviews/${detail.reviewId}/manual`)}>
            教师手动批改
          </button>
        ) : null}
      </div>
      <div className="page-grid annotation-reading-shell">
      <section className="annotation-layout">
        <article className="panel annotation-report-panel">
          <div className="annotation-report-grid">
            <div className="annotation-report-paper">
              <div className="annotation-paper-wrap" ref={paperRef}>
                <div className="annotation-paper annotation-paper-report">
                  {paperTitleRowSpecs.map((spec, rowIdx) => (
                    <div
                      key={`paper-title-row-${rowIdx}`}
                      className="annotation-row annotation-row-paper-title"
                      style={{ gridTemplateColumns: `repeat(${CHARS_PER_LINE}, var(--essay-cell-size))` }}
                    >
                      {Array.from({ length: spec.padLeft }, (_, i) => (
                        <span
                          key={`paper-title-pl-${rowIdx}-${i}`}
                          className="annotation-cell annotation-cell-empty"
                          aria-hidden="true"
                        />
                      ))}
                      {spec.chars.map((ch, i) => (
                        <span key={`paper-title-ch-${rowIdx}-${i}`} className="annotation-cell annotation-plain annotation-cell-paper-title">
                          {ch}
                        </span>
                      ))}
                      {Array.from({ length: spec.padRight }, (_, i) => (
                        <span
                          key={`paper-title-pr-${rowIdx}-${i}`}
                          className="annotation-cell annotation-cell-empty"
                          aria-hidden="true"
                        />
                      ))}
                    </div>
                  ))}
                  {paragraphs.length ? (
                    paragraphs.map((paragraph, paragraphIndex) => (
                      <section
                        key={`paragraph-${paragraphIndex}`}
                        className={`annotation-paragraph ${paragraph.isTitle ? 'annotation-paragraph-title' : ''}`}
                        aria-label={paragraph.isTitle ? '作文标题' : `第 ${paragraphIndex + 1} 段`}
                        ref={(node) => {
                          paragraphRefs.current[paragraphIndex] = node
                        }}
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
                                if (cell.indent) {
                                  return (
                                    <span
                                      key={`indent-${paragraphIndex}-${rowIndex}-${cellIndex}`}
                                      className="annotation-cell annotation-cell-indent"
                                      aria-hidden="true"
                                    />
                                  )
                                }
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
                                const isCorrection = annotation.type === 'correction'

                                if (isCorrection) {
                                  const aboveChars = correctionAboveChars(annotation, cell.index)
                                  return (
                                    <div
                                      key={`${annotation.id}-${paragraphIndex}-${rowIndex}-${cellIndex}`}
                                      className={`annotation-cell annotation-inline annotation-inline-correction ${
                                        activeId === annotation.id ? 'annotation-inline-active' : ''
                                      }`}
                                      ref={(node) => {
                                        if (!inlineRefs.current[annotation.id]) {
                                          inlineRefs.current[annotation.id] = node
                                        }
                                      }}
                                    >
                                      {aboveChars.length ? (
                                        <div
                                          className={`annotation-correction-above-track ${
                                            aboveChars.length === 1 ? 'annotation-correction-above-track-single' : ''
                                          }`}
                                        >
                                          {aboveChars.map((ch, idx) => (
                                            <span
                                              key={`${annotation.id}-fix-${cell.index}-${idx}`}
                                              className="annotation-correction-above"
                                              data-correction-char="true"
                                            >
                                              {ch}
                                            </span>
                                          ))}
                                        </div>
                                      ) : null}
                                      <button
                                        type="button"
                                        className="annotation-char-trigger annotation-char-trigger-correction"
                                        onClick={(event) => {
                                          event.stopPropagation()
                                          focusAnnotation(annotation.id)
                                        }}
                                      >
                                        <span className="annotation-char-text annotation-char-wrong">{cell.char}</span>
                                      </button>
                                    </div>
                                  )
                                }

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
                                    {showBadge && isWave ? (
                                      <span className="annotation-inline-badge annotation-inline-badge-wave">
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
                              {row.length < CHARS_PER_LINE
                                ? Array.from({ length: CHARS_PER_LINE - row.length }).map((_, emptyIndex) => (
                                    <span
                                      key={`empty-${paragraphIndex}-${rowIndex}-${emptyIndex}`}
                                      className="annotation-cell annotation-cell-empty"
                                      aria-hidden="true"
                                    />
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
                <section className="annotation-summary-block" style={{ fontSize: '12px' }}>
                  <div className="annotation-summary-title">总评</div>
                  <p>{summaryComment}</p>
                </section>
              ) : null}
            </div>

            <aside className="annotation-report-notes">
              <p className="eyebrow">批注列表</p>
              {paragraphSuggestions.length || floatingGroups.length ? (
                <div className="annotation-floating-layer" style={{ minHeight: `${floatingMinHeight || 240}px` }}>
                  {paragraphSuggestions.map((s, idx) => (
                    <div
                      key={s.id}
                      className="annotation-paragraph-pin annotation-floating-comment annotation-floating-comment-suggestion"
                      style={{ top: `${paragraphSuggestionTopMap[s.id] ?? 0}px` }}
                      ref={(node) => {
                        paragraphSuggestionRefs.current[s.id] = node
                      }}
                    >
                      <span className="annotation-note-index annotation-note-index-suggestion">{idx + 1}</span>
                      <span className="annotation-paragraph-pin-text">{s.content}</span>
                    </div>
                  ))}
                  {floatingGroups.map((group) => (
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
                          <span className="annotation-note-index annotation-note-index-highlight">
                            {sidebarAnnotationNumberMap[annotation.id] ?? 0}
                          </span>
                          <span>{annotation.content}</span>
                        </button>
                      ))}
                    </div>
                  ))}
                </div>
              ) : null}
              {bulkSidebarCount ? (
                <div className="annotation-sidebar-bulk">
                  {bulkSidebarComments.map((c, i) => (
                    <div
                      key={c.commentId != null ? `bulk-${c.commentId}` : `bulk-${c.commentType}-${i}`}
                      className={`annotation-floating-comment annotation-floating-comment-${
                        c.commentType === 2 ? 'suggestion' : 'highlight'
                      } annotation-sidebar-bulk-item`}
                    >
                      <span
                        className={`annotation-note-index ${
                          c.commentType === 2 ? 'annotation-note-index-suggestion' : 'annotation-note-index-highlight'
                        }`}
                      >
                        {i + 1 + sidebarFloatingAnnotations.length}
                      </span>
                      <span className="annotation-sidebar-bulk-text">{c.content}</span>
                    </div>
                  ))}
                </div>
              ) : null}
              {!bulkSidebarCount && !floatingGroups.length && !paragraphSuggestions.length ? (
                <div className="empty-state annotation-empty">暂无亮点赏析与改进建议</div>
              ) : null}
            </aside>
          </div>
        </article>
      </section>

      {error ? <div className="feedback error">{error}</div> : null}
    </div>
    </>
  )
}

import { useState } from 'react'

type ManualAnnotation = {
  id: string
  type: 'suggestion' | 'revision' | 'correction'
  title: string
  selectedText: string
  startOffset: number
  endOffset: number
  content: string
  correctedText?: string
}

type ManualReviewWorkspaceProps = {
  contentText: string
  summary: string
  annotations: ManualAnnotation[]
  loading: boolean
  submitLabel?: string
  onSummaryChange: (value: string) => void
  onAnnotationsChange: (annotations: ManualAnnotation[]) => void
  onSubmit: (event: React.FormEvent) => void
}

export function ManualReviewWorkspace({
  contentText,
  summary,
  annotations,
  loading,
  submitLabel = '提交手动批改',
  onSummaryChange,
  onAnnotationsChange,
  onSubmit,
}: ManualReviewWorkspaceProps) {
  const [selectionText, setSelectionText] = useState('')
  const [selectionRange, setSelectionRange] = useState<{ start: number; end: number } | null>(null)
  const [annotationContent, setAnnotationContent] = useState('')
  const [activeAnnotationId, setActiveAnnotationId] = useState<string | null>(null)
  const [localError, setLocalError] = useState('')
  const [bubblePosition, setBubblePosition] = useState<{ top: number; left: number } | null>(null)
  const [dragRange, setDragRange] = useState<{ start: number; end: number } | null>(null)

  const activeAnnotation = annotations.find((item) => item.id === activeAnnotationId) || null
  const activeRange = dragRange
    ? {
        start: Math.min(dragRange.start, dragRange.end),
        end: Math.max(dragRange.start, dragRange.end),
      }
    : null

  const handlePaperPointerDown = (index: number) => {
    setDragRange({ start: index, end: index })
    setSelectionText('')
    setSelectionRange(null)
    setBubblePosition(null)
  }

  const handlePaperPointerEnter = (index: number) => {
    setDragRange((current) => (current ? { ...current, end: index } : current))
  }

  const handlePaperPointerUp = (index: number, container: HTMLDivElement) => {
    const nextRange = dragRange ? { start: dragRange.start, end: index } : null
    if (!nextRange) {
      return
    }

    const start = Math.min(nextRange.start, nextRange.end)
    const end = Math.max(nextRange.start, nextRange.end) + 1
    const selected = contentText.slice(start, end).trim()

    setDragRange(null)
    if (!selected) {
      return
    }

    const targetNode = container.querySelector<HTMLElement>(`[data-char-index="${index}"]`)
    const containerRect = container.getBoundingClientRect()
    const targetRect = targetNode?.getBoundingClientRect()

    setSelectionText(selected)
    setSelectionRange({ start, end })
    setBubblePosition({
      left: targetRect ? Math.min(containerRect.width - 120, Math.max(70, targetRect.right - containerRect.left + 14)) : 120,
      top: targetRect ? Math.max(18, targetRect.top - containerRect.top + 10) : 24,
    })
    setLocalError('')
  }

  const addAnnotation = () => {
    if (!selectionText || !selectionRange || !annotationContent.trim()) {
      setLocalError('请先在正文中选择文本，并填写批注内容。')
      return
    }
    const next: ManualAnnotation = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      type: 'revision',
      title: '修改意见',
      selectedText: selectionText,
      startOffset: selectionRange.start,
      endOffset: selectionRange.end,
      content: annotationContent.trim(),
    }
    onAnnotationsChange([...annotations, next])
    setActiveAnnotationId(next.id)
    setAnnotationContent('')
    setSelectionText('')
    setSelectionRange(null)
    setBubblePosition(null)
    setLocalError('')
  }

  const removeAnnotation = (id: string) => {
    onAnnotationsChange(annotations.filter((item) => item.id !== id))
    if (activeAnnotationId === id) {
      setActiveAnnotationId(null)
    }
  }

  const closeBubble = () => {
    setSelectionText('')
    setSelectionRange(null)
    setAnnotationContent('')
    setBubblePosition(null)
  }

  return (
    <form className="form-grid" onSubmit={onSubmit}>
      <div className="manual-review-layout">
        <div className="manual-review-paper panel">
          <div className="manual-review-pane-header">
            <div>
              <strong>正文批注区</strong>
              <p>先在正文中选中文字，再像 WPS 一样把批注写到右侧卡片里。</p>
            </div>
            <span className="pill">{annotations.length} 条批注</span>
          </div>

          <div className="field manual-selector-wrap">
            <span>正文</span>
            <div
              className="manual-review-selector manual-review-selector-paper"
              onMouseLeave={() => setDragRange(null)}
            >
              {Array.from(contentText || '').map((char, index) => {
                const matchedAnnotation =
                  annotations.find((item) => index >= item.startOffset && index < item.endOffset) || null
                const isActiveAnnotation = matchedAnnotation?.id === activeAnnotationId
                const isSelecting =
                  activeRange != null && index >= activeRange.start && index <= activeRange.end

                return (
                  <span
                    key={`${index}-${char}`}
                    data-char-index={index}
                    className={[
                      'manual-paper-char',
                      matchedAnnotation ? 'manual-paper-char-annotated' : '',
                      isActiveAnnotation ? 'manual-paper-char-active' : '',
                      isSelecting ? 'manual-paper-char-selecting' : '',
                    ]
                      .filter(Boolean)
                      .join(' ')}
                    onMouseDown={() => handlePaperPointerDown(index)}
                    onMouseEnter={() => handlePaperPointerEnter(index)}
                    onMouseUp={(event) => handlePaperPointerUp(index, event.currentTarget.parentElement as HTMLDivElement)}
                    onClick={() => {
                      if (matchedAnnotation) {
                        setActiveAnnotationId(matchedAnnotation.id)
                      }
                    }}
                  >
                    {char === '\n' ? '\n' : char}
                  </span>
                )
              })}
            </div>
            {selectionText && selectionRange && bubblePosition ? (
              <div
                className="manual-selection-bubble"
                style={{
                  top: bubblePosition.top,
                  left: bubblePosition.left,
                }}
              >
                <div className="manual-selection-bubble-title">添加批注</div>
                <div className="manual-selection-bubble-quote">{selectionText}</div>
                <textarea
                  rows={3}
                  value={annotationContent}
                  onChange={(event) => setAnnotationContent(event.target.value)}
                  placeholder="输入批改内容"
                />
                <div className="manual-selection-bubble-actions">
                  <button type="button" className="secondary-button" onClick={closeBubble}>
                    取消
                  </button>
                  <button type="button" className="primary-button" onClick={addAnnotation}>
                    添加
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </div>

        <aside className="manual-review-comments panel">
          <div className="manual-review-pane-header">
            <div>
              <strong>侧边批注</strong>
              <p>选中文字后直接弹出气泡输入批注，右侧只保留整理后的批注列表。</p>
            </div>
          </div>

          <div className="manual-comment-list">
            {annotations.length ? (
              annotations.map((item, index) => (
                <article
                  key={item.id}
                  className={`manual-comment-card${activeAnnotationId === item.id ? ' manual-comment-card-active' : ''}`}
                  onClick={() => setActiveAnnotationId(item.id)}
                >
                  <div className="manual-comment-index">#{index + 1}</div>
                  <div className="manual-comment-body">
                    <strong>{item.title}</strong>
                    <p>{item.selectedText}</p>
                    {item.correctedText ? (
                      <p>
                        <span className="old-text">{item.selectedText}</span> → <span className="new-text">{item.correctedText}</span>
                      </p>
                    ) : null}
                    <p>{item.content}</p>
                    <span>
                      位置 {item.startOffset}-{item.endOffset}
                    </span>
                  </div>
                  <button
                    type="button"
                    className="text-button danger-text"
                    onClick={(event) => {
                      event.stopPropagation()
                      removeAnnotation(item.id)
                    }}
                  >
                    删除
                  </button>
                </article>
              ))
            ) : (
              <div className="empty-state manual-comment-empty">还没有批注，先从左侧正文中选一段文字。</div>
            )}
          </div>

          <label className="field">
            <span>总结</span>
            <textarea rows={5} value={summary} onChange={(event) => onSummaryChange(event.target.value)} />
          </label>

          {localError ? <div className="feedback error">{localError}</div> : null}

          {activeAnnotation ? (
            <div className="manual-comment-focus">
              <strong>当前聚焦批注</strong>
              <p>{activeAnnotation.content}</p>
            </div>
          ) : null}
        </aside>
      </div>

      <button type="submit" className="primary-button" disabled={loading}>
        {loading ? '提交中...' : submitLabel}
      </button>
    </form>
  )
}

export type { ManualAnnotation }

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ReviewDetail, Paragraph, TextSegment, Annotation } from '@/types/review'

export const useReviewStore = defineStore('review', () => {
  // State
  const currentReview = ref<ReviewDetail | null>(null)
  const essayContent = ref<string>('')
  const activeHighlightId = ref<number | null>(null)
  const currentIndex = ref<number>(1)
  const totalCount = ref<number>(1)

  // Getters
  const gradePercentage = computed(() => {
    if (!currentReview.value?.totalScore) return 0
    return Math.round((currentReview.value.totalScore / 100) * 100)
  })

  const wordCount = computed(() => {
    return essayContent.value.replace(/\s/g, '').length
  })

  const summaryComments = computed(() => {
    return currentReview.value?.comments.filter((c) => c.commentType === 1) || []
  })

  const suggestionComments = computed(() => {
    return currentReview.value?.comments.filter((c) => c.commentType === 2) || []
  })

  const revisionComments = computed(() => {
    return currentReview.value?.comments.filter((c) => c.commentType === 3) || []
  })

  // 生成内容分段（带标记）
  const contentSegments = computed((): TextSegment[] => {
    if (!essayContent.value || !currentReview.value) return []

    const segments: TextSegment[] = []
    const markers: Array<{
      start: number
      end: number
      type: 'correction' | 'suggestion' | 'revision'
      number: number
      text?: string
    }> = []

    // 收集纠错标记
    if (currentReview.value.textCorrections) {
      currentReview.value.textCorrections.forEach((correction, idx) => {
        if (correction.startOffset != null && correction.endOffset != null) {
          markers.push({
            start: correction.startOffset,
            end: correction.endOffset,
            type: 'correction',
            number: idx + 1,
            text: correction.originalText,
          })
        }
      })
    }

    // 收集建议标记
    suggestionComments.value.forEach((comment, idx) => {
      if (comment.startOffset != null && comment.endOffset != null) {
        markers.push({
          start: comment.startOffset,
          end: comment.endOffset,
          type: 'suggestion',
          number: (currentReview.value?.textCorrections?.length || 0) + idx + 1,
          text: comment.relatedText,
        })
      }
    })

    // 收集修改意见标记
    revisionComments.value.forEach((comment, idx) => {
      if (comment.startOffset != null && comment.endOffset != null) {
        markers.push({
          start: comment.startOffset,
          end: comment.endOffset,
          type: 'revision',
          number:
            (currentReview.value?.textCorrections?.length || 0) +
            suggestionComments.value.length +
            idx +
            1,
          text: comment.relatedText,
        })
      }
    })

    // 按位置排序
    markers.sort((a, b) => a.start - b.start)

    // 分段
    let lastPos = 0
    markers.forEach((marker) => {
      // 添加标记前的普通文本
      if (marker.start > lastPos) {
        segments.push({
          text: essayContent.value.substring(lastPos, marker.start),
          marked: false,
        })
      }

      // 添加标记文本
      segments.push({
        text: marker.text || essayContent.value.substring(marker.start, marker.end),
        marked: true,
        type: marker.type,
        number: marker.number,
      })

      lastPos = marker.end
    })

    // 添加最后的普通文本
    if (lastPos < essayContent.value.length) {
      segments.push({
        text: essayContent.value.substring(lastPos),
        marked: false,
      })
    }

    return segments
  })

  // 格式化段落
  const formattedParagraphs = computed((): Paragraph[] => {
    const segments = contentSegments.value
    if (!segments.length) return []

    const paragraphs: Paragraph[] = []
    let currentParagraph: Paragraph = { segments: [], annotations: [] }

    segments.forEach((segment) => {
      if (!segment.marked) {
        // 普通文本，按换行符分割
        const lines = segment.text.split('\n')
        lines.forEach((line) => {
          const trimmedLine = line.trim()

          if (!trimmedLine && currentParagraph.segments.length > 0) {
            paragraphs.push(currentParagraph)
            currentParagraph = { segments: [], annotations: [] }
          } else if (trimmedLine) {
            currentParagraph.segments.push({
              text: line,
              marked: false,
            })
          }
        })
      } else {
        // 标记文本
        currentParagraph.segments.push(segment)

        // 查找对应的批注
        const annotation = findAnnotationByNumber(segment.number!)
        if (annotation) {
          currentParagraph.annotations.push(annotation)
        }
      }
    })

    if (currentParagraph.segments.length > 0) {
      paragraphs.push(currentParagraph)
    }

    return paragraphs
  })

  // 根据序号查找批注
  function findAnnotationByNumber(number: number): Annotation | null {
    if (!currentReview.value) return null

    // 纠错
    if (currentReview.value.textCorrections) {
      const correctionIdx = number - 1
      if (correctionIdx >= 0 && correctionIdx < currentReview.value.textCorrections.length) {
        const correction = currentReview.value.textCorrections[correctionIdx]
        return {
          id: 'c-' + (correction.correctionId || correctionIdx),
          number: number,
          type: 'correction',
          original: correction.originalText,
          corrected: correction.correctedText,
          suggestion: correction.suggestion,
        }
      }
    }

    const correctionCount = currentReview.value?.textCorrections?.length || 0

    // 建议
    const suggestionIdx = number - correctionCount - 1
    if (suggestionIdx >= 0 && suggestionIdx < suggestionComments.value.length) {
      const comment = suggestionComments.value[suggestionIdx]
      return {
        id: 's-' + comment.commentId,
        number: number,
        type: 'suggestion',
        relatedText: comment.relatedText,
        content: comment.content,
      }
    }

    // 修改意见
    const revisionIdx = number - correctionCount - suggestionComments.value.length - 1
    if (revisionIdx >= 0 && revisionIdx < revisionComments.value.length) {
      const comment = revisionComments.value[revisionIdx]
      return {
        id: 'r-' + comment.commentId,
        number: number,
        type: 'revision',
        relatedText: comment.relatedText,
        content: comment.content,
      }
    }

    return null
  }

  // Actions
  function setReview(review: ReviewDetail) {
    currentReview.value = review
  }

  function setEssayContent(content: string) {
    essayContent.value = content
  }

  function setActiveHighlight(id: number | null) {
    activeHighlightId.value = id
  }

  function setProgress(current: number, total: number) {
    currentIndex.value = current
    totalCount.value = total
  }

  function reset() {
    currentReview.value = null
    essayContent.value = ''
    activeHighlightId.value = null
    currentIndex.value = 1
    totalCount.value = 1
  }

  return {
    // State
    currentReview,
    essayContent,
    activeHighlightId,
    currentIndex,
    totalCount,

    // Getters
    gradePercentage,
    wordCount,
    summaryComments,
    suggestionComments,
    revisionComments,
    contentSegments,
    formattedParagraphs,

    // Actions
    setReview,
    setEssayContent,
    setActiveHighlight,
    setProgress,
    reset,
    findAnnotationByNumber,
  }
})

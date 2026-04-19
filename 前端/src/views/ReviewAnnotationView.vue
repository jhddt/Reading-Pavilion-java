<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useReviewStore } from '@/store/review'
import { NButton, NSpace, NTag, NCard, NIcon } from 'naive-ui'
import { ArrowBackOutline } from '@vicons/ionicons5'
import http from '@/api/http'
import { annotate } from 'rough-notation'
import type { RoughAnnotation, RoughAnnotationConfig } from 'rough-notation/lib/model'

const route = useRoute()
const router = useRouter()
const reviewStore = useReviewStore()
const GRID_COLUMNS = 16

const activeAnnotationId = ref<number | null>(null)

const summaryText = computed(() => {
  const raw = reviewStore.summaryComments?.[0]?.content || ''
  if (!raw) return ''

  const normalized = raw.replace(/\r\n/g, '\n').trim()
  const nextSectionMatch = normalized.match(/\n\s*【(?:改进建议|修改意见|总分|各维度得分详情|评分详情)】/)

  if (!nextSectionMatch || nextSectionMatch.index == null) {
    return normalized
  }

  return normalized.slice(0, nextSectionMatch.index).trim()
})

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 批注数据结构
interface Annotation {
  id: number
  type: 'correction' | 'suggestion' | 'revision'
  startOffset: number
  endOffset: number
  originalText: string
  content: string
  correctedText?: string
  suggestion?: string
}

interface AnnotationCell {
  id: string
  char: string
  annotation: Annotation | null
  isMarker: boolean
  isIndent: boolean
  isPlaceholder: boolean
}

interface AnnotationSegment {
  id: string
  annotation: Annotation | null
  cells: AnnotationCell[]
}

// 合并所有批注
const annotations = computed((): Annotation[] => {
  const result: Annotation[] = []
  const review = reviewStore.currentReview
  if (!review) return result

  // 纠错批注
  review.textCorrections?.forEach((correction, idx) => {
    if (correction.startOffset != null && correction.endOffset != null) {
      result.push({
        id: idx + 1,
        type: 'correction',
        startOffset: correction.startOffset,
        endOffset: correction.endOffset,
        originalText: correction.originalText,
        correctedText: correction.correctedText,
        content: correction.suggestion || '错误纠正',
        suggestion: correction.suggestion,
      })
    }
  })

  // 建议批注
  reviewStore.suggestionComments.forEach((comment, idx) => {
    if (comment.startOffset != null && comment.endOffset != null) {
      result.push({
        id: (review.textCorrections?.length || 0) + idx + 1,
        type: 'suggestion',
        startOffset: comment.startOffset,
        endOffset: comment.endOffset,
        originalText: comment.relatedText || '',
        content: comment.content,
      })
    }
  })

  // 修改意见批注
  reviewStore.revisionComments.forEach((comment, idx) => {
    if (comment.startOffset != null && comment.endOffset != null) {
      result.push({
        id:
          (review.textCorrections?.length || 0) +
          reviewStore.suggestionComments.length +
          idx +
          1,
        type: 'revision',
        startOffset: comment.startOffset,
        endOffset: comment.endOffset,
        originalText: comment.relatedText || '',
        content: comment.content,
      })
    }
  })

  // 按位置排序
  return result.sort((a, b) => a.startOffset - b.startOffset)
})

const findAnnotationByOffset = (offset: number) => {
  return annotations.value.find(
    (annotation) => offset >= annotation.startOffset && offset < annotation.endOffset
  ) || null
}

const segmentElements = new Map<string, HTMLElement>()
const roughAnnotations = new Map<string, RoughAnnotation>()
let resizeObserver: ResizeObserver | null = null

const setSegmentRef = (segmentId: string, element: unknown) => {
  if (element instanceof HTMLElement) {
    segmentElements.set(segmentId, element)
  } else {
    segmentElements.delete(segmentId)
  }
}

const goToRereview = () => {
  const review = reviewStore.currentReview
  if (!review?.essayId) return
  router.push(`/reviews/${review.reviewId}/rerun`)
}

const groupCellsToSegments = (cells: AnnotationCell[], lineId: string) => {
  const segments: AnnotationSegment[] = []
  let current: AnnotationSegment | null = null

  cells.forEach((cell, index) => {
    const nextAnnotationId = cell.annotation?.id ?? null
    const currentAnnotationId = current?.annotation?.id ?? null

    if (!current || nextAnnotationId !== currentAnnotationId) {
      current = {
        id: `${lineId}-segment-${index}`,
        annotation: cell.annotation,
        cells: [cell],
      }
      segments.push(current)
      return
    }

    current.cells.push(cell)
  })

  return segments
}

const essayParagraphs = computed(() => {
  const rawContent = (reviewStore.essayContent || '').replace(/\r\n/g, '\n')
  if (!rawContent) return []

  let currentOffset = 0

  const padCells = (cells: AnnotationCell[], paragraphIndex: number, chunkIndex: number) => {
    const padded = [...cells]
    while (padded.length < GRID_COLUMNS) {
      padded.push({
        id: `${paragraphIndex}-${chunkIndex}-placeholder-${padded.length}`,
        char: '\u00A0',
        annotation: null,
        isMarker: false,
        isIndent: false,
        isPlaceholder: true,
      })
    }
    return padded
  }

  return rawContent
    .split('\n')
    .map((paragraph, paragraphIndex) => {
      const normalizedLine = paragraph.replace(/\t/g, '  ')
      const startOffset = currentOffset
      const chars = [...normalizedLine]
      const cells = chars.map((char, charIndex) => {
        const sourceOffset = startOffset + charIndex
        const annotation = findAnnotationByOffset(sourceOffset)
        return {
          id: `${paragraphIndex}-0-${charIndex}`,
          char: char === ' ' ? '\u00A0' : char,
          annotation,
          isMarker: annotation?.startOffset === sourceOffset,
          isIndent: char === ' ',
          isPlaceholder: false,
        }
      })

      currentOffset += normalizedLine.length + 1

      const paragraphAnnotations = annotations.value.filter(
        (annotation) =>
          annotation.startOffset < startOffset + normalizedLine.length &&
          annotation.endOffset > startOffset
      )

      const rows = []
      if (!cells.length) {
        rows.push({
          id: `${paragraphIndex}-0-empty`,
          cells: padCells([], paragraphIndex, 0),
          segments: groupCellsToSegments(padCells([], paragraphIndex, 0), `${paragraphIndex}-0-empty`),
          annotations: [],
        })
      } else {
        for (let i = 0; i < cells.length; i += GRID_COLUMNS) {
          const paddedCells = padCells(cells.slice(i, i + GRID_COLUMNS), paragraphIndex, i)
          rows.push({
            id: `${paragraphIndex}-0-${i}`,
            cells: paddedCells,
            segments: groupCellsToSegments(paddedCells, `${paragraphIndex}-0-${i}`),
            annotations: paragraphAnnotations,
          })
        }
      }

      return {
        id: `paragraph-${paragraphIndex}`,
        lines: rows,
      }
    })
})

// 点击批注 - 展开/收起
const handleAnnotationClick = async (annotationId: number) => {
  activeAnnotationId.value = activeAnnotationId.value === annotationId ? null : annotationId
  
  // 等待 DOM 更新后滚动到批注位置
  await nextTick()
  
  const annotationCard = document.querySelector(`[data-annotation-card="${annotationId}"]`) as HTMLElement
  if (annotationCard) {
    annotationCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }
}

// 获取批注类型的样式类
const getAnnotationClass = (type: string) => {
  return `annotation-${type}`
}

// 获取批注类型的标签
const getAnnotationLabel = (type: string) => {
  switch (type) {
    case 'correction':
      return '纠错'
    case 'suggestion':
      return '建议'
    case 'revision':
      return '修改'
    default:
      return ''
  }
}

const buildRoughConfig = (annotation: Annotation, isActive: boolean): RoughAnnotationConfig => {
  const colorMap = {
    correction: '#ef4444',
    suggestion: '#87cefa',
    revision: '#ffb347',
  }

  return {
    type: annotation.type === 'correction' ? 'underline' : 'highlight',
    color: colorMap[annotation.type],
    strokeWidth: isActive ? 2.2 : 1.5,
    animationDuration: 600,
    iterations: 2,
    padding: annotation.type === 'correction' ? 2 : 3,
    multiline: true,
  }
}

const syncRoughAnnotations = async () => {
  await nextTick()

  roughAnnotations.forEach((annotation) => annotation.remove())
  roughAnnotations.clear()

  essayParagraphs.value.forEach((paragraph: any) => {
    paragraph.lines.forEach((line: any) => {
      line.segments?.forEach((segment: AnnotationSegment) => {
        if (!segment.annotation) return
        const element = segmentElements.get(segment.id)
        if (!element) return

        const annotation = annotate(
          element,
          buildRoughConfig(segment.annotation, activeAnnotationId.value === segment.annotation.id)
        )
        annotation.show()
        roughAnnotations.set(segment.id, annotation)
      })
    })
  })
}

// 加载数据
const loadData = async () => {
  const reviewId = route.params.reviewId as string
  if (!reviewId) return

  try {
    const res = await http.get(`/review/record/${reviewId}`)
    reviewStore.setReview(res.data)

    if (res.data?.essayId) {
      const essayRes = await http.get(`/essay/${res.data.essayId}`)
      const essay = essayRes.data
      reviewStore.setEssayContent(essay.finalContent || essay.originalContent || '')
    }
  } catch (e: any) {
    console.error('加载失败:', e)
    alert(e.message || '加载失败')
  }
}

const setupResizeObserver = () => {
  if (typeof ResizeObserver === 'undefined') return

  resizeObserver?.disconnect()
  resizeObserver = new ResizeObserver(() => {
    syncRoughAnnotations()
  })

  segmentElements.forEach((element) => {
    resizeObserver?.observe(element)
  })

  resizeObserver.observe(document.body)
}

onMounted(async () => {
  await loadData()
  await syncRoughAnnotations()
  setupResizeObserver()
})

watch(
  [essayParagraphs, activeAnnotationId, annotations],
  async () => {
    await syncRoughAnnotations()
    setupResizeObserver()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  roughAnnotations.forEach((annotation) => annotation.remove())
  roughAnnotations.clear()
  segmentElements.clear()
})
</script>

<template>
  <div class="annotation-view animate-fade-in" v-if="reviewStore.currentReview">
    <!-- 顶部工具栏 -->
    <div class="toolbar animate-slide-in-left">
      <div class="toolbar-main">
        <n-button text @click="router.back()" class="back-btn">
          <template #icon>
            <n-icon><ArrowBackOutline /></n-icon>
          </template>
          返回
        </n-button>
        <div class="title">{{ reviewStore.currentReview.essayTitle || '未命名作文' }}</div>
      </div>
      <div class="toolbar-side">
        <div class="meta">
          <n-space>
            <n-tag :bordered="false" size="small">
              📅 {{ formatDate(reviewStore.currentReview.createTime) }}
            </n-tag>
            <n-tag :bordered="false" size="small">📝 {{ reviewStore.wordCount }} 字</n-tag>
            <n-tag :bordered="false" size="small" type="success">
              ⭐ {{ reviewStore.currentReview.totalScore?.toFixed(0) || 0 }} 分
            </n-tag>
          </n-space>
        </div>
        <n-button type="primary" class="rereview-btn" @click="goToRereview">
          继续批改
        </n-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="content-wrapper">
      <div class="essay-container">
        <!-- 作文内容 -->
        <div class="essay-content essay-paper-shell">
          <div class="essay-grid-sheet" v-if="essayParagraphs.length">
            <template v-for="paragraph in essayParagraphs" :key="paragraph.id">
              <template v-for="line in paragraph.lines" :key="line.id">
                <div class="essay-paragraph">
                  <span
                    v-for="segment in line.segments"
                    :key="segment.id"
                    class="essay-segment"
                    :class="[
                      segment.annotation ? getAnnotationClass(segment.annotation.type) : '',
                      { active: segment.annotation && activeAnnotationId === segment.annotation.id },
                    ]"
                    :style="{ gridColumn: `span ${segment.cells.length}` }"
                    :ref="(el) => setSegmentRef(segment.id, el)"
                    :data-annotation-id="segment.annotation?.id"
                    :title="segment.annotation ? `${getAnnotationLabel(segment.annotation.type)} #${segment.annotation.id}` : ''"
                    @click="segment.annotation && handleAnnotationClick(segment.annotation.id)"
                  >
                    <span
                      v-for="cell in segment.cells"
                      :key="cell.id"
                      class="essay-cell"
                      :class="[
                        cell.isIndent ? 'essay-cell-indent' : '',
                        cell.isPlaceholder ? 'essay-cell-placeholder' : '',
                      ]"
                    >
                      {{ cell.char }}
                      <span v-if="cell.annotation && cell.isMarker" class="annotation-marker">
                        {{ cell.annotation.id }}
                      </span>
                    </span>
                  </span>
                </div>
              </template>
            </template>
          </div>

          <div v-else class="essay-empty">暂无作文内容</div>
        </div>

        <!-- 批注列表（侧边栏） -->
        <div class="annotations-sidebar" v-progressive-blur-scroll>
          <div class="sidebar-title">批改批注 ({{ annotations.length }})</div>
          <div class="annotations-list">
            <n-card
              v-for="annotation in annotations"
              :key="annotation.id"
              class="annotation-card"
              :class="[
                `card-${annotation.type}`,
                { active: activeAnnotationId === annotation.id },
              ]"
              :data-annotation-card="annotation.id"
              size="small"
              @click="handleAnnotationClick(annotation.id)"
            >
              <div class="card-header">
                <n-tag
                  :type="
                    annotation.type === 'correction'
                      ? 'error'
                      : annotation.type === 'suggestion'
                        ? 'info'
                        : 'warning'
                  "
                  size="small"
                  :bordered="false"
                >
                  {{ getAnnotationLabel(annotation.type) }} #{{ annotation.id }}
                </n-tag>
                <span class="card-offset">
                  {{ annotation.startOffset }} - {{ annotation.endOffset }}
                </span>
              </div>

              <!-- 纠错卡片 -->
              <div v-if="annotation.type === 'correction'" class="card-content">
                <div class="correction-detail">
                  <span class="text-error">{{ annotation.originalText }}</span>
                  <span class="arrow">→</span>
                  <span class="text-correct">{{ annotation.correctedText }}</span>
                </div>
                <div v-if="annotation.suggestion" class="card-text">
                  {{ annotation.suggestion }}
                </div>
              </div>

              <!-- 建议/修改卡片 -->
              <div v-else class="card-content">
                <div v-if="annotation.originalText" class="card-quote">
                  "{{ annotation.originalText }}"
                </div>
                <div class="card-text">{{ annotation.content }}</div>
              </div>
            </n-card>
          </div>
        </div>
      </div>

      <div v-if="summaryText" class="summary-panel essay-paper-shell">
        <div class="summary-panel__title">总评</div>
        <div class="summary-panel__body">
          {{ summaryText }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.annotation-view {
  min-height: 100vh;
  background: transparent;
  display: flex;
  flex-direction: column;
}

/* 工具栏 */
.toolbar {
  background: rgba(255, 250, 243, 0.88);
  padding: 18px 24px;
  border: 1px solid rgba(56, 44, 31, 0.08);
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
}

.toolbar:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.toolbar-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  flex: 1;
}

.toolbar-side {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.back-btn {
  font-size: 14px;
  transition: all 0.2s ease;
}

.back-btn:hover {
  transform: translateX(-4px);
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #212121;
  flex: 1;
  transition: all 0.3s ease;
}

.title:hover {
  color: var(--brand);
}

.meta {
  display: flex;
  gap: 8px;
}

.rereview-btn {
  border-radius: 999px;
  padding: 0 20px;
  height: 38px;
  font-weight: 700;
}

/* 主内容区 */
.content-wrapper {
  flex: 1;
  padding: 20px 0 0;
}

.essay-container {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: 24px;
  align-items: start;
}

/* 作文内容 */
.essay-content {
  padding: 24px;
  border-radius: 24px;
  box-shadow: var(--shadow);
  border: 1px solid rgba(56, 44, 31, 0.08);
}

.essay-paper-shell {
  background:
    radial-gradient(circle at top right, rgba(34, 77, 105, 0.05), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(253, 249, 242, 0.96));
}

.essay-grid-sheet {
  width: 100%;
  max-width: 820px;
  padding: 18px 18px 22px;
  border-radius: 18px;
  border: 1px solid rgba(77, 92, 116, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(250, 246, 238, 0.98));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.essay-empty {
  color: var(--muted);
  font-size: 15px;
}

.essay-paragraph {
  display: grid;
  grid-template-columns: repeat(16, 34px);
  gap: 0;
  margin: 0;
}

.essay-paragraph + .essay-paragraph {
  margin-top: 10px;
}

.essay-segment {
  position: relative;
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 34px;
  align-items: stretch;
  justify-self: start;
  border-radius: 10px;
  background: transparent;
  transition: transform 0.2s ease;
}

.essay-segment[class*='annotation-'] {
  cursor: pointer;
}

.essay-segment[class*='annotation-']:hover {
  z-index: 2;
}

.essay-segment.active {
  z-index: 3;
}

.essay-cell {
  position: relative;
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0 -1px -1px 0;
  border: 1px solid rgba(77, 92, 116, 0.2);
  color: #1f2d3d;
  font-size: 21px;
  line-height: 1;
  font-family: 'KaiTi', 'STKaiti', 'FangSong', serif;
  background: rgba(255, 255, 255, 0.5);
  transition: all 0.2s ease;
  white-space: pre;
  overflow: visible;
}

.essay-cell-indent {
  background: rgba(245, 247, 250, 0.72);
}

.essay-cell-placeholder {
  color: transparent;
  background: rgba(255, 255, 255, 0.24);
}

.annotation-correction .essay-cell {
  color: #7f1d1d;
}

.annotation-suggestion .essay-cell {
  color: #134e6f;
}

.annotation-revision .essay-cell {
  color: #92400e;
}

.essay-segment.active .essay-cell {
  transform: translateY(-1px);
}

.essay-segment.active .essay-cell:not(.essay-cell-placeholder) {
  background: rgba(255, 252, 245, 0.92);
}

.annotation-marker {
  position: absolute;
  top: -8px;
  right: -7px;
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: white;
  border-radius: 999px;
  background: #1b4f73;
  box-shadow: 0 4px 10px rgba(18, 41, 58, 0.18);
  border: 2px solid rgba(255, 250, 243, 0.96);
}

.correction-detail {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 15px;
  flex-wrap: wrap;
}

.text-error {
  color: #d32f2f;
  text-decoration: line-through;
  font-weight: 500;
}

.arrow {
  color: #757575;
  font-weight: bold;
}

.text-correct {
  color: #2e7d32;
  font-weight: 600;
}

/* 侧边栏 */
.annotations-sidebar {
  position: sticky;
  top: 20px;
  background: rgba(255, 250, 243, 0.92);
  border-radius: 24px;
  box-shadow: var(--shadow);
  padding: 16px;
  border: 1px solid rgba(56, 44, 31, 0.08);
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #212121;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e0e0e0;
}

.annotations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.annotation-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-left: 4px solid transparent;
  scroll-margin-top: 24px;
}

.annotation-card:hover {
  transform: translateX(8px) scale(1.02);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.annotation-card.active {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
  transform: translateX(8px) scale(1.05);
}

.card-correction {
  border-left-color: #f44336;
}

.card-suggestion {
  border-left-color: #2196f3;
}

.card-revision {
  border-left-color: #ff9800;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.card-offset {
  font-size: 11px;
  color: var(--muted);
}

.card-content {
  font-size: 13px;
}

.card-quote {
  font-style: italic;
  color: #757575;
  margin-bottom: 8px;
  padding: 6px;
  background: #f5f5f5;
  border-radius: 4px;
}

.card-text {
  line-height: 1.6;
  color: #424242;
}

.summary-panel {
  margin-top: 24px;
  padding: 24px;
  border-radius: 24px;
  border: 1px solid rgba(56, 44, 31, 0.08);
  box-shadow: var(--shadow);
}

.summary-panel__title {
  margin-bottom: 14px;
  font-family: 'STSong', 'SimSun', serif;
  font-size: 26px;
  font-weight: 700;
  color: var(--brand-deep);
}

.summary-panel__body {
  min-height: 220px;
  padding: 20px 22px;
  border-radius: 18px;
  border: 1px solid rgba(77, 92, 116, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(250, 246, 238, 0.98));
  color: #2158d2;
  font-family: 'KaiTi', 'STKaiti', 'FangSong', serif;
  font-size: 28px;
  line-height: 1.95;
  white-space: pre-wrap;
}

/* 响应式 */
@media (max-width: 1200px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-main,
  .toolbar-side {
    justify-content: space-between;
  }

  .essay-container {
    grid-template-columns: 1fr;
  }

  .annotations-sidebar {
    position: relative;
    top: 0;
    max-height: none;
  }

  .essay-paragraph {
    grid-template-columns: repeat(12, 34px);
  }
}

@media (max-width: 720px) {
  .toolbar-main,
  .toolbar-side {
    flex-direction: column;
    align-items: stretch;
  }

  .meta {
    width: 100%;
  }
}
</style>

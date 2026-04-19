<template>
  <div v-if="essay" class="fs-grid fs-grid-two">
    <div class="fs-card">
      <div class="fs-card-header">
        <div class="fs-card-title">{{ essay.title || '未命名作文' }}</div>
        <span class="fs-tag" :class="statusClass(essay.status)">{{ statusText(essay.status) }}</span>
      </div>
      <div class="fs-card-body">
        <div class="essay-meta-grid">
          <div class="meta-item">
            <span>作文编号</span>
            <strong>{{ essay.id }}</strong>
          </div>
          <div class="meta-item">
            <span>字数</span>
            <strong>{{ essay.wordCount || 0 }}</strong>
          </div>
          <div class="meta-item">
            <span>提交方式</span>
            <strong>{{ submitTypeText(essay.submitType) }}</strong>
          </div>
          <div class="meta-item">
            <span>创建时间</span>
            <strong>{{ formatDate(essay.createTime) }}</strong>
          </div>
        </div>

        <div class="fs-paper essay-paper-shell" style="margin-top: 18px">
          <h3>正文内容</h3>
          <div class="essay-content" :class="{ 'essay-content-empty': !essayParagraphs.length }">
            <template v-if="essayParagraphs.length">
              <div class="essay-grid-sheet">
                <template v-for="paragraph in essayParagraphs" :key="paragraph.id">
                  <div v-for="line in paragraph.lines" :key="line.id" class="essay-paragraph">
                    <span
                      v-for="cell in line.cells"
                      :key="cell.id"
                      class="essay-cell"
                      :class="{
                        'essay-cell-indent': cell.isIndent,
                        'essay-cell-placeholder': cell.isPlaceholder,
                      }"
                    >
                      {{ cell.char }}
                    </span>
                  </div>
                </template>
              </div>
            </template>
            <span v-else>暂无内容</span>
          </div>
        </div>
      </div>
    </div>

    <div class="fs-stack">
      <div class="fs-card">
        <div class="fs-card-header">
          <div class="fs-card-title">操作</div>
        </div>
        <div class="fs-card-body fs-stack">
          <div class="action-panel">
            <div class="action-panel-copy">
              <h3>{{ statusText(essay.status) }}</h3>
              <p>{{ detailAdvice }}</p>
              <div v-if="selectedRule && essay.status >= 1" class="selected-rule-compact">
                已选评分细则：{{ selectedRule.ruleName }}
              </div>
            </div>

            <div class="action-panel-buttons">
              <button
                v-if="essay.status === 0"
                class="fs-btn fs-btn-primary btn-ripple shine-effect"
                @click="onSubmit"
              >
                提交作文
              </button>

              <button
                v-else-if="essay.status === 1"
                class="fs-btn fs-btn-primary btn-ripple shine-effect"
                @click="onReview"
                :disabled="reviewing"
              >
                <span v-if="reviewing" class="loading-spinner"></span>
                {{ reviewing ? '提交中...' : '进入细则选择并开始批改' }}
              </button>

              <button
                v-else-if="essay.status === 2"
                class="fs-btn fs-btn-primary btn-ripple shine-effect"
                @click="viewReviews"
              >
                查看批改进度
              </button>

              <button
                v-else
                class="fs-btn fs-btn-primary btn-ripple shine-effect"
                @click="viewReviews"
              >
                查看批改结果
              </button>

              <button class="fs-btn fs-btn-outline btn-ripple" @click="$router.back()">返回上一页</button>
              <button v-if="essay.status === 1" class="fs-btn fs-btn-outline btn-ripple" @click="onWithdraw">撤回作文</button>
              <button v-if="essay.status === 0" class="fs-btn fs-btn-danger btn-ripple" @click="onDelete">删除草稿</button>
            </div>
          </div>
        </div>
      </div>

      <div class="fs-card">
        <div class="fs-card-header">
          <div class="fs-card-title">流程说明</div>
        </div>
        <div class="fs-card-body fs-stack">
          <div class="meta-item">
            <span>状态说明</span>
            <strong>{{ detailAdvice }}</strong>
          </div>
          <div class="meta-item">
            <span>下一步</span>
            <strong>{{ recommendation }}</strong>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="fs-card">
    <div class="fs-empty">正在加载作文详情...</div>
  </div>

  <div v-if="reviewConfirmVisible" class="confirm-overlay" @click.self="closeReviewConfirm">
    <div class="confirm-dialog">
      <div class="confirm-badge">批改确认</div>
      <h3>按所选细则提交批改</h3>
      <p>
        这一步会先进行错字修改，再进入内容批改与总评生成。确认后，系统将按你选中的评分细则完成整套流程。
      </p>
      <div class="confirm-rule-block">
        <div class="confirm-rule-title">选择本次评分细则</div>
        <div v-if="enabledRules.length" v-progressive-blur-scroll class="rule-pick-list">
          <button
            v-for="rule in enabledRules"
            :key="rule.ruleId"
            class="rule-pick"
            :class="{ 'rule-pick-active': selectedRuleId === rule.ruleId }"
            @click="selectRule(rule.ruleId)"
          >
            <div class="rule-pick-head">
              <strong>{{ rule.ruleName }}</strong>
              <span class="fs-tag fs-tag-success">启用</span>
            </div>
            <div class="rule-pick-meta">
              {{ rule.gradeLevel || '未设置学段' }} / {{ rule.reviewType || '通用作文' }}
            </div>
            <div v-if="rule.customRequirement" class="rule-pick-desc">
              {{ rule.customRequirement }}
            </div>
          </button>
        </div>
        <div v-else class="fs-empty">当前没有可用的评分细则，请先到配置页启用至少一条。</div>
      </div>
      <div class="confirm-grid">
        <div class="confirm-tile">
          <span>作文</span>
          <strong>{{ essay?.title || '未命名作文' }}</strong>
        </div>
        <div class="confirm-tile">
          <span>评分细则</span>
          <strong>{{ selectedRule?.ruleName || '未选择' }}</strong>
        </div>
        <div class="confirm-tile">
          <span>批改阶段</span>
          <strong>错字修改 -> 内容批改</strong>
        </div>
        <div class="confirm-tile">
          <span>当前字数</span>
          <strong>{{ essay?.wordCount || 0 }} 字</strong>
        </div>
      </div>
      <div class="confirm-actions">
        <button class="fs-btn fs-btn-outline" @click="closeReviewConfirm">返回调整</button>
        <button class="fs-btn fs-btn-primary" @click="confirmReview" :disabled="reviewing || !selectedRuleId">
          <span v-if="reviewing" class="loading-spinner"></span>
          {{ reviewing ? '提交中...' : '确认开始批改' }}
        </button>
      </div>
    </div>
  </div>

  <div v-if="submitConfirmVisible" class="confirm-overlay" @click.self="closeSubmitConfirm">
    <div class="confirm-dialog">
      <div class="confirm-badge">提交作文</div>
      <h3>确认提交当前作文</h3>
      <p>
        提交后，这篇作文会进入“可批改”状态。你仍然可以先查看正文，再在“提交智能批改”时选择评分细则并开始批改。
      </p>
      <div class="confirm-grid">
        <div class="confirm-tile">
          <span>作文标题</span>
          <strong>{{ essay?.title || '未命名作文' }}</strong>
        </div>
        <div class="confirm-tile">
          <span>当前字数</span>
          <strong>{{ essay?.wordCount || 0 }} 字</strong>
        </div>
        <div class="confirm-tile">
          <span>提交方式</span>
          <strong>{{ submitTypeText(essay?.submitType) }}</strong>
        </div>
        <div class="confirm-tile">
          <span>提交后状态</span>
          <strong>已提交，可发起智能批改</strong>
        </div>
      </div>
      <div class="confirm-actions">
        <button class="fs-btn fs-btn-outline" @click="closeSubmitConfirm">再检查一下</button>
        <button class="fs-btn fs-btn-primary" @click="confirmSubmit" :disabled="submittingEssay">
          <span v-if="submittingEssay" class="loading-spinner"></span>
          {{ submittingEssay ? '提交中...' : '确认提交作文' }}
        </button>
      </div>
    </div>
  </div>

  <div v-if="deleteConfirmVisible" class="confirm-overlay" @click.self="closeDeleteConfirm">
    <div class="confirm-dialog confirm-dialog-danger">
      <div class="confirm-badge confirm-badge-danger">删除草稿</div>
      <h3>确认删除这篇草稿？</h3>
      <p>
        删除后将无法恢复当前草稿内容、排版和已整理的正文文本。这个操作只建议在确认不再需要这篇作文时使用。
      </p>
      <div class="confirm-grid">
        <div class="confirm-tile">
          <span>作文标题</span>
          <strong>{{ essay?.title || '未命名作文' }}</strong>
        </div>
        <div class="confirm-tile">
          <span>当前字数</span>
          <strong>{{ essay?.wordCount || 0 }} 字</strong>
        </div>
        <div class="confirm-tile">
          <span>提交方式</span>
          <strong>{{ submitTypeText(essay?.submitType) }}</strong>
        </div>
        <div class="confirm-tile">
          <span>删除后结果</span>
          <strong>草稿会被永久移除</strong>
        </div>
      </div>
      <div class="confirm-actions">
        <button class="fs-btn fs-btn-outline" @click="closeDeleteConfirm">保留草稿</button>
        <button class="fs-btn fs-btn-danger" @click="confirmDelete" :disabled="deletingEssay">
          <span v-if="deletingEssay" class="loading-spinner"></span>
          {{ deletingEssay ? '删除中...' : '确认删除草稿' }}
        </button>
      </div>
    </div>
  </div>

  <div v-if="reviewProgressVisible" class="progress-overlay">
    <div class="progress-dialog">
      <div class="progress-topline">
        <span class="progress-pill">{{ progressStatusLabel }}</span>
        <strong>{{ selectedRule?.ruleName || '已选细则' }}</strong>
      </div>

      <h3>{{ progressTitle }}</h3>
      <p>{{ progressDescription }}</p>

      <div class="progress-track">
        <div class="progress-bar" :style="{ width: `${progressPercent}%` }"></div>
      </div>

      <div class="progress-stage-list">
        <div
          v-for="(stage, index) in progressStages"
          :key="stage.key"
          class="progress-stage"
          :class="stageClass(index)"
        >
          <div class="progress-stage-index">{{ index + 1 }}</div>
          <div class="progress-stage-content">
            <strong>{{ stage.title }}</strong>
            <span>{{ stage.description }}</span>
          </div>
        </div>
      </div>

      <div class="progress-stats">
        <div class="progress-stat">
          <span>作文标题</span>
          <strong>{{ essay?.title || '未命名作文' }}</strong>
        </div>
        <div class="progress-stat">
          <span>任务编号</span>
          <strong>{{ currentReviewId || '生成中' }}</strong>
        </div>
      </div>

      <div v-if="reviewError" class="progress-error">
        {{ reviewError }}
      </div>

      <div class="progress-actions">
        <button class="fs-btn fs-btn-outline" @click="closeReviewProgress">
          {{ reviewSucceeded ? '暂时留在这里' : '返回作文页' }}
        </button>
        <button v-if="reviewSucceeded" class="fs-btn fs-btn-primary" @click="goToReviewProgress">查看批改详情</button>
        <button v-else class="fs-btn fs-btn-primary" @click="viewReviews">查看批改记录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const router = useRouter()
const GRID_COLUMNS = 16

const essay = ref(null)
const reviewing = ref(false)
const reviewConfirmVisible = ref(false)
const reviewProgressVisible = ref(false)
const submitConfirmVisible = ref(false)
const deleteConfirmVisible = ref(false)
const currentReviewId = ref(null)
const enabledRules = ref([])
const selectedRuleId = ref(null)
const currentProgressStage = ref(0)
const reviewSucceeded = ref(false)
const reviewError = ref('')
const submittingEssay = ref(false)
const deletingEssay = ref(false)

let progressTimers = []
let reviewPollTimer = null

const progressStages = [
  {
    key: 'prepare',
    title: '批改任务已创建',
    description: '系统已经锁定本次作文正文和所选评分细则。',
  },
  {
    key: 'correction',
    title: '错字修改处理中',
    description: '先检查错别字、病句和基础表达问题，为后续评分打底。',
  },
  {
    key: 'review',
    title: '内容批改生成中',
    description: '正在结合评分细则输出分项得分、总评和修改建议。',
  },
  {
    key: 'done',
    title: '批改结果整理完成',
    description: '结果已生成，可以查看总评、批注和修改明细。',
  },
]

const storageKey = computed(() => (essay.value?.id ? `essay-rule-${essay.value.id}` : 'essay-rule-temp'))
const activeReviewStorageKey = computed(() =>
  essay.value?.id ? `essay-active-review-${essay.value.id}` : 'essay-active-review-temp'
)

const selectedRule = computed(
  () => enabledRules.value.find((rule) => rule.ruleId === selectedRuleId.value) || null
)

const progressPercent = computed(() => {
  if (reviewError.value) return 100
  if (reviewSucceeded.value) return 100
  return [18, 42, 74, 92][currentProgressStage.value] || 18
})

const progressStatusLabel = computed(() => {
  if (reviewError.value) return '批改失败'
  if (reviewSucceeded.value) return '批改完成'
  return '智能批改中'
})

const progressTitle = computed(() => {
  if (reviewError.value) return '本次批改没有成功完成'
  if (reviewSucceeded.value) return '本次批改已经完成'
  return progressStages[Math.min(currentProgressStage.value, 2)].title
})

const progressDescription = computed(() => {
  if (reviewError.value) return reviewError.value
  if (reviewSucceeded.value) return '错字修改和内容批改都已经结束，现在可以进入详情页查看完整结果。'
  return progressStages[Math.min(currentProgressStage.value, 2)].description
})

const statusText = (status) => {
  if (status === 0) return '草稿'
  if (status === 1) return '已提交'
  if (status === 2) return '批改中'
  if (status === 3) return '已批改'
  if (status === 4) return '已归档'
  return '未知'
}

const statusClass = (status) => {
  if (status === 0) return 'fs-tag-warning'
  if (status === 1) return 'fs-tag-primary'
  if (status === 2) return 'fs-tag-neutral'
  if (status === 3) return 'fs-tag-success'
  return 'fs-tag-neutral'
}

const submitTypeText = (type) => {
  if (type === 0) return '图片录入'
  if (type === 1) return '文档导入'
  if (type === 2) return '文本输入'
  return '未知'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '暂无'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const detailAdvice = computed(() => {
  if (!essay.value) return ''
  if (essay.value.status === 0) return '作文还在草稿阶段，建议先确认正文，再进入后续批改流程。'
  if (essay.value.status === 1) return '作文已经提交，下一步应该先选择评分细则，再发起智能批改。'
  if (essay.value.status === 2) return '系统正在后台处理批改任务，错字修改和内容批改会依次完成。'
  if (essay.value.status === 3) return '作文已经完成批改。若对当前结果不满意，可以重新选择评分细则再次批改，并保留所有历史版本。'
  return '该作文已经归档，可作为历史样本继续查看。'
})

const recommendation = computed(() => {
  if (!essay.value) return ''
  if (essay.value.status === 0) {
    return essay.value.submitType === 0
      ? '图片录入的作文，先核对 OCR 文本，再点击“提交作文”。'
      : '当前先把作文定稿，然后提交作文，最后点击“提交智能批改”时再选评分细则。'
  }
  if (!enabledRules.value.length) return '请先到评分细则配置页启用至少一条细则。'
  if (essay.value.status === 2) return '可以等待当前任务完成，或进入批改记录页查看最新进度。'
  if (essay.value.status >= 3) return '你可以继续更换评分细则再次批改，系统会保留每一次批改历史。'
  return '点击“提交智能批改”后，再在弹出的确认层中选择本次评分细则。'
})

const buildLineCells = (line, paragraphIndex, lineIndex) => {
  const normalizedLine = line.replace(/\t/g, '  ')
  const chars = [...normalizedLine]

  return chars.map((char, charIndex) => ({
    id: `${paragraphIndex}-${lineIndex}-${charIndex}`,
    char: char === ' ' ? '\u00A0' : char,
    isIndent: char === ' ',
    isPlaceholder: false,
  }))
}

const padLineCells = (cells, paragraphIndex, lineIndex, chunkIndex) => {
  const padded = [...cells]
  while (padded.length < GRID_COLUMNS) {
    padded.push({
      id: `${paragraphIndex}-${lineIndex}-${chunkIndex}-placeholder-${padded.length}`,
      char: '\u00A0',
      isIndent: false,
      isPlaceholder: true,
    })
  }
  return padded
}

const splitLineIntoGridRows = (line, paragraphIndex, lineIndex) => {
  const baseCells = buildLineCells(line, paragraphIndex, lineIndex)
  if (!baseCells.length) {
    return [
      {
        id: `${paragraphIndex}-${lineIndex}-empty`,
        cells: padLineCells([], paragraphIndex, lineIndex, 0),
      },
    ]
  }

  const rows = []
  for (let i = 0; i < baseCells.length; i += GRID_COLUMNS) {
    rows.push({
      id: `${paragraphIndex}-${lineIndex}-${i}`,
      cells: padLineCells(baseCells.slice(i, i + GRID_COLUMNS), paragraphIndex, lineIndex, i),
    })
  }
  return rows
}

const normalizeEssayContent = (content) => {
  return content
    .replace(/\r\n/g, '\n')
    .split('\n')
    .map((paragraph, paragraphIndex) => {
      const rows = splitLineIntoGridRows(paragraph, paragraphIndex, 0)
      return {
        id: `paragraph-${paragraphIndex}`,
        lines: rows,
      }
    })
}

const essayParagraphs = computed(() => {
  const content = essay.value?.finalContent || essay.value?.originalContent || ''
  return normalizeEssayContent(content)
})

const stageClass = (index) => ({
  'is-completed': reviewSucceeded.value ? true : index < currentProgressStage.value,
  'is-active': !reviewSucceeded.value && index === currentProgressStage.value,
  'is-pending': !reviewSucceeded.value && index > currentProgressStage.value,
  'is-failed': !!reviewError.value,
})

const selectRule = (ruleId) => {
  selectedRuleId.value = ruleId
}

const restoreSelectedRule = () => {
  const cached = window.localStorage.getItem(storageKey.value)
  const cachedRuleId = cached ? Number(cached) : null
  if (cachedRuleId && enabledRules.value.some((rule) => rule.ruleId === cachedRuleId)) {
    selectedRuleId.value = cachedRuleId
    return
  }
  selectedRuleId.value = enabledRules.value[0]?.ruleId || null
}

const loadRules = async () => {
  const res = await http.get('/review/rules', {
    params: { enabledOnly: true },
  })
  enabledRules.value = res.data || []
  restoreSelectedRule()

  const preferredRuleId = Number(route.query.preferredRuleId || '')
  if (preferredRuleId && enabledRules.value.some((rule) => rule.ruleId === preferredRuleId)) {
    selectedRuleId.value = preferredRuleId
  }
}

const loadData = async () => {
  const essayId = route.params.id
  if (!essayId) return
  try {
    const res = await http.get(`/essay/${essayId}`)
    essay.value = res.data || null
    await loadRules()
    await recoverActiveReview()
    await openRereviewFromQuery()
  } catch (e) {
    alert(e.message || '加载失败')
    router.back()
  }
}

const clearProgressRuntime = () => {
  progressTimers.forEach((timer) => window.clearTimeout(timer))
  progressTimers = []
  if (reviewPollTimer) {
    window.clearInterval(reviewPollTimer)
    reviewPollTimer = null
  }
}

const persistActiveReviewId = (reviewId) => {
  if (!reviewId) return
  window.localStorage.setItem(activeReviewStorageKey.value, String(reviewId))
}

const clearActiveReviewId = () => {
  window.localStorage.removeItem(activeReviewStorageKey.value)
}

const beginProgressAnimation = () => {
  clearProgressRuntime()
  currentProgressStage.value = 0
  reviewSucceeded.value = false
  reviewError.value = ''

  progressTimers.push(window.setTimeout(() => {
    if (!reviewSucceeded.value && !reviewError.value) currentProgressStage.value = 1
  }, 900))

  progressTimers.push(window.setTimeout(() => {
    if (!reviewSucceeded.value && !reviewError.value) currentProgressStage.value = 2
  }, 2800))

  progressTimers.push(window.setTimeout(() => {
    if (!reviewSucceeded.value && !reviewError.value) currentProgressStage.value = 3
  }, 6200))
}

const syncProgressFromStatus = (status) => {
  if (status === 1) {
    if (currentProgressStage.value < 1) currentProgressStage.value = 1
    if (essay.value) essay.value.status = 2
    return
  }

  if (status === 2) {
    reviewSucceeded.value = true
    reviewError.value = ''
    currentProgressStage.value = 3
    clearProgressRuntime()
    clearActiveReviewId()
    if (essay.value) essay.value.status = 3
    return
  }

  if (status === 3 || status === 4) {
    currentProgressStage.value = 3
    clearProgressRuntime()
    clearActiveReviewId()
    if (essay.value) essay.value.status = 1
  }
}

const pollReviewStatus = () => {
  if (!currentReviewId.value) return
  reviewPollTimer = window.setInterval(async () => {
    try {
      const res = await http.get(`/review/status/${currentReviewId.value}`)
      const status = res.data
      if (!status) return

      if (status.status === 1) {
        syncProgressFromStatus(status.status)
        return
      }

      if (status.status === 2) {
        syncProgressFromStatus(status.status)
        await loadData()
      } else if (status.status === 3 || status.status === 4) {
        reviewError.value = status.errorMsg || '批改任务执行失败，请稍后重试。'
        syncProgressFromStatus(status.status)
        await loadData()
      }
    } catch (e) {
      reviewError.value = e.message || '轮询批改状态失败'
      clearProgressRuntime()
    }
  }, 2000)
}

const recoverActiveReview = async () => {
  const cachedReviewId = Number(window.localStorage.getItem(activeReviewStorageKey.value) || '')
  if (!cachedReviewId || !essay.value || essay.value.status !== 2) return

  currentReviewId.value = cachedReviewId
  reviewProgressVisible.value = true
  beginProgressAnimation()

  try {
    const res = await http.get(`/review/status/${cachedReviewId}`)
    const status = res.data
    if (!status) return

    if (status.status === 1) {
      syncProgressFromStatus(status.status)
      pollReviewStatus()
      return
    }

    if (status.status === 2) {
      syncProgressFromStatus(status.status)
      reviewProgressVisible.value = false
      return
    }

    reviewError.value = status.errorMsg || '批改任务执行失败，请稍后重试。'
    syncProgressFromStatus(status.status)
  } catch (e) {
    reviewError.value = e.message || '恢复批改进度失败'
    clearProgressRuntime()
  }
}

const openRereviewFromQuery = async () => {
  if (route.query.rereview !== '1' || !essay.value) return
  if (essay.value.status < 3 || !enabledRules.value.length) return

  reviewConfirmVisible.value = true
  await router.replace({
    path: route.path,
    query: {},
  })
}

const onReview = async () => {
  if (!essay.value?.id || reviewing.value) return
  if (!enabledRules.value.length) {
    alert('当前没有可用的评分细则，请先到配置页启用至少一条')
    return
  }
  reviewConfirmVisible.value = true
}

const closeReviewConfirm = () => {
  if (reviewing.value) return
  reviewConfirmVisible.value = false
}

const closeSubmitConfirm = () => {
  if (submittingEssay.value) return
  submitConfirmVisible.value = false
}

const closeDeleteConfirm = () => {
  if (deletingEssay.value) return
  deleteConfirmVisible.value = false
}

const confirmReview = async () => {
  if (!essay.value?.id || reviewing.value || !selectedRuleId.value) return
  try {
    reviewing.value = true
    const res = await http.post(`/review/essay/${essay.value.id}`, null, {
      params: { ruleId: selectedRuleId.value },
    })
    currentReviewId.value = res.data?.reviewId || null
    persistActiveReviewId(currentReviewId.value)
    reviewConfirmVisible.value = false
    reviewProgressVisible.value = true
    if (essay.value) essay.value.status = 2
    beginProgressAnimation()
    pollReviewStatus()
  } catch (e) {
    alert(e.message || '发起批改失败')
  } finally {
    reviewing.value = false
  }
}

const closeReviewProgress = () => {
  reviewProgressVisible.value = false
}

const goToReviewProgress = () => {
  reviewProgressVisible.value = false
  if (currentReviewId.value) {
    router.push(`/reviews/${currentReviewId.value}`)
    return
  }
  viewReviews()
}

const viewReviews = () => {
  router.push({
    path: '/reviews',
    query: { essayId: essay.value.id },
  })
}

const onSubmit = async () => {
  if (!essay.value?.id) return
  submitConfirmVisible.value = true
}

const confirmSubmit = async () => {
  if (!essay.value?.id || submittingEssay.value) return
  try {
    submittingEssay.value = true
    await http.put(`/essay/${essay.value.id}/submit`)
    submitConfirmVisible.value = false
    await loadData()
  } catch (e) {
    alert(e.message || '提交失败')
  } finally {
    submittingEssay.value = false
  }
}

const onWithdraw = async () => {
  if (!essay.value?.id) return
  if (!window.confirm(`确定撤回作文「${essay.value.title}」吗？`)) return
  try {
    await http.put(`/essay/${essay.value.id}/withdraw`)
    await loadData()
  } catch (e) {
    alert(e.message || '撤回失败')
  }
}

const onDelete = async () => {
  if (!essay.value?.id) return
  deleteConfirmVisible.value = true
}

const confirmDelete = async () => {
  if (!essay.value?.id || deletingEssay.value) return
  try {
    deletingEssay.value = true
    await http.delete(`/essay/${essay.value.id}`)
    deleteConfirmVisible.value = false
    router.push('/essays')
  } catch (e) {
    alert(e.message || '删除失败')
  } finally {
    deletingEssay.value = false
  }
}

watch(selectedRuleId, (value) => {
  if (!value) return
  window.localStorage.setItem(storageKey.value, String(value))
})

watch(
  () => route.params.id,
  () => {
    clearProgressRuntime()
    reviewProgressVisible.value = false
    currentReviewId.value = null
    loadData()
  }
)

onMounted(loadData)
onBeforeUnmount(clearProgressRuntime)
</script>

<style scoped>
.essay-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(56, 44, 31, 0.08);
}

.meta-item span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.meta-item strong {
  font-size: 15px;
  line-height: 1.7;
  font-weight: 600;
}

.essay-content {
  margin-top: 6px;
}

.essay-grid-sheet {
  max-width: 760px;
  padding: 18px 18px 22px;
  border-radius: 18px;
  border: 1px solid rgba(77, 92, 116, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(250, 246, 238, 0.98));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.essay-paper-shell {
  background:
    radial-gradient(circle at top right, rgba(34, 77, 105, 0.05), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(253, 249, 242, 0.96));
}

.essay-content,
.essay-content-empty {
  margin: 0;
}

.essay-content-empty {
  font-size: 15px;
  color: var(--muted);
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

.essay-cell {
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
  white-space: pre;
}

.essay-cell-indent {
  background: rgba(240, 246, 255, 0.35);
}

.essay-cell-placeholder {
  color: transparent;
}

.action-panel {
  display: grid;
  gap: 18px;
  padding: 18px 20px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(34, 77, 105, 0.08), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(250, 245, 238, 0.96));
  border: 1px solid rgba(56, 44, 31, 0.08);
}

.action-panel-copy h3 {
  margin: 0 0 10px;
  font-family: 'STSong', 'SimSun', serif;
  font-size: 30px;
  line-height: 1.2;
}

.action-panel-copy p {
  margin: 0;
  color: #5b534a;
  line-height: 1.9;
}

.action-panel-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.selected-rule-compact {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(34, 77, 105, 0.08);
  color: #21475f;
  font-size: 14px;
  font-weight: 600;
}

.rule-pick-list {
  display: grid;
  gap: 10px;
}

.confirm-rule-block .rule-pick-list {
  max-height: 260px;
  padding-right: 6px;
  overflow-y: auto;
}

.confirm-rule-block .rule-pick-list::-webkit-scrollbar {
  width: 8px;
}

.confirm-rule-block .rule-pick-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(34, 77, 105, 0.28);
}

.confirm-rule-block .rule-pick-list::-webkit-scrollbar-track {
  border-radius: 999px;
  background: rgba(34, 77, 105, 0.08);
}

.rule-pick {
  text-align: left;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(56, 44, 31, 0.08);
  background: rgba(255, 255, 255, 0.76);
  transition: all 0.25s ease;
}

.rule-pick:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(33, 37, 41, 0.08);
}

.rule-pick-active {
  border-color: rgba(34, 77, 105, 0.36);
  background: linear-gradient(180deg, rgba(235, 245, 250, 0.94), rgba(248, 251, 253, 0.98));
}

.rule-pick-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.rule-pick-meta {
  margin-top: 8px;
  color: #6f665d;
  font-size: 13px;
}

.rule-pick-desc {
  margin-top: 8px;
  color: #4f4b45;
  font-size: 13px;
  line-height: 1.7;
}

.confirm-rule-block {
  margin-top: 18px;
}

.confirm-rule-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 700;
  color: #4e473f;
}

.fs-btn {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fs-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.loading-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 6px;
  vertical-align: middle;
}

.confirm-overlay,
.progress-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  backdrop-filter: blur(12px);
}

.confirm-overlay {
  background: rgba(26, 23, 19, 0.34);
}

.progress-overlay {
  z-index: 85;
  background:
    radial-gradient(circle at top, rgba(34, 77, 105, 0.22), transparent 30%),
    rgba(24, 22, 19, 0.46);
}

.confirm-dialog,
.progress-dialog {
  width: min(100%, 620px);
  border-radius: 30px;
  box-shadow: 0 28px 80px rgba(15, 24, 33, 0.28);
}

.confirm-dialog {
  padding: 28px;
  border: 1px solid rgba(56, 44, 31, 0.12);
  background:
    radial-gradient(circle at top right, rgba(34, 77, 105, 0.1), transparent 28%),
    linear-gradient(180deg, rgba(255, 251, 245, 0.98), rgba(251, 245, 236, 0.98));
}

.confirm-dialog-danger {
  background:
    radial-gradient(circle at top right, rgba(139, 75, 75, 0.14), transparent 28%),
    linear-gradient(180deg, rgba(255, 248, 247, 0.98), rgba(252, 239, 237, 0.98));
}

.confirm-badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.confirm-badge-danger {
  background: var(--danger-soft);
  color: var(--danger);
}

.confirm-dialog h3,
.progress-dialog h3 {
  margin: 16px 0 10px;
  font-family: 'STSong', 'SimSun', serif;
  line-height: 1.2;
}

.confirm-dialog h3 {
  font-size: 28px;
}

.progress-dialog h3 {
  font-size: 30px;
}

.confirm-dialog p,
.progress-dialog p {
  margin: 0;
  font-size: 15px;
  line-height: 1.85;
}

.confirm-dialog p {
  color: #564c43;
}

.progress-dialog {
  padding: 30px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.16), transparent 28%),
    linear-gradient(145deg, rgba(34, 77, 105, 0.96), rgba(20, 49, 70, 0.96));
  color: #fff;
}

.progress-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.progress-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.progress-topline strong {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.84);
}

.progress-track {
  margin-top: 22px;
  height: 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #f7d9a1, #ffffff);
  transition: width 0.45s ease;
}

.progress-stage-list {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}

.progress-stage {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.72);
}

.progress-stage-index {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
  font-weight: 700;
}

.progress-stage-content {
  display: grid;
  gap: 4px;
}

.progress-stage-content strong {
  color: inherit;
}

.progress-stage-content span {
  font-size: 13px;
  line-height: 1.7;
}

.progress-stage.is-active {
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.96);
}

.progress-stage.is-completed {
  background: rgba(247, 217, 161, 0.2);
  color: rgba(255, 255, 255, 0.96);
}

.progress-stage.is-pending {
  opacity: 0.75;
}

.progress-stage.is-failed {
  background: rgba(183, 52, 52, 0.2);
  color: rgba(255, 237, 237, 0.96);
}

.progress-stats,
.confirm-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.progress-stat,
.confirm-tile {
  padding: 16px 18px;
  border-radius: 18px;
}

.progress-stat {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.confirm-tile {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(56, 44, 31, 0.08);
}

.progress-stat span,
.confirm-tile span {
  display: block;
  font-size: 12px;
  margin-bottom: 8px;
}

.progress-stat span {
  color: rgba(255, 255, 255, 0.68);
}

.confirm-tile span {
  color: var(--muted);
}

.progress-stat strong,
.confirm-tile strong {
  display: block;
  line-height: 1.5;
  word-break: break-word;
}

.progress-error {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(158, 43, 43, 0.24);
  color: #fff1f1;
}

.progress-actions,
.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 22px;
}

@media (max-width: 860px) {
  .essay-meta-grid,
  .progress-stats,
  .confirm-grid {
    grid-template-columns: 1fr;
  }

  .confirm-actions,
  .progress-actions,
  .progress-topline {
    flex-direction: column;
    align-items: stretch;
  }

  .essay-paragraph {
    grid-template-columns: repeat(12, 34px);
  }

  .confirm-dialog,
  .progress-dialog {
    padding: 22px;
    border-radius: 24px;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>

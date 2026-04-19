<template>
  <div v-if="reviewDetail" class="fs-grid fs-grid-two">
    <div class="fs-card">
      <div class="fs-card-header">
        <div class="fs-card-title">继续批改</div>
        <button class="fs-btn fs-btn-outline" @click="$router.back()">返回</button>
      </div>
      <div class="fs-card-body fs-stack">
        <div class="rer-review-hero">
          <h3>{{ reviewDetail.essayTitle || '未命名作文' }}</h3>
          <p>
            你正在基于第 {{ reviewDetail.reviewVersion || 1 }} 次批改结果，重新选择评分细则发起新一轮批改。
            系统会保留旧记录，新结果会作为新的批改版本写入历史。
          </p>
        </div>

        <div class="rer-meta-grid">
          <div class="rer-meta-item">
            <span>当前批改记录</span>
            <strong>#{{ reviewDetail.reviewId }}</strong>
          </div>
          <div class="rer-meta-item">
            <span>原评分细则</span>
            <strong>{{ reviewDetail.ruleName || '未记录细则' }}</strong>
          </div>
          <div class="rer-meta-item">
            <span>作文编号</span>
            <strong>{{ reviewDetail.essayId }}</strong>
          </div>
          <div class="rer-meta-item">
            <span>当前总分</span>
            <strong>{{ reviewDetail.totalScore != null ? `${reviewDetail.totalScore.toFixed(1)} 分` : '待评分' }}</strong>
          </div>
        </div>

        <div class="rer-rule-panel">
          <div class="rer-section-header">
            <div class="rer-section-title">选择新的评分细则</div>
            <button class="fs-btn fs-btn-outline fs-btn-sm" @click="goToRuleConfig">
              添加评分细则
            </button>
          </div>
          <div v-if="enabledRules.length" class="rule-pick-list" v-progressive-blur-scroll>
            <button
              v-for="rule in enabledRules"
              :key="rule.ruleId"
              class="rule-pick"
              :class="{ 'rule-pick-active': selectedRuleId === rule.ruleId }"
              @click="selectRule(rule.ruleId)"
            >
              <div class="rule-pick-head">
                <strong>{{ rule.ruleName }}</strong>
                <span
                  class="fs-tag"
                  :class="rule.ruleId === reviewDetail.ruleId ? 'fs-tag-warning' : 'fs-tag-success'"
                >
                  {{ rule.ruleId === reviewDetail.ruleId ? '当前所用' : '可选' }}
                </span>
              </div>
              <div class="rule-pick-meta">
                {{ rule.gradeLevel || '未设置学段' }} / {{ rule.reviewType || '通用作文' }}
              </div>
              <div v-if="rule.topicRequirement" class="rule-pick-desc">
                题目要求：{{ rule.topicRequirement }}
              </div>
              <div v-if="rule.customRequirement" class="rule-pick-desc">
                附加要求：{{ rule.customRequirement }}
              </div>
            </button>
          </div>
          <div v-else class="fs-empty">当前没有可用的评分细则，请先到配置页启用至少一条。</div>
        </div>
      </div>
    </div>

    <div class="fs-stack">
      <div class="fs-card">
        <div class="fs-card-header">
          <div class="fs-card-title">发起说明</div>
        </div>
        <div class="fs-card-body fs-stack">
          <div class="rer-meta-item">
            <span>本次将使用</span>
            <strong>{{ selectedRule?.ruleName || '尚未选择' }}</strong>
          </div>
          <div class="rer-meta-item">
            <span>批改流程</span>
            <strong>错字修改 -> 内容批改 -> 生成新版本</strong>
          </div>
          <div class="rer-meta-item">
            <span>结果保存方式</span>
            <strong>不会覆盖旧记录，会新增一条批改历史</strong>
          </div>
          <div class="rer-actions">
            <button class="fs-btn fs-btn-primary" :disabled="reviewing || !selectedRuleId" @click="confirmRereview">
              <span v-if="reviewing" class="loading-spinner"></span>
              {{ reviewing ? '提交中...' : '确认继续批改' }}
            </button>
            <button class="fs-btn fs-btn-outline" @click="goBackToReview">返回本次批改详情</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="fs-card">
    <div class="fs-empty">正在加载继续批改页面...</div>
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
          <strong>{{ reviewDetail?.essayTitle || '未命名作文' }}</strong>
        </div>
        <div class="progress-stat">
          <span>任务编号</span>
          <strong>{{ currentReviewId || '生成中' }}</strong>
        </div>
      </div>

      <div v-if="reviewError" class="progress-error">{{ reviewError }}</div>

      <div class="progress-actions">
        <button class="fs-btn fs-btn-outline" @click="closeReviewProgress">
          {{ reviewSucceeded ? '暂时留在这里' : '留在当前页' }}
        </button>
        <button v-if="reviewSucceeded" class="fs-btn fs-btn-primary" @click="goToNewReview">
          查看新的批改详情
        </button>
        <button v-else class="fs-btn fs-btn-primary" @click="goToReviewList">查看批改记录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const router = useRouter()

const reviewDetail = ref(null)
const enabledRules = ref([])
const selectedRuleId = ref(null)
const reviewing = ref(false)
const reviewProgressVisible = ref(false)
const currentReviewId = ref(null)
const currentProgressStage = ref(0)
const reviewSucceeded = ref(false)
const reviewError = ref('')

let progressTimers = []
let reviewPollTimer = null

const progressStages = [
  { key: 'prepare', title: '批改任务已创建', description: '系统已经记录新的细则选择，并开始创建任务。' },
  { key: 'correction', title: '错字修改处理中', description: '先处理基础表达问题，为后续内容评分做准备。' },
  { key: 'review', title: '内容批改生成中', description: '正在按新的评分细则生成得分、评语与修改建议。' },
  { key: 'done', title: '新批改版本已完成', description: '本轮继续批改已经结束，可以查看新的批改详情。' },
]

const selectedRule = computed(
  () => enabledRules.value.find((rule) => rule.ruleId === selectedRuleId.value) || null
)

const progressPercent = computed(() => {
  if (reviewError.value || reviewSucceeded.value) return 100
  return [18, 42, 74, 92][currentProgressStage.value] || 18
})

const progressStatusLabel = computed(() => {
  if (reviewError.value) return '批改失败'
  if (reviewSucceeded.value) return '批改完成'
  return '继续批改中'
})

const progressTitle = computed(() => {
  if (reviewError.value) return '这次继续批改没有成功完成'
  if (reviewSucceeded.value) return '新的批改版本已经生成'
  return progressStages[Math.min(currentProgressStage.value, 2)].title
})

const progressDescription = computed(() => {
  if (reviewError.value) return reviewError.value
  if (reviewSucceeded.value) return '系统已生成新的批改记录，你可以直接进入新版本详情查看结果。'
  return progressStages[Math.min(currentProgressStage.value, 2)].description
})

const selectRule = (ruleId) => {
  selectedRuleId.value = ruleId
}

const loadRules = async () => {
  const res = await http.get('/review/rules', {
    params: { enabledOnly: true },
  })
  enabledRules.value = res.data || []
}

const loadData = async () => {
  const reviewId = route.params.reviewId
  if (!reviewId) return
  const [detailRes] = await Promise.all([
    http.get(`/review/record/${reviewId}`),
    loadRules(),
  ])
  reviewDetail.value = detailRes.data || null
  if (!selectedRuleId.value) {
    selectedRuleId.value =
      enabledRules.value.find((rule) => rule.ruleId !== reviewDetail.value?.ruleId)?.ruleId ||
      enabledRules.value[0]?.ruleId ||
      null
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
    return
  }

  if (status === 2) {
    reviewSucceeded.value = true
    reviewError.value = ''
    currentProgressStage.value = 3
    clearProgressRuntime()
    return
  }

  if (status === 3 || status === 4) {
    currentProgressStage.value = 3
    clearProgressRuntime()
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
      } else if (status.status === 3 || status.status === 4) {
        reviewError.value = status.errorMsg || '批改任务执行失败，请稍后重试。'
        syncProgressFromStatus(status.status)
      }
    } catch (e) {
      reviewError.value = e.message || '轮询批改状态失败'
      clearProgressRuntime()
    }
  }, 2000)
}

const confirmRereview = async () => {
  if (!reviewDetail.value?.essayId || !selectedRuleId.value || reviewing.value) return
  try {
    reviewing.value = true
    const res = await http.post(`/review/essay/${reviewDetail.value.essayId}`, null, {
      params: { ruleId: selectedRuleId.value },
    })
    currentReviewId.value = res.data?.reviewId || null
    reviewProgressVisible.value = true
    beginProgressAnimation()
    pollReviewStatus()
  } catch (e) {
    alert(e.message || '发起继续批改失败')
  } finally {
    reviewing.value = false
  }
}

const closeReviewProgress = () => {
  reviewProgressVisible.value = false
}

const goToNewReview = () => {
  if (!currentReviewId.value) return
  router.push(`/reviews/${currentReviewId.value}`)
}

const goToReviewList = () => {
  if (!reviewDetail.value?.essayId) {
    router.push('/reviews')
    return
  }
  router.push({
    path: '/reviews',
    query: { essayId: reviewDetail.value.essayId },
  })
}

const goBackToReview = () => {
  if (!reviewDetail.value?.reviewId) return
  router.push(`/reviews/${reviewDetail.value.reviewId}`)
}

const goToRuleConfig = () => {
  router.push('/dimensions/create')
}

const stageClass = (index) => ({
  'is-completed': reviewSucceeded.value ? true : index < currentProgressStage.value,
  'is-active': !reviewSucceeded.value && index === currentProgressStage.value,
  'is-pending': !reviewSucceeded.value && index > currentProgressStage.value,
  'is-failed': !!reviewError.value,
})

onMounted(async () => {
  try {
    await loadData()
  } catch (e) {
    alert(e.message || '加载继续批改页面失败')
    router.back()
  }
})

onBeforeUnmount(clearProgressRuntime)
</script>

<style scoped>
.rer-review-hero {
  padding: 22px 24px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(34, 77, 105, 0.08), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(250, 245, 238, 0.96));
  border: 1px solid rgba(56, 44, 31, 0.08);
}

.rer-review-hero h3 {
  margin: 0 0 12px;
  font-family: 'STSong', 'SimSun', serif;
  font-size: 30px;
  line-height: 1.2;
}

.rer-review-hero p {
  margin: 0;
  color: #5b534a;
  line-height: 1.9;
}

.rer-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.rer-meta-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(56, 44, 31, 0.08);
}

.rer-meta-item span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.rer-meta-item strong {
  font-size: 15px;
  line-height: 1.7;
  font-weight: 600;
}

.rer-rule-panel {
  display: grid;
  gap: 10px;
}

.rer-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.rer-section-title {
  font-size: 14px;
  font-weight: 700;
  color: #4e473f;
}

.rule-pick-list {
  display: grid;
  gap: 10px;
  max-height: 520px;
  overflow-y: auto;
  padding-right: 6px;
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

.rer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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

.progress-overlay {
  position: fixed;
  inset: 0;
  z-index: 85;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  backdrop-filter: blur(12px);
  background:
    radial-gradient(circle at top, rgba(34, 77, 105, 0.22), transparent 30%),
    rgba(24, 22, 19, 0.46);
}

.progress-dialog {
  width: min(100%, 620px);
  padding: 30px;
  border-radius: 30px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.16), transparent 28%),
    linear-gradient(145deg, rgba(34, 77, 105, 0.96), rgba(20, 49, 70, 0.96));
  box-shadow: 0 28px 80px rgba(15, 24, 33, 0.28);
  color: #fff;
}

.progress-dialog h3 {
  margin: 16px 0 10px;
  font-family: 'STSong', 'SimSun', serif;
  font-size: 30px;
  line-height: 1.2;
}

.progress-dialog p {
  margin: 0;
  font-size: 15px;
  line-height: 1.85;
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

.progress-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.progress-stat {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.progress-stat span {
  display: block;
  font-size: 12px;
  margin-bottom: 8px;
  color: rgba(255, 255, 255, 0.68);
}

.progress-stat strong {
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

.progress-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 22px;
}

@media (max-width: 860px) {
  .rer-meta-grid,
  .progress-stats {
    grid-template-columns: 1fr;
  }

  .rer-section-header {
    align-items: stretch;
  }

  .progress-actions,
  .progress-topline {
    flex-direction: column;
    align-items: stretch;
  }

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

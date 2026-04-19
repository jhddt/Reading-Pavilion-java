<template>
  <div class="fs-grid">
    <div class="fs-card">
      <div class="fs-card-header">
        <div class="fs-card-title">筛选条件</div>
        <button class="fs-btn fs-btn-outline" @click="loadData">刷新记录</button>
      </div>
      <div class="fs-card-body">
        <div class="filter-grid">
          <div class="fs-form-item">
            <label class="fs-form-label">批改状态</label>
            <select v-model="status" class="fs-select">
              <option :value="null">全部</option>
              <option :value="1">处理中</option>
              <option :value="2">成功</option>
              <option :value="3">失败</option>
            </select>
          </div>
          <div class="fs-form-item">
            <label class="fs-form-label">评审者类型</label>
            <select v-model="reviewerType" class="fs-select">
              <option :value="null">全部</option>
              <option :value="0">AI</option>
              <option :value="1">教师</option>
            </select>
          </div>
          <div class="fs-stat-card">
            <span>当前页记录</span>
            <strong>{{ records.length }}</strong>
          </div>
          <div class="fs-stat-card">
            <span>成功任务</span>
            <strong>{{ successCount }}</strong>
          </div>
        </div>
      </div>
    </div>

    <div class="fs-card">
      <div class="fs-card-body">
        <div v-if="records.length" class="review-list">
          <div v-for="(item, index) in records" :key="item.reviewId" class="review-item stagger-item hover-lift" :style="{ animationDelay: `${index * 0.05}s` }">
            <div class="review-item__main">
              <div class="review-item__title">{{ item.essayTitle || '未命名作文' }}</div>
              <div class="review-item__meta">
                第 {{ item.reviewVersion || 1 }} 次批改 · {{ formatDate(item.startTime) }} · {{ item.modelVersion || '未记录模型' }}
              </div>
              <button class="review-item__rule" type="button" @click.stop="openRuleDetail(item.reviewId)">
                <strong>{{ item.ruleName || '未记录细则' }}</strong>
                <span>{{ item.gradeLevel || '未设置学段' }} / {{ item.reviewType || '通用作文' }}</span>
                <em>点击查看完整细则</em>
              </button>
              <div class="review-item__chips">
                <span class="fs-tag" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
                <span class="fs-tag" :class="item.reviewerType === 0 ? 'fs-tag-primary' : 'fs-tag-success'">
                  {{ item.reviewerType === 0 ? 'AI评审' : '教师评审' }}
                </span>
                <span v-if="item.latestVersion" class="fs-tag fs-tag-warning">
                  当前最新
                </span>
                <span class="fs-tag fs-tag-neutral">
                  {{ item.totalScore != null ? `${item.totalScore.toFixed(1)} 分` : '待评分' }}
                </span>
              </div>
            </div>

            <div class="review-item__aside">
              <div class="review-item__stats">
                <div class="mini-stat">
                  <span>记录编号</span>
                  <strong>{{ item.reviewId }}</strong>
                </div>
                <div class="mini-stat">
                  <span>总分</span>
                  <strong>{{ item.totalScore != null ? item.totalScore.toFixed(0) : '-' }}</strong>
                </div>
                <div class="mini-stat">
                  <span>状态</span>
                  <strong>{{ statusText(item.status) }}</strong>
                </div>
                <div class="mini-stat">
                  <span>来源</span>
                  <strong>{{ item.reviewerType === 0 ? '模型' : '人工' }}</strong>
                </div>
                <div class="mini-stat">
                  <span>细则</span>
                  <strong>{{ item.ruleName || '-' }}</strong>
                </div>
                <div class="mini-stat">
                  <span>版本</span>
                  <strong>V{{ item.reviewVersion || 1 }}</strong>
                </div>
              </div>

              <div class="review-actions">
                <button class="fs-btn fs-btn-outline fs-btn-sm btn-ripple" @click.stop="openRuleDetail(item.reviewId)">查看评分细则</button>
                <button class="fs-btn fs-btn-outline fs-btn-sm btn-ripple" @click.stop="goToDetail(item)">查看详情</button>
                <button class="fs-btn fs-btn-danger fs-btn-sm btn-ripple" @click.stop="confirmDelete(item.reviewId)">删除记录</button>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="fs-empty">
          <div class="fs-empty-icon">📋</div>
          <div class="fs-empty-text">暂无批改记录</div>
        </div>

        <div v-if="records.length" class="fs-pagination">
          <span class="fs-pagination-info">第 {{ page }} 页</span>
          <button class="fs-pagination-btn" :disabled="page === 1" @click="changePage(page - 1)">上一页</button>
          <button class="fs-pagination-btn" :disabled="!hasMore" @click="changePage(page + 1)">下一页</button>
        </div>

        <div v-if="error" class="fs-form-error" style="margin-top: 12px">
          {{ error }}
        </div>
      </div>
    </div>
  </div>

  <div v-if="ruleDialogVisible" class="confirm-overlay" @click.self="closeRuleDetail">
    <div class="confirm-dialog review-rule-dialog">
      <div class="confirm-badge">细则详情</div>
      <h3>{{ selectedRuleDetail?.ruleName || '未记录细则' }}</h3>
      <p>
        {{ selectedRuleDetail?.gradeLevel || '未设置学段' }} / {{ selectedRuleDetail?.reviewType || '通用作文' }}
      </p>

      <div v-if="ruleDialogLoading" class="fs-empty" style="padding: 24px 0">正在加载细则详情...</div>

      <div v-else class="rule-detail-grid">
        <div v-if="selectedRuleDetail?.topicRequirement" class="rule-detail-block">
          <span>题目要求</span>
          <strong>{{ selectedRuleDetail?.topicRequirement }}</strong>
        </div>
        <div v-if="selectedRuleDetail?.beautifyLevel" class="rule-detail-block">
          <span>润色等级</span>
          <strong>{{ selectedRuleDetail?.beautifyLevel }}</strong>
        </div>
        <div v-if="selectedRuleDetail?.customRequirement" class="rule-detail-block rule-detail-block--full">
          <span>附加要求</span>
          <strong>{{ selectedRuleDetail?.customRequirement }}</strong>
        </div>
      </div>

      <div
        v-if="
          !ruleDialogLoading &&
          !selectedRuleDetail?.topicRequirement &&
          !selectedRuleDetail?.beautifyLevel &&
          !selectedRuleDetail?.customRequirement
        "
        class="fs-empty"
        style="padding: 24px 0"
      >
        当前评分细则暂无可展示的要求
      </div>

      <div class="confirm-actions" style="margin-top: 18px">
        <button class="fs-btn fs-btn-outline" @click="closeRuleDetail">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const router = useRouter()

const records = ref([])
const page = ref(1)
const pageSize = ref(10)
const hasMore = ref(false)
const status = ref(null)
const reviewerType = ref(null)
const error = ref('')
const ruleDialogVisible = ref(false)
const ruleDialogLoading = ref(false)
const selectedRuleDetail = ref(null)

const successCount = computed(() => records.value.filter((item) => item.status === 2).length)

const statusText = (s) => {
  const map = {
    0: '初始化',
    1: '处理中',
    2: '成功',
    3: '失败',
    4: '超时',
  }
  return map[s] || '未知'
}

const statusClass = (s) => {
  const map = {
    0: 'fs-tag-neutral',
    1: 'fs-tag-primary',
    2: 'fs-tag-success',
    3: 'fs-tag-danger',
    4: 'fs-tag-danger',
  }
  return map[s] || 'fs-tag-neutral'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '暂无时间'
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const loadData = async () => {
  error.value = ''
  try {
    const res = await http.get('/review/records', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        status: status.value ?? undefined,
        reviewerType: reviewerType.value ?? undefined,
        essayId: route.query.essayId ?? undefined,
      },
    })
    const data = res.data || {}
    records.value = data.records || data.rows || []
    const total = data.total || 0
    hasMore.value = page.value * pageSize.value < total
  } catch (e) {
    error.value = e.message || '加载失败'
  }
}

const changePage = (p) => {
  page.value = p
  loadData()
}

const confirmDelete = async (reviewId) => {
  if (!window.confirm('确定要删除这条批改记录吗？')) return
  error.value = ''
  try {
    await http.delete(`/review/record/${reviewId}`)
    await loadData()
  } catch (e) {
    error.value = e.message || '删除失败'
  }
}

const goToDetail = (item) => {
  router.push(`/reviews/${item.reviewId}`)
}

const openRuleDetail = async (reviewId) => {
  if (!reviewId) return
  ruleDialogVisible.value = true
  ruleDialogLoading.value = true
  selectedRuleDetail.value = null
  try {
    const res = await http.get(`/review/record/${reviewId}`)
    selectedRuleDetail.value = res.data || null
  } catch (e) {
    error.value = e.message || '加载细则详情失败'
  } finally {
    ruleDialogLoading.value = false
  }
}

const closeRuleDetail = () => {
  ruleDialogVisible.value = false
  ruleDialogLoading.value = false
  selectedRuleDetail.value = null
}

watch([status, reviewerType], () => {
  page.value = 1
  loadData()
})

watch(
  () => route.query.essayId,
  () => {
    page.value = 1
    loadData()
  }
)

onMounted(loadData)
</script>

<style scoped>
.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.fs-stat-card {
  transition: all 0.3s ease;
}

.fs-stat-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.review-list {
  display: grid;
  gap: 14px;
}

.review-item {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
  gap: 14px;
  align-items: stretch;
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(56, 44, 31, 0.08);
  cursor: pointer;
}

.review-item__main {
  display: grid;
  gap: 12px;
}

.review-item__title {
  font-size: 18px;
  font-weight: 700;
  transition: all 0.3s ease;
}

.review-item:hover .review-item__title {
  color: var(--brand);
  transform: translateX(4px);
}

.review-item__meta {
  color: var(--muted);
  font-size: 13px;
}

.review-item__rule {
  display: grid;
  gap: 4px;
  text-align: left;
  width: 100%;
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid rgba(56, 44, 31, 0.08);
  background: rgba(247, 242, 235, 0.78);
  transition: all 0.25s ease;
  cursor: pointer;
}

.review-item__rule:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(33, 37, 41, 0.08);
  border-color: rgba(34, 77, 105, 0.18);
}

.review-item__rule strong {
  font-size: 15px;
  font-weight: 700;
}

.review-item__rule span {
  color: var(--muted);
  font-size: 13px;
}

.review-item__rule em {
  font-style: normal;
  color: var(--brand);
  font-size: 12px;
  font-weight: 600;
}

.review-item__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.fs-tag {
  transition: all 0.2s ease;
}

.fs-tag:hover {
  transform: scale(1.05);
}

.review-item__aside {
  display: grid;
  gap: 12px;
  align-content: space-between;
  padding: 18px;
  border-radius: 20px;
  background: rgba(244, 237, 228, 0.75);
  border: 1px solid rgba(56, 44, 31, 0.08);
  transition: all 0.3s ease;
}

.review-item:hover .review-item__aside {
  background: rgba(244, 237, 228, 0.95);
}

.review-item__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mini-stat {
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(56, 44, 31, 0.08);
  transition: all 0.3s ease;
}

.mini-stat:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.95);
}

.mini-stat span {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.mini-stat strong {
  font-size: 18px;
  transition: all 0.2s ease;
}

.mini-stat:hover strong {
  color: var(--brand);
  transform: scale(1.1);
  display: inline-block;
}

.review-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.fs-btn {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fs-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.fs-btn:active:not(:disabled) {
  transform: translateY(0);
}

.fs-empty {
  animation: fadeInUp 0.6s ease-out;
}

.fs-empty-icon {
  font-size: 64px;
  animation: bounce 2s ease-in-out infinite;
}

.review-rule-dialog {
  width: min(100%, 640px);
}

.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(26, 23, 19, 0.36);
  backdrop-filter: blur(12px);
}

.confirm-dialog {
  width: min(100%, 640px);
  padding: 28px;
  border-radius: 28px;
  border: 1px solid rgba(56, 44, 31, 0.12);
  background:
    radial-gradient(circle at top right, rgba(34, 77, 105, 0.1), transparent 28%),
    linear-gradient(180deg, rgba(255, 251, 245, 0.98), rgba(251, 245, 236, 0.98));
  box-shadow: 0 28px 80px rgba(15, 24, 33, 0.28);
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

.confirm-dialog h3 {
  margin: 16px 0 10px;
  font-family: 'STSong', 'SimSun', serif;
  font-size: 28px;
  line-height: 1.2;
}

.confirm-dialog p {
  margin: 0;
  color: #564c43;
  line-height: 1.8;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.rule-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.rule-detail-block {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(56, 44, 31, 0.08);
}

.rule-detail-block--full {
  grid-column: 1 / -1;
}

.rule-detail-block span {
  display: block;
  margin-bottom: 6px;
  color: var(--muted);
  font-size: 12px;
}

.rule-detail-block strong {
  display: block;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

@media (max-width: 1280px) {
  .filter-grid,
  .review-item,
  .review-item__stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .rule-detail-grid {
    grid-template-columns: 1fr;
  }

  .confirm-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

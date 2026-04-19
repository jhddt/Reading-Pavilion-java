<template>
  <div class="fs-grid">
    <div class="fs-card">
      <div class="fs-card-header">
        <div class="fs-card-title">作文列表</div>
        <div class="fs-toolbar" style="margin-bottom: 0">
          <button class="fs-btn fs-btn-outline" @click="loadData">刷新列表</button>
          <button class="fs-btn fs-btn-primary" @click="$router.push('/essays/create')">新建作文</button>
        </div>
      </div>
      <div class="fs-card-body">
        <div class="fs-stat-grid">
          <div class="fs-stat-card">
            <span>当前页数量</span>
            <strong>{{ essays.length }}</strong>
          </div>
          <div class="fs-stat-card">
            <span>草稿数量</span>
            <strong>{{ draftCount }}</strong>
          </div>
          <div class="fs-stat-card">
            <span>已批改数量</span>
            <strong>{{ reviewedCount }}</strong>
          </div>
        </div>
      </div>
    </div>

    <div class="fs-card">
      <div class="fs-card-body">
        <div v-if="essays.length" class="essay-list">
          <div v-for="(item, index) in essays" :key="item.id" class="essay-item stagger-item hover-lift" :style="{ animationDelay: `${index * 0.05}s` }">
            <div class="essay-item__main">
              <div class="essay-item__title">{{ item.title || '未命名作文' }}</div>
              <div class="essay-item__meta">
                {{ submitTypeText(item.submitType) }} · {{ item.wordCount || 0 }} 字 · {{ formatDate(item.createTime) }}
              </div>
              <div class="essay-item__chips">
                <span class="fs-tag" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
                <span class="fs-tag fs-tag-primary">{{ submitTypeText(item.submitType) }}</span>
                <span v-if="item.wordCount" class="fs-tag fs-tag-neutral">{{ item.wordCount }} 字</span>
              </div>
            </div>

            <div class="essay-item__aside">
              <div class="essay-item__stats">
                <div class="mini-stat">
                  <span>作文编号</span>
                  <strong>{{ item.id }}</strong>
                </div>
                <div class="mini-stat">
                  <span>状态</span>
                  <strong>{{ statusText(item.status) }}</strong>
                </div>
                <div class="mini-stat">
                  <span>创建时间</span>
                  <strong>{{ shortDate(item.createTime) }}</strong>
                </div>
                <div class="mini-stat">
                  <span>当前阶段</span>
                  <strong>{{ statusText(item.status) }}</strong>
                </div>
              </div>

              <div class="essay-actions">
                <button class="fs-btn fs-btn-primary fs-btn-sm btn-ripple essay-action-detail" @click="viewDetail(item)">查看详情</button>
                <button class="fs-btn fs-btn-sm btn-ripple essay-action-review" @click="viewReviews(item)">批改记录</button>
                <button
                  v-if="item.status === 0"
                  class="fs-btn fs-btn-sm btn-ripple essay-action-delete"
                  @click="onDelete(item)"
                >
                  删除草稿
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="fs-empty">
          <div class="fs-empty-icon">📝</div>
          <div class="fs-empty-text">暂无作文，点击右上角新建作文</div>
        </div>

        <div v-if="essays.length" class="fs-pagination">
          <span class="fs-pagination-info">第 {{ page }} 页</span>
          <button class="fs-pagination-btn" :disabled="page === 1" @click="changePage(page - 1)">
            上一页
          </button>
          <button class="fs-pagination-btn" :disabled="!hasMore" @click="changePage(page + 1)">
            下一页
          </button>
        </div>

        <div v-if="error" class="fs-form-error" style="margin-top: 12px">
          {{ error }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()

const essays = ref([])
const page = ref(1)
const pageSize = ref(10)
const hasMore = ref(false)
const error = ref('')

const draftCount = computed(() => essays.value.filter((item) => item.status === 0).length)
const reviewedCount = computed(() => essays.value.filter((item) => item.status === 3).length)

const statusText = (status) => {
  const map = {
    0: '草稿',
    1: '已提交',
    2: '批改中',
    3: '已批改',
    4: '已归档',
  }
  return map[status] || '未知'
}

const statusClass = (status) => {
  const map = {
    0: 'fs-tag-warning',
    1: 'fs-tag-primary',
    2: 'fs-tag-neutral',
    3: 'fs-tag-success',
    4: 'fs-tag-neutral',
  }
  return map[status] || 'fs-tag-neutral'
}

const submitTypeText = (type) => {
  const map = {
    0: '图片录入',
    1: '文档导入',
    2: '文本输入',
  }
  return map[type] || '未知来源'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '暂无时间'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const shortDate = (dateStr) => {
  if (!dateStr) return '暂无'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const loadData = async () => {
  error.value = ''
  try {
    const res = await http.get('/essay/list', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
      },
    })
    const data = res.data || {}
    essays.value = data.records || data.rows || []
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

const viewDetail = (item) => {
  router.push(`/essays/${item.id}`)
}

const viewReviews = (item) => {
  router.push({
    path: '/reviews',
    query: { essayId: item.id },
  })
}

const onDelete = async (item) => {
  if (!item.id) return
  if (!window.confirm(`确定删除草稿「${item.title}」吗？`)) return
  try {
    await http.delete(`/essay/${item.id}`)
    await loadData()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.essay-list {
  display: grid;
  gap: 14px;
}

.essay-item {
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

.essay-item__main {
  display: grid;
  gap: 12px;
}

.essay-item__title {
  font-size: 18px;
  font-weight: 700;
  transition: all 0.3s ease;
}

.essay-item:hover .essay-item__title {
  color: var(--brand);
  transform: translateX(4px);
}

.essay-item__meta {
  color: var(--muted);
  font-size: 13px;
}

.essay-item__chips {
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

.essay-item__aside {
  display: grid;
  gap: 12px;
  align-content: space-between;
  padding: 18px;
  border-radius: 20px;
  background: rgba(244, 237, 228, 0.75);
  border: 1px solid rgba(56, 44, 31, 0.08);
  transition: all 0.3s ease;
}

.essay-item:hover .essay-item__aside {
  background: rgba(244, 237, 228, 0.95);
}

.essay-item__stats {
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

.essay-actions {
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

.fs-stat-card {
  transition: all 0.3s ease;
}

.fs-stat-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.fs-empty {
  animation: fadeInUp 0.6s ease-out;
}

.fs-empty-icon {
  font-size: 64px;
  animation: bounce 2s ease-in-out infinite;
}

.essay-action-detail {
  background: linear-gradient(135deg, var(--brand), var(--brand-deep));
  color: #fff;
  box-shadow: 0 10px 18px rgba(34, 77, 105, 0.18);
}

.essay-action-review {
  background: rgba(255, 255, 255, 0.92);
  color: var(--ink);
  border: 1px solid rgba(56, 44, 31, 0.12);
}

.essay-action-review:hover:not(:disabled) {
  background: var(--gold-soft);
  color: #7b5d18;
  border-color: rgba(168, 132, 56, 0.28);
}

.essay-action-delete {
  background: linear-gradient(135deg, #b46a6a, var(--danger));
  color: #fff;
  box-shadow: 0 10px 18px rgba(139, 75, 75, 0.16);
}

@media (max-width: 1280px) {
  .essay-item,
  .essay-item__stats {
    grid-template-columns: 1fr;
  }
}
</style>

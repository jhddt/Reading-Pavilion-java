<template>
  <div class="rp-card" v-if="detail">
    <div class="rp-card-header">
      <div class="rp-card-title">
        批改详情 · ID {{ detail.reviewId }} （作文 {{ detail.essayId }}）
      </div>
      <button class="rp-btn rp-btn-outline" @click="$router.back()">返回</button>
    </div>

    <div style="margin-bottom: 12px; font-size: 13px">
      <div>
        <strong>总分：</strong>
        <span v-if="detail.totalScore != null">{{ detail.totalScore.toFixed(2) }}</span>
        <span v-else>-</span>
      </div>
      <div style="margin-top: 4px">
        <strong>批改版本：</strong>
        第 {{ detail.reviewVersion || 1 }} 次
        <span v-if="detail.latestVersion"> · 当前最新</span>
      </div>
      <div style="margin-top: 4px">
        <strong>评分细则：</strong>
        {{ detail.ruleName || '-' }} · {{ detail.gradeLevel || '未设置学段' }} / {{ detail.reviewType || '通用作文' }}
      </div>
      <div style="margin-top: 4px">
        <strong>模型版本：</strong>
        {{ detail.modelVersion || '-' }} ·
        <strong>评审者：</strong>
        {{ detail.reviewerType === 0 ? 'AI' : '教师' }}
      </div>
      <div style="margin-top: 4px">
        <strong>开始时间：</strong>
        {{ detail.startTime || '-' }} ·
        <strong>结束时间：</strong>
        {{ detail.endTime || '-' }}
      </div>
    </div>

    <div
      v-if="detail.customRequirement || detail.topicRequirement || detail.beautifyLevel"
      style="margin-bottom: 12px; padding: 12px; border-radius: 12px; background: #f8fafc; border: 1px solid #e5e7eb; font-size: 13px"
    >
      <div v-if="detail.topicRequirement"><strong>题目要求：</strong>{{ detail.topicRequirement }}</div>
      <div v-if="detail.beautifyLevel" style="margin-top: 4px"><strong>润色等级：</strong>{{ detail.beautifyLevel }}</div>
      <div v-if="detail.customRequirement" style="margin-top: 4px"><strong>附加要求：</strong>{{ detail.customRequirement }}</div>
    </div>

    <div style="display: flex; gap: 10px; margin-bottom: 12px">
      <button class="rp-btn rp-btn-primary" @click="goToRereview">
        更换细则再次批改
      </button>
      <button class="rp-btn rp-btn-outline" @click="goToEssay">
        查看作文原文
      </button>
    </div>

    <h4 style="margin: 12px 0 8px; font-size: 14px">历史版本</h4>
    <div v-if="history.length" style="display: grid; gap: 10px; margin-bottom: 12px">
      <div
        v-for="item in history"
        :key="item.reviewId"
        class="rp-card"
        style="padding: 10px 12px; box-shadow: none; border: 1px solid #e5e7eb; cursor: pointer"
        @click="goToReview(item.reviewId)"
      >
        <div style="display: flex; align-items: center; justify-content: space-between; gap: 12px; font-size: 13px">
          <strong>第 {{ item.reviewVersion || 1 }} 次 · {{ item.ruleName || '未记录细则' }}</strong>
          <span v-if="item.latestVersion" style="color: #b45309">当前最新</span>
        </div>
        <div style="margin-top: 6px; font-size: 12px; color: #6b7280">
          {{ item.gradeLevel || '未设置学段' }} / {{ item.reviewType || '通用作文' }} ·
          {{ formatDate(item.startTime) }} ·
          {{ statusText(item.status) }} ·
          {{ item.totalScore != null ? `${item.totalScore.toFixed(1)} 分` : '待评分' }}
        </div>
      </div>
    </div>
    <div v-else class="rp-empty">暂无历史版本</div>

    <h4 style="margin: 12px 0 8px; font-size: 14px">各维度得分</h4>
    <table class="rp-table" v-if="detail.scores && detail.scores.length">
      <thead>
        <tr>
          <th>维度</th>
          <th>得分</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in detail.scores" :key="s.dimensionId">
          <td>{{ s.dimensionName }}</td>
          <td>{{ s.score != null ? s.score.toFixed(2) : '-' }}</td>
        </tr>
      </tbody>
    </table>
    <div v-else class="rp-empty">暂无维度得分（可能未配置评分维度）</div>

    <h4 style="margin: 16px 0 8px; font-size: 14px">AI 评语</h4>
    <div v-if="detail.comments && detail.comments.length" style="display: grid; gap: 10px">
      <div
        v-for="c in detail.comments"
        :key="c.commentId"
        class="rp-card"
        style="padding: 10px 12px; box-shadow: none; border: 1px solid #e5e7eb"
      >
        <div v-if="c.commentType !== 1" style="font-size: 13px; font-weight: 600; margin-bottom: 6px">
          <span v-if="c.commentType === 2">【改进建议】</span>
          <span v-else-if="c.commentType === 3">【修改意见】</span>
          <span v-else>【其他】</span>
        </div>
        <div style="white-space: pre-wrap; font-size: 13px">
          {{ formatCommentContent(c.commentType, c.content) }}
        </div>
      </div>
    </div>
    <div v-else class="rp-empty">暂无评语</div>

    <h4 style="margin: 16px 0 8px; font-size: 14px">文本纠错</h4>
    <div
      v-if="detail.textCorrections && detail.textCorrections.length"
      style="display: grid; gap: 10px"
    >
      <div
        v-for="(tc, index) in detail.textCorrections"
        :key="index"
        class="rp-card"
        style="padding: 10px 12px; box-shadow: none; border: 1px solid #e5e7eb"
      >
        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px">
          <span
            style="
              display: inline-block;
              padding: 2px 8px;
              font-size: 12px;
              border-radius: 4px;
              background: #fef3c7;
              color: #92400e;
            "
          >
            {{ getErrorTypeLabel(tc.errorType) }}
          </span>
          <span style="font-size: 12px; color: #6b7280">
            位置: {{ tc.startOffset }}-{{ tc.endOffset }}
          </span>
        </div>
        <div style="display: flex; gap: 12px; align-items: center; margin-bottom: 6px">
          <div style="flex: 1">
            <div style="font-size: 12px; color: #6b7280; margin-bottom: 2px">原文</div>
            <div
              style="
                padding: 6px 8px;
                background: #fee2e2;
                border-radius: 4px;
                font-size: 13px;
                text-decoration: line-through;
                color: #991b1b;
              "
            >
              {{ tc.originalText }}
            </div>
          </div>
          <div style="font-size: 18px; color: #9ca3af">→</div>
          <div style="flex: 1">
            <div style="font-size: 12px; color: #6b7280; margin-bottom: 2px">修改建议</div>
            <div
              style="
                padding: 6px 8px;
                background: #d1fae5;
                border-radius: 4px;
                font-size: 13px;
                color: #065f46;
              "
            >
              {{ tc.correctedText }}
            </div>
          </div>
        </div>
        <div v-if="tc.suggestion" style="font-size: 12px; color: #6b7280; margin-top: 6px">
          💡 {{ tc.suggestion }}
        </div>
      </div>
    </div>
    <div v-else class="rp-empty">暂无纠错记录</div>
  </div>
  <div v-else class="rp-empty">正在加载批改详情...</div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const history = ref([])

const loadData = async () => {
  const reviewId = route.params.reviewId
  if (!reviewId) return
  try {
    // GET /review/record/{reviewId}
    const res = await http.get(`/review/record/${reviewId}`)
    detail.value = res.data || null
    if (detail.value?.essayId) {
      const historyRes = await http.get(`/review/essay/${detail.value.essayId}/records`)
      history.value = historyRes.data || []
    } else {
      history.value = []
    }
  } catch (e) {
    alert(e.message || '加载失败')
  }
}

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

const formatDate = (dateStr) => {
  if (!dateStr) return '暂无时间'
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const goToReview = (reviewId) => {
  if (!reviewId || String(reviewId) === String(route.params.reviewId)) return
  router.push(`/reviews/${reviewId}/summary`)
}

const goToEssay = () => {
  if (!detail.value?.essayId) return
  router.push(`/essays/${detail.value.essayId}`)
}

const goToRereview = () => {
  if (!detail.value?.reviewId) return
  router.push(`/reviews/${detail.value.reviewId}/rerun`)
}

const getErrorTypeLabel = (type) => {
  const labels = {
    spelling: '错别字',
    grammar: '语法错误',
    punctuation: '标点错误',
    word_choice: '用词不当',
    redundancy: '重复冗余',
    style: '文风问题',
  }
  return labels[type] || type || '其他'
}

const formatCommentContent = (commentType, content) => {
  if (!content) return ''
  if (commentType !== 1) return content
  return content.replace(/^【总评】\s*/u, '').trim()
}

onMounted(loadData)
watch(
  () => route.params.reviewId,
  () => loadData()
)
</script>

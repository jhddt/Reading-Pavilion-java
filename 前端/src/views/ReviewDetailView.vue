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
        <strong>模型版本：</strong>{{ detail.modelVersion || '-' }}
        · <strong>评审者：</strong>{{ detail.reviewerType === 0 ? 'AI' : '教师' }}
      </div>
      <div style="margin-top: 4px">
        <strong>开始时间：</strong>{{ detail.startTime || '-' }} ·
        <strong>结束时间：</strong>{{ detail.endTime || '-' }}
      </div>
    </div>

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
        <div style="font-size: 13px; font-weight: 600; margin-bottom: 6px">
          <span v-if="c.commentType === 1">【总评】</span>
          <span v-else-if="c.commentType === 2">【改进建议】</span>
          <span v-else-if="c.commentType === 3">【修改意见】</span>
          <span v-else>【其他】</span>
        </div>
        <div style="white-space: pre-wrap; font-size: 13px">
          {{ c.content }}
        </div>
      </div>
    </div>
    <div v-else class="rp-empty">暂无评语</div>

    <h4 style="margin: 16px 0 8px; font-size: 14px">文本纠错</h4>
    <div v-if="detail.textCorrections && detail.textCorrections.length" style="display: grid; gap: 10px">
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
  <div v-else class="rp-empty">
    正在加载批改详情...
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import http from '../api/http';

const route = useRoute();
const detail = ref(null);

const loadData = async () => {
  const reviewId = route.params.reviewId;
  if (!reviewId) return;
  try {
    // GET /review/record/{reviewId}
    const res = await http.get(`/review/record/${reviewId}`);
    detail.value = res.data || null;
  } catch (e) {
    alert(e.message || '加载失败');
  }
};

const getErrorTypeLabel = (type) => {
  const labels = {
    spelling: '错别字',
    grammar: '语法错误',
    punctuation: '标点错误',
    word_choice: '用词不当',
    redundancy: '重复冗余',
    style: '文风问题',
  };
  return labels[type] || type || '其他';
};

onMounted(loadData);
watch(
  () => route.params.reviewId,
  () => loadData(),
);
</script>


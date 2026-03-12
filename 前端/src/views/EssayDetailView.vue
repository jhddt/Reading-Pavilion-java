<template>
  <div class="rp-card" v-if="essay">
    <div class="rp-card-header">
      <div class="rp-card-title">作文详情 · ID {{ essay.id }}</div>
      <button class="rp-btn rp-btn-outline" @click="$router.back()">返回</button>
    </div>

    <div style="margin-bottom: 16px">
      <div style="display: flex; gap: 16px; margin-bottom: 12px">
        <div style="flex: 1">
          <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px">标题</div>
          <div style="font-size: 16px; font-weight: 600">{{ essay.title }}</div>
        </div>
        <div>
          <div style="font-size: 12px; color: #6b7280; margin-bottom: 4px">状态</div>
          <span class="rp-badge rp-badge-neutral">{{ statusText(essay.status) }}</span>
        </div>
      </div>

      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; font-size: 13px">
        <div>
          <span style="color: #6b7280">字数：</span>
          <span>{{ essay.wordCount || 0 }}</span>
        </div>
        <div>
          <span style="color: #6b7280">提交方式：</span>
          <span>{{ submitTypeText(essay.submitType) }}</span>
        </div>
        <div>
          <span style="color: #6b7280">创建时间：</span>
          <span>{{ essay.createTime || '-' }}</span>
        </div>
      </div>
    </div>

    <h4 style="margin: 16px 0 8px; font-size: 14px; font-weight: 600">作文内容</h4>
    <div
      style="
        padding: 16px;
        background: #f9fafb;
        border-radius: 8px;
        white-space: pre-wrap;
        line-height: 1.8;
        font-size: 14px;
        max-height: 600px;
        overflow-y: auto;
      "
    >
      {{ essay.finalContent || essay.originalContent || '暂无内容' }}
    </div>

    <div style="margin-top: 16px; display: flex; gap: 8px">
      <button
        class="rp-btn rp-btn-primary"
        @click="onReview"
        :disabled="reviewing"
      >
        {{ reviewing ? '批改中...' : '发起批改' }}
      </button>
      <button
        class="rp-btn rp-btn-outline"
        @click="viewReviews"
      >
        查看批改记录
      </button>
      <button
        v-if="essay.status === 0"
        class="rp-btn rp-btn-outline"
        @click="onSubmit"
      >
        提交作文
      </button>
      <button
        v-if="essay.status === 1"
        class="rp-btn rp-btn-outline"
        @click="onWithdraw"
      >
        撤回作文
      </button>
      <button
        v-if="essay.status === 0"
        class="rp-btn rp-btn-outline"
        style="margin-left: auto"
        @click="onDelete"
      >
        删除草稿
      </button>
    </div>
  </div>
  <div v-else class="rp-empty">
    正在加载作文详情...
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import http from '../api/http';

const route = useRoute();
const router = useRouter();
const essay = ref(null);
const reviewing = ref(false);

const statusText = (status) => {
  if (status === 0) return '草稿';
  if (status === 1) return '已提交';
  if (status === 2) return '批改中';
  if (status === 3) return '已批改';
  if (status === 4) return '已归档';
  return '未知';
};

const submitTypeText = (type) => {
  if (type === 0) return '图片';
  if (type === 1) return '文档';
  if (type === 2) return '文本';
  return '未知';
};

const loadData = async () => {
  const essayId = route.params.id;
  if (!essayId) return;
  try {
    // GET /essay/{id}
    const res = await http.get(`/essay/${essayId}`);
    essay.value = res.data || null;
  } catch (e) {
    alert(e.message || '加载失败');
    router.back();
  }
};

const onReview = async () => {
  if (!essay.value?.id || reviewing.value) return;
  if (!window.confirm(`确定对作文「${essay.value.title}」发起 AI 批改吗？\n\n批改将在后台异步进行，请稍后在「批改记录」中查看结果。`)) return;
  try {
    reviewing.value = true;
    const res = await http.post(`/review/essay/${essay.value.id}`);
    const reviewId = res.data?.reviewId;
    if (reviewId) {
      alert(`批改已发起（ID: ${reviewId}），正在后台处理中...\n\n请稍后在「批改记录」中查看结果。`);
    } else {
      alert('批改已发起，正在后台处理中...\n\n请稍后在「批改记录」中查看结果。');
    }
  } catch (e) {
    alert(e.message || '发起批改失败');
  } finally {
    reviewing.value = false;
  }
};

const viewReviews = () => {
  router.push({
    path: '/reviews',
    query: { essayId: essay.value.id },
  });
};

const onSubmit = async () => {
  if (!essay.value?.id) return;
  if (essay.value.status !== 0) {
    alert('只有草稿状态的作文可以提交');
    return;
  }
  if (!window.confirm(`确定提交作文「${essay.value.title}」吗？提交后将变为已提交状态。`)) return;
  try {
    await http.put(`/essay/${essay.value.id}/submit`);
    await loadData();
  } catch (e) {
    alert(e.message || '提交失败');
  }
};

const onWithdraw = async () => {
  if (!essay.value?.id) return;
  if (essay.value.status !== 1) {
    alert('只有已提交状态的作文可以撤回');
    return;
  }
  if (!window.confirm(`确定撤回作文「${essay.value.title}」吗？撤回后将变为草稿状态。`)) return;
  try {
    await http.put(`/essay/${essay.value.id}/withdraw`);
    await loadData();
  } catch (e) {
    alert(e.message || '撤回失败');
  }
};

const onDelete = async () => {
  if (!essay.value?.id) return;
  if (essay.value.status !== 0) {
    alert('只有草稿状态的作文可以删除');
    return;
  }
  if (!window.confirm(`确定删除草稿「${essay.value.title}」吗？删除后将无法恢复。`)) return;
  try {
    await http.delete(`/essay/${essay.value.id}`);
    alert('删除成功');
    router.push('/essays');
  } catch (e) {
    alert(e.message || '删除失败');
  }
};

onMounted(loadData);
watch(
  () => route.params.id,
  () => loadData(),
);
</script>

<template>
  <div class="rp-card">
    <div class="rp-card-header">
      <div class="rp-card-title">我的作文</div>
      <div>
        <button class="rp-btn rp-btn-outline" @click="loadData" style="margin-right: 8px">
          刷新
        </button>
        <button class="rp-btn rp-btn-primary" @click="$router.push('/essays/create')">
          新建文本作文
        </button>
      </div>
    </div>

    <table class="rp-table" v-if="essays.length">
      <thead>
        <tr>
          <th>作文ID</th>
          <th>标题</th>
          <th>字数</th>
          <th>状态</th>
          <th>创建时间</th>
          <th style="width: 380px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in essays" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.title }}</td>
          <td>{{ item.wordCount }}</td>
          <td>
            <span class="rp-badge rp-badge-neutral">{{ statusText(item.status) }}</span>
          </td>
          <td>{{ item.createTime || '-' }}</td>
          <td>
            <button
              class="rp-btn rp-btn-outline"
              @click="viewDetail(item)"
              style="margin-right: 6px"
            >
              查看详情
            </button>
            <button
              class="rp-btn rp-btn-outline"
              :disabled="!!reviewingId"
              @click="onReview(item)"
            >
              <span v-if="reviewingId === item.id">批改中...</span>
              <span v-else>发起批改</span>
            </button>
            <button
              class="rp-btn rp-btn-outline"
              style="margin-left: 6px"
              @click="viewReviews(item)"
            >
              查看批改记录
            </button>
            <button
              v-if="item.status === 0"
              class="rp-btn rp-btn-outline"
              style="margin-left: 6px"
              @click="onSubmitEssay(item)"
            >
              提交作文
            </button>
            <button
              v-if="item.status === 1"
              class="rp-btn rp-btn-outline"
              style="margin-left: 6px"
              @click="onWithdrawEssay(item)"
            >
              撤回作文
            </button>
            <button
              v-if="item.status === 0"
              class="rp-btn rp-btn-outline"
              style="margin-left: 6px"
              @click="onDelete(item)"
            >
              删除草稿
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="rp-empty">暂无作文，请先创建一篇作文草稿</div>

    <div class="rp-pagination">
      <button
        class="rp-btn rp-btn-outline"
        :disabled="page === 1"
        @click="changePage(page - 1)"
      >
        上一页
      </button>
      <span>第 {{ page }} 页</span>
      <button
        class="rp-btn rp-btn-outline"
        :disabled="!hasMore"
        @click="changePage(page + 1)"
      >
        下一页
      </button>
    </div>

    <div v-if="error" class="rp-error-text" style="margin-top: 8px">
      {{ error }}
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import http from '../api/http';

const router = useRouter();

const essays = ref([]);
const page = ref(1);
const pageSize = ref(10);
const hasMore = ref(false);
const error = ref('');
const reviewingId = ref(null);

const statusText = (status) => {
  // 对应后端 EssayStatus 枚举：
  // 0-草稿，1-已提交，2-批改中，3-已批改，4-已归档
  if (status === 0) return '草稿';
  if (status === 1) return '已提交';
  if (status === 2) return '批改中';
  if (status === 3) return '已批改';
  if (status === 4) return '已归档';
  return '未知';
};

const loadData = async () => {
  error.value = '';
  try {
    // 建议在 EssayController 中提供分页查询接口：
    // GET /essay/list?page={page}&pageSize={pageSize}
    // 这里假设返回 Page<EssayEntity>，包装在 Result 中
    const res = await http.get('/essay/list', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
      },
    });
    const data = res.data || {};
    essays.value = data.records || data.rows || [];
    const total = data.total || 0;
    hasMore.value = page.value * pageSize.value < total;
  } catch (e) {
    error.value = e.message || '加载失败';
  }
};

const changePage = (p) => {
  page.value = p;
  loadData();
};

const viewDetail = (item) => {
  router.push(`/essays/${item.id}`);
};

const onReview = async (item) => {
  if (!item.id || reviewingId.value) return;
  if (!window.confirm(`确定对作文「${item.title}」发起 AI 批改吗？\n\n批改将在后台异步进行，请稍后在「批改记录」中查看结果。`)) return;
  try {
    reviewingId.value = item.id;
    // POST /review/essay/{id} - 异步批改，立即返回
    const res = await http.post(`/review/essay/${item.id}`);
    const reviewId = res.data?.reviewId;
    if (reviewId) {
      alert(`批改已发起（ID: ${reviewId}），正在后台处理中...\n\n请稍后在「批改记录」中查看结果。`);
    } else {
      alert('批改已发起，正在后台处理中...\n\n请稍后在「批改记录」中查看结果。');
    }
  } catch (e) {
    alert(e.message || '发起批改失败');
  } finally {
    reviewingId.value = null;
  }
};

const viewReviews = (item) => {
  router.push({
    path: '/reviews',
    query: { essayId: item.id },
  });
};

const onSubmitEssay = async (item) => {
  if (!item.id) return;
  if (item.status !== 0) {
    alert('只有草稿状态的作文可以提交');
    return;
  }
  if (!window.confirm(`确定提交作文「${item.title}」吗？提交后将变为已提交状态。`)) return;
  try {
    // PUT /essay/{id}/submit
    await http.put(`/essay/${item.id}/submit`);
    await loadData();
  } catch (e) {
    alert(e.message || '提交失败');
  }
};

const onWithdrawEssay = async (item) => {
  if (!item.id) return;
  if (item.status !== 1) {
    alert('只有已提交状态的作文可以撤回');
    return;
  }
  if (!window.confirm(`确定撤回作文「${item.title}」吗？撤回后将变为草稿状态。`)) return;
  try {
    // PUT /essay/{id}/withdraw
    await http.put(`/essay/${item.id}/withdraw`);
    await loadData();
  } catch (e) {
    alert(e.message || '撤回失败');
  }
};

const onDelete = async (item) => {
  if (!item.id) return;
  if (item.status !== 0) {
    alert('只有草稿状态的作文可以删除');
    return;
  }
  if (!window.confirm(`确定删除草稿「${item.title}」吗？删除后将无法恢复。`)) return;
  try {
    // DELETE /essay/{id}，后端已实现逻辑删除
    await http.delete(`/essay/${item.id}`);
    await loadData();
  } catch (e) {
    alert(e.message || '删除失败');
  }
};

onMounted(loadData);
</script>


<template>
  <div class="rp-card">
    <div class="rp-card-header">
      <div class="rp-card-title">批改记录</div>
      <div>
        <button class="rp-btn rp-btn-outline" @click="loadData">刷新</button>
      </div>
    </div>

    <div class="rp-form" style="flex-direction: row; gap: 12px; margin-bottom: 12px">
      <div class="rp-form-row" style="max-width: 200px">
        <label class="rp-form-label">批改状态</label>
        <select v-model="status" class="rp-select">
          <option :value="null">全部</option>
          <option :value="0">INIT</option>
          <option :value="1">PROCESSING</option>
          <option :value="2">SUCCESS</option>
          <option :value="3">FAIL</option>
          <option :value="4">TIMEOUT</option>
        </select>
      </div>
      <div class="rp-form-row" style="max-width: 200px">
        <label class="rp-form-label">评审者类型</label>
        <select v-model="reviewerType" class="rp-select">
          <option :value="null">全部</option>
          <option :value="0">AI</option>
          <option :value="1">教师</option>
        </select>
      </div>
    </div>

    <table class="rp-table" v-if="records.length">
      <thead>
        <tr>
          <th>批改ID</th>
          <th>作文ID</th>
          <th>作文标题</th>
          <th>评审者</th>
          <th>模型版本</th>
          <th>总分</th>
          <th>状态</th>
          <th>开始时间</th>
          <th>结束时间</th>
          <th style="width: 140px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in records" :key="item.reviewId">
          <td>{{ item.reviewId }}</td>
          <td>{{ item.essayId }}</td>
          <td>{{ item.essayTitle || '-' }}</td>
          <td>
            <span class="rp-tag">
              {{ item.reviewerType === 0 ? 'AI' : '教师' }}
            </span>
          </td>
          <td>{{ item.modelVersion }}</td>
          <td>{{ item.totalScore != null ? item.totalScore.toFixed(2) : '-' }}</td>
          <td>
            <span
              class="rp-badge"
              :class="{
                'rp-badge-warning': item.status === 1,
                'rp-badge-success': item.status === 2,
                'rp-badge-danger': item.status === 3 || item.status === 4,
                'rp-badge-neutral': item.status === 0,
              }"
            >
              {{ statusText(item.status) }}
            </span>
          </td>
          <td>{{ item.startTime || '-' }}</td>
          <td>{{ item.endTime || '-' }}</td>
          <td>
            <button
              class="rp-btn rp-btn-outline"
              @click="goToDetail(item)"
              style="margin-right: 8px"
            >
              查看详情
            </button>
            <button
              class="rp-btn rp-btn-danger"
              @click="confirmDelete(item.reviewId)"
            >
              删除
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="rp-empty">暂无批改记录</div>

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
import { onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import http from '../api/http';

const route = useRoute();
const router = useRouter();

const records = ref([]);
const page = ref(1);
const pageSize = ref(10);
const hasMore = ref(false);
const status = ref(null);
const reviewerType = ref(null);
const error = ref('');

const statusText = (s) => {
  if (s === 0) return 'INIT';
  if (s === 1) return 'PROCESSING';
  if (s === 2) return 'SUCCESS';
  if (s === 3) return 'FAIL';
  if (s === 4) return 'TIMEOUT';
  return 'UNKNOWN';
};

const loadData = async () => {
  error.value = '';
  try {
    // GET /review/records?page=&pageSize=&status=&reviewerType=
    const res = await http.get('/review/records', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        status: status.value ?? undefined,
        reviewerType: reviewerType.value ?? undefined,
      },
    });
    const data = res.data || {};
    records.value = data.records || data.rows || [];
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

const confirmDelete = async (reviewId) => {
  if (!confirm('确定要删除这条批改记录吗？删除后无法恢复。')) {
    return;
  }
  
  error.value = '';
  try {
    await http.delete(`/review/record/${reviewId}`);
    // 删除成功后重新加载数据
    await loadData();
  } catch (e) {
    error.value = e.message || '删除失败';
  }
};

const goToDetail = (item) => {
  // 根据作文类型跳转到不同的详情页
  // submitType: 0-图片，1-文档，2-文本
  if (item.submitType === 0) {
    // 图片类型，跳转到图片标注页面
    router.push(`/reviews/${item.reviewId}/image`);
  } else {
    // 文档和文本类型，跳转到普通详情页
    router.push(`/reviews/${item.reviewId}`);
  }
};

watch([status, reviewerType], () => {
  page.value = 1;
  loadData();
});

onMounted(() => {
  // 如果从作文列表带了 essayId 过来，可以未来扩展为按作文过滤
  const essayId = route.query.essayId;
  if (essayId) {
    // 此处暂时不做 essayId 过滤，如需要可在后端扩展接口
  }
  loadData();
});
</script>


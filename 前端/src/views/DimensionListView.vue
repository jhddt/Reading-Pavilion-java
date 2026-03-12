<template>
  <div class="rp-card">
    <div class="rp-card-header">
      <div class="rp-card-title">评分维度配置</div>
      <button class="rp-btn rp-btn-outline" @click="loadData">刷新</button>
    </div>

    <form class="rp-form" style="max-width: 540px; margin-bottom: 16px" @submit.prevent="onCreate">
      <div class="rp-form-row">
        <label class="rp-form-label">新增维度名称</label>
        <input v-model="form.dimensionName" class="rp-input" placeholder="如：内容评价、结构分析" required />
      </div>
      <div class="rp-form-row" style="flex-direction: row; gap: 12px">
        <div style="flex: 1">
          <label class="rp-form-label">权重（0-100，对应百分比）</label>
          <input
            v-model.number="form.weight"
            type="number"
            step="1"
            min="0"
            max="100"
            class="rp-input"
            required
          />
        </div>
        <div style="flex: 1">
          <label class="rp-form-label">满分值</label>
          <input
            v-model.number="form.maxScore"
            type="number"
            step="1"
            min="1"
            class="rp-input"
            required
          />
        </div>
      </div>
      <button type="submit" class="rp-btn rp-btn-primary" :disabled="creating">
        {{ creating ? '创建中...' : '新增维度' }}
      </button>
      <div v-if="error" class="rp-error-text" style="margin-top: 4px">
        {{ error }}
      </div>
    </form>

    <table class="rp-table" v-if="dimensions.length">
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>权重</th>
          <th>满分值</th>
          <th>状态</th>
          <th>创建时间</th>
          <th style="width: 200px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in dimensions" :key="d.dimensionId">
          <td>{{ d.dimensionId }}</td>
          <td>{{ d.dimensionName }}</td>
          <td>{{ d.weight }}</td>
          <td>{{ d.maxScore }}</td>
          <td>
            <span
              class="rp-badge"
              :class="d.status === 1 ? 'rp-badge-success' : 'rp-badge-neutral'"
            >
              {{ d.status === 1 ? '启用' : '禁用' }}
            </span>
          </td>
          <td>{{ d.createTime || '-' }}</td>
          <td>
            <button
              class="rp-btn rp-btn-outline"
              @click="toggleStatus(d)"
              style="margin-right: 6px"
            >
              {{ d.status === 1 ? '禁用' : '启用' }}
            </button>
            <button class="rp-btn rp-btn-outline" @click="remove(d)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="rp-empty">暂无评分维度，可先新增一条配置</div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import http from '../api/http';

const dimensions = ref([]);
const error = ref('');
const creating = ref(false);

const form = reactive({
  dimensionName: '',
  weight: 25,
  maxScore: 25,
});

const loadData = async () => {
  error.value = '';
  try {
    // GET /review/dimensions?enabledOnly=false
    const res = await http.get('/review/dimensions', {
      params: { enabledOnly: false },
    });
    dimensions.value = res.data || [];
  } catch (e) {
    error.value = e.message || '加载失败';
  }
};

const onCreate = async () => {
  creating.value = true;
  error.value = '';
  try {
    await http.post('/review/dimensions', {
      dimensionName: form.dimensionName,
      weight: form.weight,
      maxScore: form.maxScore,
    });
    form.dimensionName = '';
    await loadData();
  } catch (e) {
    error.value = e.message || '创建失败';
  } finally {
    creating.value = false;
  }
};

const toggleStatus = async (d) => {
  try {
    await http.patch(`/review/dimensions/${d.dimensionId}/status`, null, {
      params: { enabled: d.status !== 1 },
    });
    await loadData();
  } catch (e) {
    alert(e.message || '更新状态失败');
  }
};

const remove = async (d) => {
  if (!window.confirm(`确定删除维度「${d.dimensionName}」吗？`)) return;
  try {
    await http.delete(`/review/dimensions/${d.dimensionId}`);
    await loadData();
  } catch (e) {
    alert(e.message || '删除失败');
  }
};

onMounted(loadData);
</script>


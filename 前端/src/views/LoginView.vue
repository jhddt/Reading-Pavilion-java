<template>
  <div class="rp-login-page">
    <div class="rp-login-card">
      <div class="rp-login-title">阅读亭 · 作文批改</div>
      <div class="rp-login-subtitle">请先登录后使用作文批改与维度配置功能</div>

      <form class="rp-form" @submit.prevent="onSubmit">
        <div class="rp-form-row">
          <label class="rp-form-label">用户名</label>
          <input
            v-model="form.username"
            class="rp-input"
            autocomplete="username"
            placeholder="请输入用户名"
            required
          />
        </div>

        <div class="rp-form-row">
          <label class="rp-form-label">密码</label>
          <input
            v-model="form.password"
            type="password"
            class="rp-input"
            autocomplete="current-password"
            placeholder="请输入密码"
            required
          />
        </div>

        <div v-if="error" class="rp-error-text">
          {{ error }}
        </div>

        <button type="submit" class="rp-btn rp-btn-primary" :disabled="loading" style="margin-top: 4px">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import http from '../api/http';
import { useAuthStore } from '../store/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const form = reactive({
  username: '',
  password: '',
});

const loading = ref(false);
const error = ref('');

const onSubmit = async () => {
  if (!form.username || !form.password) return;
  loading.value = true;
  error.value = '';
  try {
    // 假设后端登录接口为 POST /user/login，返回 { code, message, data: { token, username } }
    const res = await http.post('/user/login', {
      username: form.username,
      password: form.password,
    });
    const data = res.data || {};
    if (!data.token) {
      throw new Error('登录返回数据中缺少 token 字段');
    }
    authStore.setToken(data.token);
    authStore.setUsername(data.username || form.username);

    const redirect = route.query.redirect || '/essays';
    router.replace(redirect);
  } catch (e) {
    error.value = e.message || '登录失败';
  } finally {
    loading.value = false;
  }
};
</script>


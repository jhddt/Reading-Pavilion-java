<template>
  <div class="auth-page">
    <div class="auth-shell">
      <div class="auth-brand auth-brand-warm animate-slide-in-left">
        <div class="auth-brand__seal animate-scale-in">P</div>
        <h1 class="animate-fade-in-up">PenPilot</h1>
      </div>

      <div class="auth-card animate-slide-in-right">
        <div class="auth-card__header">
          <h2>注册</h2>
        </div>

        <form class="fs-form" @submit.prevent="onSubmit">
          <div class="fs-form-item">
            <label class="fs-form-label" for="username">用户名</label>
            <input
              id="username"
              v-model="form.userName"
              type="text"
              class="fs-input"
              placeholder="请输入用户名"
              autocomplete="username"
              required
            />
          </div>

          <div class="fs-form-item">
            <label class="fs-form-label" for="password">密码</label>
            <input
              id="password"
              v-model="form.password"
              type="password"
              class="fs-input"
              placeholder="请输入密码"
              autocomplete="new-password"
              required
            />
          </div>

          <div class="fs-form-item">
            <label class="fs-form-label" for="password2">确认密码</label>
            <input
              id="password2"
              v-model="form.password2"
              type="password"
              class="fs-input"
              placeholder="请再次输入密码"
              autocomplete="new-password"
              required
            />
          </div>

          <div v-if="error" class="fs-form-error">{{ error }}</div>

          <button type="submit" class="fs-btn fs-btn-primary fs-btn-lg auth-submit btn-ripple shine-effect" :disabled="loading">
            <span v-if="loading" class="loading-spinner"></span>
            {{ loading ? '注册中...' : '注册并进入系统' }}
          </button>

          <div class="auth-footer">
            <span>已有账号</span>
            <a @click.prevent="router.push('/login')">立即登录</a>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  userName: '',
  password: '',
  password2: '',
})

const loading = ref(false)
const error = ref('')

const onSubmit = async () => {
  error.value = ''
  if (!form.userName || !form.password) return
  if (form.password !== form.password2) {
    error.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    await http.post('/user/add', {
      userName: form.userName,
      password: form.password,
    })

    const res = await http.post('/user/login', {
      username: form.userName,
      password: form.password,
    })
    const login = res?.data || {}
    if (!login.token) throw new Error('注册成功，但自动登录失败')

    authStore.setToken(login.token)
    authStore.setUser({
      userId: login.userId ?? null,
      userName: login.userName || form.userName,
      role: login.role ?? 1,
    })

    router.replace('/essays')
  } catch (e) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px;
  position: relative;
  background: 
    radial-gradient(circle at 20% 30%, rgba(183, 96, 55, 0.08), transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(145, 70, 41, 0.06), transparent 50%);
}

.auth-shell {
  width: min(1180px, 100%);
  display: grid;
  grid-template-columns: 1.02fr 0.98fr;
  gap: 28px;
  align-items: stretch;
  position: relative;
  z-index: 1;
}

.auth-brand,
.auth-card {
  border-radius: 34px;
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.auth-brand:hover,
.auth-card:hover {
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12);
}

.auth-brand {
  padding: 34px;
  color: #fff;
}

.auth-brand-warm {
  background:
    linear-gradient(135deg, rgba(183, 96, 55, 0.95), rgba(145, 70, 41, 0.95)),
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.16), transparent 34%);
  background-size: 200% 200%;
  animation: gradientShift 8s ease infinite;
}

.auth-brand__seal {
  width: 58px;
  height: 58px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.14);
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 22px;
  font-family: "STSong", "SimSun", serif;
  transition: all 0.3s ease;
}

.auth-brand__seal:hover {
  transform: rotate(360deg) scale(1.1);
  background: rgba(255, 255, 255, 0.24);
}

.auth-brand h1 {
  margin: 0;
  font-family: "STSong", "SimSun", serif;
  font-size: 44px;
  transition: all 0.3s ease;
}

.auth-brand h1:hover {
  transform: translateX(10px);
  text-shadow: 2px 2px 8px rgba(0, 0, 0, 0.2);
}

.auth-brand p,
.auth-card__header p {
  display: none;
}

.auth-brand p {
  margin: 18px 0 0;
  max-width: 520px;
  line-height: 1.95;
  color: rgba(255, 255, 255, 0.82);
}

.auth-card {
  padding: 34px;
  background: rgba(255, 250, 243, 0.88);
  backdrop-filter: blur(12px);
}

.auth-card__header {
  margin-bottom: 24px;
}

.auth-card__header h2 {
  margin: 0 0 8px;
  font-size: 30px;
}

.auth-card__header p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
}

.fs-form-item {
  transition: all 0.3s ease;
}

.fs-form-item:focus-within {
  transform: translateX(4px);
}

.fs-input,
.fs-select {
  transition: all 0.3s ease;
}

.fs-input:focus,
.fs-select:focus {
  transform: scale(1.01);
  box-shadow: 0 4px 12px rgba(183, 96, 55, 0.15);
}

.auth-submit {
  width: 100%;
  position: relative;
  overflow: hidden;
}

.loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 8px;
  vertical-align: middle;
}

.auth-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  color: var(--muted);
  font-size: 14px;
}

.auth-footer a {
  color: var(--brand);
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.auth-footer a::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--brand);
  transition: width 0.3s ease;
}

.auth-footer a:hover::after {
  width: 100%;
}

.fs-form-error {
  animation: shake 0.5s ease;
}

@media (max-width: 980px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }
}
</style>

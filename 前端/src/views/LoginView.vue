<template>
  <div class="auth-page">
    <div class="auth-stage">
      <div class="auth-orbit auth-orbit--left animate-rotate-slow"></div>
      <div class="auth-orbit auth-orbit--right animate-rotate-slow"></div>
      <div class="auth-glow auth-glow--gold animate-float"></div>
      <div class="auth-glow auth-glow--blue animate-drift"></div>
    </div>

    <div ref="authShellRef" class="auth-shell auth-shell--single">
      <div class="auth-card-wrap animate-slide-in-right">
        <div class="auth-mascot" :class="{ 'auth-mascot--covering': showPassword }" :style="mascotStyle" aria-hidden="true">
          <div class="auth-mascot__shape">
            <div class="auth-mascot__eye auth-mascot__eye--left">
              <span class="auth-mascot__pupil"></span>
            </div>
            <div class="auth-mascot__eye auth-mascot__eye--right">
              <span class="auth-mascot__pupil"></span>
            </div>
            <div class="auth-mascot__cover"></div>
          </div>
        </div>
        <div class="auth-card">
          <div class="auth-card__header">
            <h2>PenPilot</h2>
          </div>

          <form class="fs-form" @submit.prevent="onSubmit">
            <div class="fs-form-item">
              <label class="fs-form-label" for="username">用户名</label>
              <input
                id="username"
                v-model="form.username"
                type="text"
                class="fs-input"
                placeholder="请输入用户名"
                autocomplete="username"
                @keydown.enter.prevent="focusPasswordInput"
              />
            </div>

            <div class="fs-form-item">
              <label class="fs-form-label" for="password">密码</label>
              <div class="password-wrap">
                <input
                  id="password"
                  ref="passwordInputRef"
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="fs-input password-input"
                  placeholder="请输入密码"
                  autocomplete="current-password"
                />
                <button type="button" class="password-toggle" @click="showPassword = !showPassword">
                  {{ showPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>

            <div v-if="error" class="fs-form-error">{{ error }}</div>

            <button type="submit" class="fs-btn fs-btn-primary fs-btn-lg auth-submit btn-ripple shine-effect" :disabled="loading">
              <span v-if="loading" class="loading-spinner">
                <i></i>
              </span>
              {{ loading ? '登录中...' : '登录' }}
            </button>

            <div class="auth-footer">
              <span>还没有账号</span>
              <a @click.prevent="router.push('/register')">立即注册</a>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
})

const loading = ref(false)
const error = ref('')
const showPassword = ref(false)
const authShellRef = ref(null)
const passwordInputRef = ref(null)
const mascotStyle = reactive({
  '--mascot-pupil-x': '0px',
  '--mascot-pupil-y': '0px',
})

const focusPasswordInput = () => {
  passwordInputRef.value?.focus()
}

const updateMascotEyes = (event) => {
  const shell = authShellRef.value
  if (!shell) return

  const rect = shell.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height * 0.28
  const normalizedX = Math.max(-1, Math.min(1, (event.clientX - centerX) / (rect.width / 2)))
  const normalizedY = Math.max(-1, Math.min(1, (event.clientY - centerY) / (rect.height / 2)))

  mascotStyle['--mascot-pupil-x'] = `${(normalizedX * 5).toFixed(2)}px`
  mascotStyle['--mascot-pupil-y'] = `${(normalizedY * 4).toFixed(2)}px`
}

const resetMascotEyes = () => {
  mascotStyle['--mascot-pupil-x'] = '0px'
  mascotStyle['--mascot-pupil-y'] = '0px'
}

onMounted(() => {
  window.addEventListener('mousemove', updateMascotEyes)
  window.addEventListener('mouseleave', resetMascotEyes)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', updateMascotEyes)
  window.removeEventListener('mouseleave', resetMascotEyes)
})

const onSubmit = async () => {
  if (!form.username || !form.password) {
    error.value = !form.username ? '请输入用户名' : '请输入密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await http.post('/user/login', {
      username: form.username,
      password: form.password,
    })
    const login = res?.data || {}
    if (!login.token) throw new Error('登录失败，未返回 token')

    authStore.setToken(login.token)
    authStore.setUser({
      userId: login.userId ?? null,
      userName: login.userName || form.username,
      role: login.role ?? null,
    })

    const redirect = route.query.redirect || '/essays'
    router.replace(redirect)
  } catch (e) {
    error.value = e.message || '登录失败'
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
  padding: 20px;
  position: relative;
  overflow: hidden;
  background: 
    radial-gradient(circle at 20% 30%, rgba(34, 77, 105, 0.08), transparent 42%),
    radial-gradient(circle at 80% 70%, rgba(23, 57, 79, 0.07), transparent 40%);
}

.auth-stage {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.auth-orbit,
.auth-orbit::before,
.auth-orbit::after {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(34, 77, 105, 0.09);
}

.auth-orbit {
  width: 460px;
  height: 460px;
}

.auth-orbit::before,
.auth-orbit::after {
  content: '';
  inset: 42px;
}

.auth-orbit::after {
  inset: 108px;
}

.auth-orbit--left {
  top: -180px;
  left: -130px;
}

.auth-orbit--right {
  right: -120px;
  bottom: -200px;
  animation-direction: reverse;
}

.auth-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(14px);
  opacity: 0.68;
}

.auth-glow--gold {
  width: 280px;
  height: 280px;
  left: 10%;
  bottom: 8%;
  background: radial-gradient(circle, rgba(255, 191, 73, 0.26), transparent 70%);
}

.auth-glow--blue {
  width: 340px;
  height: 340px;
  right: 8%;
  top: 12%;
  background: radial-gradient(circle, rgba(56, 126, 212, 0.18), transparent 70%);
}

.auth-shell {
  width: min(390px, 100%);
  position: relative;
  z-index: 1;
}

.auth-shell--single {
  display: flex;
  justify-content: center;
}

.auth-card {
  border-radius: 34px;
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.auth-card:hover {
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12);
}

.auth-card__header p {
  display: none;
}

.auth-card {
  padding: 24px 22px 20px;
  width: 100%;
  background:
    linear-gradient(180deg, rgba(255, 250, 243, 0.94), rgba(255, 251, 246, 0.82)),
    radial-gradient(circle at top right, rgba(34, 77, 105, 0.08), transparent 32%);
  backdrop-filter: blur(16px);
  position: relative;
  z-index: 2;
}

.auth-card-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
}

.auth-mascot {
  display: flex;
  justify-content: center;
  pointer-events: none;
}

.auth-mascot__shape {
  position: relative;
  width: 108px;
  height: 108px;
  background:
    radial-gradient(circle at 24% 20%, rgba(244, 204, 188, 0.8), transparent 34%),
    radial-gradient(circle at 75% 16%, rgba(98, 187, 230, 0.78), transparent 30%),
    radial-gradient(circle at 42% 82%, rgba(240, 88, 170, 0.78), transparent 26%),
    radial-gradient(circle at 80% 82%, rgba(244, 206, 190, 0.74), transparent 30%),
    linear-gradient(180deg, rgba(239, 222, 210, 0.94), rgba(93, 168, 224, 0.86) 42%, rgba(239, 122, 205, 0.9) 72%, rgba(243, 210, 188, 0.92));
  border-radius: 50%;
  box-shadow:
    0 20px 36px rgba(162, 149, 179, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  filter: saturate(1.03);
}

.auth-mascot__eye {
  position: absolute;
  top: 34px;
  width: 24px;
  height: 34px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.25);
  animation: mascotBlink 5.6s ease-in-out infinite;
  transition: transform 0.22s ease, opacity 0.24s ease;
}

.auth-mascot__eye--left {
  left: 28px;
}

.auth-mascot__eye--right {
  right: 28px;
}

.auth-mascot__pupil {
  position: absolute;
  left: 7px;
  top: 10px;
  width: 10px;
  height: 14px;
  border-radius: 50%;
  background: linear-gradient(180deg, rgba(65, 88, 118, 0.95), rgba(39, 49, 71, 0.98));
  transform: translate(var(--mascot-pupil-x), var(--mascot-pupil-y));
  transition: transform 0.14s ease-out;
}

.auth-mascot__cover {
  position: absolute;
  left: 50%;
  top: 39px;
  width: 72px;
  height: 20px;
  transform: translate(-50%, -12px);
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(248, 226, 212, 0.95), rgba(248, 239, 230, 0.98), rgba(243, 218, 205, 0.95));
  box-shadow: 0 10px 18px rgba(165, 125, 122, 0.14);
  opacity: 0;
  transition: opacity 0.2s ease, transform 0.24s ease;
}

.auth-mascot--covering .auth-mascot__cover {
  opacity: 1;
  transform: translate(-50%, 0);
}

.auth-mascot--covering .auth-mascot__eye {
  transform: scaleY(0.18);
  opacity: 0.36;
  transition: transform 0.24s ease, opacity 0.24s ease;
  animation: none;
}

.auth-card__header {
  margin-bottom: 16px;
}

.auth-card__header h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.1;
}

.auth-card__header p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
}

.fs-form {
  display: grid;
  gap: 14px;
}

:deep(.fs-form-item) {
  margin-bottom: 0;
  transition: all 0.3s ease;
}

:deep(.fs-form-item):focus-within {
  transform: translateX(4px);
}

:deep(.fs-form-label) {
  margin-bottom: 6px;
  font-size: 14px;
}

.fs-input {
  transition: all 0.3s ease;
  height: 48px;
  padding: 0 16px;
}

.fs-input:focus {
  transform: translateY(-1px) scale(1.01);
  box-shadow: 0 12px 24px rgba(34, 77, 105, 0.12);
}

.password-wrap {
  position: relative;
}

.password-input {
  padding-right: 68px;
}

.password-toggle {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  border: 0;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1;
}

.password-toggle:hover {
  color: var(--brand);
  background: rgba(34, 77, 105, 0.08);
}

.auth-submit {
  width: 100%;
  position: relative;
  overflow: hidden;
  min-height: 48px;
  box-shadow: 0 16px 30px rgba(34, 77, 105, 0.24);
  margin-top: 2px;
}

.loading-spinner {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  margin-right: 10px;
  vertical-align: middle;
}

.loading-spinner::before,
.loading-spinner i {
  content: '';
  grid-area: 1 / 1;
  border-radius: 50%;
}

.loading-spinner::before {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.22);
  border-top-color: rgba(255, 255, 255, 0.94);
  animation: spin 0.8s linear infinite;
}

.loading-spinner i {
  width: 7px;
  height: 7px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 0 14px rgba(255, 255, 255, 0.45);
  animation: pulse 1s ease-in-out infinite;
}

.auth-submit:hover:not(:disabled) {
  transform: translateY(-2px);
}

.auth-submit:disabled {
  box-shadow: 0 12px 24px rgba(34, 77, 105, 0.18);
}

.auth-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  color: var(--muted);
  font-size: 14px;
  padding-top: 0;
}

.fs-form-error {
  padding: 9px 12px;
  border-radius: 12px;
  background: rgba(139, 75, 75, 0.08);
  border: 1px solid rgba(139, 75, 75, 0.12);
  animation: shake 0.5s ease;
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 980px) {
  .auth-stage {
    opacity: 0.7;
  }

}

@media (max-width: 760px) {
  .auth-page {
    padding: 14px;
  }

  .auth-mascot__shape {
    width: 96px;
    height: 96px;
  }

  .auth-mascot__eye {
    top: 31px;
    width: 20px;
    height: 30px;
  }

  .auth-mascot__eye--left {
    left: 25px;
  }

  .auth-mascot__eye--right {
    right: 25px;
  }

  .auth-mascot__cover {
    top: 35px;
    width: 64px;
    height: 18px;
  }

  .auth-card {
    padding: 22px 18px 18px;
    border-radius: 24px;
  }

}

@media (max-width: 520px) {
  .auth-shell {
    width: min(340px, 100%);
  }

  .auth-card__header h2 {
    font-size: 22px;
  }

  .password-toggle {
    right: 8px;
  }

  .fs-input {
    height: 46px;
  }

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

@keyframes mascotBlink {
  0%,
  44%,
  48%,
  100% {
    transform: scaleY(1);
  }

  46% {
    transform: scaleY(0.08);
  }
}
</style>

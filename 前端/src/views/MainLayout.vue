<template>
  <div class="fs-app-shell">
    <header class="fs-topbar">
      <div class="fs-brand">
        <div class="fs-brand-seal">P</div>
        <div class="fs-brand-text">
          <h1>PenPilot</h1>
        </div>
      </div>

      <div class="fs-topbar-right">
        <nav class="fs-nav">
          <RouterLink to="/essays" class="fs-nav-link shine-effect">作文列表</RouterLink>
          <RouterLink to="/essays/create" class="fs-nav-link shine-effect">新建作文</RouterLink>
          <RouterLink to="/reviews" class="fs-nav-link shine-effect">批改记录</RouterLink>
          <RouterLink to="/dimensions" class="fs-nav-link shine-effect">评分维度</RouterLink>
        </nav>

        <div
          class="fs-account-menu"
          @mouseenter="showAccountDropdown"
          @mouseleave="hideAccountDropdown"
        >
          <button
            type="button"
            class="fs-account-chip fs-account-chip--compact hover-scale"
            @click="toggleAccountMenu"
          >
            <div class="fs-account-avatar">
              <img v-if="authStore.user?.avatarPreviewUrl" :src="authStore.user.avatarPreviewUrl" alt="用户头像" class="fs-account-avatar__img" />
              <span v-else>{{ userInitial }}</span>
            </div>
            <div class="fs-account-meta">
              <div class="fs-account-name">{{ authStore.userName || '当前用户' }}</div>
              <div class="fs-account-sub">{{ roleText }} · 已登录</div>
            </div>
          </button>

          <Transition name="account-dropdown">
            <div v-if="accountDropdownOpen" class="fs-account-dropdown">
              <button type="button" class="fs-account-dropdown__item" @click.stop="onLogout">
                退出登录
              </button>
            </div>
          </Transition>
        </div>

        <!-- 模态卡片弹窗 -->
        <Teleport to="body">
          <Transition name="modal-fade">
            <div 
              v-if="accountMenuOpen" 
              class="fs-modal-overlay" 
              @click="closeAccountMenu"
              style="position: fixed; inset: 0; z-index: 2147483647; pointer-events: auto;"
            >
              <Transition name="modal-scale">
                <div 
                  v-if="accountMenuOpen" 
                  class="fs-modal-card animate-fade-in-up" 
                  v-progressive-blur-scroll
                  @click.stop
                  style="position: relative; z-index: 2147483647; pointer-events: auto;"
                >
                  <button type="button" class="fs-modal-close" @click="closeAccountMenu">
                    <span>✕</span>
                  </button>

                  <div class="fs-modal-header">
                    <div class="fs-account-avatar fs-account-avatar--large">
                      <img v-if="authStore.user?.avatarPreviewUrl" :src="authStore.user.avatarPreviewUrl" alt="用户头像" class="fs-account-avatar__img" />
                      <span v-else>{{ userInitial }}</span>
                    </div>
                    <div class="fs-modal-header-meta">
                      <h3>{{ authStore.userName || '当前用户' }}</h3>
                      <span>{{ roleText }}</span>
                    </div>
                  </div>

                  <div class="fs-modal-body">
                    <div class="fs-info-section">
                      <h4 class="fs-section-title">账户信息</h4>
                      <div class="fs-info-grid">
                        <div class="fs-info-item">
                          <span class="fs-info-label">头像状态</span>
                          <strong class="fs-info-value">{{ authStore.user?.avatarPreviewUrl ? '已上传' : '未设置' }}</strong>
                        </div>
                        <div class="fs-info-item">
                          <span class="fs-info-label">用户 ID</span>
                          <strong class="fs-info-value">{{ authStore.userId ?? '-' }}</strong>
                        </div>
                        <div class="fs-info-item">
                          <span class="fs-info-label">账户状态</span>
                          <strong class="fs-info-value">{{ statusText }}</strong>
                        </div>
                        <div class="fs-info-item">
                          <span class="fs-info-label">创建时间</span>
                          <strong class="fs-info-value">{{ formatDateTime(authStore.user?.createTime) }}</strong>
                        </div>
                        <div class="fs-info-item">
                          <span class="fs-info-label">更新时间</span>
                          <strong class="fs-info-value">{{ formatDateTime(authStore.user?.updateTime) }}</strong>
                        </div>
                        <div class="fs-info-item">
                          <span class="fs-info-label">头像更新</span>
                          <strong class="fs-info-value">{{ formatDateTime(authStore.user?.avatarUpdateTime) }}</strong>
                        </div>
                      </div>
                    </div>

                    <div class="fs-form-section">
                      <h4 class="fs-section-title">修改个人信息</h4>
                      <form class="fs-profile-form" @submit.prevent="saveProfile">
                        <label class="fs-form-field">
                          <span class="fs-form-label">用户名</span>
                          <input v-model.trim="profileForm.userName" class="fs-input" placeholder="请输入用户名" />
                        </label>

                        <label class="fs-form-field">
                          <span class="fs-form-label">上传头像</span>
                          <input
                            id="account-avatar-file"
                            ref="avatarInputRef"
                            type="file"
                            class="fs-file-input"
                            accept="image/png,image/jpeg,image/jpg"
                            @change="handleAvatarChange"
                          />
                        </label>

                        <div class="fs-upload-preview">
                          <label for="account-avatar-file" class="fs-icon-picker" title="选择头像图片">
                            <span class="fs-icon-picker__glyph">+</span>
                          </label>
                          <span class="fs-upload-name">{{ selectedAvatarName || '未选择图片' }}</span>
                          <button type="button" class="fs-btn fs-btn-outline fs-btn-sm" :disabled="avatarUploading" @click="uploadAvatar">
                            {{ avatarUploading ? '上传中...' : '上传头像' }}
                          </button>
                        </div>

                        <label class="fs-form-field">
                          <span class="fs-form-label">当前密码</span>
                          <input
                            v-model="profileForm.currentPassword"
                            type="password"
                            class="fs-input"
                            placeholder="修改密码时必填"
                          />
                        </label>

                        <label class="fs-form-field">
                          <span class="fs-form-label">新密码</span>
                          <input
                            v-model="profileForm.newPassword"
                            type="password"
                            class="fs-input"
                            placeholder="不修改密码可留空"
                          />
                        </label>

                        <div
                          v-if="profileMessage"
                          class="fs-form-message"
                          :class="{ 'fs-form-message--error': profileMessageType === 'error' }"
                        >
                          {{ profileMessage }}
                        </div>

                        <div class="fs-form-actions">
                          <button type="button" class="fs-btn fs-btn-outline" @click="resetProfileForm">重置</button>
                          <button type="submit" class="fs-btn fs-btn-primary" :disabled="profileSaving">
                            {{ profileSaving ? '保存中...' : '保存修改' }}
                          </button>
                        </div>
                      </form>

                    </div>
                  </div>
                </div>
              </Transition>
            </div>
          </Transition>
        </Teleport>
      </div>
    </header>

    <main class="fs-page animate-fade-in-up">
      <section class="page-header">
        <h2 class="animate-slide-in-left">{{ pageTitle }}</h2>
      </section>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const avatarInputRef = ref(null)
const profileSaving = ref(false)
const avatarUploading = ref(false)
const profileMessage = ref('')
const profileMessageType = ref('success')
const selectedAvatarFile = ref(null)
const selectedAvatarName = ref('')
const accountMenuOpen = ref(false)
const accountDropdownOpen = ref(false)
const profileForm = reactive({
  userName: '',
  currentPassword: '',
  newPassword: '',
})

const pageTitle = computed(() => route.meta.title || 'PenPilot')

const userInitial = computed(() => {
  const name = authStore.userName || '用'
  return name.charAt(0).toUpperCase()
})

const roleText = computed(() => {
  const role = authStore.role
  if (role === 2 || role === '2') return '教师账户'
  if (role === 3 || role === '3') return '管理账户'
  if (role === 'teacher' || role === 'TEACHER') return '教师账户'
  if (role === 'admin' || role === 'ADMIN') return '管理账户'
  return '学生账户'
})

const statusText = computed(() => {
  const status = authStore.user?.status
  if (status === 1 || status === '1') return '启用'
  if (status === 0 || status === '0') return '禁用'
  return '未知'
})

const onLogout = () => {
  accountDropdownOpen.value = false
  closeAccountMenu()
  authStore.logout()
  router.push('/login')
}

const formatDateTime = (value) => {
  if (!value) return '未设置'
  return String(value).replace('T', ' ')
}

const fillProfileForm = () => {
  profileForm.userName = authStore.user?.userName || ''
  profileForm.currentPassword = ''
  profileForm.newPassword = ''
  selectedAvatarFile.value = null
  selectedAvatarName.value = ''
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

const resetProfileForm = () => {
  fillProfileForm()
  profileMessage.value = ''
}

const toggleAccountMenu = () => {
  accountMenuOpen.value = !accountMenuOpen.value
  if (accountMenuOpen.value) {
    fillProfileForm()
    profileMessage.value = ''
    // 禁止背景滚动
    document.body.style.overflow = 'hidden'
    // 暂时禁用自定义光标
    document.documentElement.classList.remove('has-target-cursor')
    document.body.classList.remove('has-target-cursor')
  } else {
    // 恢复背景滚动
    document.body.style.overflow = ''
    // 恢复自定义光标
    document.documentElement.classList.add('has-target-cursor')
    document.body.classList.add('has-target-cursor')
  }
}

const closeAccountMenu = () => {
  accountMenuOpen.value = false
  document.body.style.overflow = ''
  // 恢复自定义光标
  document.documentElement.classList.add('has-target-cursor')
  document.body.classList.add('has-target-cursor')
}

const showAccountDropdown = () => {
  accountDropdownOpen.value = true
}

const hideAccountDropdown = () => {
  accountDropdownOpen.value = false
}

const handleDocumentClick = (event) => {
  // 移除此函数，因为现在使用模态框，点击遮罩层关闭
}

const fetchCurrentUser = async () => {
  if (!authStore.token) return

  try {
    const res = await http.get('/user/me')
    const profile = res?.data
    if (!profile) return

    authStore.setUser({
      userId: profile.userId ?? null,
      userName: profile.userName || authStore.userName || '当前用户',
      role: profile.role ?? null,
      avatarUrl: profile.avatarUrl ?? null,
      avatarPreviewUrl: profile.avatarPreviewUrl ?? null,
      status: profile.status ?? null,
      avatarUpdateTime: profile.avatarUpdateTime ?? null,
      createTime: profile.createTime ?? null,
      updateTime: profile.updateTime ?? null,
    })
  } catch (error) {
    console.error('获取当前用户信息失败', error)
  }
}

const saveProfile = async () => {
  profileMessage.value = ''

  if (!profileForm.userName) {
    profileMessageType.value = 'error'
    profileMessage.value = '用户名不能为空'
    return
  }

  if (profileForm.newPassword && !profileForm.currentPassword) {
    profileMessageType.value = 'error'
    profileMessage.value = '修改密码时请填写当前密码'
    return
  }

  profileSaving.value = true
  try {
    await http.put('/user/me', {
      userName: profileForm.userName,
      currentPassword: profileForm.currentPassword || null,
      newPassword: profileForm.newPassword || null,
    })

    await fetchCurrentUser()
    fillProfileForm()
    profileMessageType.value = 'success'
    profileMessage.value = '个人信息已更新'
  } catch (error) {
    profileMessageType.value = 'error'
    profileMessage.value = error?.message || '保存失败'
  } finally {
    profileSaving.value = false
  }
}

const handleAvatarChange = (event) => {
  const file = event.target?.files?.[0]
  if (!file) {
    selectedAvatarFile.value = null
    selectedAvatarName.value = ''
    return
  }

  const isImage = ['image/png', 'image/jpeg', 'image/jpg'].includes(file.type)
  if (!isImage) {
    profileMessageType.value = 'error'
    profileMessage.value = '头像仅支持 PNG 或 JPG 图片'
    if (avatarInputRef.value) {
      avatarInputRef.value.value = ''
    }
    selectedAvatarFile.value = null
    selectedAvatarName.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    profileMessageType.value = 'error'
    profileMessage.value = '头像大小不能超过 5MB'
    if (avatarInputRef.value) {
      avatarInputRef.value.value = ''
    }
    selectedAvatarFile.value = null
    selectedAvatarName.value = ''
    return
  }

  selectedAvatarFile.value = file || null
  selectedAvatarName.value = file?.name || ''
  profileMessage.value = ''
}

const uploadAvatar = async () => {
  if (!selectedAvatarFile.value) {
    profileMessageType.value = 'error'
    profileMessage.value = '请先选择头像图片'
    return
  }

  avatarUploading.value = true
  profileMessage.value = ''
  try {
    const formData = new FormData()
    formData.append('file', selectedAvatarFile.value)
    const res = await http.post('/user/me/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    const profile = res?.data
    if (profile) {
      authStore.setUser({
        userId: profile.userId ?? null,
        userName: profile.userName || authStore.userName || '当前用户',
        role: profile.role ?? null,
        avatarUrl: profile.avatarUrl ?? null,
        avatarPreviewUrl: profile.avatarPreviewUrl ?? null,
        status: profile.status ?? null,
        avatarUpdateTime: profile.avatarUpdateTime ?? null,
        createTime: profile.createTime ?? null,
        updateTime: profile.updateTime ?? null,
      })
    }
    selectedAvatarFile.value = null
    selectedAvatarName.value = ''
    if (avatarInputRef.value) {
      avatarInputRef.value.value = ''
    }
    profileMessageType.value = 'success'
    profileMessage.value = '头像上传成功'
  } catch (error) {
    profileMessageType.value = 'error'
    profileMessage.value = error?.message || '头像上传失败'
  } finally {
    avatarUploading.value = false
  }
}

onMounted(() => {
  fetchCurrentUser()
})

onBeforeUnmount(() => {
  // 清理：恢复滚动
  document.body.style.overflow = ''
})

watch(accountMenuOpen, (open) => {
  if (open) {
    fillProfileForm()
    profileMessage.value = ''
  }
})
</script>

<style>
/* 全局模态框样式 - 用于 Teleport 到 body 的元素 */
.fs-modal-overlay {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  background: rgba(0, 0, 0, 0.6) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  z-index: 2147483647 !important;
  padding: 20px !important;
  pointer-events: auto !important;
  cursor: default !important;
}

.fs-modal-card {
  position: relative !important;
  width: 100% !important;
  max-width: 560px !important;
  max-height: 90vh !important;
  overflow-y: auto !important;
  background: rgba(255, 251, 245, 0.98) !important;
  border-radius: 24px !important;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.2) !important;
  border: 1px solid rgba(56, 44, 31, 0.1) !important;
  z-index: 2147483647 !important;
  pointer-events: auto !important;
  cursor: default !important;
}

.fs-modal-card *,
.fs-modal-card input,
.fs-modal-card button,
.fs-modal-card label {
  pointer-events: auto !important;
  cursor: default !important;
}

.fs-modal-card button {
  cursor: pointer !important;
}

.fs-modal-card input[type="text"],
.fs-modal-card input[type="password"],
.fs-modal-card input[type="file"] {
  cursor: text !important;
}
</style>

<style scoped>
.page-header {
  margin-bottom: 16px;
  padding: 4px 4px 0;
}

.page-header h2 {
  margin: 0;
  font-family: "STSong", "SimSun", serif;
  font-size: 32px;
  font-weight: 600;
  transition: all 0.3s ease;
  position: relative;
}

.page-header h2::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--brand), transparent);
  transition: width 0.5s ease;
}

.page-header:hover h2::after {
  width: 100px;
}

.fs-nav-link {
  position: relative;
  transition: all 0.3s ease;
}

.fs-nav-link::before {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: var(--brand);
  transition: width 0.3s ease;
}

.fs-nav-link:hover::before,
.fs-nav-link.router-link-active::before {
  width: 100%;
}

.fs-account-menu {
  position: relative;
}

.fs-account-chip {
  transition: all 0.3s ease;
  cursor: pointer;
}

.fs-account-chip--compact {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 186px;
  padding: 8px 12px 8px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(56, 44, 31, 0.08);
  box-shadow: 0 12px 24px rgba(31, 41, 56, 0.08);
  backdrop-filter: blur(10px);
  appearance: none;
}

.fs-account-chip:hover {
  box-shadow: 0 16px 28px rgba(31, 41, 56, 0.12);
  transform: translateY(-2px);
}

.fs-account-avatar {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, #224d69, #17394f 70%, #315f7d);
  box-shadow: 0 8px 16px rgba(34, 77, 105, 0.18);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  transition: transform 0.3s ease;
  overflow: hidden;
}

.fs-account-avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.fs-account-chip:hover .fs-account-avatar {
  transform: rotate(8deg) scale(1.06);
}

.fs-account-avatar--large {
  width: 80px;
  height: 80px;
  border-radius: 24px;
  font-size: 32px;
}

.fs-account-meta {
  position: relative;
  z-index: 1;
  min-width: 0;
  flex: 1;
  text-align: left;
}

.fs-account-name {
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fs-account-sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 模态框样式 */
.fs-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  padding: 20px;
}

.fs-modal-card {
  position: relative;
  width: 100%;
  max-width: 560px;
  max-height: 90vh;
  overflow-y: auto;
  background: rgba(255, 251, 245, 0.98);
  border-radius: 24px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(56, 44, 31, 0.1);
  z-index: 100000;
}

.fs-modal-close {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(56, 44, 31, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10;
}

.fs-modal-close:hover {
  background: rgba(255, 255, 255, 1);
  transform: rotate(90deg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.fs-modal-close span {
  font-size: 20px;
  color: var(--ink);
  line-height: 1;
}

.fs-modal-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 32px 32px 24px;
  border-bottom: 1px solid rgba(56, 44, 31, 0.08);
}

.fs-modal-header-meta {
  flex: 1;
  min-width: 0;
}

.fs-modal-header-meta h3 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: var(--ink);
}

.fs-modal-header-meta span {
  color: var(--muted);
  font-size: 14px;
}

.fs-modal-body {
  padding: 24px 32px 32px;
}

.fs-info-section {
  margin-bottom: 32px;
}

.fs-section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}

.fs-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.fs-info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(56, 44, 31, 0.06);
}

.fs-info-label {
  color: var(--muted);
  font-size: 12px;
}

.fs-info-value {
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
  word-break: break-all;
}

.fs-form-section {
  margin-bottom: 0;
}

.fs-profile-form {
  display: grid;
  gap: 16px;
}

.fs-form-field {
  display: grid;
  gap: 8px;
}

.fs-form-label {
  color: var(--muted);
  font-size: 13px;
  font-weight: 600;
}

.fs-form-file {
  font-size: 13px;
}

.fs-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.fs-upload-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(56, 44, 31, 0.06);
}

.fs-icon-picker {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(34, 77, 105, 0.14), rgba(34, 77, 105, 0.22));
  border: 1px solid rgba(34, 77, 105, 0.16);
  color: var(--brand);
  cursor: pointer;
  transition: all 0.25s ease;
}

.fs-icon-picker:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(34, 77, 105, 0.14);
  background: linear-gradient(135deg, rgba(34, 77, 105, 0.2), rgba(34, 77, 105, 0.28));
}

.fs-icon-picker__glyph {
  font-size: 22px;
  line-height: 1;
  font-weight: 500;
}

.fs-upload-name {
  min-width: 0;
  flex: 1;
  color: var(--muted);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fs-form-message {
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(72, 106, 65, 0.1);
  color: var(--green);
  font-size: 13px;
  font-weight: 500;
}

.fs-form-message--error {
  background: rgba(139, 75, 75, 0.1);
  color: var(--danger);
}

.fs-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.fs-account-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  min-width: 156px;
  padding: 8px;
  border-radius: 16px;
  background: rgba(255, 251, 245, 0.98);
  border: 1px solid rgba(56, 44, 31, 0.1);
  box-shadow: 0 18px 36px rgba(31, 41, 56, 0.16);
  backdrop-filter: blur(12px);
  z-index: 30;
}

.fs-account-dropdown__item {
  width: 100%;
  padding: 10px 12px;
  border: 0;
  border-radius: 12px;
  background: rgba(139, 75, 75, 0.08);
  color: var(--danger);
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}

.fs-account-dropdown__item:hover {
  background: rgba(139, 75, 75, 0.14);
  color: #7b3f3f;
  transform: translateY(-1px);
}

.account-dropdown-enter-active,
.account-dropdown-leave-active {
  transition: all 0.2s ease;
}

.account-dropdown-enter-from,
.account-dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* 过渡动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-scale-enter-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-scale-leave-active {
  transition: all 0.2s ease;
}

.modal-scale-enter-from {
  opacity: 0;
  transform: scale(0.9) translateY(-20px);
}

.modal-scale-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(10px);
}

.fs-btn {
  transition: all 0.3s ease;
}

.fs-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.fs-btn:active {
  transform: translateY(0);
}
</style>

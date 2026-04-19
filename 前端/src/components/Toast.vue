<template>
  <Teleport to="body">
    <Transition name="toast">
      <div v-if="visible" :class="['toast', `toast-${type}`]" @click="close">
        <div class="toast-icon">{{ icon }}</div>
        <div class="toast-content">
          <div v-if="title" class="toast-title">{{ title }}</div>
          <div class="toast-message">{{ message }}</div>
        </div>
        <button class="toast-close" @click.stop="close">✕</button>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'info', // success, error, warning, info
  },
  title: {
    type: String,
    default: '',
  },
  message: {
    type: String,
    required: true,
  },
  duration: {
    type: Number,
    default: 3000,
  },
})

const emit = defineEmits(['close'])

const visible = ref(false)

const icon = computed(() => {
  const icons = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ',
  }
  return icons[props.type] || icons.info
})

const close = () => {
  visible.value = false
  setTimeout(() => {
    emit('close')
  }, 300)
}

onMounted(() => {
  visible.value = true
  if (props.duration > 0) {
    setTimeout(close, props.duration)
  }
})
</script>

<style scoped>
.toast {
  position: fixed;
  top: 24px;
  right: 24px;
  min-width: 320px;
  max-width: 480px;
  padding: 16px 20px;
  border-radius: 16px;
  background: white;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 9999;
  cursor: pointer;
  transition: all 0.3s ease;
}

.toast:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
}

.toast-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  flex-shrink: 0;
}

.toast-content {
  flex: 1;
}

.toast-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 4px;
  color: #212121;
}

.toast-message {
  font-size: 14px;
  color: #616161;
  line-height: 1.5;
}

.toast-close {
  width: 24px;
  height: 24px;
  border: none;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  color: #757575;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.toast-close:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #212121;
}

/* 类型样式 */
.toast-success {
  border-left: 4px solid #4caf50;
}

.toast-success .toast-icon {
  background: #e8f5e9;
  color: #4caf50;
}

.toast-error {
  border-left: 4px solid #f44336;
}

.toast-error .toast-icon {
  background: #ffebee;
  color: #f44336;
}

.toast-warning {
  border-left: 4px solid #ff9800;
}

.toast-warning .toast-icon {
  background: #fff3e0;
  color: #ff9800;
}

.toast-info {
  border-left: 4px solid #2196f3;
}

.toast-info .toast-icon {
  background: #e3f2fd;
  color: #2196f3;
}

/* 过渡动画 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%) translateY(-20px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%) scale(0.8);
}
</style>

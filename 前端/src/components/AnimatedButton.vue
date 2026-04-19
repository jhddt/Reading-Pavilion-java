<template>
  <button 
    :class="['animated-btn', `animated-btn-${variant}`, { 'is-loading': loading }]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="btn-spinner"></span>
    <span class="btn-content">
      <slot />
    </span>
    <span class="btn-ripple"></span>
  </button>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary', // primary, secondary, danger, success
  },
  loading: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['click'])

const rippleActive = ref(false)

const handleClick = (e) => {
  if (props.disabled || props.loading) return
  
  // 触发波纹效果
  const button = e.currentTarget
  const ripple = button.querySelector('.btn-ripple')
  const rect = button.getBoundingClientRect()
  const size = Math.max(rect.width, rect.height)
  const x = e.clientX - rect.left - size / 2
  const y = e.clientY - rect.top - size / 2
  
  ripple.style.width = ripple.style.height = size + 'px'
  ripple.style.left = x + 'px'
  ripple.style.top = y + 'px'
  ripple.classList.add('active')
  
  setTimeout(() => {
    ripple.classList.remove('active')
  }, 600)
  
  emit('click', e)
}
</script>

<style scoped>
.animated-btn {
  position: relative;
  padding: 10px 20px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
}

.animated-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.animated-btn:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
}

.animated-btn:not(:disabled):active {
  transform: translateY(0);
}

/* 变体样式 */
.animated-btn-primary {
  background: linear-gradient(135deg, #224d69, #17394f);
  color: white;
}

.animated-btn-primary:hover {
  background: linear-gradient(135deg, #2a5f7f, #1d4560);
}

.animated-btn-secondary {
  background: #f5f5f5;
  color: #333;
}

.animated-btn-secondary:hover {
  background: #e0e0e0;
}

.animated-btn-danger {
  background: linear-gradient(135deg, #f44336, #d32f2f);
  color: white;
}

.animated-btn-danger:hover {
  background: linear-gradient(135deg, #e53935, #c62828);
}

.animated-btn-success {
  background: linear-gradient(135deg, #4caf50, #388e3c);
  color: white;
}

.animated-btn-success:hover {
  background: linear-gradient(135deg, #43a047, #2e7d32);
}

/* 加载状态 */
.is-loading {
  pointer-events: none;
}

.btn-spinner {
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

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.btn-content {
  position: relative;
  z-index: 1;
}

/* 波纹效果 */
.btn-ripple {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  transform: scale(0);
  pointer-events: none;
}

.btn-ripple.active {
  animation: ripple-animation 0.6s ease-out;
}

@keyframes ripple-animation {
  to {
    transform: scale(4);
    opacity: 0;
  }
}

/* 光泽效果 */
.animated-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 50%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  transition: left 0.5s;
}

.animated-btn:hover::before {
  left: 100%;
}
</style>

<template>
  <div
    v-if="enabled"
    class="target-cursor"
    :class="{ 'target-cursor--active': isTargetActive, 'target-cursor--hidden': !isVisible }"
    :style="cursorStyle"
    aria-hidden="true"
  >
    <div class="target-cursor__corner target-cursor__corner--tl"></div>
    <div class="target-cursor__corner target-cursor__corner--tr"></div>
    <div class="target-cursor__corner target-cursor__corner--bl"></div>
    <div class="target-cursor__corner target-cursor__corner--br"></div>
    <div class="target-cursor__dot"></div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  spinDuration: {
    type: Number,
    default: 2,
  },
  hideDefaultCursor: {
    type: Boolean,
    default: true,
  },
  parallaxOn: {
    type: Boolean,
    default: true,
  },
  hoverDuration: {
    type: Number,
    default: 0.2,
  },
})

const pointer = { x: 0, y: 0 }
const rendered = { x: 0, y: 0, width: 28, height: 28, rotate: 0 }
const activeRect = { width: 28, height: 28, offsetX: 0, offsetY: 0 }

const enabled = ref(false)
const isVisible = ref(false)
const isTargetActive = ref(false)
const hasPointer = ref(false)
const cursorStyle = ref({
  transform: 'translate3d(-50%, -50%, 0)',
  width: '28px',
  height: '28px',
  '--tc-duration': `${props.spinDuration}s`,
  '--tc-hover-duration': `${props.hoverDuration}s`,
})

let rafId = 0

const targetSelector = [
  'a',
  'button',
  'input',
  'textarea',
  'select',
  'label[for]',
  '[role="button"]',
  '[tabindex]:not([tabindex="-1"])',
  '.cursor-target',
  '.fs-btn',
  '.password-toggle',
  '.fs-nav-link',
  '.n-button',
  '.n-base-selection',
].join(', ')

const updateCursorStyle = () => {
  const transform = `translate3d(${rendered.x}px, ${rendered.y}px, 0) translate(-50%, -50%) rotate(${rendered.rotate}deg)`
  cursorStyle.value = {
    transform,
    width: `${rendered.width}px`,
    height: `${rendered.height}px`,
    '--tc-duration': `${props.spinDuration}s`,
    '--tc-hover-duration': `${props.hoverDuration}s`,
  }
}

const animate = () => {
  const followEase = isTargetActive.value ? 0.52 : 0.42
  const sizeEase = 0.34
  const targetX = pointer.x + activeRect.offsetX
  const targetY = pointer.y + activeRect.offsetY

  rendered.x += (targetX - rendered.x) * followEase
  rendered.y += (targetY - rendered.y) * followEase
  rendered.width += (activeRect.width - rendered.width) * sizeEase
  rendered.height += (activeRect.height - rendered.height) * sizeEase

  if (props.spinDuration > 0) {
    if (isTargetActive.value) {
      rendered.rotate += (0 - rendered.rotate) * 0.26
    } else {
      rendered.rotate = (rendered.rotate + 360 / (props.spinDuration * 60)) % 360
    }
  } else if (rendered.rotate !== 0) {
    rendered.rotate += (0 - rendered.rotate) * 0.3
  }

  updateCursorStyle()
  rafId = window.requestAnimationFrame(animate)
}

const resetTarget = () => {
  isTargetActive.value = false
  activeRect.width = 28
  activeRect.height = 28
  activeRect.offsetX = 0
  activeRect.offsetY = 0
}

const setTarget = (element) => {
  const rect = element.getBoundingClientRect()
  const paddingX = 8
  const paddingY = 8

  isTargetActive.value = true
  activeRect.width = rect.width + paddingX * 2
  activeRect.height = rect.height + paddingY * 2
  activeRect.offsetX = props.parallaxOn ? rect.left + rect.width / 2 - pointer.x : 0
  activeRect.offsetY = props.parallaxOn ? rect.top + rect.height / 2 - pointer.y : 0
}

const handleMove = (event) => {
  pointer.x = event.clientX
  pointer.y = event.clientY
  isVisible.value = true

  if (!hasPointer.value) {
    hasPointer.value = true
    rendered.x = pointer.x
    rendered.y = pointer.y
    updateCursorStyle()
  }

  const matchedTarget = event.target instanceof Element ? event.target.closest(targetSelector) : null
  if (matchedTarget) {
    setTarget(matchedTarget)
    return
  }

  resetTarget()
}

const handleLeave = () => {
  isVisible.value = false
  resetTarget()
}

const handleEnter = () => {
  isVisible.value = true
}

const applyCursorClass = (shouldHide) => {
  document.documentElement.classList.toggle('has-target-cursor', shouldHide)
  document.body.classList.toggle('has-target-cursor', shouldHide)
}

const prefersFinePointer = computed(() => window.matchMedia('(pointer: fine)').matches)

onMounted(() => {
  enabled.value = prefersFinePointer.value
  if (!enabled.value) return

  if (props.hideDefaultCursor) {
    applyCursorClass(true)
  }

  window.addEventListener('pointermove', handleMove, { passive: true })
  window.addEventListener('mouseleave', handleLeave)
  window.addEventListener('mouseenter', handleEnter)
  rafId = window.requestAnimationFrame(animate)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', handleMove)
  window.removeEventListener('mouseleave', handleLeave)
  window.removeEventListener('mouseenter', handleEnter)
  window.cancelAnimationFrame(rafId)
  applyCursorClass(false)
})
</script>

<style scoped>
.target-cursor {
  position: fixed;
  left: 0;
  top: 0;
  z-index: 9999;
  pointer-events: none;
  opacity: 1;
  transition:
    opacity 0.16s ease,
    width var(--tc-hover-duration) ease,
    height var(--tc-hover-duration) ease;
  will-change: transform, width, height;
}

.target-cursor--hidden {
  opacity: 0;
}

.target-cursor__corner {
  position: absolute;
  width: 12px;
  height: 12px;
  border-color: rgba(20, 47, 70, 0.92);
  border-style: solid;
  transition:
    transform var(--tc-hover-duration) ease,
    border-color var(--tc-hover-duration) ease;
}

.target-cursor__corner--tl {
  top: 0;
  left: 0;
  border-width: 2px 0 0 2px;
}

.target-cursor__corner--tr {
  top: 0;
  right: 0;
  border-width: 2px 2px 0 0;
}

.target-cursor__corner--bl {
  bottom: 0;
  left: 0;
  border-width: 0 0 2px 2px;
}

.target-cursor__corner--br {
  right: 0;
  bottom: 0;
  border-width: 0 2px 2px 0;
}

.target-cursor__dot {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(20, 47, 70, 0.96);
  transform: translate(-50%, -50%);
  box-shadow: 0 0 0 6px rgba(34, 77, 105, 0.08);
  transition:
    transform var(--tc-hover-duration) ease,
    opacity var(--tc-hover-duration) ease,
    box-shadow var(--tc-hover-duration) ease;
}

.target-cursor--active .target-cursor__corner {
  border-color: rgba(34, 77, 105, 1);
}

.target-cursor--active .target-cursor__dot {
  transform: translate(-50%, -50%) scale(0.75);
  box-shadow: 0 0 0 10px rgba(34, 77, 105, 0.06);
}

:global(html.has-target-cursor),
:global(body.has-target-cursor),
:global(body.has-target-cursor *),
:global(html.has-target-cursor *) {
  cursor: none !important;
}

@media (pointer: coarse) {
  .target-cursor {
    display: none;
  }
}
</style>

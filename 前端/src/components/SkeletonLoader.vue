<template>
  <div :class="['skeleton-loader', `skeleton-${type}`]">
    <div v-if="type === 'card'" class="skeleton-card">
      <div class="skeleton-header">
        <div class="skeleton-avatar"></div>
        <div class="skeleton-lines">
          <div class="skeleton-line skeleton-line-short"></div>
          <div class="skeleton-line skeleton-line-medium"></div>
        </div>
      </div>
      <div class="skeleton-body">
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line skeleton-line-short"></div>
      </div>
    </div>

    <div v-else-if="type === 'list'" class="skeleton-list">
      <div v-for="i in count" :key="i" class="skeleton-list-item">
        <div class="skeleton-circle"></div>
        <div class="skeleton-lines">
          <div class="skeleton-line"></div>
          <div class="skeleton-line skeleton-line-medium"></div>
        </div>
      </div>
    </div>

    <div v-else-if="type === 'text'" class="skeleton-text">
      <div v-for="i in count" :key="i" class="skeleton-line" :class="`skeleton-line-${i % 3 === 0 ? 'short' : 'long'}`"></div>
    </div>

    <div v-else class="skeleton-box"></div>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'card', // card, list, text, box
  },
  count: {
    type: Number,
    default: 3,
  },
})
</script>

<style scoped>
.skeleton-loader {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 骨架屏基础元素 */
.skeleton-line,
.skeleton-circle,
.skeleton-avatar,
.skeleton-box {
  background: linear-gradient(
    90deg,
    #f0f0f0 0px,
    #e8e8e8 40px,
    #f0f0f0 80px
  );
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s ease-in-out infinite;
  border-radius: 8px;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.skeleton-line {
  height: 16px;
  margin-bottom: 12px;
}

.skeleton-line-short {
  width: 60%;
}

.skeleton-line-medium {
  width: 80%;
}

.skeleton-line-long {
  width: 100%;
}

.skeleton-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton-box {
  width: 100%;
  height: 200px;
}

/* 卡片骨架 */
.skeleton-card {
  padding: 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.skeleton-header {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.skeleton-lines {
  flex: 1;
}

.skeleton-body {
  margin-top: 16px;
}

/* 列表骨架 */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-list-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.08);
}

/* 文本骨架 */
.skeleton-text {
  padding: 16px;
}
</style>

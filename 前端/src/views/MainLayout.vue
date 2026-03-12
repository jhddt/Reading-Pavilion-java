<template>
  <div class="rp-layout">
    <aside class="rp-sidebar">
      <div class="rp-sidebar-title">阅读亭 · 作文批改</div>
      <ul class="rp-menu">
        <li>
          <RouterLink to="/essays" class="rp-menu-item">
            我的作文
          </RouterLink>
        </li>
        <li>
          <RouterLink to="/essays/create" class="rp-menu-item">
            新建作文
          </RouterLink>
        </li>
        <li>
          <RouterLink to="/reviews" class="rp-menu-item">
            批改记录
          </RouterLink>
        </li>
        <li>
          <RouterLink to="/dimensions" class="rp-menu-item">
            评分维度配置
          </RouterLink>
        </li>
      </ul>
    </aside>
    <main class="rp-main">
      <header class="rp-header">
        <div class="rp-header-title">
          {{ pageTitle }}
        </div>
        <div class="rp-header-user">
          <span v-if="authStore.username">👤 {{ authStore.username }}</span>
          <button class="rp-btn rp-btn-outline" @click="onLogout">退出登录</button>
        </div>
      </header>
      <section class="rp-content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const pageTitle = computed(() => route.meta.title || '阅读亭 · 作文批改');

const onLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>


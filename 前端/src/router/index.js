import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../store/auth';

const LoginView = () => import('../views/LoginView.vue');
const EssayListView = () => import('../views/EssayListView.vue');
const EssayCreateTextView = () => import('../views/EssayCreateTextView.vue');
const EssayDetailView = () => import('../views/EssayDetailView.vue');
const ReviewListView = () => import('../views/ReviewListView.vue');
const ReviewDetailView = () => import('../views/ReviewDetailView.vue');
const ReviewImageDetailView = () => import('../views/ReviewImageDetailView.vue');
const DimensionListView = () => import('../views/DimensionListView.vue');

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { public: true },
  },
  {
    path: '/',
    redirect: '/essays',
  },
  {
    path: '/',
    component: () => import('../views/MainLayout.vue'),
    children: [
      {
        path: 'essays',
        name: 'EssayList',
        component: EssayListView,
        meta: { title: '我的作文' },
      },
      {
        path: 'essays/create',
        name: 'EssayCreateText',
        component: EssayCreateTextView,
        meta: { title: '新建作文' },
      },
      {
        path: 'essays/:id',
        name: 'EssayDetail',
        component: EssayDetailView,
        meta: { title: '作文详情' },
        props: true,
      },
      {
        path: 'reviews',
        name: 'ReviewList',
        component: ReviewListView,
        meta: { title: '批改记录' },
      },
      {
        path: 'reviews/:reviewId',
        name: 'ReviewDetail',
        component: ReviewDetailView,
        meta: { title: '批改详情' },
        props: true,
      },
      {
        path: 'reviews/:reviewId/image',
        name: 'ReviewImageDetail',
        component: ReviewImageDetailView,
        meta: { title: '批改详情（图片）' },
        props: true,
      },
      {
        path: 'dimensions',
        name: 'DimensionList',
        component: DimensionListView,
        meta: { title: '评分维度配置' },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  if (to.meta.public) {
    // 已登录访问登录页，跳到首页
    if (to.path === '/login' && authStore.isAuthenticated) {
      return next('/essays');
    }
    return next();
  }

  if (!authStore.isAuthenticated) {
    return next({
      path: '/login',
      query: { redirect: to.fullPath },
    });
  }

  next();
});

export default router;


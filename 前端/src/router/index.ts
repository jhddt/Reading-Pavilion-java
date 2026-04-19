import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../store/auth'

const LoginView = () => import('../views/LoginView.vue')
const RegisterView = () => import('../views/RegisterView.vue')
const EssayListView = () => import('../views/EssayListView.vue')
const EssayCreateTextView = () => import('../views/EssayCreateTextView.vue')
const EssayDetailView = () => import('../views/EssayDetailView.vue')
const ReviewListView = () => import('../views/ReviewListView.vue')
const ReviewAnnotationView = () => import('../views/ReviewAnnotationView.vue')
const ReviewDetailView = () => import('../views/ReviewDetailView.vue')
const ReviewRereviewView = () => import('../views/ReviewRereviewView.vue')
const DimensionListView = () => import('../views/DimensionListView.vue')
const ReviewRuleCreateView = () => import('../views/ReviewRuleCreateView.vue')
const ReviewRuleEditView = () => import('../views/ReviewRuleEditView.vue')
const NotFoundView = () => import('../views/NotFoundView.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { public: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterView,
    meta: { public: true },
  },
  {
    path: '/',
    redirect: '/essays',
  },
  {
    path: '/preview/404',
    name: 'NotFoundPreview',
    component: NotFoundView,
    meta: { public: true, title: '404 页面预览' },
  },
  {
    path: '/404-preview',
    redirect: '/preview/404',
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
        name: 'ReviewAnnotationDetail',
        component: ReviewAnnotationView,
        meta: { title: '批改详情' },
        props: true,
      },
      {
        path: 'reviews/:reviewId/rerun',
        name: 'ReviewRereview',
        component: ReviewRereviewView,
        meta: { title: '继续批改' },
        props: true,
      },
      {
        path: 'reviews/:reviewId/summary',
        name: 'ReviewSummaryDetail',
        component: ReviewDetailView,
        meta: { title: '批改详情总览' },
        props: true,
      },
      {
        path: 'dimensions',
        name: 'DimensionList',
        component: DimensionListView,
        meta: { title: '评分维度配置' },
      },
      {
        path: 'dimensions/create',
        name: 'DimensionCreate',
        component: ReviewRuleCreateView,
        meta: { title: '新增评分细则' },
      },
      {
        path: 'dimensions/:ruleId/edit',
        name: 'DimensionEdit',
        component: ReviewRuleEditView,
        meta: { title: '编辑评分细则' },
        props: true,
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFoundView,
    meta: { public: true, title: '页面未找到' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  if (to.meta.public) {
    // 已登录访问登录页，跳到首页
    if ((to.path === '/login' || to.path === '/register') && authStore.isAuthenticated) {
      return next('/essays')
    }
    return next()
  }

  if (!authStore.isAuthenticated) {
    return next({
      path: '/login',
      query: { redirect: to.fullPath },
    })
  }

  next()
})

export default router

import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { useAuthStore } from '../store/auth'

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 120000, // 120秒超时（批改接口已改为异步，但保留较长超时作为兜底）
})

const isAuthExpiredMessage = (message?: string | null) => {
  if (!message) return false
  return ['登录已失效', '重新登录', '未认证', 'token', 'jwt', '认证失败', '无效登录'].some((keyword) =>
    message.toLowerCase().includes(keyword.toLowerCase())
  )
}

const redirectToLogin = () => {
  const authStore = useAuthStore()
  authStore.logout()
  const current = `${window.location.pathname}${window.location.search}`
  const target = current && current !== '/login' ? `/login?redirect=${encodeURIComponent(current)}` : '/login'
  window.location.replace(target)
}

instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = authStore.authHeader
    }
    return config
  },
  (error) => Promise.reject(error)
)

instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const data = response.data
    // 兼容后端 Result 结构：{ code, message, data }
    if (data && typeof data === 'object' && 'code' in data && data.code !== 200) {
      if (isAuthExpiredMessage(data.message)) {
        redirectToLogin()
      }
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data as any
  },
  (error) => {
    const message = error?.response?.data?.message || error?.message
    if ((error.response && error.response.status === 401) || isAuthExpiredMessage(message)) {
      redirectToLogin()
    }
    return Promise.reject(error)
  }
)

export default instance

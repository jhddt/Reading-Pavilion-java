import axios from 'axios';
import { useAuthStore } from '../store/auth';

const instance = axios.create({
  baseURL: '/api',
  timeout: 120000, // 120秒超时（批改接口已改为异步，但保留较长超时作为兜底）
});

instance.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore();
    if (authStore.token) {
      config.headers.Authorization = authStore.authHeader;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

instance.interceptors.response.use(
  (response) => {
    const data = response.data;
    // 兼容后端 Result 结构：{ code, message, data }
    if (data && typeof data === 'object' && 'code' in data && data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败'));
    }
    return data;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      const authStore = useAuthStore();
      authStore.logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

export default instance;


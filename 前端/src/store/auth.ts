import { defineStore } from 'pinia'

const TOKEN_KEY = 'rp_token'
const USER_KEY = 'rp_user'

interface User {
  userId: number | null
  userName: string
  role: string | number | null
  avatarUrl?: string | null
  avatarPreviewUrl?: string | null
  status?: number | null
  avatarUpdateTime?: string | null
  createTime?: string | null
  updateTime?: string | null
}

interface AuthState {
  token: string
  user: User | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: (() => {
      try {
        const raw = localStorage.getItem(USER_KEY)
        return raw ? JSON.parse(raw) : null
      } catch {
        return null
      }
    })(),
  }),
  getters: {
    isAuthenticated: (state): boolean => !!state.token,
    authHeader: (state): string => (state.token ? `Bearer ${state.token}` : ''),
    userName: (state): string => state.user?.userName || '',
    userId: (state): number | null => state.user?.userId ?? null,
    role: (state): string | number | null => state.user?.role ?? null,
  },
  actions: {
    setToken(token: string) {
      this.token = token
      if (token) {
        localStorage.setItem(TOKEN_KEY, token)
      } else {
        localStorage.removeItem(TOKEN_KEY)
      }
    },
    setUser(user: User | null) {
      this.user = user || null
      if (this.user) {
        localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      } else {
        localStorage.removeItem(USER_KEY)
      }
    },
    logout() {
      this.setToken('')
      this.setUser(null)
    },
  },
})

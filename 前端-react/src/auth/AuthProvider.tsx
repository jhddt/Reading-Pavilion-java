import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import { api } from '../lib/api'
import type { LoginResponse, UserProfile } from '../types'

type AuthContextValue = {
  token: string
  user: UserProfile | null
  loading: boolean
  isAuthenticated: boolean
  login: (login: LoginResponse) => void
  logout: () => void
  refreshProfile: () => Promise<UserProfile | null>
  updateUser: (user: UserProfile | null) => void
}

const TOKEN_KEY = 'rp_react_token'
const USER_KEY = 'rp_react_user'

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: PropsWithChildren) {
  const [token, setToken] = useState<string>(() => localStorage.getItem(TOKEN_KEY) || '')
  const [user, setUser] = useState<UserProfile | null>(() => {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserProfile) : null
  })
  const [loading, setLoading] = useState(false)

  const updateUser = (nextUser: UserProfile | null) => {
    setUser(nextUser)
    if (nextUser) {
      localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }

  const logout = () => {
    setToken('')
    updateUser(null)
    localStorage.removeItem(TOKEN_KEY)
  }

  const login = (loginData: LoginResponse) => {
    setToken(loginData.token)
    localStorage.setItem(TOKEN_KEY, loginData.token)
    updateUser({
      userId: loginData.userId,
      userName: loginData.userName,
      role: loginData.role,
    })
  }

  const refreshProfile = async () => {
    if (!token) return null
    setLoading(true)
    try {
      const profile = await api.get<UserProfile>('/user/me', token)
      updateUser(profile)
      return profile
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!token || user?.createTime) return
    refreshProfile().catch(() => undefined)
  }, [token, user?.createTime])

  const value = useMemo(
    () => ({
      token,
      user,
      loading,
      isAuthenticated: Boolean(token),
      login,
      logout,
      refreshProfile,
      updateUser,
    }),
    [loading, token, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}

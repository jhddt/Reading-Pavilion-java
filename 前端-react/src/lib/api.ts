const API_BASE = '/api'
const TOKEN_KEY = 'rp_react_token'
const USER_KEY = 'rp_react_user'

type RequestOptions = RequestInit & {
  token?: string
}

function isAuthExpiredMessage(message?: string | null) {
  if (!message) return false
  return ['登录已失效', '重新登录', '未认证', 'token', 'jwt', '认证失败', '无效登录'].some((keyword) =>
    message.toLowerCase().includes(keyword.toLowerCase()),
  )
}

function redirectToLogin() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  const current = `${window.location.pathname}${window.location.search}`
  const target = current && current !== '/login' ? `/login?redirect=${encodeURIComponent(current)}` : '/login'
  window.location.replace(target)
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers)

  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.token) {
    headers.set('Authorization', `Bearer ${options.token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  const rawText = await response.text()
  const payload = rawText ? JSON.parse(rawText) : null

  if (response.status === 401) {
    redirectToLogin()
    const error = new Error('登录已失效，请重新登录')
    ;(error as Error & { status?: number }).status = 401
    throw error
  }

  if (!response.ok) {
    if (isAuthExpiredMessage(payload?.message)) {
      redirectToLogin()
    }
    throw new Error(payload?.message || '请求失败')
  }

  if (payload && typeof payload === 'object' && 'code' in payload && payload.code !== 200) {
    if (isAuthExpiredMessage(payload.message)) {
      redirectToLogin()
    }
    throw new Error(payload.message || '请求失败')
  }

  return payload?.data as T
}

export const api = {
  get: <T>(path: string, token?: string) => request<T>(path, { method: 'GET', token }),
  post: <T>(path: string, body?: BodyInit | object | null, token?: string) =>
    request<T>(path, {
      method: 'POST',
      body: body instanceof FormData ? body : body != null ? JSON.stringify(body) : null,
      token,
    }),
  put: <T>(path: string, body?: BodyInit | object | null, token?: string) =>
    request<T>(path, {
      method: 'PUT',
      body: body instanceof FormData ? body : body != null ? JSON.stringify(body) : null,
      token,
    }),
  patch: <T>(path: string, body?: BodyInit | object | null, token?: string) =>
    request<T>(path, {
      method: 'PATCH',
      body: body instanceof FormData ? body : body != null ? JSON.stringify(body) : null,
      token,
    }),
  delete: <T>(path: string, token?: string) => request<T>(path, { method: 'DELETE', token }),
}

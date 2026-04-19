import { useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'
import type { LoginResponse } from '../types'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()
  const [userName, setUserName] = useState('')
  const [password, setPassword] = useState('')
  const [registerUserName, setRegisterUserName] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [registerPassword2, setRegisterPassword2] = useState('')
  const [registerMode, setRegisterMode] = useState(false)
  const [showLoginPassword, setShowLoginPassword] = useState(false)
  const [showRegisterPassword, setShowRegisterPassword] = useState(false)
  const [showRegisterPassword2, setShowRegisterPassword2] = useState(false)
  const [loading, setLoading] = useState(false)
  const [registerLoading, setRegisterLoading] = useState(false)
  const [error, setError] = useState('')
  const [registerError, setRegisterError] = useState('')
  const loginPasswordRef = useRef<HTMLInputElement | null>(null)

  const redirect = new URLSearchParams(location.search).get('redirect') || '/dashboard'

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!userName || !password) {
      setError(!userName ? '请输入用户名' : '请输入密码')
      return
    }
    setLoading(true)
    setError('')
    try {
      const data = await api.post<LoginResponse>('/user/login', { username: userName, password })
      login(data)
      navigate(redirect, { replace: true })
    } catch (err) {
      setError((err as Error).message || '登录失败')
    } finally {
      setLoading(false)
    }
  }

  const onRegisterSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!registerUserName || !registerPassword || !registerPassword2) {
      setRegisterError('请完整填写注册信息')
      return
    }
    if (registerPassword !== registerPassword2) {
      setRegisterError('两次输入的密码不一致')
      return
    }

    setRegisterLoading(true)
    setRegisterError('')
    try {
      await api.post<void>('/user/add', {
        userName: registerUserName,
        password: registerPassword,
      })
      const data = await api.post<LoginResponse>('/user/login', {
        username: registerUserName,
        password: registerPassword,
      })
      login(data)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setRegisterError((err as Error).message || '注册失败')
    } finally {
      setRegisterLoading(false)
    }
  }

  const EyeIcon = ({ open }: { open: boolean }) =>
    open ? (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M2.4 12s3.6-6 9.6-6 9.6 6 9.6 6-3.6 6-9.6 6-9.6-6-9.6-6Z" fill="none" stroke="currentColor" strokeWidth="1.8" />
        <circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" strokeWidth="1.8" />
      </svg>
    ) : (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M3 3l18 18" fill="none" stroke="currentColor" strokeWidth="1.8" />
        <path d="M5.2 7.1C3.3 9.1 2.4 12 2.4 12s3.6 6 9.6 6c2.1 0 3.9-.6 5.4-1.6" fill="none" stroke="currentColor" strokeWidth="1.8" />
        <path d="M9.2 5.6A10.7 10.7 0 0 1 12 6c6 0 9.6 6 9.6 6s-.9 1.4-2.5 2.8" fill="none" stroke="currentColor" strokeWidth="1.8" />
      </svg>
    )

  return (
    <div className="auth-page">
      <div className="auth-flip-shell">
        <div className={`auth-flip-switch ${registerMode ? 'is-register' : ''}`}>
          <input
            className="auth-flip-toggle"
            type="checkbox"
            checked={registerMode}
            onChange={(event) => setRegisterMode(event.target.checked)}
          />
          <button
            type="button"
            className="auth-flip-slider"
            onClick={() => setRegisterMode((value) => !value)}
            aria-label={registerMode ? '切换到登录' : '切换到注册'}
          />
          <span className="auth-flip-side" />
          <div className="auth-flip-inner">
            <div className="auth-flip-front">
              <div className="auth-flip-title">登录 PenPilot</div>
              <form className="auth-flip-form" onSubmit={onSubmit}>
                <input
                  className="auth-flip-input"
                  placeholder="用户名"
                  value={userName}
                  onChange={(event) => setUserName(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault()
                      loginPasswordRef.current?.focus()
                    }
                  }}
                />
                <div className="auth-flip-password">
                  <input
                    ref={loginPasswordRef}
                    className="auth-flip-input"
                    type={showLoginPassword ? 'text' : 'password'}
                    placeholder="密码"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                  />
                  <button
                    type="button"
                    className="auth-flip-eye"
                    onClick={() => setShowLoginPassword((value) => !value)}
                    aria-label={showLoginPassword ? '隐藏密码' : '显示密码'}
                  >
                    <EyeIcon open={showLoginPassword} />
                  </button>
                </div>
                {error ? <div className="feedback error">{error}</div> : null}
                <button type="submit" className="auth-flip-btn" disabled={loading}>
                  {loading ? '登录中...' : '登录'}
                </button>
              </form>
            </div>
            <div className="auth-flip-back">
              <div className="auth-flip-title">注册账号</div>
              <form className="auth-flip-form" onSubmit={onRegisterSubmit}>
                <input
                  className="auth-flip-input"
                  placeholder="用户名"
                  value={registerUserName}
                  onChange={(event) => setRegisterUserName(event.target.value)}
                />
                <div className="auth-flip-password">
                  <input
                    className="auth-flip-input"
                    type={showRegisterPassword ? 'text' : 'password'}
                    placeholder="密码"
                    value={registerPassword}
                    onChange={(event) => setRegisterPassword(event.target.value)}
                  />
                  <button
                    type="button"
                    className="auth-flip-eye"
                    onClick={() => setShowRegisterPassword((value) => !value)}
                    aria-label={showRegisterPassword ? '隐藏密码' : '显示密码'}
                  >
                    <EyeIcon open={showRegisterPassword} />
                  </button>
                </div>
                <div className="auth-flip-password">
                  <input
                    className="auth-flip-input"
                    type={showRegisterPassword2 ? 'text' : 'password'}
                    placeholder="确认密码"
                    value={registerPassword2}
                    onChange={(event) => setRegisterPassword2(event.target.value)}
                  />
                  <button
                    type="button"
                    className="auth-flip-eye"
                    onClick={() => setShowRegisterPassword2((value) => !value)}
                    aria-label={showRegisterPassword2 ? '隐藏密码' : '显示密码'}
                  >
                    <EyeIcon open={showRegisterPassword2} />
                  </button>
                </div>
                {registerError ? <div className="feedback error">{registerError}</div> : null}
                <button type="submit" className="auth-flip-btn" disabled={registerLoading}>
                  {registerLoading ? '注册中...' : '注册并登录'}
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

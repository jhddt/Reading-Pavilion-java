import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'
import type { LoginResponse } from '../types'

export function RegisterPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [form, setForm] = useState({
    userName: '',
    password: '',
    password2: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (form.password !== form.password2) {
      setError('两次输入的密码不一致')
      return
    }

    setLoading(true)
    setError('')
    try {
      await api.post<void>('/user/add', {
        userName: form.userName,
        password: form.password,
      })
      const loginData = await api.post<LoginResponse>('/user/login', {
        username: form.userName,
        password: form.password,
      })
      login(loginData)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError((err as Error).message || '注册失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <p className="eyebrow">Reading Pavilion</p>
        <h2>注册新账号</h2>
        <form className="form-grid" onSubmit={onSubmit}>
          <label className="field">
            <span>用户名</span>
            <input
              value={form.userName}
              onChange={(event) => setForm((prev) => ({ ...prev, userName: event.target.value }))}
            />
          </label>
          <label className="field">
            <span>密码</span>
            <input
              type="password"
              value={form.password}
              onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
            />
          </label>
          <label className="field">
            <span>确认密码</span>
            <input
              type="password"
              value={form.password2}
              onChange={(event) => setForm((prev) => ({ ...prev, password2: event.target.value }))}
            />
          </label>
          {error ? <div className="feedback error">{error}</div> : null}
          <button type="submit" className="primary-button full-button" disabled={loading}>
            {loading ? '注册中...' : '注册并进入系统'}
          </button>
        </form>
        <p className="auth-switch">
          已有账号？<Link to="/login">立即登录</Link>
        </p>
      </div>
    </div>
  )
}

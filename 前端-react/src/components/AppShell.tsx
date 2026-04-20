import { useEffect, useId, useMemo, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'
import { hasAnyRole } from '../auth/roles'
import { formatDateTime, roleText } from '../lib/format'

const navigationItems = [
  { to: '/dashboard', label: '概览', icon: '📊', description: '系统概览与快捷入口' },
  { to: '/essays', label: '作文', icon: '📝', description: '作文列表与新建内容' },
  { to: '/reviews', label: '批改', icon: '✅', description: '批改记录与结果查看' },
  { to: '/dimensions', label: '细则', icon: '📐', description: '批改细则与规则管理', minRole: 'STUDENT' },
  { to: '/dimension-library', label: '维度', icon: '📏', description: '公共评分维度库管理', adminOnly: true },
  { to: '/audit-logs', label: '日志', icon: '🧾', description: '管理员操作审计日志', adminOnly: true },
  { to: '/users/import', label: '用户', icon: '👥', description: '管理员管理全部用户与导入', adminOnly: true },
]

export function AppShell() {
  const navigate = useNavigate()
  const avatarInputId = useId()
  const { token, user, logout, updateUser, refreshProfile } = useAuth()
  const visibleNavigationItems = useMemo(
    () =>
      navigationItems.filter((item) => {
        if ('adminOnly' in item && item.adminOnly) return hasAnyRole(user?.role, ['ADMIN'])
        if ('minRole' in item && item.minRole) return hasAnyRole(user?.role, ['STUDENT', 'TEACHER', 'ADMIN'])
        return true
      }),
    [user?.role],
  )

  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [profileEditing, setProfileEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [avatarUploading, setAvatarUploading] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [userName, setUserName] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)

  useEffect(() => {
    setUserName(user?.userName || '')
  }, [user?.userName])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const submitProfile = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!userName.trim()) {
      setError('用户名不能为空')
      return
    }
    if (newPassword && !currentPassword) {
      setError('修改密码时请填写当前密码')
      return
    }

    setSaving(true)
    setError('')
    setMessage('')
    try {
      await api.put<void>(
        '/user/me',
        {
          userName: userName.trim(),
          currentPassword: currentPassword || null,
          newPassword: newPassword || null,
        },
        token,
      )
      await refreshProfile()
      setCurrentPassword('')
      setNewPassword('')
      setMessage('个人信息已更新')
    } catch (err) {
      setError((err as Error).message || '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const uploadAvatar = async () => {
    if (!selectedFile) {
      setError('请先选择头像图片')
      return
    }
    setAvatarUploading(true)
    setError('')
    setMessage('')
    try {
      const formData = new FormData()
      formData.append('file', selectedFile)
      const profile = await api.post<typeof user>('/user/me/avatar', formData, token)
      updateUser(profile ?? null)
      setSelectedFile(null)
      setMessage('头像上传成功')
    } catch (err) {
      setError((err as Error).message || '头像上传失败')
    } finally {
      setAvatarUploading(false)
    }
  }

  const closeProfileModal = () => {
    setProfileOpen(false)
    setProfileEditing(false)
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
        <div className="brand-block">
          <div className="brand-mark">RP</div>
          <div>
            <h1>PenPilot</h1>
          </div>
        </div>

        <nav className="sidebar-nav">
          {visibleNavigationItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-item ${isActive ? 'nav-item-active' : ''}`}
              onClick={() => setSidebarOpen(false)}
            >
              <span className="nav-item-key" aria-hidden="true">
                {item.icon}
              </span>
              <span>
                <strong>{item.label}</strong>
              </span>
            </NavLink>
          ))}
        </nav>

        <button type="button" className="user-panel plain-button" onClick={() => setProfileOpen(true)} title={user?.userName || '当前用户'}>
          <div className="user-avatar">
            {user?.avatarPreviewUrl ? (
              <img src={user.avatarPreviewUrl} alt={user.userName || '用户头像'} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} />
            ) : (
              (user?.userName || '用').slice(0, 1).toUpperCase()
            )}
          </div>
        </button>
      </aside>

      <div className="main-wrap">
        <main className="content-area">
          <Outlet />
        </main>
      </div>

      {profileOpen ? (
        <div className="overlay" onClick={closeProfileModal}>
          <div className="modal-card profile-modal-shell" onClick={(event) => event.stopPropagation()}>
            <div className={`profile-showcase ${profileEditing ? 'is-expanded' : ''}`}>
              <div className="profile-card-fancy">
                <div className="profile-card-avatar">
                  {user?.avatarPreviewUrl ? (
                    <img src={user.avatarPreviewUrl} alt={user.userName || '用户头像'} />
                  ) : (
                    <div className="profile-card-avatar-fallback">
                      {(user?.userName || 'U').slice(0, 1).toUpperCase()}
                    </div>
                  )}
                  <div className="profile-card-accent" />
                </div>

                <div className="profile-card-headings">
                  <p>{user?.userName || '当前用户'}</p>
                  <span>{roleText(user?.role)}</span>
                </div>

                <div className="profile-card-details">
                  <div>
                    <strong>用户 ID</strong>
                    <span>{user?.userId ?? '-'}</span>
                  </div>
                  <div>
                    <strong>创建时间</strong>
                    <span>{formatDateTime(user?.createTime)}</span>
                  </div>
                  <div>
                    <strong>更新时间</strong>
                    <span>{formatDateTime(user?.updateTime)}</span>
                  </div>
                  <div>
                    <strong>账户状态</strong>
                    <span>{user?.status === 1 || user?.status === '1' ? '启用' : '未知'}</span>
                  </div>
                </div>

                <div className="profile-card-actions">
                  <button
                    type="button"
                    className="profile-card-edit-button"
                    onClick={() => setProfileEditing((value) => !value)}
                  >
                    <span>修改个人信息</span>
                    <span className={`profile-card-edit-arrow ${profileEditing ? 'is-open' : ''}`}>›</span>
                  </button>
                  <button type="button" className="profile-card-logout-button" onClick={handleLogout}>
                    退出登录
                  </button>
                </div>

                <div className="profile-card-bar" />
              </div>

              <div className={`profile-edit-drawer ${profileEditing ? 'is-open' : ''}`}>
                <form className="form-grid profile-edit-panel" onSubmit={submitProfile}>
                  <label className="field">
                    <span>用户名</span>
                    <input value={userName} onChange={(event) => setUserName(event.target.value)} />
                  </label>
                  <label className="field">
                    <span>当前密码</span>
                    <input
                      type="password"
                      value={currentPassword}
                      onChange={(event) => setCurrentPassword(event.target.value)}
                      placeholder="修改密码时填写"
                    />
                  </label>
                  <label className="field">
                    <span>新密码</span>
                    <input
                      type="password"
                      value={newPassword}
                      onChange={(event) => setNewPassword(event.target.value)}
                      placeholder="不修改可留空"
                    />
                  </label>
                  <div className="field">
                    <span>上传头像</span>
                    <div className="upload-card-box profile-upload-card">
                      <label htmlFor={avatarInputId} className="upload-card-header">
                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                          <path d="M7 10V9C7 6.23858 9.23858 4 12 4C14.7614 4 17 6.23858 17 9V10C19.2091 10 21 11.7909 21 14C21 15.4806 20.1956 16.8084 19 17.5M7 10C4.79086 10 3 11.7909 3 14C3 15.4806 3.8044 16.8084 5 17.5M7 10C7.43285 10 7.84965 10.0688 8.24006 10.1959M12 12V21M12 12L15 15M12 12L9 15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                        <p>Browse File to upload!</p>
                      </label>
                      <label htmlFor={avatarInputId} className="upload-card-footer">
                        <svg viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
                          <path d="M15.331 6H8.5v20h15V14.154h-8.169z" />
                          <path d="M18.153 6h-.009v5.342H23.5v-.002z" />
                        </svg>
                        <p>{selectedFile?.name || 'Not selected file'}</p>
                        <button
                          type="button"
                          className="upload-card-trash"
                          onClick={(event) => {
                            event.preventDefault()
                            event.stopPropagation()
                            setSelectedFile(null)
                          }}
                          disabled={!selectedFile}
                        >
                          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M5.16565 10.1534C5.07629 8.99181 5.99473 8 7.15975 8H16.8402C18.0053 8 18.9237 8.9918 18.8344 10.1534L18.142 19.1534C18.0619 20.1954 17.193 21 16.1479 21H7.85206C6.80699 21 5.93811 20.1954 5.85795 19.1534L5.16565 10.1534Z" stroke="currentColor" strokeWidth="2" />
                            <path d="M19.5 5H4.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                            <path d="M10 3C10 2.44772 10.4477 2 11 2H13C13.5523 2 14 2.44772 14 3V5H10V3Z" stroke="currentColor" strokeWidth="2" />
                          </svg>
                        </button>
                      </label>
                      <input
                        id={avatarInputId}
                        className="upload-card-input"
                        type="file"
                        accept="image/png,image/jpeg,image/jpg"
                        onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
                      />
                    </div>
                  </div>
                  {message ? <div className="feedback success">{message}</div> : null}
                  {error ? <div className="feedback error">{error}</div> : null}
                  <div className="action-row">
                    <button type="button" className="secondary-button" onClick={uploadAvatar} disabled={avatarUploading}>
                      {avatarUploading ? '上传中...' : '上传头像'}
                    </button>
                    <button type="submit" className="primary-button" disabled={saving}>
                      {saving ? '保存中...' : '保存修改'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

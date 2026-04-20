import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'

type ImportResult = {
  totalRows: number
  successCount: number
  failCount: number
  errors: string[]
}

type AdminUser = {
  id: number
  userName: string
  role: number
  status: number
  createTime?: string
}

export function UserImportPage() {
  const { token, user } = useAuth()
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<ImportResult | null>(null)
  const [users, setUsers] = useState<AdminUser[]>([])
  const [usersLoading, setUsersLoading] = useState(false)
  const [usersError, setUsersError] = useState('')
  const [savingId, setSavingId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)

  const roleText = (role: number) => {
    if (role === 3) return '管理员'
    if (role === 2) return '教师'
    return '学生'
  }

  const loadUsers = async () => {
    setUsersLoading(true)
    setUsersError('')
    try {
      const data = await api.get<AdminUser[]>('/user/selectAll', token)
      setUsers((data || []).sort((a, b) => a.id - b.id))
    } catch (err) {
      setUsersError((err as Error).message || '用户列表加载失败')
    } finally {
      setUsersLoading(false)
    }
  }

  useEffect(() => {
    loadUsers().catch(() => undefined)
  }, [token])

  const submitImport = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!file) {
      setError('请先选择 CSV 文件')
      return
    }
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const data = await api.post<ImportResult>('/user/admin/import', formData, token)
      setResult(data)
      await loadUsers()
    } catch (err) {
      setError((err as Error).message || '导入失败')
    } finally {
      setLoading(false)
    }
  }

  const patchUser = (id: number, patch: Partial<AdminUser>) => {
    setUsers((prev) => prev.map((item) => (item.id === id ? { ...item, ...patch } : item)))
  }

  const saveUser = async (item: AdminUser) => {
    setSavingId(item.id)
    setUsersError('')
    try {
      await api.put<void>(
        `/user/${item.id}`,
        {
          userName: item.userName,
          role: item.role,
          status: item.status,
        },
        token,
      )
    } catch (err) {
      setUsersError((err as Error).message || '保存失败')
    } finally {
      setSavingId(null)
    }
  }

  const removeUser = async (item: AdminUser) => {
    if (item.id === user?.userId) {
      setUsersError('不能删除当前登录账号')
      return
    }
    if (!window.confirm(`确定删除用户「${item.userName}」吗？`)) return
    setDeletingId(item.id)
    setUsersError('')
    try {
      await api.delete<void>(`/user/${item.id}`, token)
      setUsers((prev) => prev.filter((current) => current.id !== item.id))
    } catch (err) {
      setUsersError((err as Error).message || '删除失败')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="page-grid">
      <section className="panel user-admin-panel enhanced-user-panel">
        <div className="user-admin-heading enhanced-heading">
          <div>
            <p className="eyebrow">管理员能力</p>
            <h3>全部用户管理</h3>
          </div>
          <div className="heading-actions">
            <span className="total-users-badge">共 {users.length} 个用户</span>
          </div>
        </div>
        {usersError ? <div className="feedback error">{usersError}</div> : null}
        {usersLoading ? (
          <div className="feedback loading-feedback">用户列表加载中...</div>
        ) : (
          <div className="stack-list user-admin-list enhanced-list">
            {users.map((item) => (
              <article key={item.id} className="list-row user-admin-row enhanced-user-row">
                <div className="user-admin-meta">
                  <div className="user-row-avatar">
                    {item.userName.slice(0, 1).toUpperCase()}
                  </div>
                  <div className="user-row-info">
                    <div className="user-row-title">
                      <strong>{item.userName}</strong>
                      <span className={`status-badge ${item.status === 1 ? 'status-active' : 'status-disabled'}`}>
                        {item.status === 1 ? '已启用' : '已禁用'}
                      </span>
                    </div>
                    <p>
                      <span>ID: {item.id}</span>
                      <span className="dot-divider">·</span>
                      <span>角色: {roleText(item.role)}</span>
                    </p>
                  </div>
                </div>
                <div className="action-row user-admin-actions">
                  <select
                    className="user-admin-select enhanced-select"
                    value={item.role}
                    onChange={(event) => patchUser(item.id, { role: Number(event.target.value) })}
                  >
                    <option value={1}>学生</option>
                    <option value={2}>教师</option>
                    <option value={3}>管理员</option>
                  </select>
                  <select
                    className="user-admin-select enhanced-select"
                    value={item.status}
                    onChange={(event) => patchUser(item.id, { status: Number(event.target.value) })}
                  >
                    <option value={1}>启用</option>
                    <option value={0}>禁用</option>
                  </select>
                  <div className="btn-group">
                    <button
                      type="button"
                      className="secondary-button user-admin-btn enhanced-btn"
                      onClick={() => saveUser(item)}
                      disabled={savingId === item.id}
                    >
                      {savingId === item.id ? '保存中' : '保存'}
                    </button>
                    <button
                      type="button"
                      className="danger-button user-admin-btn enhanced-btn-danger"
                      onClick={() => removeUser(item)}
                      disabled={deletingId === item.id}
                    >
                      {deletingId === item.id ? '删除中' : '删除'}
                    </button>
                  </div>
                </div>
              </article>
            ))}
            {!users.length ? <div className="empty-state">暂无用户数据</div> : null}
          </div>
        )}
      </section>

      <section className="split-grid enhanced-split-grid">
        <article className="panel enhanced-import-panel">
          <div className="import-header">
            <div className="icon-circle">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
            </div>
            <div>
              <p className="eyebrow">管理员能力</p>
              <h3>批量导入用户</h3>
            </div>
          </div>
          <form className="form-grid import-form" onSubmit={submitImport}>
            <div className="upload-zone">
              <input
                type="file"
                id="csv-upload"
                className="file-input-hidden"
                accept=".csv,text/csv"
                onChange={(event) => setFile(event.target.files?.[0] || null)}
              />
              <label htmlFor="csv-upload" className="upload-label">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="upload-icon"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="12" y1="18" x2="12" y2="12"></line><line x1="9" y1="15" x2="12" y2="12"></line><line x1="15" y1="15" x2="12" y2="12"></line></svg>
                <span className="upload-text">
                  {file ? file.name : '点击选择 CSV 文件，或拖拽文件到此处'}
                </span>
                {file && <span className="upload-hint">已选择文件，准备导入</span>}
              </label>
            </div>
            <button type="submit" className="primary-button import-submit-btn" disabled={loading || !file}>
              {loading ? '正在导入中...' : '开始批量导入'}
            </button>
          </form>
          {error ? <div className="feedback error">{error}</div> : null}
        </article>

        <article className="panel panel-soft enhanced-doc-panel">
          <div className="doc-header">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
            <div>
              <p className="eyebrow">文件格式说明</p>
              <h3>CSV 列定义要求</h3>
            </div>
          </div>
          <div className="doc-content">
            <ul className="plain-list doc-list">
              <li>
                <span className="doc-bullet"></span>
                <span><strong>表头要求：</strong> 必须包含 <code className="code-inline">userName,password,role,status</code></span>
              </li>
              <li>
                <span className="doc-bullet"></span>
                <span><strong>角色映射：</strong> <code className="code-inline">1</code> 代表学生，<code className="code-inline">2</code> 代表老师，<code className="code-inline">3</code> 代表管理员</span>
              </li>
              <li>
                <span className="doc-bullet"></span>
                <span><strong>状态设定：</strong> <code className="code-inline">1</code> 代表启用，<code className="code-inline">0</code> 代表禁用（选填，默认启用）</span>
              </li>
              <li className="doc-example">
                <span className="doc-bullet doc-bullet-example"></span>
                <span><strong>数据示例：</strong> <code className="code-block">zhangsan,123456,1,1</code></span>
              </li>
            </ul>
          </div>
        </article>
      </section>

      {result ? (
        <section className="panel">
          <p className="eyebrow">导入结果</p>
          <h3>成功 {result.successCount} / 总计 {result.totalRows}</h3>
          <p>失败 {result.failCount}</p>
          {result.errors?.length ? (
            <div className="stack-list">
              {result.errors.map((item, idx) => (
                <div key={`${item}-${idx}`} className="feedback error">
                  {item}
                </div>
              ))}
            </div>
          ) : (
            <div className="feedback success">未发现错误</div>
          )}
        </section>
      ) : null}
    </div>
  )
}

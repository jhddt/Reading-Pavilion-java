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
      <section className="panel user-admin-panel">
        <div className="user-admin-heading">
          <p className="eyebrow">管理员能力</p>
          <h3>全部用户管理</h3>
        </div>
        {usersError ? <div className="feedback error">{usersError}</div> : null}
        {usersLoading ? (
          <div className="feedback">用户列表加载中...</div>
        ) : (
          <div className="stack-list user-admin-list">
            {users.map((item) => (
              <article key={item.id} className="list-row user-admin-row">
                <div className="user-admin-meta">
                  <strong>{item.userName}</strong>
                  <p>ID: {item.id} · 角色: {roleText(item.role)} · 状态: {item.status === 1 ? '启用' : '禁用'}</p>
                </div>
                <div className="action-row user-admin-actions">
                  <select
                    className="user-admin-select"
                    value={item.role}
                    onChange={(event) => patchUser(item.id, { role: Number(event.target.value) })}
                  >
                    <option value={1}>学生</option>
                    <option value={2}>教师</option>
                    <option value={3}>管理员</option>
                  </select>
                  <select
                    className="user-admin-select"
                    value={item.status}
                    onChange={(event) => patchUser(item.id, { status: Number(event.target.value) })}
                  >
                    <option value={1}>启用</option>
                    <option value={0}>禁用</option>
                  </select>
                  <button
                    type="button"
                    className="secondary-button user-admin-btn"
                    onClick={() => saveUser(item)}
                    disabled={savingId === item.id}
                  >
                    {savingId === item.id ? '保存中...' : '保存'}
                  </button>
                  <button
                    type="button"
                    className="danger-button user-admin-btn"
                    onClick={() => removeUser(item)}
                    disabled={deletingId === item.id}
                  >
                    {deletingId === item.id ? '删除中...' : '删除'}
                  </button>
                </div>
              </article>
            ))}
            {!users.length ? <div className="empty-state">暂无用户数据</div> : null}
          </div>
        )}
      </section>

      <section className="split-grid">
        <article className="panel">
          <p className="eyebrow">管理员能力</p>
          <h3>批量导入用户</h3>
          <form className="form-grid" onSubmit={submitImport}>
            <label className="field">
              <span>选择 CSV 文件</span>
              <input
                type="file"
                accept=".csv,text/csv"
                onChange={(event) => setFile(event.target.files?.[0] || null)}
              />
            </label>
            <button type="submit" className="primary-button" disabled={loading}>
              {loading ? '导入中...' : '开始导入'}
            </button>
          </form>
          {error ? <div className="feedback error">{error}</div> : null}
        </article>

        <article className="panel panel-soft">
          <p className="eyebrow">文件格式</p>
          <h3>CSV 列定义</h3>
          <ul className="plain-list">
            <li>表头：`userName,password,role,status`</li>
            <li>角色：`1` 学生，`2` 老师，`3` 管理员</li>
            <li>状态：`1` 启用，`0` 禁用（可选，默认 1）</li>
            <li>示例：`zhangsan,123456,1,1`</li>
          </ul>
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

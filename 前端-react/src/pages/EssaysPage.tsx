import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { hasAnyRole } from '../auth/roles'
import { api } from '../lib/api'
import { essayStatusText, formatDateTime, submitTypeText } from '../lib/format'
import type { Essay, PageData } from '../types'

export function EssaysPage() {
  const { token, user } = useAuth()
  const navigate = useNavigate()
  const [essays, setEssays] = useState<Essay[]>([])
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)
  const [hasMore, setHasMore] = useState(false)
  const [status, setStatus] = useState<number | ''>('')
  const [error, setError] = useState('')
  const isTeacherOrAdmin = hasAnyRole(user?.role, ['TEACHER', 'ADMIN'])

  const loadData = async () => {
    setError('')
    try {
      const params = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      })
      if (status !== '') params.set('status', String(status))
      const data = await api.get<PageData<Essay>>(`/essay/list?${params.toString()}`, token)
      const records = data.records || data.rows || []
      setEssays(records)
      setHasMore(page * pageSize < (data.total || 0))
    } catch (err) {
      setError((err as Error).message || '加载失败')
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined)
  }, [page, status])

  const onDelete = async (essay: Essay) => {
    if (!window.confirm(`确定删除草稿「${essay.title || '未命名作文'}」吗？`)) return
    try {
      await api.delete<void>(`/essay/${essay.id}`, token)
      await loadData()
    } catch (err) {
      setError((err as Error).message || '删除失败')
    }
  }

  return (
    <div className="page-grid">
      <section className="section-heading essays-heading">
        <div>
          <p className="eyebrow">作文列表</p>
          <h3>管理当前账号下的作文</h3>
        </div>
        <div className="action-row essays-heading-actions">
          <select value={status} onChange={(event) => setStatus(event.target.value === '' ? '' : Number(event.target.value))}>
            <option value="">全部状态</option>
            {!isTeacherOrAdmin ? <option value="0">草稿</option> : null}
            <option value="1">已提交</option>
            <option value="2">批改中</option>
            <option value="3">已批改</option>
          </select>
          <Link to="/essays/create" className="primary-button link-button">
            新建作文
          </Link>
        </div>
      </section>

      <section className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>标题</th>
              <th>提交方式</th>
              <th>字数</th>
              <th>创建时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {essays.map((essay) => (
              <tr key={essay.id}>
                <td className="essay-title-cell">
                  <strong>{essay.title || '未命名作文'}</strong>
                </td>
                <td>{submitTypeText(essay.submitType)}</td>
                <td>{essay.wordCount || 0} 字</td>
                <td>{formatDateTime(essay.createTime)}</td>
                <td>
                  <span className={`status-badge status-${essay.status}`}>
                    {essayStatusText(essay.status)}
                  </span>
                </td>
                <td className="actions-cell">
                  <button type="button" className="table-btn" onClick={() => navigate(`/essays/${essay.id}`)}>
                    查看
                  </button>
                  <button type="button" className="table-btn" onClick={() => navigate(`/reviews?essayId=${essay.id}`)}>
                    批改记录
                  </button>
                  {essay.status === 0 ? (
                    <button type="button" className="table-btn danger" onClick={() => onDelete(essay)}>
                      删除
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!essays.length ? <div className="empty-state">暂无作文数据</div> : null}
      </section>

      <div className="section-heading">
        <div>{error ? <div className="feedback error inline-feedback">{error}</div> : null}</div>
        <div className="action-row">
          <button type="button" className="secondary-button" disabled={page === 1} onClick={() => setPage((prev) => prev - 1)}>
            上一页
          </button>
          <button type="button" className="secondary-button" disabled={!hasMore} onClick={() => setPage((prev) => prev + 1)}>
            下一页
          </button>
        </div>
      </div>
    </div>
  )
}

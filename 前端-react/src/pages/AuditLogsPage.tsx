import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'
import type { AuditLog } from '../types'

const actionTextMap: Record<string, string> = {
  REVIEW_DIMENSION_CREATE: '新增评分维度',
  REVIEW_DIMENSION_UPDATE: '更新评分维度',
  REVIEW_DIMENSION_STATUS: '切换维度状态',
  REVIEW_DIMENSION_DELETE: '删除评分维度',
  REVIEW_ESSAY_AI: 'AI 批改作文',
  REVIEW_ESSAY_BATCH_AI: '批量 AI 批改',
  REVIEW_ESSAY_TEACHER_MANUAL: '教师手动批改',
}

const methodTextMap: Record<string, string> = {
  GET: '查询',
  POST: '新增',
  PUT: '更新',
  PATCH: '修改',
  DELETE: '删除',
}

const targetTypeTextMap: Record<string, string> = {
  dimension: '评分维度',
  review: '批改记录',
  essay: '作文',
}

function formatDateTime(value?: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

export function AuditLogsPage() {
  const { token } = useAuth()
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const loadData = async () => {
    setLoading(true)
    setError('')
    try {
      const result = await api.get<AuditLog[]>('/audit/logs?limit=200', token)
      setLogs(result)
    } catch (err) {
      setError((err as Error).message || '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined)
  }, [token])

  return (
    <div className="page-grid">
      <section className="panel create-panel audit-logs-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">操作日志</p>
            <h3>管理员审计日志</h3>
          </div>
          <button type="button" className="secondary-button" onClick={() => loadData().catch(() => undefined)} disabled={loading}>
            {loading ? '刷新中...' : '刷新'}
          </button>
        </div>

        <div className="stack-list">
          {logs.length ? (
            logs.map((item) => (
              <article key={item.logId} className="list-row audit-log-row">
                <div>
                  <strong>{actionTextMap[item.action || ''] || item.action || '未知操作'}</strong>
                  <p>
                    {item.username || '未知用户'} / {methodTextMap[item.requestMethod || ''] || item.requestMethod || '未知方法'} /{' '}
                    {item.requestPath || '-'}
                  </p>
                  <p>
                    {formatDateTime(item.createdAt)} / {item.success === 1 ? '成功' : '失败'} / 状态码 {item.resultCode ?? '-'}
                  </p>
                  {item.errorMessage ? <p>错误：{item.errorMessage}</p> : null}
                </div>
                <div className="action-row">
                  <span className="pill">{targetTypeTextMap[item.targetType || ''] || item.targetType || '未知类型'}</span>
                </div>
              </article>
            ))
          ) : (
            <div className="empty-state">暂无审计日志</div>
          )}
        </div>
      </section>

      {error ? <div className="feedback error">{error}</div> : null}
    </div>
  )
}

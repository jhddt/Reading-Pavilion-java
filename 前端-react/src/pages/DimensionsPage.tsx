import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { hasAnyRole } from '../auth/roles'
import { api } from '../lib/api'
import type { ReviewRule } from '../types'

export function DimensionsPage() {
  const { token, user } = useAuth()
  const navigate = useNavigate()
  const [rules, setRules] = useState<ReviewRule[]>([])
  const [error, setError] = useState('')
  const canManageDimension = hasAnyRole(user?.role, ['ADMIN'])
  const canManageRule = hasAnyRole(user?.role, ['STUDENT', 'TEACHER', 'ADMIN'])

  const loadData = async () => {
    try {
      setRules(await api.get<ReviewRule[]>('/review/rules?enabledOnly=false', token))
    } catch (err) {
      setError((err as Error).message || '加载失败')
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined)
  }, [token])

  const toggleRuleStatus = async (rule: ReviewRule) => {
    try {
      await api.patch<void>(`/review/rules/${rule.ruleId}/status?enabled=${rule.status !== 1}`, null, token)
      await loadData()
    } catch (err) {
      setError((err as Error).message || '更新失败')
    }
  }

  const removeRule = async (rule: ReviewRule) => {
    if (!window.confirm(`确定删除批改细则「${rule.ruleName}」吗？`)) return
    try {
      await api.delete<void>(`/review/rules/${rule.ruleId}`, token)
      await loadData()
    } catch (err) {
      setError((err as Error).message || '删除失败')
    }
  }

  return (
    <div className="page-grid">
      <section className="section-heading dimensions-heading">
        <div>
          <p className="eyebrow">批改规则</p>
          <h3>管理批改细则</h3>
        </div>
        {canManageRule ? (
          <div className="action-row">
            <Link to="/dimensions/create" className="primary-button link-button">
              新增评分细则
            </Link>
          </div>
        ) : (
          <div className="pill dimensions-readonly-pill">只读模式</div>
        )}
      </section>

      <section className="card-grid two-col">
        {rules.map((rule) => (
          <article key={rule.ruleId} className="dimension-card">
            <div className="dimension-top">
              <div className="dimension-card-heading">
                <strong>{rule.ruleName}</strong>
                <p className="dimension-card-meta">{rule.gradeLevel || '未设置学段'} / {rule.reviewType || '通用作文'}</p>
              </div>
              <span className={`dimension-status-tag ${rule.status === 1 ? 'is-on' : 'is-off'}`}>{rule.status === 1 ? '启用中' : '已停用'}</span>
            </div>
            <div className="dimension-card-body">
              {rule.topicRequirement ? (
                <div className="dimension-rule-block">
                  <span className="dimension-rule-label">题干要求</span>
                  <p>{rule.topicRequirement}</p>
                </div>
              ) : null}
              {rule.customRequirement ? (
                <div className="dimension-rule-block">
                  <span className="dimension-rule-label">自定义批改要求</span>
                  <p>{rule.customRequirement}</p>
                </div>
              ) : null}
              {rule.deductionDetail ? (
                <div className="dimension-rule-block">
                  <span className="dimension-rule-label">扣分细则</span>
                  <p>{rule.deductionDetail}</p>
                </div>
              ) : null}
            </div>
            {canManageRule ? (
              <div className="action-row top-gap">
                <button type="button" className="secondary-button" onClick={() => navigate(`/dimensions/${rule.ruleId}/edit`)}>
                  编辑细则
                </button>
                <button type="button" className="secondary-button" onClick={() => toggleRuleStatus(rule)}>
                  {rule.status === 1 ? '停用' : '启用'}
                </button>
                <button type="button" className="danger-button" onClick={() => removeRule(rule)}>
                  删除
                </button>
              </div>
            ) : null}
          </article>
        ))}
        {!rules.length ? <div className="empty-state dimensions-empty-state">暂无评分细则</div> : null}
      </section>

      {error ? <div className="feedback error">{error}</div> : null}
    </div>
  )
}

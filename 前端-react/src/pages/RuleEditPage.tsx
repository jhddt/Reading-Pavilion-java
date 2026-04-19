import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'
import type { ReviewRule } from '../types'

type RuleEditMode = 'rule' | 'rule-list'

export function RuleEditPage() {
  const { token } = useAuth()
  const navigate = useNavigate()
  const { ruleId } = useParams()
  const [mode, setMode] = useState<RuleEditMode>('rule')
  const [rule, setRule] = useState<ReviewRule | null>(null)
  const [rules, setRules] = useState<ReviewRule[]>([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const currentRule = useMemo(
    () => rule || rules.find((item) => String(item.ruleId) === String(ruleId)) || null,
    [rule, ruleId, rules],
  )

  const loadData = async () => {
    if (!ruleId) return
    setError('')
    try {
      const allRules = await api.get<ReviewRule[]>('/review/rules?enabledOnly=false', token)
      setRules(allRules)
      const found = allRules.find((item) => String(item.ruleId) === String(ruleId))
      if (!found) throw new Error('评分细则不存在')
      setRule(found)
    } catch (err) {
      setError((err as Error).message || '加载失败')
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined)
  }, [ruleId, token])

  const updateRule = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!currentRule) return
    setLoading(true)
    setError('')
    try {
      await api.put<void>(`/review/rules/${currentRule.ruleId}`, currentRule, token)
      await loadData()
    } catch (err) {
      setError((err as Error).message || '保存失败')
    } finally {
      setLoading(false)
    }
  }

  const toggleRuleStatus = async () => {
    if (!currentRule) return
    try {
      await api.patch<void>(`/review/rules/${currentRule.ruleId}/status?enabled=${currentRule.status !== 1}`, null, token)
      await loadData()
    } catch (err) {
      setError((err as Error).message || '更新失败')
    }
  }

  const removeRule = async () => {
    if (!currentRule) return
    if (!window.confirm(`确定删除批改细则「${currentRule.ruleName}」吗？`)) return
    try {
      await api.delete<void>(`/review/rules/${currentRule.ruleId}`, token)
      navigate('/dimensions')
    } catch (err) {
      setError((err as Error).message || '删除失败')
    }
  }

  if (!currentRule) return <div className="empty-state">正在加载评分细则...</div>

  return (
    <div className="page-grid">
      <section className="panel create-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">编辑评分细则</p>
            <h3>{currentRule.ruleName}</h3>
          </div>
          <button type="button" className="secondary-button" onClick={() => navigate('/dimensions')}>
            返回列表
          </button>
        </div>

        <div className="create-mode-tabs" role="tablist" aria-label="评分细则编辑模式">
          <button
            type="button"
            className={`create-mode-tab ${mode === 'rule' ? 'create-mode-tab-active' : ''}`}
            onClick={() => setMode('rule')}
          >
            细则设置
          </button>
          <button
            type="button"
            className={`create-mode-tab ${mode === 'rule-list' ? 'create-mode-tab-active' : ''}`}
            onClick={() => setMode('rule-list')}
          >
            细则列表
          </button>
        </div>

        {mode === 'rule' ? (
          <form className="form-grid" onSubmit={updateRule}>
            <label className="field">
              <span>细则名称</span>
              <input value={currentRule.ruleName} onChange={(event) => setRule({ ...currentRule, ruleName: event.target.value })} />
            </label>
            <label className="field">
              <span>批改类型</span>
              <input value={currentRule.reviewType || ''} onChange={(event) => setRule({ ...currentRule, reviewType: event.target.value })} />
            </label>
            <label className="field">
              <span>适用学段</span>
              <input value={currentRule.gradeLevel || ''} onChange={(event) => setRule({ ...currentRule, gradeLevel: event.target.value })} />
            </label>
            <label className="field">
              <span>原文美化等级</span>
              <input value={currentRule.beautifyLevel || ''} onChange={(event) => setRule({ ...currentRule, beautifyLevel: event.target.value })} />
            </label>
            <label className="field">
              <span>题干要求</span>
              <textarea value={currentRule.topicRequirement || ''} onChange={(event) => setRule({ ...currentRule, topicRequirement: event.target.value })} rows={3} />
            </label>
            <label className="field">
              <span>自定义批改要求</span>
              <textarea value={currentRule.customRequirement || ''} onChange={(event) => setRule({ ...currentRule, customRequirement: event.target.value })} rows={3} />
            </label>
            <label className="field">
              <span>扣分细则</span>
              <textarea value={currentRule.deductionDetail || ''} onChange={(event) => setRule({ ...currentRule, deductionDetail: event.target.value })} rows={3} />
            </label>
            <label className="field">
              <span>补充提示词</span>
              <textarea value={currentRule.promptTemplate || ''} onChange={(event) => setRule({ ...currentRule, promptTemplate: event.target.value })} rows={3} />
            </label>
            <div className="action-row">
              <button type="submit" className="primary-button" disabled={loading}>
                {loading ? '保存中...' : '保存细则'}
              </button>
              <button type="button" className="secondary-button" onClick={toggleRuleStatus}>
                {currentRule.status === 1 ? '停用细则' : '启用细则'}
              </button>
              <button type="button" className="danger-button" onClick={removeRule}>
                删除细则
              </button>
            </div>
          </form>
        ) : null}

        {mode === 'rule-list' ? (
          <div className="stack-list">
            {rules.length ? (
              rules.map((item) => (
                <article key={item.ruleId} className="list-row">
                  <div>
                    <strong>{item.ruleName}</strong>
                    <p>
                      {item.gradeLevel || '未设置学段'} / {item.reviewType || '通用作文'} / {item.status === 1 ? '启用中' : '已停用'}
                    </p>
                  </div>
                  <div className="action-row">
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => {
                        navigate(`/dimensions/${item.ruleId}/edit`)
                        setMode('rule')
                      }}
                    >
                      编辑
                    </button>
                  </div>
                </article>
              ))
            ) : (
              <div className="empty-state">暂无评分细则</div>
            )}
          </div>
        ) : null}
      </section>

      {error ? <div className="feedback error">{error}</div> : null}
    </div>
  )
}

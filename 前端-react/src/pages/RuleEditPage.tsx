import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'
import { WRITING_GRADE_OPTIONS, buildWritingTechniquesJson, defaultWritingTechniquesConfig, getAvailableTechniques, safeParseWritingTechniquesJson, type WritingTechniquesConfig } from '../lib/writingTechniques'
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
  const [writingMode, setWritingMode] = useState<'form' | 'json'>('form')
  const [wtConfig, setWtConfig] = useState<WritingTechniquesConfig>(defaultWritingTechniquesConfig())

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
      const parsed = safeParseWritingTechniquesJson(found.writingTechniques || '')
      if (parsed.ok) {
        setWtConfig(parsed.config)
        setWritingMode('form')
      } else {
        setWtConfig(defaultWritingTechniquesConfig())
        setWritingMode('json')
      }
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
            <label className="field">
              <span>写作手法要求</span>
              <div className="action-row" style={{ justifyContent: 'flex-start' }}>
                <button
                  type="button"
                  className={`secondary-button ${writingMode === 'form' ? 'is-active' : ''}`}
                  onClick={() => {
                    setWritingMode('form')
                    const json = buildWritingTechniquesJson(wtConfig)
                    setRule({ ...currentRule, writingTechniques: json })
                  }}
                >
                  表单模式
                </button>
                <button
                  type="button"
                  className={`secondary-button ${writingMode === 'json' ? 'is-active' : ''}`}
                  onClick={() => setWritingMode('json')}
                >
                  高级 JSON
                </button>
              </div>
            </label>

            {writingMode === 'form' ? (
              <div className="form-grid">
                <div className="card-grid two-col">
                  <label className="field">
                    <span>写作手法学段</span>
                    <select
                      value={wtConfig.grade_level}
                      onChange={(event) => {
                        const next = { ...wtConfig, grade_level: event.target.value as any }
                        setWtConfig(next)
                        setRule({ ...currentRule, writingTechniques: buildWritingTechniquesJson(next) })
                      }}
                    >
                      {WRITING_GRADE_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="field">
                    <span>最少手法总数</span>
                    <input
                      type="number"
                      min={0}
                      value={wtConfig.min_total_count}
                      onChange={(event) => {
                        const next = { ...wtConfig, min_total_count: Number(event.target.value) }
                        setWtConfig(next)
                        setRule({ ...currentRule, writingTechniques: buildWritingTechniquesJson(next) })
                      }}
                    />
                  </label>
                </div>
                <label className="field">
                  <span>必须包含的写作手法（多选）</span>
                  <div className="helper-text" style={{ marginBottom: '8px' }}>
                    根据所选学段，系统会显示该学段应掌握的写作手法。点击手法名称即可添加或移除。
                  </div>
                  <div className="technique-grid">
                    {getAvailableTechniques(wtConfig.grade_level).map((technique) => {
                      const isSelected = wtConfig.required_techniques.includes(technique.name)
                      return (
                        <button
                          key={technique.name}
                          type="button"
                          className={`technique-button ${isSelected ? 'technique-button-selected' : ''}`}
                          onClick={() => {
                            const next = isSelected
                              ? { ...wtConfig, required_techniques: wtConfig.required_techniques.filter((x) => x !== technique.name) }
                              : { ...wtConfig, required_techniques: [...wtConfig.required_techniques, technique.name] }
                            setWtConfig(next)
                            setRule({ ...currentRule, writingTechniques: buildWritingTechniquesJson(next) })
                          }}
                          title={technique.description}
                        >
                          {isSelected ? '✓ ' : ''}{technique.name}
                        </button>
                      )
                    })}
                  </div>
                  <div className="helper-text" style={{ marginTop: '8px' }}>
                    已选择 {wtConfig.required_techniques.length} 项手法
                  </div>
                </label>
                <label className="field">
                  <span>自动生成的 JSON（只读）</span>
                  <textarea rows={6} value={currentRule.writingTechniques || ''} readOnly />
                </label>
              </div>
            ) : (
              <label className="field">
                <span>写作手法要求（JSON）</span>
                <textarea
                  value={currentRule.writingTechniques || ''}
                  onChange={(event) => setRule({ ...currentRule, writingTechniques: event.target.value })}
                  rows={8}
                  placeholder='例如：{"grade_level":"小学","required_techniques":["比喻"],"min_total_count":2}'
                />
              </label>
            )}
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

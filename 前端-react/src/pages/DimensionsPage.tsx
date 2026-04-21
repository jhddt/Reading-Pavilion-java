import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthProvider'
import { hasAnyRole } from '../auth/roles'
import { api } from '../lib/api'
import {
  WRITING_GRADE_OPTIONS,
  buildWritingTechniquesJson,
  defaultWritingTechniquesConfig,
  safeParseWritingTechniquesJson,
  tryFormatWritingTechniquesJson,
  type WritingTechniquesConfig,
} from '../lib/writingTechniques'
import type { ReviewRule } from '../types'

const emptyForm = {
  ruleName: '',
  reviewType: '',
  gradeLevel: '',
  beautifyLevel: '',
  topicRequirement: '',
  customRequirement: '',
  deductionDetail: '',
  promptTemplate: '',
  writingTechniques: '',
}

export function DimensionsPage() {
  const { token, user } = useAuth()
  const [rules, setRules] = useState<ReviewRule[]>([])
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)
  const [saving, setSaving] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingForm, setEditingForm] = useState<Partial<ReviewRule>>(emptyForm)
  const [createForm, setCreateForm] = useState<Partial<ReviewRule>>(emptyForm)
  const [editingWritingMode, setEditingWritingMode] = useState<'form' | 'json'>('form')
  const [creatingWritingMode, setCreatingWritingMode] = useState<'form' | 'json'>('form')
  const [editingWtConfig, setEditingWtConfig] = useState<WritingTechniquesConfig>(defaultWritingTechniquesConfig())
  const [creatingWtConfig, setCreatingWtConfig] = useState<WritingTechniquesConfig>(defaultWritingTechniquesConfig())
  const [editingTechniqueDraft, setEditingTechniqueDraft] = useState('')
  const [creatingTechniqueDraft, setCreatingTechniqueDraft] = useState('')
  const canManageRule = hasAnyRole(user?.role, ['STUDENT', 'TEACHER', 'ADMIN'])

  const loadData = async () => {
    setError('')
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
    setError('')
    try {
      await api.patch<void>(`/review/rules/${rule.ruleId}/status?enabled=${rule.status !== 1}`, null, token)
      await loadData()
    } catch (err) {
      setError((err as Error).message || '更新失败')
    }
  }

  const removeRule = async (rule: ReviewRule) => {
    if (!window.confirm(`确定删除批改细则「${rule.ruleName}」吗？`)) return
    setError('')
    try {
      await api.delete<void>(`/review/rules/${rule.ruleId}`, token)
      await loadData()
    } catch (err) {
      setError((err as Error).message || '删除失败')
    }
  }

  const startEdit = (rule: ReviewRule) => {
    setEditingId(rule.ruleId)
    const parsed = safeParseWritingTechniquesJson(rule.writingTechniques || '')
    if (parsed.ok) {
      setEditingWtConfig(parsed.config)
      setEditingWritingMode('form')
    } else {
      setEditingWtConfig(defaultWritingTechniquesConfig())
      setEditingWritingMode('json')
    }
    setEditingTechniqueDraft('')
    setEditingForm({
      ruleName: rule.ruleName,
      reviewType: rule.reviewType || '',
      gradeLevel: rule.gradeLevel || '',
      beautifyLevel: rule.beautifyLevel || '',
      topicRequirement: rule.topicRequirement || '',
      customRequirement: rule.customRequirement || '',
      deductionDetail: rule.deductionDetail || '',
      promptTemplate: rule.promptTemplate || '',
      writingTechniques: parsed.ok ? buildWritingTechniquesJson(parsed.config) : rule.writingTechniques || '',
    })
  }

  const cancelEdit = () => {
    setEditingId(null)
    setEditingForm(emptyForm)
    setEditingWtConfig(defaultWritingTechniquesConfig())
    setEditingWritingMode('form')
    setEditingTechniqueDraft('')
  }

  const saveEdit = async (ruleId: number) => {
    const payload = {
      ruleId,
      ...editingForm,
      ruleName: (editingForm.ruleName || '').trim(),
    }
    if (!payload.ruleName) {
      setError('细则名称不能为空')
      return
    }

    setSaving(true)
    setError('')
    try {
      await api.put<void>(`/review/rules/${ruleId}`, payload, token)
      cancelEdit()
      await loadData()
    } catch (err) {
      setError((err as Error).message || '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const createRule = async () => {
    const payload = {
      ...createForm,
      ruleName: (createForm.ruleName || '').trim(),
    }
    if (!payload.ruleName) {
      setError('细则名称不能为空')
      return
    }

    setSaving(true)
    setError('')
    try {
      await api.post<ReviewRule>('/review/rules', payload, token)
      setCreateForm(emptyForm)
      setCreating(false)
      setCreatingWtConfig(defaultWritingTechniquesConfig())
      setCreatingWritingMode('form')
      setCreatingTechniqueDraft('')
      await loadData()
    } catch (err) {
      setError((err as Error).message || '创建失败')
    } finally {
      setSaving(false)
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
            <button type="button" className="primary-button" onClick={() => setCreating(true)}>
              新增评分细则
            </button>
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
                <p className="dimension-card-meta">
                  {rule.gradeLevel || '未设置学段'} / {rule.reviewType || '通用作文'}
                </p>
              </div>
              <span className={`dimension-status-tag ${rule.status === 1 ? 'is-on' : 'is-off'}`}>
                {rule.status === 1 ? '启用中' : '已停用'}
              </span>
            </div>

            {editingId === rule.ruleId ? (
              <div className="form-grid">
                <label className="field">
                  <span>细则名称</span>
                  <input
                    value={editingForm.ruleName || ''}
                    onChange={(event) => setEditingForm((prev) => ({ ...prev, ruleName: event.target.value }))}
                  />
                </label>
                <div className="card-grid two-col">
                  <label className="field">
                    <span>批改类型</span>
                    <input
                      value={editingForm.reviewType || ''}
                      onChange={(event) => setEditingForm((prev) => ({ ...prev, reviewType: event.target.value }))}
                    />
                  </label>
                  <label className="field">
                    <span>适用学段</span>
                    <input
                      value={editingForm.gradeLevel || ''}
                      onChange={(event) => setEditingForm((prev) => ({ ...prev, gradeLevel: event.target.value }))}
                    />
                  </label>
                </div>
                <label className="field">
                  <span>题干要求</span>
                  <textarea
                    rows={3}
                    value={editingForm.topicRequirement || ''}
                    onChange={(event) =>
                      setEditingForm((prev) => ({ ...prev, topicRequirement: event.target.value }))
                    }
                  />
                </label>
                <label className="field">
                  <span>自定义批改要求</span>
                  <textarea
                    rows={3}
                    value={editingForm.customRequirement || ''}
                    onChange={(event) =>
                      setEditingForm((prev) => ({ ...prev, customRequirement: event.target.value }))
                    }
                  />
                </label>
                <label className="field">
                  <span>扣分细则</span>
                  <textarea
                    rows={3}
                    value={editingForm.deductionDetail || ''}
                    onChange={(event) =>
                      setEditingForm((prev) => ({ ...prev, deductionDetail: event.target.value }))
                    }
                  />
                </label>
                <label className="field">
                  <span>写作手法要求</span>
                  <div className="action-row" style={{ justifyContent: 'flex-start' }}>
                    <button
                      type="button"
                      className={`secondary-button ${editingWritingMode === 'form' ? 'is-active' : ''}`}
                      onClick={() => {
                        setEditingWritingMode('form')
                        const json = buildWritingTechniquesJson(editingWtConfig)
                        setEditingForm((prev) => ({ ...prev, writingTechniques: json }))
                      }}
                    >
                      表单模式
                    </button>
                    <button
                      type="button"
                      className={`secondary-button ${editingWritingMode === 'json' ? 'is-active' : ''}`}
                      onClick={() => setEditingWritingMode('json')}
                    >
                      高级 JSON
                    </button>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => {
                        const formatted = tryFormatWritingTechniquesJson(editingForm.writingTechniques || '')
                        if (!formatted.ok) {
                          setError(`写作手法 JSON 格式不正确：${formatted.error}`)
                          return
                        }
                        setEditingForm((prev) => ({ ...prev, writingTechniques: formatted.json }))
                      }}
                    >
                      格式化 JSON
                    </button>
                  </div>
                </label>

                {editingWritingMode === 'form' ? (
                  <div className="form-grid">
                    <div className="card-grid two-col">
                      <label className="field">
                        <span>写作手法学段</span>
                        <select
                          value={editingWtConfig.grade_level}
                          onChange={(event) => {
                            const next = { ...editingWtConfig, grade_level: event.target.value as any }
                            setEditingWtConfig(next)
                            setEditingForm((prev) => ({ ...prev, writingTechniques: buildWritingTechniquesJson(next) }))
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
                          value={editingWtConfig.min_total_count}
                          onChange={(event) => {
                            const next = { ...editingWtConfig, min_total_count: Number(event.target.value) }
                            setEditingWtConfig(next)
                            setEditingForm((prev) => ({ ...prev, writingTechniques: buildWritingTechniquesJson(next) }))
                          }}
                        />
                      </label>
                    </div>

                    <label className="field">
                      <span>必须包含的写作手法</span>
                      <div className="action-row" style={{ justifyContent: 'flex-start', flexWrap: 'wrap' }}>
                        {editingWtConfig.required_techniques.length ? (
                          editingWtConfig.required_techniques.map((t) => (
                            <button
                              type="button"
                              key={t}
                              className="secondary-button"
                              onClick={() => {
                                const next = {
                                  ...editingWtConfig,
                                  required_techniques: editingWtConfig.required_techniques.filter((x) => x !== t),
                                }
                                setEditingWtConfig(next)
                                setEditingForm((prev) => ({ ...prev, writingTechniques: buildWritingTechniquesJson(next) }))
                              }}
                              title="点击移除"
                            >
                              {t} ×
                            </button>
                          ))
                        ) : (
                          <span className="helper-text">暂无，下面可以添加</span>
                        )}
                      </div>
                      <div className="action-row" style={{ justifyContent: 'flex-start' }}>
                        <input
                          value={editingTechniqueDraft}
                          onChange={(event) => setEditingTechniqueDraft(event.target.value)}
                          placeholder="例如：比喻、拟人、排比"
                          onKeyDown={(event) => {
                            if (event.key !== 'Enter') return
                            event.preventDefault()
                            const nextName = editingTechniqueDraft.trim()
                            if (!nextName) return
                            const next = {
                              ...editingWtConfig,
                              required_techniques: Array.from(new Set([...editingWtConfig.required_techniques, nextName])),
                            }
                            setEditingWtConfig(next)
                            setEditingTechniqueDraft('')
                            setEditingForm((prev) => ({ ...prev, writingTechniques: buildWritingTechniquesJson(next) }))
                          }}
                        />
                        <button
                          type="button"
                          className="secondary-button"
                          onClick={() => {
                            const nextName = editingTechniqueDraft.trim()
                            if (!nextName) return
                            const next = {
                              ...editingWtConfig,
                              required_techniques: Array.from(new Set([...editingWtConfig.required_techniques, nextName])),
                            }
                            setEditingWtConfig(next)
                            setEditingTechniqueDraft('')
                            setEditingForm((prev) => ({ ...prev, writingTechniques: buildWritingTechniquesJson(next) }))
                          }}
                        >
                          添加
                        </button>
                      </div>
                    </label>

                    <label className="field">
                      <span>自动生成的 JSON（只读）</span>
                      <textarea rows={6} value={editingForm.writingTechniques || ''} readOnly />
                    </label>
                  </div>
                ) : (
                  <label className="field">
                    <span>写作手法要求（JSON）</span>
                    <textarea
                      rows={8}
                      value={editingForm.writingTechniques || ''}
                      onChange={(event) =>
                        setEditingForm((prev) => ({ ...prev, writingTechniques: event.target.value }))
                      }
                      placeholder='例如：{"grade_level":"小学","required_techniques":["比喻"],"min_total_count":2}'
                    />
                  </label>
                )}

                <div className="action-row top-gap">
                  <button type="button" className="primary-button" onClick={() => saveEdit(rule.ruleId)} disabled={saving}>
                    {saving ? '保存中...' : '保存'}
                  </button>
                  <button type="button" className="secondary-button" onClick={cancelEdit}>
                    取消
                  </button>
                </div>
              </div>
            ) : (
              <>
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
                  {rule.writingTechniques ? (() => {
                    const parsed = safeParseWritingTechniquesJson(rule.writingTechniques)
                    if (!parsed.ok) {
                      return (
                        <div className="dimension-rule-block">
                          <span className="dimension-rule-label">写作手法要求</span>
                          <p className="helper-text">已配置（格式异常，建议进入编辑页修正）</p>
                        </div>
                      )
                    }
                    const required = parsed.config.required_techniques || []
                    return (
                      <div className="dimension-rule-block">
                        <span className="dimension-rule-label">写作手法要求</span>
                        <div className="helper-text">
                          学段：<strong>{parsed.config.grade_level}</strong>，最少手法总数：<strong>{parsed.config.min_total_count}</strong>
                        </div>
                        {required.length ? (
                          <div className="action-row" style={{ justifyContent: 'flex-start', flexWrap: 'wrap', marginTop: 8 }}>
                            {required.map((t) => (
                              <span key={t} className="pill" style={{ padding: '4px 10px' }}>
                                {t}
                              </span>
                            ))}
                          </div>
                        ) : (
                          <p className="helper-text" style={{ marginTop: 6 }}>必须手法：未设置</p>
                        )}
                      </div>
                    )
                  })() : null}
                </div>
                {canManageRule ? (
                  <div className="action-row top-gap">
                    <button type="button" className="secondary-button" onClick={() => startEdit(rule)}>
                      编辑
                    </button>
                    <button type="button" className="secondary-button" onClick={() => toggleRuleStatus(rule)}>
                      {rule.status === 1 ? '停用' : '启用'}
                    </button>
                    <button type="button" className="danger-button" onClick={() => removeRule(rule)}>
                      删除
                    </button>
                  </div>
                ) : null}
              </>
            )}
          </article>
        ))}
        {!rules.length ? <div className="empty-state dimensions-empty-state">暂无评分细则</div> : null}
      </section>

      {error ? <div className="feedback error">{error}</div> : null}

      {creating ? (
        <div className="overlay" onClick={() => setCreating(false)}>
          <div className="modal-card" onClick={(event) => event.stopPropagation()}>
            <div className="section-heading">
              <div>
                <p className="eyebrow">新增评分细则</p>
                <h3>填写细则基础信息</h3>
              </div>
            </div>
            <div className="form-grid">
              <label className="field">
                <span>细则名称</span>
                <input
                  value={createForm.ruleName || ''}
                  onChange={(event) => setCreateForm((prev) => ({ ...prev, ruleName: event.target.value }))}
                />
              </label>
              <div className="card-grid two-col">
                <label className="field">
                  <span>批改类型</span>
                  <input
                    value={createForm.reviewType || ''}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, reviewType: event.target.value }))}
                  />
                </label>
                <label className="field">
                  <span>适用学段</span>
                  <input
                    value={createForm.gradeLevel || ''}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, gradeLevel: event.target.value }))}
                  />
                </label>
              </div>
              <label className="field">
                <span>题干要求</span>
                <textarea
                  rows={3}
                  value={createForm.topicRequirement || ''}
                  onChange={(event) => setCreateForm((prev) => ({ ...prev, topicRequirement: event.target.value }))}
                />
              </label>
              <label className="field">
                <span>自定义批改要求</span>
                <textarea
                  rows={3}
                  value={createForm.customRequirement || ''}
                  onChange={(event) => setCreateForm((prev) => ({ ...prev, customRequirement: event.target.value }))}
                />
              </label>
              <label className="field">
                <span>扣分细则</span>
                <textarea
                  rows={3}
                  value={createForm.deductionDetail || ''}
                  onChange={(event) => setCreateForm((prev) => ({ ...prev, deductionDetail: event.target.value }))}
                />
              </label>
              <label className="field">
                <span>写作手法要求</span>
                <div className="action-row" style={{ justifyContent: 'flex-start' }}>
                  <button
                    type="button"
                    className={`secondary-button ${creatingWritingMode === 'form' ? 'is-active' : ''}`}
                    onClick={() => {
                      setCreatingWritingMode('form')
                      const json = buildWritingTechniquesJson(creatingWtConfig)
                      setCreateForm((prev) => ({ ...prev, writingTechniques: json }))
                    }}
                  >
                    表单模式
                  </button>
                  <button
                    type="button"
                    className={`secondary-button ${creatingWritingMode === 'json' ? 'is-active' : ''}`}
                    onClick={() => setCreatingWritingMode('json')}
                  >
                    高级 JSON
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => {
                      if (!createForm.writingTechniques?.trim()) {
                        const template = buildWritingTechniquesJson(creatingWtConfig)
                        setCreateForm((prev) => ({ ...prev, writingTechniques: template }))
                        return
                      }
                      const formatted = tryFormatWritingTechniquesJson(createForm.writingTechniques || '')
                      if (!formatted.ok) {
                        setError(`写作手法 JSON 格式不正确：${formatted.error}`)
                        return
                      }
                      setCreateForm((prev) => ({ ...prev, writingTechniques: formatted.json }))
                    }}
                  >
                    格式化/生成 JSON
                  </button>
                </div>
              </label>

              {creatingWritingMode === 'form' ? (
                <div className="form-grid">
                  <div className="card-grid two-col">
                    <label className="field">
                      <span>写作手法学段</span>
                      <select
                        value={creatingWtConfig.grade_level}
                        onChange={(event) => {
                          const next = { ...creatingWtConfig, grade_level: event.target.value as any }
                          setCreatingWtConfig(next)
                          setCreateForm((prev) => ({ ...prev, writingTechniques: buildWritingTechniquesJson(next) }))
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
                        value={creatingWtConfig.min_total_count}
                        onChange={(event) => {
                          const next = { ...creatingWtConfig, min_total_count: Number(event.target.value) }
                          setCreatingWtConfig(next)
                          setCreateForm((prev) => ({ ...prev, writingTechniques: buildWritingTechniquesJson(next) }))
                        }}
                      />
                    </label>
                  </div>

                  <label className="field">
                    <span>必须包含的写作手法</span>
                    <div className="action-row" style={{ justifyContent: 'flex-start', flexWrap: 'wrap' }}>
                      {creatingWtConfig.required_techniques.length ? (
                        creatingWtConfig.required_techniques.map((t) => (
                          <button
                            type="button"
                            key={t}
                            className="secondary-button"
                            onClick={() => {
                              const next = {
                                ...creatingWtConfig,
                                required_techniques: creatingWtConfig.required_techniques.filter((x) => x !== t),
                              }
                              setCreatingWtConfig(next)
                              setCreateForm((prev) => ({ ...prev, writingTechniques: buildWritingTechniquesJson(next) }))
                            }}
                            title="点击移除"
                          >
                            {t} ×
                          </button>
                        ))
                      ) : (
                        <span className="helper-text">暂无，下面可以添加</span>
                      )}
                    </div>
                    <div className="action-row" style={{ justifyContent: 'flex-start' }}>
                      <input
                        value={creatingTechniqueDraft}
                        onChange={(event) => setCreatingTechniqueDraft(event.target.value)}
                        placeholder="例如：比喻、拟人、排比"
                        onKeyDown={(event) => {
                          if (event.key !== 'Enter') return
                          event.preventDefault()
                          const nextName = creatingTechniqueDraft.trim()
                          if (!nextName) return
                          const next = {
                            ...creatingWtConfig,
                            required_techniques: Array.from(new Set([...creatingWtConfig.required_techniques, nextName])),
                          }
                          setCreatingWtConfig(next)
                          setCreatingTechniqueDraft('')
                          setCreateForm((prev) => ({ ...prev, writingTechniques: buildWritingTechniquesJson(next) }))
                        }}
                      />
                      <button
                        type="button"
                        className="secondary-button"
                        onClick={() => {
                          const nextName = creatingTechniqueDraft.trim()
                          if (!nextName) return
                          const next = {
                            ...creatingWtConfig,
                            required_techniques: Array.from(new Set([...creatingWtConfig.required_techniques, nextName])),
                          }
                          setCreatingWtConfig(next)
                          setCreatingTechniqueDraft('')
                          setCreateForm((prev) => ({ ...prev, writingTechniques: buildWritingTechniquesJson(next) }))
                        }}
                      >
                        添加
                      </button>
                    </div>
                  </label>

                  <label className="field">
                    <span>自动生成的 JSON（只读）</span>
                    <textarea rows={6} value={createForm.writingTechniques || ''} readOnly />
                  </label>
                </div>
              ) : (
                <label className="field">
                  <span>写作手法要求（JSON）</span>
                  <textarea
                    rows={8}
                    value={createForm.writingTechniques || ''}
                    onChange={(event) =>
                      setCreateForm((prev) => ({ ...prev, writingTechniques: event.target.value }))
                    }
                    placeholder='例如：{"grade_level":"小学","required_techniques":["比喻"],"min_total_count":2}'
                  />
                </label>
              )}

              <div className="action-row">
                <button type="button" className="primary-button" onClick={createRule} disabled={saving}>
                  {saving ? '创建中...' : '确认新增'}
                </button>
                <button type="button" className="secondary-button" onClick={() => setCreating(false)}>
                  取消
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

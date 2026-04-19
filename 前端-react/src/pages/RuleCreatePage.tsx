import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'
import type { ReviewRule } from '../types'

type RuleCreateMode = 'basic' | 'requirements'

export function RuleCreatePage() {
  const { token } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState<RuleCreateMode>('basic')
  const [form, setForm] = useState({
    ruleName: '',
    reviewType: '',
    gradeLevel: '',
    promptTemplate: '',
    topicRequirement: '',
    beautifyLevel: '',
    customRequirement: '',
    deductionDetail: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const created = await api.post<ReviewRule>('/review/rules', form, token)
      navigate(created.ruleId ? `/dimensions/${created.ruleId}/edit` : '/dimensions')
    } catch (err) {
      setError((err as Error).message || '创建失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page-grid">
      <section className="panel create-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">新增评分细则</p>
            <h3>先建立规则本体</h3>
          </div>
        </div>

        <div className="create-mode-tabs" role="tablist" aria-label="评分细则创建步骤">
          <button
            type="button"
            className={`create-mode-tab ${mode === 'basic' ? 'create-mode-tab-active' : ''}`}
            onClick={() => setMode('basic')}
          >
            基础信息
          </button>
          <button
            type="button"
            className={`create-mode-tab ${mode === 'requirements' ? 'create-mode-tab-active' : ''}`}
            onClick={() => setMode('requirements')}
          >
            批改要求
          </button>
        </div>

        <form className="form-grid" onSubmit={onSubmit}>
          {mode === 'basic' ? (
            <>
              <div>
                <p className="eyebrow">步骤一</p>
                <h3 className="create-mode-title">填写基础信息</h3>
              </div>
              <label className="field">
                <span>细则名称</span>
                <input value={form.ruleName} onChange={(event) => setForm((prev) => ({ ...prev, ruleName: event.target.value }))} required />
              </label>
              <label className="field">
                <span>批改类型</span>
                <input value={form.reviewType} onChange={(event) => setForm((prev) => ({ ...prev, reviewType: event.target.value }))} />
              </label>
              <label className="field">
                <span>适用学段</span>
                <input value={form.gradeLevel} onChange={(event) => setForm((prev) => ({ ...prev, gradeLevel: event.target.value }))} />
              </label>
              <label className="field">
                <span>原文美化等级</span>
                <input value={form.beautifyLevel} onChange={(event) => setForm((prev) => ({ ...prev, beautifyLevel: event.target.value }))} />
              </label>
              <div className="helper-text">建议先完成基础信息，再切换到“批改要求”补充题干、扣分标准和自定义要求。</div>
            </>
          ) : (
            <>
              <div>
                <p className="eyebrow">步骤二</p>
                <h3 className="create-mode-title">填写批改要求</h3>
              </div>
              <label className="field">
                <span>题干要求</span>
                <textarea value={form.topicRequirement} onChange={(event) => setForm((prev) => ({ ...prev, topicRequirement: event.target.value }))} rows={3} />
              </label>
              <label className="field">
                <span>自定义批改要求</span>
                <textarea value={form.customRequirement} onChange={(event) => setForm((prev) => ({ ...prev, customRequirement: event.target.value }))} rows={3} />
              </label>
              <label className="field">
                <span>扣分细则</span>
                <textarea value={form.deductionDetail} onChange={(event) => setForm((prev) => ({ ...prev, deductionDetail: event.target.value }))} rows={3} />
              </label>
              <label className="field">
                <span>补充提示词</span>
                <textarea value={form.promptTemplate} onChange={(event) => setForm((prev) => ({ ...prev, promptTemplate: event.target.value }))} rows={3} />
              </label>
              <div className="helper-text">创建成功后会进入细则编辑页；评分维度可后续按需补充，不再作为必配项。</div>
            </>
          )}
          <div className="action-row">
            <button type="submit" className="primary-button" disabled={loading}>
              {loading ? '创建中...' : '创建细则'}
            </button>
          </div>
        </form>
      </section>
      {error ? <div className="feedback error">{error}</div> : null}
    </div>
  )
}

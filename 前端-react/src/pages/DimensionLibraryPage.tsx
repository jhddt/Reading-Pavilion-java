import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'
import type { ScoreDimension } from '../types'

type DimensionLibraryMode = 'list' | 'create'

export function DimensionLibraryPage() {
  const { token } = useAuth()
  const [mode, setMode] = useState<DimensionLibraryMode>('list')
  const [dimensions, setDimensions] = useState<ScoreDimension[]>([])
  const [editingId, setEditingId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    dimensionName: '',
    weight: 25,
    maxScore: 25,
    description: '',
    sortOrder: 0,
  })

  const loadData = async () => {
    setError('')
    try {
      const list = await api.get<ScoreDimension[]>('/review/dimensions?enabledOnly=false', token)
      console.info('[DimensionLibrary] loaded dimensions', { count: list.length })
      setDimensions(list)
    } catch (err) {
      console.error('[DimensionLibrary] failed to load dimensions', err)
      setError((err as Error).message || '加载失败')
    }
  }

  useEffect(() => {
    loadData().catch(() => undefined)
  }, [token])

  const createDimension = async (event: React.FormEvent) => {
    event.preventDefault()
    try {
      console.info('[DimensionLibrary] create dimension', { name: form.dimensionName })
      await api.post<ScoreDimension>('/review/dimensions', { ...form }, token)
      setForm({
        dimensionName: '',
        weight: 25,
        maxScore: 25,
        description: '',
        sortOrder: 0,
      })
      await loadData()
    } catch (err) {
      console.error('[DimensionLibrary] failed to create dimension', err)
      setError((err as Error).message || '新增评分项失败')
    }
  }

  const saveDimension = async (item: ScoreDimension) => {
    try {
      console.info('[DimensionLibrary] save dimension', { dimensionId: item.dimensionId, name: item.dimensionName })
      await api.put<void>(`/review/dimensions/${item.dimensionId}`, item, token)
      await loadData()
    } catch (err) {
      console.error('[DimensionLibrary] failed to save dimension', { dimensionId: item.dimensionId, err })
      setError((err as Error).message || '保存评分项失败')
    }
  }

  const toggleDimensionStatus = async (item: ScoreDimension) => {
    try {
      console.info('[DimensionLibrary] toggle dimension status', {
        dimensionId: item.dimensionId,
        from: item.status,
        toEnabled: item.status !== 1,
      })
      await api.patch<void>(`/review/dimensions/${item.dimensionId}/status?enabled=${item.status !== 1}`, null, token)
      await loadData()
    } catch (err) {
      console.error('[DimensionLibrary] failed to toggle dimension status', { dimensionId: item.dimensionId, err })
      setError((err as Error).message || '更新评分项失败')
    }
  }

  const removeDimension = async (item: ScoreDimension) => {
    if (!window.confirm(`确定删除评分项「${item.dimensionName}」吗？`)) return
    try {
      console.info('[DimensionLibrary] delete dimension', { dimensionId: item.dimensionId, name: item.dimensionName })
      await api.delete<void>(`/review/dimensions/${item.dimensionId}`, token)
      await loadData()
    } catch (err) {
      console.error('[DimensionLibrary] failed to delete dimension', { dimensionId: item.dimensionId, err })
      setError((err as Error).message || '删除评分项失败')
    }
  }

  return (
    <div className="page-grid">
      <section className="panel create-panel">
        <div className="section-heading">
          <div>
            <h3>评分维度库</h3>
          </div>
        </div>

        <div className="create-mode-tabs" role="tablist" aria-label="评分维度库页面模式">
          <button
            type="button"
            className={`create-mode-tab ${mode === 'list' ? 'create-mode-tab-active' : ''}`}
            onClick={() => setMode('list')}
          >
            维度列表
          </button>
          <button
            type="button"
            className={`create-mode-tab ${mode === 'create' ? 'create-mode-tab-active' : ''}`}
            onClick={() => setMode('create')}
          >
            新增维度
          </button>
        </div>

        {mode === 'create' ? (
          <form className="form-grid" onSubmit={createDimension}>
            <label className="field">
              <span>评分项名称</span>
              <input value={form.dimensionName} onChange={(event) => setForm((prev) => ({ ...prev, dimensionName: event.target.value }))} required />
            </label>
            <div className="card-grid three-col">
              <label className="field">
                <span>权重</span>
                <input type="number" value={form.weight} onChange={(event) => setForm((prev) => ({ ...prev, weight: Number(event.target.value) }))} />
              </label>
              <label className="field">
                <span>满分值</span>
                <input type="number" value={form.maxScore} onChange={(event) => setForm((prev) => ({ ...prev, maxScore: Number(event.target.value) }))} />
              </label>
              <label className="field">
                <span>排序值</span>
                <input type="number" value={form.sortOrder} onChange={(event) => setForm((prev) => ({ ...prev, sortOrder: Number(event.target.value) }))} />
              </label>
            </div>
            <label className="field">
              <span>评分说明</span>
              <textarea value={form.description} onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))} rows={3} />
            </label>
            <button type="submit" className="primary-button">新增评分项</button>
          </form>
        ) : null}

        {mode === 'list' ? (
          <div className="stack-list">
            {dimensions.length ? (
              dimensions.map((item) => (
                <div key={item.dimensionId} className="dimension-edit-card dimension-mini-card">
                  <div className="dimension-mini-top">
                    <span className="dimension-mini-tag">公共维度</span>
                    <span className={`dimension-mini-status ${item.status === 1 ? 'is-on' : 'is-off'}`}>
                      {item.status === 1 ? '启用' : '禁用'}
                    </span>
                  </div>

                  <h4 className="dimension-mini-title">{item.dimensionName}</h4>
                  <p className="dimension-mini-desc">{item.description || '暂无评分说明'}</p>

                  <div className="dimension-mini-stats">
                    <span>权重 {item.weight}</span>
                    <span>满分 {item.maxScore}</span>
                    <span>排序 {item.sortOrder ?? 0}</span>
                  </div>

                  {editingId === item.dimensionId ? (
                    <div className="dimension-mini-editor">
                      <div className="card-grid three-col">
                        <label className="field">
                          <span>名称</span>
                          <input
                            value={item.dimensionName}
                            onChange={(event) =>
                              setDimensions((prev) =>
                                prev.map((row) =>
                                  row.dimensionId === item.dimensionId ? { ...row, dimensionName: event.target.value } : row,
                                ),
                              )
                            }
                          />
                        </label>
                        <label className="field">
                          <span>权重</span>
                          <input
                            type="number"
                            value={item.weight}
                            onChange={(event) =>
                              setDimensions((prev) =>
                                prev.map((row) =>
                                  row.dimensionId === item.dimensionId ? { ...row, weight: Number(event.target.value) } : row,
                                ),
                              )
                            }
                          />
                        </label>
                        <label className="field">
                          <span>满分</span>
                          <input
                            type="number"
                            value={item.maxScore}
                            onChange={(event) =>
                              setDimensions((prev) =>
                                prev.map((row) =>
                                  row.dimensionId === item.dimensionId ? { ...row, maxScore: Number(event.target.value) } : row,
                                ),
                              )
                            }
                          />
                        </label>
                      </div>
                      <label className="field">
                        <span>排序值</span>
                        <input
                          type="number"
                          value={item.sortOrder ?? 0}
                          onChange={(event) =>
                            setDimensions((prev) =>
                              prev.map((row) =>
                                row.dimensionId === item.dimensionId ? { ...row, sortOrder: Number(event.target.value) } : row,
                              ),
                            )
                          }
                        />
                      </label>
                      <label className="field">
                        <span>评分说明</span>
                        <textarea
                          value={item.description || ''}
                          onChange={(event) =>
                            setDimensions((prev) =>
                              prev.map((row) =>
                                row.dimensionId === item.dimensionId ? { ...row, description: event.target.value } : row,
                              ),
                            )
                          }
                          rows={2}
                        />
                      </label>
                    </div>
                  ) : null}

                  <div className="action-row dimension-mini-actions">
                    {editingId === item.dimensionId ? (
                      <button
                        type="button"
                        className="secondary-button"
                        onClick={() => {
                          setEditingId(null)
                          loadData().catch(() => undefined)
                        }}
                      >
                        取消
                      </button>
                    ) : (
                      <button type="button" className="secondary-button" onClick={() => setEditingId(item.dimensionId)}>
                        编辑
                      </button>
                    )}
                    <button type="button" className="secondary-button" onClick={() => saveDimension(item)}>
                      保存
                    </button>
                    <button type="button" className="secondary-button" onClick={() => toggleDimensionStatus(item)}>
                      {item.status === 1 ? '禁用' : '启用'}
                    </button>
                    <button type="button" className="danger-button" onClick={() => removeDimension(item)}>
                      删除
                    </button>
                  </div>
                </div>
              ))
            ) : (
              <div className="empty-state">暂无公共评分维度</div>
            )}
          </div>
        ) : null}
      </section>

      {error ? <div className="feedback error">{error}</div> : null}
    </div>
  )
}

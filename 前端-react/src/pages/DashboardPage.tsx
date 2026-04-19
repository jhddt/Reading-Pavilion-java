import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthProvider'
import { api } from '../lib/api'
import type { Essay, PageData, ReviewRecord, ReviewRule } from '../types'
import type { CSSProperties } from 'react'

export function DashboardPage() {
  const { token } = useAuth()
  const [essayCount, setEssayCount] = useState(0)
  const [reviewCount, setReviewCount] = useState(0)
  const [ruleCount, setRuleCount] = useState(0)
  const [pendingCount, setPendingCount] = useState(0)

  useEffect(() => {
    Promise.all([
      api.get<PageData<Essay>>('/essay/list?page=1&pageSize=10', token),
      api.get<PageData<ReviewRecord>>('/review/records?page=1&pageSize=10', token),
      api.get<ReviewRule[]>('/review/rules?enabledOnly=false', token),
    ])
      .then(([essayPage, reviewPage, rules]) => {
        const essays = essayPage.records || essayPage.rows || []
        const reviews = reviewPage.records || reviewPage.rows || []
        setEssayCount(essayPage.total || essays.length)
        setReviewCount(reviewPage.total || reviews.length)
        setPendingCount(essays.filter((item) => item.status === 1 || item.status === 2).length)
        setRuleCount(rules.length)
      })
      .catch(() => undefined)
  }, [token])

  const chartItems = [
    { key: 'essay', label: '作文总数', value: essayCount },
    { key: 'review', label: '批改记录', value: reviewCount },
    { key: 'rule', label: '细则数量', value: ruleCount },
    { key: 'pending', label: '待处理作文', value: pendingCount },
  ]
  const maxValue = Math.max(...chartItems.map((item) => item.value), 1)
  const pendingRatio = essayCount > 0 ? Math.round((pendingCount / essayCount) * 100) : 0

  return (
    <div className="page-grid">
      <section className="card-grid four-col">
        <article className="metric-card">
          <p>作文总数</p>
          <strong>{essayCount}</strong>
          <span>来自当前账号的作文数据。</span>
        </article>
        <article className="metric-card">
          <p>批改记录</p>
          <strong>{reviewCount}</strong>
          <span>包含历史版本和当前最新结果。</span>
        </article>
        <article className="metric-card">
          <p>细则数量</p>
          <strong>{ruleCount}</strong>
          <span>细则与评分维度都可继续配置。</span>
        </article>
        <article className="metric-card">
          <p>待处理作文</p>
          <strong>{pendingCount}</strong>
          <span>已提交或批改中的作文数量。</span>
        </article>
      </section>

      <section className="card-grid two-col dashboard-chart-grid">
        <article className="panel dashboard-chart-panel">
          <div className="section-heading">
            <div>
              <p className="eyebrow">柱状图</p>
              <h3>核心指标对比</h3>
            </div>
          </div>
          <div className="dashboard-bar-chart">
            {chartItems.map((item) => {
              const percent = Math.max(8, Math.round((item.value / maxValue) * 100))
              return (
                <div key={item.key} className="dashboard-bar-item">
                  <div className="dashboard-bar-meta">
                    <span>{item.label}</span>
                    <strong>{item.value}</strong>
                  </div>
                  <div className="dashboard-bar-track">
                    <div className="dashboard-bar-fill" style={{ width: `${percent}%` }} />
                  </div>
                </div>
              )
            })}
          </div>
        </article>

        <article className="panel dashboard-chart-panel">
          <div className="section-heading">
            <div>
              <p className="eyebrow">环形图</p>
              <h3>待处理占比</h3>
            </div>
          </div>
          <div className="dashboard-ring-wrap">
            <div
              className="dashboard-ring"
              style={
                {
                  '--ring-percent': `${pendingRatio}%`,
                } as CSSProperties
              }
            >
              <span>{pendingRatio}%</span>
            </div>
            <p className="helper-text">
              待处理 {pendingCount} / 总作文 {essayCount}
            </p>
          </div>
        </article>
      </section>
    </div>
  )
}

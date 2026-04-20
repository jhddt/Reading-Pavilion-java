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
    { key: 'essay', label: '作文总数', value: essayCount, color: '#3b82f6' },
    { key: 'review', label: '批改记录', value: reviewCount, color: '#10b981' },
    { key: 'rule', label: '细则数量', value: ruleCount, color: '#8b5cf6' },
    { key: 'pending', label: '待处理作文', value: pendingCount, color: '#f59e0b' },
  ]
  const maxValue = Math.max(...chartItems.map((item) => item.value), 1)
  const pendingRatio = essayCount > 0 ? Math.round((pendingCount / essayCount) * 100) : 0

  return (
    <div className="page-grid">
      <section className="card-grid four-col">
        <article className="dash-card metric-card">
          <div className="dash-card-icon" style={{ background: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6' }}>
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
          </div>
          <div className="dash-card-content">
            <p>作文总数</p>
            <strong>{essayCount}</strong>
            <span>来自当前账号的作文数据。</span>
          </div>
        </article>

        <article className="dash-card metric-card">
          <div className="dash-card-icon" style={{ background: 'rgba(16, 185, 129, 0.1)', color: '#10b981' }}>
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
          </div>
          <div className="dash-card-content">
            <p>批改记录</p>
            <strong>{reviewCount}</strong>
            <span>包含历史版本和当前最新结果。</span>
          </div>
        </article>

        <article className="dash-card metric-card">
          <div className="dash-card-icon" style={{ background: 'rgba(139, 92, 246, 0.1)', color: '#8b5cf6' }}>
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
          </div>
          <div className="dash-card-content">
            <p>细则数量</p>
            <strong>{ruleCount}</strong>
            <span>细则与评分维度都可继续配置。</span>
          </div>
        </article>

        <article className="dash-card metric-card">
          <div className="dash-card-icon" style={{ background: 'rgba(245, 158, 11, 0.1)', color: '#f59e0b' }}>
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
          </div>
          <div className="dash-card-content">
            <p>待处理作文</p>
            <strong>{pendingCount}</strong>
            <span>已提交或批改中的作文数量。</span>
          </div>
        </article>
      </section>

      <section className="card-grid two-col dashboard-chart-grid">
        <article className="panel dashboard-chart-panel enhanced-panel">
          <div className="section-heading">
            <div>
              <p className="eyebrow">柱状图</p>
              <h3>核心指标对比</h3>
            </div>
          </div>
          <div className="dashboard-bar-chart">
            {chartItems.map((item) => {
              const percent = Math.max(2, Math.round((item.value / maxValue) * 100))
              return (
                <div key={item.key} className="dashboard-bar-item">
                  <div className="dashboard-bar-meta">
                    <span style={{ fontWeight: 500 }}>{item.label}</span>
                    <strong style={{ color: item.color }}>{item.value}</strong>
                  </div>
                  <div className="dashboard-bar-track">
                    <div 
                      className="dashboard-bar-fill enhanced-fill" 
                      style={{ 
                        width: `${percent}%`, 
                        backgroundColor: item.color,
                        boxShadow: `0 2px 8px ${item.color}40`
                      }} 
                    />
                  </div>
                </div>
              )
            })}
          </div>
        </article>

        <article className="panel dashboard-chart-panel enhanced-panel">
          <div className="section-heading">
            <div>
              <p className="eyebrow">环形图</p>
              <h3>待处理占比</h3>
            </div>
          </div>
          <div className="dashboard-ring-wrap">
            <div
              className="dashboard-ring enhanced-ring"
              style={
                {
                  '--ring-percent': `${pendingRatio}%`,
                } as CSSProperties
              }
            >
              <div className="ring-inner">
                <strong>{pendingRatio}%</strong>
                <span>占比</span>
              </div>
            </div>
            <p className="helper-text ring-helper">
              <span className="dot pending-dot"></span>待处理 {pendingCount} 
              <span className="divider">/</span>
              <span className="dot total-dot"></span>总作文 {essayCount}
            </p>
          </div>
        </article>
      </section>
    </div>
  )
}

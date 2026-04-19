import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <p className="eyebrow">404</p>
        <h2>页面不存在</h2>
        <p className="helper-text">你访问的路径暂时没有对应页面。</p>
        <Link className="primary-button link-button full-button" to="/dashboard">
          返回首页
        </Link>
      </div>
    </div>
  )
}

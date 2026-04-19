export function formatDateTime(value?: string | null) {
  if (!value) return '暂无'
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatShortDateTime(value?: string | null) {
  if (!value) return '暂无'
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function roleText(role?: number | string | null) {
  if (role === 2 || role === '2' || role === 'teacher' || role === 'TEACHER') return '教师账户'
  if (role === 3 || role === '3' || role === 'admin' || role === 'ADMIN') return '管理账户'
  return '学生账户'
}

export function essayStatusText(status?: number) {
  const map: Record<number, string> = { 0: '草稿', 1: '已提交', 2: '批改中', 3: '已批改', 4: '已归档' }
  return status != null ? map[status] || '未知' : '未知'
}

export function reviewStatusText(status?: number) {
  const map: Record<number, string> = { 0: '初始化', 1: '处理中', 2: '成功', 3: '失败', 4: '超时' }
  return status != null ? map[status] || '未知' : '未知'
}

export function submitTypeText(type?: number) {
  const map: Record<number, string> = { 0: '图片录入', 1: '文档导入', 2: '文本输入' }
  return type != null ? map[type] || '未知来源' : '未知来源'
}

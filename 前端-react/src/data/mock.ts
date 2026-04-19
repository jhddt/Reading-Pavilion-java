export const navigationItems = [
  {
    to: '/dashboard',
    label: '概览',
    short: '01',
    description: '首页总览与快捷操作',
  },
  {
    to: '/essays',
    label: '作文',
    short: '02',
    description: '作文列表与内容状态',
  },
  {
    to: '/reviews',
    label: '批改',
    short: '03',
    description: '批改进度与历史记录',
  },
  {
    to: '/dimensions',
    label: '维度',
    short: '04',
    description: '评分细则与配置面板',
  },
]

export const overviewStats = [
  { label: '作文总数', value: '128' },
  { label: '待批改', value: '17' },
  { label: '规则数', value: '12' },
]

export const dashboardHighlights = [
  {
    title: '今日批改效率',
    value: '86%',
    text: '批改成功率稳定，失败任务数量明显下降。',
  },
  {
    title: '教师参与度',
    value: '23人',
    text: '本周人工复核次数增加，更适合做精修对照。',
  },
  {
    title: '最近提交峰值',
    value: '14:30',
    text: '下午时段提交量最高，适合提前分配算力与审核资源。',
  },
]

export const taskItems = [
  {
    title: '新建批改规则',
    desc: '为高一议论文新增一套结构化评分标准。',
    status: '建议优先处理',
  },
  {
    title: '检查失败任务',
    desc: '有 3 条记录因内容解析异常中断。',
    status: '需要复核',
  },
  {
    title: '优化学生端提交路径',
    desc: '文档导入成功率高，文本录入页仍有提升空间。',
    status: '本周关注',
  },
]

export const essays = [
  {
    title: '记一次春游',
    type: '文本输入',
    words: 862,
    status: '已批改',
    teacher: '王老师',
    time: '2026-04-16 10:12',
  },
  {
    title: '我眼中的责任',
    type: '图片录入',
    words: 1035,
    status: '批改中',
    teacher: '李老师',
    time: '2026-04-16 09:28',
  },
  {
    title: '一堂难忘的语文课',
    type: '文档导入',
    words: 788,
    status: '草稿',
    teacher: '陈老师',
    time: '2026-04-15 18:46',
  },
]

export const reviews = [
  {
    essayTitle: '记一次春游',
    version: 'V3',
    score: '86',
    reviewer: 'AI + 教师',
    rule: '初中记叙文标准',
    status: '成功',
  },
  {
    essayTitle: '我眼中的责任',
    version: 'V1',
    score: '待评分',
    reviewer: 'AI',
    rule: '高中议论文标准',
    status: '处理中',
  },
  {
    essayTitle: '以光为题',
    version: 'V2',
    score: '71',
    reviewer: '教师',
    rule: '高考作文综合标准',
    status: '复核完成',
  },
]

export const dimensions = [
  {
    name: '立意表达',
    detail: '重点关注中心是否鲜明、表达是否完整。',
    ratio: '30%',
  },
  {
    name: '结构层次',
    detail: '查看段落组织、衔接过渡与文章推进节奏。',
    ratio: '25%',
  },
  {
    name: '语言质量',
    detail: '聚焦用词准确度、句式变化与整体流畅性。',
    ratio: '25%',
  },
  {
    name: '书写与规范',
    detail: '用于 OCR 稿件的识别质量与基础规范判断。',
    ratio: '20%',
  },
]

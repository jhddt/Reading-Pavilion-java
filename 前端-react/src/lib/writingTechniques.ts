export type WritingGradeLevel = '小学低年级' | '小学中年级' | '小学高年级' | '初中'

export const WRITING_GRADE_OPTIONS: Array<{ value: WritingGradeLevel; label: string }> = [
  { value: '小学低年级', label: '小学低年级（1-2年级）' },
  { value: '小学中年级', label: '小学中年级（3-4年级）' },
  { value: '小学高年级', label: '小学高年级（5-6年级）' },
  { value: '初中', label: '初中（7-9年级）' },
]

// 32项写作手法完整列表
export const ALL_WRITING_TECHNIQUES = [
  // 小学低年级（1-2年级）- 8项
  { name: '比喻', grade: '小学低年级', description: '用"像"、"好像"等词把事物写得生动形象' },
  { name: '拟人', grade: '小学低年级', description: '把事物当作人来写，赋予人的动作和感情' },
  { name: '排比', grade: '小学低年级', description: '用结构相似的句子增强语气' },
  { name: '夸张', grade: '小学低年级', description: '故意夸大或缩小事物特征' },
  { name: '反问', grade: '小学低年级', description: '用疑问的形式表达确定的意思' },
  { name: '设问', grade: '小学低年级', description: '自己提出问题并回答' },
  { name: '对比', grade: '小学低年级', description: '把两种不同的事物或情况放在一起比较' },
  { name: '反复', grade: '小学低年级', description: '为了强调某个意思，重复使用某些词语或句子' },
  
  // 小学中年级（3-4年级）新增 - 8项
  { name: '借景抒情', grade: '小学中年级', description: '通过描写景物来表达感情' },
  { name: '动静结合', grade: '小学中年级', description: '既写动态又写静态，使画面生动' },
  { name: '点面结合', grade: '小学中年级', description: '既写整体又写个别，详略得当' },
  { name: '正面描写', grade: '小学中年级', description: '直接描写人物的外貌、语言、动作、心理' },
  { name: '侧面描写', grade: '小学中年级', description: '通过其他人或事物间接表现主要对象' },
  { name: '前后照应', grade: '小学中年级', description: '文章前后内容相互呼应' },
  { name: '首尾呼应', grade: '小学中年级', description: '开头和结尾相互照应' },
  { name: '过渡衔接', grade: '小学中年级', description: '用词语或句子连接上下文' },
  
  // 小学高年级（5-6年级）新增 - 8项
  { name: '托物言志', grade: '小学高年级', description: '借助某种事物来表达志向或情感' },
  { name: '欲扬先抑', grade: '小学高年级', description: '先贬低再赞扬，形成对比' },
  { name: '以小见大', grade: '小学高年级', description: '通过小事反映大道理' },
  { name: '象征', grade: '小学高年级', description: '用具体事物表现抽象概念' },
  { name: '联想想象', grade: '小学高年级', description: '由一事物想到另一事物' },
  { name: '细节描写', grade: '小学高年级', description: '对细小环节进行具体描绘' },
  { name: '环境描写', grade: '小学高年级', description: '描写自然环境或社会环境' },
  { name: '心理描写', grade: '小学高年级', description: '描写人物内心活动' },
  
  // 初中新增 - 8项
  { name: '对偶', grade: '初中', description: '字数相等、结构相同的两个句子或短语' },
  { name: '引用', grade: '初中', description: '引用名言、诗句、典故等' },
  { name: '反语', grade: '初中', description: '用相反的话来表达本意，带有讽刺意味' },
  { name: '双关', grade: '初中', description: '一个词语同时关顾两种意思' },
  { name: '顶真', grade: '初中', description: '前一句的结尾与后一句的开头相同' },
  { name: '通感', grade: '初中', description: '把不同感官的感觉沟通起来' },
  { name: '白描', grade: '初中', description: '用简练的笔墨勾勒形象' },
  { name: '渲染', grade: '初中', description: '对环境、气氛等进行多方面描写' },
] as const

// 按学段分组
export const TECHNIQUES_BY_GRADE = {
  '小学低年级': ALL_WRITING_TECHNIQUES.filter(t => t.grade === '小学低年级'),
  '小学中年级': ALL_WRITING_TECHNIQUES.filter(t => t.grade === '小学低年级' || t.grade === '小学中年级'),
  '小学高年级': ALL_WRITING_TECHNIQUES.filter(t => t.grade !== '初中'),
  '初中': ALL_WRITING_TECHNIQUES,
}

export type WritingTechniquesConfig = {
  grade_level: WritingGradeLevel
  required_techniques: string[]
  min_total_count: number
}

export function defaultWritingTechniquesConfig(): WritingTechniquesConfig {
  return { grade_level: '小学低年级', required_techniques: [], min_total_count: 0 }
}

// 获取指定学段可用的写作手法列表
export function getAvailableTechniques(gradeLevel: WritingGradeLevel) {
  return TECHNIQUES_BY_GRADE[gradeLevel] || []
}

export function safeParseWritingTechniquesJson(raw?: string | null) {
  const text = (raw || '').trim()
  if (!text) return { ok: true as const, config: defaultWritingTechniquesConfig(), raw: '' }
  try {
    const json = JSON.parse(text)
    if (!json || typeof json !== 'object' || Array.isArray(json)) {
      return { ok: false as const, error: '必须是 JSON 对象', raw: text }
    }

    const grade = json.grade_level
    const allowed = new Set(WRITING_GRADE_OPTIONS.map((o) => o.value))
    const gradeLevel: WritingGradeLevel = allowed.has(grade) ? grade : '小学低年级'
    const required = Array.isArray(json.required_techniques)
      ? json.required_techniques.map((x: unknown) => String(x || '').trim()).filter(Boolean)
      : []
    const minTotal = Number.isFinite(Number(json.min_total_count)) ? Number(json.min_total_count) : 0

    return {
      ok: true as const,
      config: {
        grade_level: gradeLevel,
        required_techniques: Array.from(new Set(required)),
        min_total_count: Math.max(0, Math.floor(minTotal)),
      },
      raw: text,
    }
  } catch (e) {
    return { ok: false as const, error: (e as Error).message || 'JSON 解析失败', raw: text }
  }
}

export function buildWritingTechniquesJson(config: WritingTechniquesConfig) {
  const normalized: WritingTechniquesConfig = {
    grade_level: config.grade_level,
    required_techniques: Array.from(new Set((config.required_techniques || []).map((x) => x.trim()).filter(Boolean))),
    min_total_count: Math.max(0, Math.floor(Number(config.min_total_count || 0))),
  }
  return JSON.stringify(normalized, null, 2)
}

export function tryFormatWritingTechniquesJson(raw?: string | null) {
  const text = (raw || '').trim()
  if (!text) return { ok: true as const, json: '' }
  try {
    const obj = JSON.parse(text)
    if (!obj || typeof obj !== 'object' || Array.isArray(obj)) {
      return { ok: false as const, error: '必须是 JSON 对象' }
    }
    return { ok: true as const, json: JSON.stringify(obj, null, 2) }
  } catch (e) {
    return { ok: false as const, error: (e as Error).message || 'JSON 解析失败' }
  }
}


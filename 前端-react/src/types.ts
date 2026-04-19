export interface LoginResponse {
  token: string
  userId: number | null
  userName: string
  role: number | string | null
}

export interface UserProfile {
  userId: number | null
  userName: string
  role: number | string | null
  avatarUrl?: string | null
  avatarPreviewUrl?: string | null
  status?: number | string | null
  avatarUpdateTime?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface Essay {
  id: number
  userId: number
  title: string
  submitType: number
  originalContent?: string
  finalContent?: string
  beautifiedContent?: string
  wordCount?: number
  status: number
  createTime?: string
  updateTime?: string
}

export interface PageData<T> {
  records?: T[]
  rows?: T[]
  total?: number
  size?: number
  current?: number
  pages?: number
}

export interface ReviewRecord {
  reviewId: number
  essayId: number
  essayTitle?: string
  reviewVersion?: number
  latestVersion?: boolean
  ruleId?: number
  ruleName?: string
  gradeLevel?: string
  reviewType?: string
  reviewerType?: number
  status?: number
  totalScore?: number | null
  modelVersion?: string
  startTime?: string
  endTime?: string
  createTime?: string
}

export interface ReviewScore {
  dimensionId: number
  dimensionName: string
  score: number | null
}

export interface ReviewComment {
  commentId: number
  commentType?: number
  content: string
  relatedText?: string
  startOffset?: number | null
  endOffset?: number | null
}

export interface TextCorrection {
  errorType?: string
  startOffset?: number
  endOffset?: number
  originalText?: string
  correctedText?: string
  suggestion?: string
}

export interface ReviewDetail extends ReviewRecord {
  comments?: ReviewComment[]
  scores?: ReviewScore[]
  textCorrections?: TextCorrection[]
  topicRequirement?: string
  beautifyLevel?: string
  customRequirement?: string
  deductionDetail?: string
}

export interface ReviewStatus {
  reviewId: number
  status: number
  errorMsg?: string
}

export interface ReviewRule {
  ruleId: number
  ruleName: string
  reviewType?: string
  gradeLevel?: string
  promptTemplate?: string
  topicRequirement?: string
  beautifyLevel?: string
  customRequirement?: string
  deductionDetail?: string
  status?: number
}

export interface ScoreDimension {
  dimensionId: number
  ruleId?: number | null
  dimensionName: string
  weight: number
  maxScore: number
  description?: string
  sortOrder?: number
  status?: number
}

export interface AuditLog {
  logId: number
  userId?: number | null
  username?: string | null
  action?: string | null
  targetType?: string | null
  targetId?: string | null
  requestMethod?: string | null
  requestPath?: string | null
  requestIp?: string | null
  requestId?: string | null
  resultCode?: number | null
  success?: number | null
  errorMessage?: string | null
  createdAt?: string | null
}

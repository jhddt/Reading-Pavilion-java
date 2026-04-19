// 批改相关类型定义

export interface ReviewScore {
  scoreId?: number
  dimensionId: number
  dimensionName: string
  weightSnapshot: number
  score: number
}

export interface ReviewComment {
  commentId: number
  commentType: number // 1-总评 2-建议 3-修改意见
  content: string
  startOffset?: number
  endOffset?: number
  relatedText?: string
  createTime?: string
}

export interface TextCorrection {
  correctionId?: number
  originalText: string
  correctedText: string
  startOffset?: number
  endOffset?: number
  errorType?: string
  suggestion?: string
}

export interface ReviewDetail {
  reviewId: number
  essayId: number
  essayTitle?: string
  reviewerType: number
  reviewerId?: number
  modelVersion: string
  startTime: string
  endTime?: string
  totalScore?: number
  status: number
  errorMsg?: string
  createTime: string
  ruleId?: number
  ruleName?: string
  reviewType?: string
  gradeLevel?: string
  topicRequirement?: string
  beautifyLevel?: string
  customRequirement?: string
  deductionDetail?: string
  reviewVersion?: number
  latestVersion?: boolean
  scores: ReviewScore[]
  comments: ReviewComment[]
  textCorrections: TextCorrection[]
}

export interface ReviewRecord {
  reviewId: number
  essayId: number
  taskId?: number
  ruleVersion?: string
  reviewerType: number
  reviewerId?: number
  modelVersion?: string
  startTime?: string
  endTime?: string
  totalScore?: number
  status: number
  errorMsg?: string
  retryCount?: number
  createTime?: string
  essayTitle?: string
  submitType?: number
  ruleId?: number
  ruleName?: string
  reviewType?: string
  gradeLevel?: string
  reviewVersion?: number
  latestVersion?: boolean
}

export interface ReviewStatus {
  reviewId: number
  essayId: number
  status: number
  totalScore?: number
  errorMsg?: string
  updateTime?: string
}

export interface TextSegment {
  text: string
  marked: boolean
  type?: 'correction' | 'suggestion' | 'revision'
  number?: number
}

export interface Annotation {
  id: string
  number: number
  type: 'correction' | 'suggestion' | 'revision'
  original?: string
  corrected?: string
  suggestion?: string
  relatedText?: string
  content?: string
}

export interface Paragraph {
  segments: TextSegment[]
  annotations: Annotation[]
}

// 维度颜色映射
export const DIMENSION_COLORS = [
  '#FFC107', // Focus & Argument - 黄色
  '#FF9800', // Organization - 橙色
  '#F44336', // Evidence - 红色
  '#2196F3', // Clarity - 蓝色
  '#4CAF50', // Length - 绿色
]

// 分数等级颜色
export const GRADE_COLORS = {
  high: '#4CAF50', // ≥90% 绿色
  medium: '#FFC107', // 70-89% 黄色
  low: '#F44336', // <70% 红色
}

// 建议卡片颜色
export const SUGGESTION_COLORS = {
  background: '#E3F2FD', // 浅蓝色背景
  border: '#2196F3', // 蓝色边框
  text: '#2196F3', // 蓝色文字
}

// 箭头颜色
export const ARROW_COLOR = 'rgba(244, 67, 54, 0.8)' // 红色箭头

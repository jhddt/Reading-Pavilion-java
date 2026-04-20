/**
 * 模型常把「改进建议 / 修改意见 / 总分」等段落拼进同一条总评正文；总评区只展示其前的整体评价部分。
 * 与 Vue 端 ReviewAnnotationView 的 summaryText 逻辑对齐，并兼容「。【改进建议】」等无换行衔接。
 */
const BUNDLED_SECTION_HEAD =
  /\s*【+\s*(?:改进建议|修改意见|总分|各维度得分详情|评分详情)\s*】/u

export function extractOverallSummaryLead(raw: string): string {
  if (!raw) return ''
  const normalized = raw.replace(/\r\n?/g, '\n').trim()
  const withoutTag = normalized.replace(/^【总评】\s*/u, '')
  const hit = withoutTag.match(BUNDLED_SECTION_HEAD)
  if (!hit || hit.index == null) return withoutTag.trim()
  return withoutTag.slice(0, hit.index).trim()
}

export function normalizeOcrTextForDisplay(raw?: string | null) {
  if (!raw) return ''
  return raw
    .replace(/\r\n?/g, '\n')
    .replace(/(?<=[\u4e00-\u9fff])[\t ]+(?=[\u4e00-\u9fff])/g, '')
    .replace(/[\t ]+([，。！？；：、“”‘’《》【】（）])/g, '$1')
    .replace(/([，。！？；：、“”‘’《》【】（）])[\t ]+/g, '$1')
    .trim()
}


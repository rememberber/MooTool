export function splitTranslationText(text: string, maxLength: number): string[] {
  if (!Number.isInteger(maxLength) || maxLength < 2) throw new Error('Invalid translation chunk size')
  if (text.length <= maxLength) return [text]
  const chunks: string[] = []
  let offset = 0
  while (offset < text.length) {
    let end = Math.min(offset + maxLength, text.length)
    if (end < text.length) {
      const minimumNaturalBreak = offset + Math.floor(maxLength / 2)
      for (let cursor = end; cursor > minimumNaturalBreak; cursor -= 1) {
        if (/[\s.!?。！？,，;；:：]/u.test(text[cursor - 1])) {
          end = cursor
          break
        }
      }
      const previousCodeUnit = text.charCodeAt(end - 1)
      const nextCodeUnit = text.charCodeAt(end)
      if (previousCodeUnit >= 0xD800 && previousCodeUnit <= 0xDBFF && nextCodeUnit >= 0xDC00 && nextCodeUnit <= 0xDFFF) {
        end -= 1
      }
    }
    chunks.push(text.slice(offset, end))
    offset = end
  }
  return chunks
}


type ClipboardImageData = Pick<DataTransfer, 'files' | 'items' | 'types'>

export type TextSelection = { start: number; end: number }

export type MarkdownImageInsertion = TextSelection & {
  text: string
  caret: number
}

const imageFileExtension = /\.(?:png|jpe?g|gif|bmp|webp)$/i

export function clipboardImageFile(data: ClipboardImageData | null): File | null {
  if (!data) return null
  const file = Array.from(data.files).find(isImageFile)
  if (file) return file
  for (const item of Array.from(data.items)) {
    if (!item.type.toLocaleLowerCase().startsWith('image/')) continue
    const itemFile = item.getAsFile()
    if (itemFile) return itemFile
  }
  return null
}

export function clipboardContainsImage(data: ClipboardImageData | null): boolean {
  if (!data) return false
  return Array.from(data.items).some((item) => item.type.toLocaleLowerCase().startsWith('image/'))
    || Array.from(data.files).some(isImageFile)
    || Array.from(data.types).some((type) => type.toLocaleLowerCase().startsWith('image/'))
}

export function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.addEventListener('load', () => typeof reader.result === 'string' ? resolve(reader.result) : reject(new Error('Unable to read clipboard image')))
    reader.addEventListener('error', () => reject(reader.error ?? new Error('Unable to read clipboard image')))
    reader.readAsDataURL(file)
  })
}

export function prepareMarkdownImageInsertion(
  content: string,
  requestedSelection: TextSelection,
  markdown: string
): MarkdownImageInsertion {
  const start = Math.max(0, Math.min(requestedSelection.start, content.length))
  const end = Math.max(start, Math.min(requestedSelection.end, content.length))
  const leadingBreak = start > 0 && content[start - 1] !== '\n' ? '\n' : ''
  const trailingBreak = end < content.length && content[end] !== '\n' ? '\n' : ''
  const text = `${leadingBreak}${markdown}${trailingBreak}`
  return { start, end, text, caret: start + text.length }
}

function isImageFile(file: Pick<File, 'name' | 'type'>): boolean {
  return file.type.toLocaleLowerCase().startsWith('image/') || imageFileExtension.test(file.name)
}

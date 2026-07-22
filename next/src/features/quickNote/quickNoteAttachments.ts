type ImageTransferData = Pick<DataTransfer, 'files' | 'items' | 'types'>

export type TextSelection = { start: number; end: number }

export type MarkdownImageInsertion = TextSelection & {
  text: string
  caret: number
}

const imageFileExtension = /\.(?:png|jpe?g|gif|bmp|webp)$/i

export function imageFilesFromDataTransfer(data: ImageTransferData | null): File[] {
  if (!data) return []
  const files = Array.from(data.files).filter(isImageFile)
  if (files.length > 0) return files
  return Array.from(data.items).flatMap((item) => {
    if (!item.type.toLocaleLowerCase().startsWith('image/')) return []
    const file = item.getAsFile()
    return file ? [file] : []
  })
}

export function clipboardImageFile(data: ImageTransferData | null): File | null {
  return imageFilesFromDataTransfer(data)[0] ?? null
}

export function clipboardContainsImage(data: ImageTransferData | null): boolean {
  if (!data) return false
  return Array.from(data.items).some((item) => item.type.toLocaleLowerCase().startsWith('image/'))
    || Array.from(data.files).some(isImageFile)
    || Array.from(data.types).some((type) => type.toLocaleLowerCase().startsWith('image/'))
}

export function dataTransferContainsFiles(data: ImageTransferData | null): boolean {
  if (!data) return false
  return data.files.length > 0
    || Array.from(data.items).some((item) => item.kind === 'file')
    || Array.from(data.types).some((type) => type.toLocaleLowerCase() === 'files')
}

export function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.addEventListener('load', () => typeof reader.result === 'string' ? resolve(reader.result) : reject(new Error('Unable to read image file')))
    reader.addEventListener('error', () => reject(reader.error ?? new Error('Unable to read image file')))
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

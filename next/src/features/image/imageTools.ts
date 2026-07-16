export type ImageOutputFormat = 'auto' | 'png' | 'jpeg'
export type WatermarkPosition = 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left' | 'center' | 'tile'
export type ImageOutputMode = 'keep' | 'overwrite'

export type CompressImageOptions = {
  quality: number
  scale: number
  format: ImageOutputFormat
}

export type WatermarkImageOptions = {
  text: string
  opacity: number
  color: string
  position: WatermarkPosition
  fontSize: 'auto' | 'small' | 'medium' | 'large'
  diagonal: boolean
}

export type CropRect = { x: number; y: number; width: number; height: number }

export async function compressImage(dataUrl: string, options: CompressImageOptions): Promise<string> {
  const image = await loadImage(dataUrl)
  const { width, height } = scaledDimensions(image.naturalWidth, image.naturalHeight, options.scale)
  const canvas = createCanvas(width, height)
  const context = getContext(canvas)
  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'
  if (options.format === 'jpeg') {
    context.fillStyle = '#FFFFFF'
    context.fillRect(0, 0, width, height)
  }
  context.drawImage(image, 0, 0, width, height)
  const mime = options.format === 'jpeg' ? 'image/jpeg' : options.format === 'png' ? 'image/png' : dataUrl.startsWith('data:image/jpeg') ? 'image/jpeg' : 'image/png'
  return canvas.toDataURL(mime, clamp(options.quality, 0.01, 1))
}

export async function watermarkImage(dataUrl: string, options: WatermarkImageOptions): Promise<string> {
  if (!options.text.trim()) throw new Error('Watermark text is required')
  const image = await loadImage(dataUrl)
  const canvas = createCanvas(image.naturalWidth, image.naturalHeight)
  const context = getContext(canvas)
  context.drawImage(image, 0, 0)
  const fontSize = resolveWatermarkFontSize(image.naturalWidth, image.naturalHeight, options.fontSize)
  context.font = `700 ${fontSize}px system-ui, sans-serif`
  context.textBaseline = 'alphabetic'
  context.fillStyle = withAlpha(options.color, clamp(options.opacity, 0.01, 1))
  const metrics = context.measureText(options.text.trim())
  const textWidth = metrics.width
  const textHeight = fontSize * 1.2
  const margin = Math.max(8, Math.round(Math.min(canvas.width, canvas.height) * 0.02))
  if (options.position === 'tile') {
    const stepX = textWidth + margin * 3
    const stepY = textHeight + margin * 3
    for (let y = -canvas.height; y < canvas.height * 2; y += stepY) {
      for (let x = -canvas.width; x < canvas.width * 2; x += stepX) drawText(context, options.text.trim(), x, y, options.diagonal)
    }
  } else {
    const point = watermarkAnchor(canvas.width, canvas.height, textWidth, textHeight, options.position, margin)
    drawText(context, options.text.trim(), point.x, point.y, options.diagonal)
  }
  return canvas.toDataURL(dataUrl.startsWith('data:image/jpeg') ? 'image/jpeg' : 'image/png', 0.92)
}

export async function cropImage(dataUrl: string, rect: CropRect): Promise<string> {
  const image = await loadImage(dataUrl)
  const x = Math.max(0, Math.round(rect.x))
  const y = Math.max(0, Math.round(rect.y))
  const width = Math.min(image.naturalWidth - x, Math.max(1, Math.round(rect.width)))
  const height = Math.min(image.naturalHeight - y, Math.max(1, Math.round(rect.height)))
  if (x >= image.naturalWidth || y >= image.naturalHeight || width < 1 || height < 1) throw new Error('Crop rectangle is outside the image')
  const canvas = createCanvas(width, height)
  getContext(canvas).drawImage(image, x, y, width, height, 0, 0, width, height)
  return canvas.toDataURL('image/png')
}

export function scaledDimensions(width: number, height: number, scale: number): { width: number; height: number } {
  const normalized = clamp(scale, 0.1, 1)
  return { width: Math.max(1, Math.round(width * normalized)), height: Math.max(1, Math.round(height * normalized)) }
}

export function processedImageName(name: string, suffix: 'compressed' | 'watermarked', format: ImageOutputFormat = 'auto'): string {
  const extensionIndex = name.lastIndexOf('.')
  const base = extensionIndex > 0 ? name.slice(0, extensionIndex) : name
  const currentExtension = extensionIndex > 0 ? name.slice(extensionIndex + 1).toLowerCase() : 'png'
  const extension = format === 'auto' ? (currentExtension === 'jpg' || currentExtension === 'jpeg' ? 'jpg' : 'png') : format === 'jpeg' ? 'jpg' : 'png'
  return `${base}_${suffix}.${extension}`
}

export function watermarkAnchor(width: number, height: number, textWidth: number, textHeight: number, position: Exclude<WatermarkPosition, 'tile'>, margin: number): { x: number; y: number } {
  if (position === 'top-left') return { x: margin, y: margin + textHeight }
  if (position === 'top-right') return { x: width - textWidth - margin, y: margin + textHeight }
  if (position === 'bottom-left') return { x: margin, y: height - margin }
  if (position === 'center') return { x: (width - textWidth) / 2, y: (height + textHeight) / 2 }
  return { x: width - textWidth - margin, y: height - margin }
}

export function ensureImageDataUrl(value: string): string {
  const trimmed = value.trim()
  if (/^data:image\/[\w.+-]+;base64,/.test(trimmed)) return trimmed
  if (/^[A-Za-z0-9+/=\s]+$/.test(trimmed)) return `data:image/png;base64,${trimmed.replace(/\s+/g, '')}`
  throw new Error('Invalid image Base64')
}

function resolveWatermarkFontSize(width: number, height: number, mode: WatermarkImageOptions['fontSize']): number {
  const base = Math.round(Math.min(width, height) * 0.05)
  if (mode === 'small') return Math.max(16, Math.min(base, 28))
  if (mode === 'medium') return Math.max(24, Math.min(base + 8, 42))
  if (mode === 'large') return Math.max(32, Math.min(base + 16, 64))
  return Math.max(18, Math.min(base, 48))
}

function withAlpha(color: string, opacity: number): string {
  const hex = color.replace('#', '')
  if (!/^[0-9a-fA-F]{6}$/.test(hex)) return `rgba(255,255,255,${opacity})`
  const channels = [0, 2, 4].map((index) => Number.parseInt(hex.slice(index, index + 2), 16))
  return `rgba(${channels[0]},${channels[1]},${channels[2]},${opacity})`
}

function drawText(context: CanvasRenderingContext2D, text: string, x: number, y: number, diagonal: boolean): void {
  context.save()
  if (diagonal) {
    context.translate(x, y)
    context.rotate(-Math.PI / 4)
    context.translate(-x, -y)
  }
  context.fillText(text, x, y)
  context.restore()
}

function loadImage(dataUrl: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('Unable to load image'))
    image.src = dataUrl
  })
}

function createCanvas(width: number, height: number): HTMLCanvasElement {
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  return canvas
}

function getContext(canvas: HTMLCanvasElement): CanvasRenderingContext2D {
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Canvas is unavailable')
  return context
}

function clamp(value: number, minimum: number, maximum: number): number {
  return Math.min(maximum, Math.max(minimum, value))
}

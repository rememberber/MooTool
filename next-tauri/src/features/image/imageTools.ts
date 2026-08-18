export type ImageFormat = 'auto' | 'png' | 'jpeg'
export type WatermarkPosition = 'top-left' | 'top-right' | 'center' | 'bottom-left' | 'bottom-right' | 'tile'
export type ImageToolErrorCode = 'invalidBase64' | 'watermarkText' | 'imageRead' | 'canvas'

export class ImageToolError extends Error {
  constructor(readonly code: ImageToolErrorCode) {
    super(`IMAGE_TOOL_${code}`)
    this.name = 'ImageToolError'
  }
}

export interface CompressOptions {
  quality: number
  scale: number
  format: ImageFormat
}

export interface WatermarkOptions {
  text: string
  opacity: number
  color: string
  fontSize: number
  position: WatermarkPosition
  diagonal: boolean
}

export function processedImageName(name: string, suffix: 'compressed' | 'watermarked', format: ImageFormat = 'auto'): string {
  const dot = name.lastIndexOf('.')
  const base = dot > 0 ? name.slice(0, dot) : name
  const sourceExtension = dot > 0 ? name.slice(dot + 1).toLowerCase() : 'png'
  const extension = format === 'jpeg' ? 'jpg' : format === 'png' ? 'png' : ['jpg', 'jpeg'].includes(sourceExtension) ? 'jpg' : 'png'
  return `${base}_${suffix}.${extension}`
}

export function scaledDimensions(width: number, height: number, scale: number): { width: number; height: number } {
  const normalized = Math.min(1, Math.max(0.1, scale))
  return { width: Math.max(1, Math.round(width * normalized)), height: Math.max(1, Math.round(height * normalized)) }
}

export function ensureImageDataUrl(value: string): string {
  const trimmed = value.trim()
  if (/^data:image\/(?:png|jpeg|webp|gif);base64,[A-Za-z0-9+/=\s]+$/i.test(trimmed)) return trimmed.replace(/\s/g, '')
  if (/^[A-Za-z0-9+/=\s]+$/.test(trimmed)) return `data:image/png;base64,${trimmed.replace(/\s/g, '')}`
  throw new ImageToolError('invalidBase64')
}

export async function imageDimensions(dataUrl: string): Promise<{ width: number; height: number }> {
  const image = await loadImage(dataUrl)
  return { width: image.naturalWidth, height: image.naturalHeight }
}

export async function compressImage(dataUrl: string, options: CompressOptions): Promise<string> {
  const image = await loadImage(dataUrl)
  const dimensions = scaledDimensions(image.naturalWidth, image.naturalHeight, options.scale)
  const canvas = createCanvas(dimensions.width, dimensions.height)
  const context = context2d(canvas)
  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'
  if (options.format === 'jpeg') {
    context.fillStyle = '#fff'
    context.fillRect(0, 0, canvas.width, canvas.height)
  }
  context.drawImage(image, 0, 0, canvas.width, canvas.height)
  const mimeType = options.format === 'jpeg' ? 'image/jpeg' : options.format === 'png' ? 'image/png' : dataUrl.startsWith('data:image/jpeg') ? 'image/jpeg' : 'image/png'
  return canvas.toDataURL(mimeType, Math.min(1, Math.max(0.05, options.quality)))
}

export async function watermarkImage(dataUrl: string, options: WatermarkOptions): Promise<string> {
  if (!options.text.trim()) throw new ImageToolError('watermarkText')
  const image = await loadImage(dataUrl)
  const canvas = createCanvas(image.naturalWidth, image.naturalHeight)
  const context = context2d(canvas)
  context.drawImage(image, 0, 0)
  const fontSize = Math.min(200, Math.max(12, options.fontSize))
  context.font = `700 ${fontSize}px system-ui, sans-serif`
  context.textBaseline = 'alphabetic'
  context.fillStyle = colorWithOpacity(options.color, options.opacity)
  const width = context.measureText(options.text.trim()).width
  const height = fontSize * 1.15
  const margin = Math.max(10, Math.round(Math.min(canvas.width, canvas.height) * 0.025))
  if (options.position === 'tile') {
    for (let y = -canvas.height; y < canvas.height * 2; y += height + margin * 4) {
      for (let x = -canvas.width; x < canvas.width * 2; x += width + margin * 4) drawWatermark(context, options.text.trim(), x, y, true)
    }
  } else {
    const point = watermarkPoint(canvas.width, canvas.height, width, height, margin, options.position)
    drawWatermark(context, options.text.trim(), point.x, point.y, options.diagonal)
  }
  return canvas.toDataURL(dataUrl.startsWith('data:image/jpeg') ? 'image/jpeg' : 'image/png', 0.94)
}

function watermarkPoint(canvasWidth: number, canvasHeight: number, textWidth: number, textHeight: number, margin: number, position: Exclude<WatermarkPosition, 'tile'>) {
  if (position === 'top-left') return { x: margin, y: margin + textHeight }
  if (position === 'top-right') return { x: canvasWidth - textWidth - margin, y: margin + textHeight }
  if (position === 'bottom-left') return { x: margin, y: canvasHeight - margin }
  if (position === 'center') return { x: (canvasWidth - textWidth) / 2, y: (canvasHeight + textHeight) / 2 }
  return { x: canvasWidth - textWidth - margin, y: canvasHeight - margin }
}

function drawWatermark(context: CanvasRenderingContext2D, text: string, x: number, y: number, diagonal: boolean) {
  context.save()
  if (diagonal) {
    context.translate(x, y)
    context.rotate(-Math.PI / 4)
    context.translate(-x, -y)
  }
  context.fillText(text, x, y)
  context.restore()
}

function colorWithOpacity(color: string, opacity: number): string {
  const value = color.replace('#', '')
  const alpha = Math.min(1, Math.max(0.01, opacity))
  if (!/^[0-9a-f]{6}$/i.test(value)) return `rgba(255,255,255,${alpha})`
  const [red, green, blue] = [0, 2, 4].map((offset) => Number.parseInt(value.slice(offset, offset + 2), 16))
  return `rgba(${red},${green},${blue},${alpha})`
}

function loadImage(dataUrl: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new ImageToolError('imageRead'))
    image.src = dataUrl
  })
}

function createCanvas(width: number, height: number): HTMLCanvasElement {
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  return canvas
}

function context2d(canvas: HTMLCanvasElement): CanvasRenderingContext2D {
  const context = canvas.getContext('2d')
  if (!context) throw new ImageToolError('canvas')
  return context
}

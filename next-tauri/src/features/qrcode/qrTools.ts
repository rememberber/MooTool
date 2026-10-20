import { BrowserQRCodeReader } from '@zxing/browser'
import { DecodeHintType } from '@zxing/library'
import QRCode from 'qrcode'

export type QrErrorCorrection = 'L' | 'M' | 'Q' | 'H'
export type QrToolErrorCode = 'empty' | 'contentLimit' | 'generate' | 'imageType' | 'imageLimit' | 'notFound' | 'size' | 'margin' | 'color'

export class QrToolError extends Error {
  constructor(
    readonly code: QrToolErrorCode,
    readonly values: Record<string, string | number> = {}
  ) {
    super(`QR_TOOL_${code}`)
    this.name = 'QrToolError'
  }
}

export interface QrOptions {
  dark: string
  errorCorrection: QrErrorCorrection
  light: string
  margin: number
  size: number
}

export const defaultQrOptions: QrOptions = {
  dark: '#111111',
  errorCorrection: 'M',
  light: '#FFFFFF',
  margin: 4,
  size: 300
}

export function normalizeQrSize(size: number): number {
  if (!Number.isFinite(size)) return 300
  return Math.min(2000, Math.max(120, Math.round(size)))
}

export async function generateQrSvg(value: string, options: QrOptions): Promise<string> {
  const text = validateContent(value)
  validateOptions(options)
  try {
    return await QRCode.toString(text, {
      type: 'svg',
      width: options.size,
      margin: options.margin,
      errorCorrectionLevel: options.errorCorrection,
      color: { dark: options.dark, light: options.light }
    })
  } catch (cause) {
    throw new QrToolError('generate', { error: cause instanceof Error ? cause.message : String(cause) })
  }
}

export async function generateQrPngDataUrl(value: string, options: QrOptions, logoDataUrl = ''): Promise<string> {
  const text = validateContent(value)
  const width = normalizeQrSize(options.size)
  try {
    const dataUrl = await QRCode.toDataURL(text, {
      width,
      margin: 4,
      errorCorrectionLevel: options.errorCorrection,
      color: { dark: '#111111', light: '#FFFFFF' }
    })
    if (!logoDataUrl) return dataUrl
    return addLogo(dataUrl, logoDataUrl, width)
  } catch (cause) {
    if (cause instanceof QrToolError) throw cause
    throw new QrToolError('generate', { error: cause instanceof Error ? cause.message : String(cause) })
  }
}

async function addLogo(qrDataUrl: string, logoDataUrl: string, size: number): Promise<string> {
  const [qrImage, logoImage] = await Promise.all([loadImage(qrDataUrl), loadImage(logoDataUrl)])
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Canvas is unavailable')
  context.drawImage(qrImage, 0, 0, size, size)
  const logoSize = Math.round(size * 0.2)
  const x = Math.round((size - logoSize) / 2)
  const y = Math.round((size - logoSize) / 2)
  const padding = Math.max(4, Math.round(size * 0.015))
  context.fillStyle = '#FFFFFF'
  context.fillRect(x - padding, y - padding, logoSize + padding * 2, logoSize + padding * 2)
  context.drawImage(logoImage, x, y, logoSize, logoSize)
  return canvas.toDataURL('image/png')
}

export function svgDataUrl(svg: string): string {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

export async function decodeQrImage(file: File): Promise<string> {
  if (!file.type.startsWith('image/')) throw new QrToolError('imageType')
  if (file.size > 15 * 1024 * 1024) throw new QrToolError('imageLimit')
  const dataUrl = await readQrLogo(file)
  return recognizeQrDataUrl(dataUrl)
}

export async function recognizeQrDataUrl(dataUrl: string): Promise<string> {
  if (!/^data:image\//.test(dataUrl)) throw new QrToolError('imageType')
  const image = await loadImage(dataUrl)
  const canvas = document.createElement('canvas')
  canvas.width = image.naturalWidth || image.width
  canvas.height = image.naturalHeight || image.height
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Canvas is unavailable')
  context.fillStyle = '#FFFFFF'
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.drawImage(image, 0, 0, canvas.width, canvas.height)
  const hints = new Map<DecodeHintType, unknown>([[DecodeHintType.TRY_HARDER, true]])
  try {
    return new BrowserQRCodeReader(hints).decodeFromCanvas(canvas).getText()
  } catch {
    try {
      hints.set(DecodeHintType.PURE_BARCODE, true)
      return new BrowserQRCodeReader(hints).decodeFromCanvas(canvas).getText()
    } catch {
      throw new QrToolError('notFound')
    }
  }
}

export function readQrLogo(file: File): Promise<string> {
  if (!file.type.startsWith('image/')) throw new QrToolError('imageType')
  if (file.size > 5 * 1024 * 1024) throw new QrToolError('imageLimit')
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(new QrToolError('imageType'))
    reader.readAsDataURL(file)
  })
}

function validateContent(value: string): string {
  const text = value.trim()
  if (!text) throw new QrToolError('empty')
  if (text.length > 16_384) throw new QrToolError('contentLimit')
  return text
}

function loadImage(source: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('Unable to decode QR image'))
    image.src = source
  })
}

function validateOptions(options: QrOptions): void {
  if (!Number.isInteger(options.size) || options.size < 120 || options.size > 2000) {
    throw new QrToolError('size')
  }
  if (!Number.isInteger(options.margin) || options.margin < 0 || options.margin > 16) {
    throw new QrToolError('margin')
  }
  if (!/^#[0-9a-f]{6}$/i.test(options.dark) || !/^#[0-9a-f]{6}$/i.test(options.light)) {
    throw new QrToolError('color')
  }
}

import { BrowserQRCodeReader } from '@zxing/browser'
import { DecodeHintType } from '@zxing/library'
import QRCode from 'qrcode'

export type QrErrorCorrection = 'L' | 'M' | 'Q' | 'H'

export async function generateQrDataUrl(content: string, size: number, errorCorrectionLevel: QrErrorCorrection, logoDataUrl = ''): Promise<string> {
  if (!content.trim()) throw new Error('QR content is required')
  const width = normalizeQrSize(size)
  const qrDataUrl = await QRCode.toDataURL(content, {
    width,
    margin: 4,
    errorCorrectionLevel,
    color: { dark: '#111111', light: '#FFFFFF' }
  })
  if (!logoDataUrl) return qrDataUrl
  return addLogo(qrDataUrl, logoDataUrl, width)
}

export async function recognizeQrDataUrl(dataUrl: string): Promise<string> {
  if (!/^data:image\//.test(dataUrl)) throw new Error('A valid image is required')
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
    hints.set(DecodeHintType.PURE_BARCODE, true)
    return new BrowserQRCodeReader(hints).decodeFromCanvas(canvas).getText()
  }
}

export function normalizeQrSize(size: number): number {
  if (!Number.isFinite(size)) return 300
  return Math.min(2000, Math.max(120, Math.round(size)))
}

async function addLogo(qrDataUrl: string, logoDataUrl: string, size: number): Promise<string> {
  if (typeof document === 'undefined') return qrDataUrl
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

function loadImage(source: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('Unable to load image'))
    image.src = source
  })
}

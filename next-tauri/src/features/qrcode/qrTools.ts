import { BrowserQRCodeReader } from '@zxing/browser'
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
  dark: '#111827',
  errorCorrection: 'M',
  light: '#ffffff',
  margin: 2,
  size: 420
}

export async function generateQrSvg(value: string, options: QrOptions): Promise<string> {
  const text = value.trim()
  if (!text) throw new QrToolError('empty')
  if (text.length > 16_384) throw new QrToolError('contentLimit')
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

export function svgDataUrl(svg: string): string {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

export async function decodeQrImage(file: File): Promise<string> {
  if (!file.type.startsWith('image/')) throw new QrToolError('imageType')
  if (file.size > 15 * 1024 * 1024) throw new QrToolError('imageLimit')
  const url = URL.createObjectURL(file)
  try {
    const result = await new BrowserQRCodeReader(undefined, { delayBetweenScanAttempts: 100 })
      .decodeFromImageUrl(url)
    return result.getText()
  } catch {
    throw new QrToolError('notFound')
  } finally {
    URL.revokeObjectURL(url)
  }
}

function validateOptions(options: QrOptions): void {
  if (!Number.isInteger(options.size) || options.size < 160 || options.size > 2048) {
    throw new QrToolError('size')
  }
  if (!Number.isInteger(options.margin) || options.margin < 0 || options.margin > 16) {
    throw new QrToolError('margin')
  }
  if (!/^#[0-9a-f]{6}$/i.test(options.dark) || !/^#[0-9a-f]{6}$/i.test(options.light)) {
    throw new QrToolError('color')
  }
}

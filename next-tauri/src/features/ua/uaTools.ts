import { UAParser } from 'ua-parser-js'

export interface UaResult {
  browser: string
  browserVersion: string
  engine: string
  engineVersion: string
  os: string
  osVersion: string
  cpu: string
  deviceType: string
  deviceBrand: string
  deviceModel: string
  mobile: boolean
  bot: boolean
}

export type UaToolErrorCode = 'empty' | 'tooLong'

export class UaToolError extends Error {
  constructor(readonly code: UaToolErrorCode) {
    super(code)
    this.name = 'UaToolError'
  }
}

export const uaPresets = [
  ['Chrome (Windows)', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'],
  ['Chrome (macOS)', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'],
  ['Firefox (Windows)', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0'],
  ['Safari (iPhone)', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1'],
  ['Chrome (Android)', 'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36'],
  ['curl', 'curl/8.7.1']
] as const

export function parseUserAgent(value: string): UaResult {
  const source = value.trim()
  if (!source) throw new UaToolError('empty')
  const result = new UAParser(source).getResult()
  const bot = /bot|crawler|spider|slurp|bingpreview|headless|facebookexternalhit/i.test(source)
  const deviceType = bot ? 'bot' : result.device.type || (/mobile|android|iphone|ipad/i.test(source) ? 'mobile' : 'desktop')
  return {
    browser: result.browser.name || 'Unknown',
    browserVersion: result.browser.version || 'Unknown',
    engine: result.engine.name || 'Unknown',
    engineVersion: result.engine.version || 'Unknown',
    os: result.os.name || 'Unknown',
    osVersion: result.os.version || 'Unknown',
    cpu: result.cpu.architecture || inferArchitecture(source),
    deviceType,
    deviceBrand: result.device.vendor || 'Unknown',
    deviceModel: result.device.model || 'Unknown',
    mobile: ['mobile', 'tablet'].includes(result.device.type ?? '') || /mobile|android|iphone|ipad/i.test(source),
    bot
  }
}

function inferArchitecture(source: string): string {
  if (/arm64|aarch64/i.test(source)) return 'arm64'
  if (/x86_64|win64|x64|amd64/i.test(source)) return 'amd64'
  if (/i[3-6]86|x86/i.test(source)) return 'ia32'
  return 'Unknown'
}

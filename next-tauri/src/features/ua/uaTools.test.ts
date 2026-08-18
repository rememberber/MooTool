import { describe, expect, it } from 'vitest'
import { parseUserAgent, uaPresets, UaToolError } from './uaTools'

describe('User-Agent analyzer', () => {
  it('parses desktop browser, engine, OS and architecture', () => {
    expect(parseUserAgent(uaPresets[0][1])).toMatchObject({
      browser: 'Chrome',
      engine: 'Blink',
      os: 'Windows',
      cpu: 'amd64',
      deviceType: 'desktop',
      mobile: false,
      bot: false
    })
  })

  it('detects mobile devices and bots', () => {
    expect(parseUserAgent(uaPresets[3][1])).toMatchObject({
      browser: 'Mobile Safari',
      os: 'iOS',
      deviceType: 'mobile',
      mobile: true
    })
    expect(parseUserAgent(uaPresets[5][1])).toMatchObject({
      deviceType: 'bot',
      bot: true
    })
  })

  it('identifies command-line clients with a fallback token', () => {
    expect(parseUserAgent('curl/8.7.1')).toMatchObject({
      browser: 'curl',
      browserVersion: 'Unknown',
      deviceType: 'desktop'
    })
  })

  it('rejects empty and oversized input', () => {
    expect(() => parseUserAgent('')).toThrowError(
      expect.objectContaining<Partial<UaToolError>>({ code: 'empty' })
    )
    expect(() => parseUserAgent('a'.repeat(16_385))).toThrowError(
      expect.objectContaining<Partial<UaToolError>>({ code: 'tooLong' })
    )
  })
})

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
    expect(parseUserAgent('Mozilla/5.0 Googlebot/2.1').bot).toBe(true)
  })

  it('leaves unrecognized clients as Unknown', () => {
    expect(parseUserAgent(uaPresets[5][1])).toMatchObject({
      browser: 'Unknown',
      browserVersion: 'Unknown',
      deviceType: 'desktop',
      bot: false
    })
  })

  it('rejects empty input', () => {
    expect(() => parseUserAgent('')).toThrowError(
      expect.objectContaining<Partial<UaToolError>>({ code: 'empty' })
    )
  })
})

import { describe, expect, it } from 'vitest'
import { parseUserAgent, uaPresets } from './uaTools'

describe('UA parser', () => {
  it('detects browser, OS and device from presets', () => {
    const chrome = parseUserAgent(uaPresets[0][1])
    expect(chrome.browser).toBe('Chrome')
    expect(chrome.os).toBe('Windows')
    expect(chrome.mobile).toBe(false)
    expect(parseUserAgent(uaPresets[3][1]).mobile).toBe(true)
  })

  it('detects bots', () => {
    expect(parseUserAgent('Mozilla/5.0 Googlebot/2.1').bot).toBe(true)
  })
})

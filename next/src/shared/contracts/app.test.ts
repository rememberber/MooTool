import { describe, expect, it } from 'vitest'
import { externalPageIds, isExternalPageId } from './app'

describe('external page IDs', () => {
  it('accepts every registered external page', () => {
    expect(externalPageIds.every(isExternalPageId)).toBe(true)
  })

  it('rejects arbitrary URLs and unknown identifiers', () => {
    expect(isExternalPageId('https://example.com')).toBe(false)
    expect(isExternalPageId('unknown')).toBe(false)
  })
})

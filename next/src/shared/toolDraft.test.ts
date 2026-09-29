import { describe, expect, it } from 'vitest'
import { mergeDraft } from './hooks/useToolDraft'
describe('draft hydration', () => {
  it('accepts partial migrated drafts and rejects malformed field types', () => {
    const defaults = { expression: '2+2', log: [] as string[], options: { global: true } }
    expect(mergeDraft(defaults, { expression: 42, log: ['old result'], options: { global: false } })).toEqual({ expression: '2+2', log: ['old result'], options: { global: false } })
    expect(mergeDraft(defaults, { log: [4] })).toEqual(defaults)
  })
})

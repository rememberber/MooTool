import { describe, expect, it } from 'vitest'
import { compareText } from './diffSegments'
import { describeTextDiffSession } from './textDiffSession'

describe('text diff session', () => {
  it('uses compact fingerprints instead of reporting document content', () => {
    const left = 'secret-before'
    const right = 'secret-after'
    const result = compareText(left, right, false)
    const description = describeTextDiffSession({
      left,
      right,
      ignoreWhitespace: false,
      added: result.added,
      removed: result.removed,
      changed: result.changed
    })

    expect(description.digest).not.toContain(left)
    expect(description.digest).not.toContain(right)
    expect(description.stats.changed).toBe(1)
  })
})

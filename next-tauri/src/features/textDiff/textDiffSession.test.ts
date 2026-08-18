import { describe, expect, it } from 'vitest'
import { compareText } from './textDiff'
import { describeTextDiffSession } from './textDiffSession'

describe('text diff session', () => {
  it('uses compact fingerprints instead of reporting document content', () => {
    const options = { ignoreCase: false, ignoreWhitespace: false }
    const left = 'secret-before'
    const right = 'secret-after'
    const description = describeTextDiffSession({
      left,
      right,
      options,
      context: 3,
      result: compareText(left, right, options)
    })

    expect(description.digest).not.toContain(left)
    expect(description.digest).not.toContain(right)
    expect(description.stats.changed).toBe(1)
  })
})

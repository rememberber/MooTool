import { contentFingerprint } from '../../shared/fingerprint'
import type { TextDiffOptions, TextDiffResult } from './textDiff'

export function describeTextDiffSession(input: {
  left: string
  right: string
  options: TextDiffOptions
  context: number
  result: TextDiffResult
}): { digest: string; stats: TextDiffResult['stats'] } {
  const { left, right, options, context, result } = input
  return {
    digest: JSON.stringify({
      leftLength: left.length,
      leftHash: contentFingerprint(left),
      rightLength: right.length,
      rightHash: contentFingerprint(right),
      options,
      context,
      stats: result.stats
    }),
    stats: result.stats
  }
}

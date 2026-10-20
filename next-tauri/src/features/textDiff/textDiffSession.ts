import { contentFingerprint } from '../../shared/fingerprint'

export function describeTextDiffSession(input: {
  left: string
  right: string
  ignoreWhitespace: boolean
  added: number
  removed: number
  changed: number
}): { digest: string; stats: { added: number; removed: number; changed: number } } {
  const stats = { added: input.added, removed: input.removed, changed: input.changed }
  return {
    digest: JSON.stringify({
      leftLength: input.left.length,
      leftHash: contentFingerprint(input.left),
      rightLength: input.right.length,
      rightHash: contentFingerprint(input.right),
      ignoreWhitespace: input.ignoreWhitespace,
      stats
    }),
    stats
  }
}

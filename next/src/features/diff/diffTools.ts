import { createTwoFilesPatch, diffLines, type ChangeObject } from 'diff'

export type DiffSegment = { value: string; added: boolean; removed: boolean }
export type DiffResult = { segments: DiffSegment[]; unified: string; added: number; removed: number; changed: number }

export function compareText(left: string, right: string, ignoreWhitespace: boolean): DiffResult {
  if (ignoreWhitespace && stripWhitespace(left) === stripWhitespace(right)) {
    return {
      segments: [{ value: right, added: false, removed: false }],
      unified: '',
      added: 0,
      removed: 0,
      changed: 0
    }
  }
  const options = { ignoreWhitespace }
  const changes = diffLines(left, right, options)
  const segments = changes.map((change: ChangeObject<string>) => ({ value: change.value, added: Boolean(change.added), removed: Boolean(change.removed) }))
  let added = 0
  let removed = 0
  for (const segment of segments) {
    const lines = countLines(segment.value)
    if (segment.added) added += lines
    if (segment.removed) removed += lines
  }
  const changed = Math.min(added, removed)
  return {
    segments,
    unified: createTwoFilesPatch('original', 'modified', left, right, '', '', { context: 3 }),
    added: Math.max(0, added - changed),
    removed: Math.max(0, removed - changed),
    changed
  }
}

function stripWhitespace(value: string): string {
  return value.replace(/\s/g, '')
}

function countLines(value: string): number {
  const normalized = value.endsWith('\n') ? value.slice(0, -1) : value
  return normalized ? normalized.split(/\r?\n/).length : 0
}

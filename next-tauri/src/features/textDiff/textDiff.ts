export type DiffRowKind = 'equal' | 'changed' | 'added' | 'removed'
export type InlineSegmentKind = 'equal' | 'changed'

export interface TextDiffOptions {
  ignoreCase: boolean
  ignoreWhitespace: boolean
}

export interface InlineSegment {
  kind: InlineSegmentKind
  text: string
}

export interface DiffSide {
  lineNumber: number
  text: string
  segments: InlineSegment[]
}

export interface DiffRow {
  kind: DiffRowKind
  left?: DiffSide
  right?: DiffSide
}

export interface CollapsedDiffRow {
  kind: 'collapsed'
  hiddenRows: number
  reason: 'unchanged' | 'limit'
}

export interface TextDiffResult {
  rows: DiffRow[]
  stats: {
    added: number
    removed: number
    changed: number
    unchanged: number
  }
  identical: boolean
}

interface SequenceStep<T> {
  kind: 'equal' | 'added' | 'removed'
  left?: T
  right?: T
}

export function compareText(
  leftText: string,
  rightText: string,
  options: TextDiffOptions
): TextDiffResult {
  const leftLines = splitLines(leftText)
  const rightLines = splitLines(rightText)
  const steps = diffSequence(
    leftLines,
    rightLines,
    (value) => normalize(value, options)
  )
  const rows: DiffRow[] = []
  let leftLine = 1
  let rightLine = 1

  for (let index = 0; index < steps.length;) {
    const step = steps[index]
    if (step.kind === 'equal') {
      const left = step.left ?? ''
      const right = step.right ?? ''
      rows.push({
        kind: 'equal',
        left: side(leftLine, left),
        right: side(rightLine, right)
      })
      leftLine += 1
      rightLine += 1
      index += 1
      continue
    }

    const removed: string[] = []
    const added: string[] = []
    while (index < steps.length && steps[index].kind !== 'equal') {
      const changed = steps[index]
      if (changed.kind === 'removed') removed.push(changed.left ?? '')
      if (changed.kind === 'added') added.push(changed.right ?? '')
      index += 1
    }

    for (const aligned of alignChangedBlock(removed, added, options)) {
      const { left, right } = aligned
      if (left !== undefined && right !== undefined) {
        const inline = compareInline(left, right, options)
        rows.push({
          kind: 'changed',
          left: side(leftLine, left, inline.left),
          right: side(rightLine, right, inline.right)
        })
        leftLine += 1
        rightLine += 1
      } else if (left !== undefined) {
        rows.push({ kind: 'removed', left: side(leftLine, left, changedSegment(left)) })
        leftLine += 1
      } else if (right !== undefined) {
        rows.push({ kind: 'added', right: side(rightLine, right, changedSegment(right)) })
        rightLine += 1
      }
    }
  }

  const stats = {
    added: rows.filter((row) => row.kind === 'added').length,
    removed: rows.filter((row) => row.kind === 'removed').length,
    changed: rows.filter((row) => row.kind === 'changed').length,
    unchanged: rows.filter((row) => row.kind === 'equal').length
  }
  return {
    rows,
    stats,
    identical: stats.added === 0 && stats.removed === 0 && stats.changed === 0
  }
}

export function collapseUnchangedRows(
  rows: DiffRow[],
  context: number
): Array<DiffRow | CollapsedDiffRow> {
  if (context < 0 || rows.every((row) => row.kind === 'equal')) return rows
  const visible = rows.map((row) => row.kind !== 'equal')
  for (let index = 0; index < rows.length; index += 1) {
    if (rows[index].kind === 'equal') continue
    for (
      let neighbor = Math.max(0, index - context);
      neighbor <= Math.min(rows.length - 1, index + context);
      neighbor += 1
    ) {
      visible[neighbor] = true
    }
  }

  const result: Array<DiffRow | CollapsedDiffRow> = []
  for (let index = 0; index < rows.length;) {
    if (visible[index]) {
      result.push(rows[index])
      index += 1
      continue
    }
    const start = index
    while (index < rows.length && !visible[index]) index += 1
    result.push({ kind: 'collapsed', hiddenRows: index - start, reason: 'unchanged' })
  }
  return result
}

export function limitRenderedRows(
  rows: Array<DiffRow | CollapsedDiffRow>,
  maximum = 4000
): Array<DiffRow | CollapsedDiffRow> {
  if (rows.length <= maximum) return rows
  const head = Math.floor(maximum / 2)
  const tail = maximum - head
  return [
    ...rows.slice(0, head),
    { kind: 'collapsed', hiddenRows: rows.length - maximum, reason: 'limit' },
    ...rows.slice(-tail)
  ]
}

export function createUnifiedDiff(
  leftName: string,
  rightName: string,
  result: TextDiffResult
): string {
  const lines = [`--- ${leftName}`, `+++ ${rightName}`]
  for (const row of result.rows) {
    if (row.kind === 'equal') lines.push(` ${row.left?.text ?? ''}`)
    if (row.kind === 'removed') lines.push(`-${row.left?.text ?? ''}`)
    if (row.kind === 'added') lines.push(`+${row.right?.text ?? ''}`)
    if (row.kind === 'changed') {
      lines.push(`-${row.left?.text ?? ''}`)
      lines.push(`+${row.right?.text ?? ''}`)
    }
  }
  return lines.join('\n')
}

function compareInline(
  left: string,
  right: string,
  options: TextDiffOptions
): { left: InlineSegment[]; right: InlineSegment[] } {
  const steps = diffSequence(
    tokenizeInline(left),
    tokenizeInline(right),
    (value) => normalize(value, options)
  )
  const leftSegments: InlineSegment[] = []
  const rightSegments: InlineSegment[] = []
  for (const step of steps) {
    if (step.kind === 'equal') {
      appendSegment(leftSegments, 'equal', step.left ?? '')
      appendSegment(rightSegments, 'equal', step.right ?? '')
    } else if (step.kind === 'removed') {
      appendSegment(leftSegments, 'changed', step.left ?? '')
    } else {
      appendSegment(rightSegments, 'changed', step.right ?? '')
    }
  }
  return { left: leftSegments, right: rightSegments }
}

function alignChangedBlock(
  removed: string[],
  added: string[],
  options: TextDiffOptions
): Array<{ left?: string; right?: string }> {
  if (removed.length * added.length > 250_000) {
    return [
      ...removed.map((left) => ({ left })),
      ...added.map((right) => ({ right }))
    ]
  }
  const width = added.length + 1
  const costs = new Float64Array((removed.length + 1) * width)
  const action: Array<'pair' | 'remove' | 'add' | undefined> = new Array(costs.length)
  for (let leftIndex = 1; leftIndex <= removed.length; leftIndex += 1) {
    costs[leftIndex * width] = leftIndex
    action[leftIndex * width] = 'remove'
  }
  for (let rightIndex = 1; rightIndex <= added.length; rightIndex += 1) {
    costs[rightIndex] = rightIndex
    action[rightIndex] = 'add'
  }

  for (let leftIndex = 1; leftIndex <= removed.length; leftIndex += 1) {
    for (let rightIndex = 1; rightIndex <= added.length; rightIndex += 1) {
      const index = leftIndex * width + rightIndex
      const removeCost = costs[(leftIndex - 1) * width + rightIndex] + 1
      const addCost = costs[leftIndex * width + rightIndex - 1] + 1
      const similarity = lineSimilarity(
        removed[leftIndex - 1],
        added[rightIndex - 1],
        options
      )
      const pairCost = costs[(leftIndex - 1) * width + rightIndex - 1]
        + (similarity >= 0.34 ? 1.15 - similarity * 0.35 : 2.1)

      if (pairCost <= removeCost && pairCost <= addCost) {
        costs[index] = pairCost
        action[index] = 'pair'
      } else if (addCost < removeCost) {
        costs[index] = addCost
        action[index] = 'add'
      } else {
        costs[index] = removeCost
        action[index] = 'remove'
      }
    }
  }

  const aligned: Array<{ left?: string; right?: string }> = []
  let leftIndex = removed.length
  let rightIndex = added.length
  while (leftIndex > 0 || rightIndex > 0) {
    const step = action[leftIndex * width + rightIndex]
    if (step === 'pair') {
      aligned.push({ left: removed[leftIndex - 1], right: added[rightIndex - 1] })
      leftIndex -= 1
      rightIndex -= 1
    } else if (step === 'add') {
      aligned.push({ right: added[rightIndex - 1] })
      rightIndex -= 1
    } else {
      aligned.push({ left: removed[leftIndex - 1] })
      leftIndex -= 1
    }
  }
  return normalizeUnpairedOrder(aligned.reverse())
}

function normalizeUnpairedOrder(
  aligned: Array<{ left?: string; right?: string }>
): Array<{ left?: string; right?: string }> {
  const result: Array<{ left?: string; right?: string }> = []
  let removed: string[] = []
  let added: string[] = []
  const flush = () => {
    result.push(
      ...removed.map((left) => ({ left })),
      ...added.map((right) => ({ right }))
    )
    removed = []
    added = []
  }
  for (const row of aligned) {
    if (row.left !== undefined && row.right !== undefined) {
      flush()
      result.push(row)
    } else if (row.left !== undefined) {
      removed.push(row.left)
    } else if (row.right !== undefined) {
      added.push(row.right)
    }
  }
  flush()
  return result
}

function lineSimilarity(
  left: string,
  right: string,
  options: TextDiffOptions
): number {
  const normalizedLeft = normalize(left, options)
  const normalizedRight = normalize(right, options)
  if (normalizedLeft === normalizedRight) return 1
  const leftTokens = new Set(tokenizeInline(normalizedLeft).filter((token) => token.trim()))
  const rightTokens = new Set(tokenizeInline(normalizedRight).filter((token) => token.trim()))
  if (leftTokens.size === 0 || rightTokens.size === 0) return 0
  let intersection = 0
  for (const token of leftTokens) {
    if (rightTokens.has(token)) intersection += 1
  }
  return (2 * intersection) / (leftTokens.size + rightTokens.size)
}

function diffSequence(
  left: string[],
  right: string[],
  key: (value: string) => string
): SequenceStep<string>[] {
  if (left.length + right.length > 4000) {
    return patienceDiff(left, right, key)
  }
  return myersDiff(left, right, key)
}

function myersDiff(
  left: string[],
  right: string[],
  key: (value: string) => string
): SequenceStep<string>[] {
  const maximum = left.length + right.length
  const frontier = new Map<number, number>([[1, 0]])
  const trace: Array<Map<number, number>> = []

  for (let depth = 0; depth <= maximum; depth += 1) {
    trace.push(new Map(frontier))
    for (let diagonal = -depth; diagonal <= depth; diagonal += 2) {
      const moveDown = diagonal === -depth || (
        diagonal !== depth
        && (frontier.get(diagonal - 1) ?? -1) < (frontier.get(diagonal + 1) ?? -1)
      )
      let x = moveDown
        ? (frontier.get(diagonal + 1) ?? 0)
        : (frontier.get(diagonal - 1) ?? 0) + 1
      let y = x - diagonal

      while (x < left.length && y < right.length && key(left[x]) === key(right[y])) {
        x += 1
        y += 1
      }
      frontier.set(diagonal, x)
      if (x >= left.length && y >= right.length) {
        return backtrack(trace, left, right, depth)
      }
    }
  }
  return []
}

function backtrack(
  trace: Array<Map<number, number>>,
  left: string[],
  right: string[],
  finalDepth: number
): SequenceStep<string>[] {
  let x = left.length
  let y = right.length
  const steps: SequenceStep<string>[] = []

  for (let depth = finalDepth; depth >= 0; depth -= 1) {
    const frontier = trace[depth]
    const diagonal = x - y
    const previousDiagonal = diagonal === -depth || (
      diagonal !== depth
      && (frontier.get(diagonal - 1) ?? -1) < (frontier.get(diagonal + 1) ?? -1)
    )
      ? diagonal + 1
      : diagonal - 1
    const previousX = frontier.get(previousDiagonal) ?? 0
    const previousY = previousX - previousDiagonal

    while (x > previousX && y > previousY) {
      steps.push({ kind: 'equal', left: left[x - 1], right: right[y - 1] })
      x -= 1
      y -= 1
    }
    if (depth === 0) break
    if (x === previousX) {
      steps.push({ kind: 'added', right: right[y - 1] })
      y -= 1
    } else {
      steps.push({ kind: 'removed', left: left[x - 1] })
      x -= 1
    }
  }
  return steps.reverse()
}

function patienceDiff(
  left: string[],
  right: string[],
  key: (value: string) => string
): SequenceStep<string>[] {
  let prefix = 0
  while (
    prefix < left.length
    && prefix < right.length
    && key(left[prefix]) === key(right[prefix])
  ) {
    prefix += 1
  }
  let suffix = 0
  while (
    suffix < left.length - prefix
    && suffix < right.length - prefix
    && key(left[left.length - 1 - suffix]) === key(right[right.length - 1 - suffix])
  ) {
    suffix += 1
  }

  const result: SequenceStep<string>[] = []
  for (let index = 0; index < prefix; index += 1) {
    result.push({ kind: 'equal', left: left[index], right: right[index] })
  }
  const leftMiddle = left.slice(prefix, left.length - suffix)
  const rightMiddle = right.slice(prefix, right.length - suffix)

  if (leftMiddle.length + rightMiddle.length <= 4000) {
    result.push(...myersDiff(leftMiddle, rightMiddle, key))
  } else {
    const anchors = patienceAnchors(leftMiddle, rightMiddle, key)
    if (anchors.length === 0) {
      result.push(
        ...leftMiddle.map((value) => ({ kind: 'removed' as const, left: value })),
        ...rightMiddle.map((value) => ({ kind: 'added' as const, right: value }))
      )
    } else {
      let previousLeft = 0
      let previousRight = 0
      for (const anchor of anchors) {
        result.push(...diffSequence(
          leftMiddle.slice(previousLeft, anchor.left),
          rightMiddle.slice(previousRight, anchor.right),
          key
        ))
        result.push({
          kind: 'equal',
          left: leftMiddle[anchor.left],
          right: rightMiddle[anchor.right]
        })
        previousLeft = anchor.left + 1
        previousRight = anchor.right + 1
      }
      result.push(...diffSequence(
        leftMiddle.slice(previousLeft),
        rightMiddle.slice(previousRight),
        key
      ))
    }
  }

  for (let index = suffix; index > 0; index -= 1) {
    result.push({
      kind: 'equal',
      left: left[left.length - index],
      right: right[right.length - index]
    })
  }
  return result
}

function patienceAnchors(
  left: string[],
  right: string[],
  key: (value: string) => string
): Array<{ left: number; right: number }> {
  const leftOccurrences = countOccurrences(left, key)
  const rightOccurrences = countOccurrences(right, key)
  const candidates: Array<{ left: number; right: number }> = []
  for (const [value, occurrence] of leftOccurrences) {
    const rightOccurrence = rightOccurrences.get(value)
    if (occurrence.count === 1 && rightOccurrence?.count === 1) {
      candidates.push({ left: occurrence.index, right: rightOccurrence.index })
    }
  }
  candidates.sort((first, second) => first.left - second.left)
  return longestIncreasingRightIndices(candidates)
}

function countOccurrences(
  values: string[],
  key: (value: string) => string
): Map<string, { count: number; index: number }> {
  const occurrences = new Map<string, { count: number; index: number }>()
  for (let index = 0; index < values.length; index += 1) {
    const normalized = key(values[index])
    const current = occurrences.get(normalized)
    occurrences.set(normalized, {
      count: (current?.count ?? 0) + 1,
      index
    })
  }
  return occurrences
}

function longestIncreasingRightIndices(
  candidates: Array<{ left: number; right: number }>
): Array<{ left: number; right: number }> {
  const tails: number[] = []
  const previous = new Int32Array(candidates.length)
  previous.fill(-1)

  for (let index = 0; index < candidates.length; index += 1) {
    let low = 0
    let high = tails.length
    while (low < high) {
      const middle = Math.floor((low + high) / 2)
      if (candidates[tails[middle]].right < candidates[index].right) low = middle + 1
      else high = middle
    }
    if (low > 0) previous[index] = tails[low - 1]
    tails[low] = index
  }

  const result: Array<{ left: number; right: number }> = []
  let current = tails.at(-1) ?? -1
  while (current >= 0) {
    result.push(candidates[current])
    current = previous[current]
  }
  return result.reverse()
}

function splitLines(text: string): string[] {
  if (text === '') return []
  return text.replace(/\r\n?/g, '\n').split('\n')
}

function tokenizeInline(text: string): string[] {
  return text.match(/\s+|[\p{L}\p{N}_]+|[^\s\p{L}\p{N}_]/gu) ?? []
}

function normalize(value: string, options: TextDiffOptions): string {
  let normalized = options.ignoreWhitespace ? value.replace(/\s+/g, ' ').trim() : value
  if (options.ignoreCase) normalized = normalized.toLocaleLowerCase()
  return normalized
}

function side(lineNumber: number, text: string, segments?: InlineSegment[]): DiffSide {
  return {
    lineNumber,
    text,
    segments: segments ?? [{ kind: 'equal', text }]
  }
}

function changedSegment(text: string): InlineSegment[] {
  return [{ kind: 'changed', text }]
}

function appendSegment(
  segments: InlineSegment[],
  kind: InlineSegmentKind,
  text: string
): void {
  if (!text) return
  const last = segments.at(-1)
  if (last?.kind === kind) last.text += text
  else segments.push({ kind, text })
}

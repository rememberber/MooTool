import { diffArrays } from 'diff'

export type DiffSegmentType = 'insert' | 'delete' | 'change'

export type DiffSegment = {
  type: DiffSegmentType
  leftStart: number
  leftEnd: number
  rightStart: number
  rightEnd: number
  wholeLine: boolean
}

export type UnifiedSpanType =
  | 'add-line'
  | 'delete-line'
  | 'hunk-line'
  | 'header-line'
  | 'add-character'
  | 'delete-character'
  | 'change-character'

export type UnifiedSpan = {
  start: number
  end: number
  type: UnifiedSpanType
}

export type UnifiedDiffView = {
  text: string
  lineSpans: UnifiedSpan[]
  characterSpans: UnifiedSpan[]
  characterEventCount: number
}

export type DiffResult = {
  leftText: string
  rightText: string
  segments: DiffSegment[]
  unified: string
  unifiedView: UnifiedDiffView
  added: number
  removed: number
  changed: number
}

type PatchDeltaType = 'insert' | 'delete' | 'change'

type PatchDelta<T> = {
  type: PatchDeltaType
  sourcePosition: number
  targetPosition: number
  source: T[]
  target: T[]
}

/**
 * Mirrors the Java DiffService contract used by the Swing text comparison tool:
 * line-level Myers diff, paired character-level changes, three-way UI ranges,
 * and a three-line-context unified patch.
 */
export function compareText(left: string, right: string, ignoreWhitespace: boolean): DiffResult {
  const leftLines = splitLines(left)
  const rightLines = splitLines(right)
  const patch = buildPatch(leftLines, rightLines)
  const segments = buildUiSegments(left, right, patch, ignoreWhitespace)
  const unifiedView = buildUnifiedView(patch, leftLines, ignoreWhitespace)

  let added = 0
  let removed = 0
  let changed = 0
  for (const delta of patch) {
    if (delta.type === 'insert') added += delta.target.length
    if (delta.type === 'delete') removed += delta.source.length
    if (delta.type === 'change') {
      const paired = Math.min(delta.source.length, delta.target.length)
      changed += paired
      removed += Math.max(0, delta.source.length - paired)
      added += Math.max(0, delta.target.length - paired)
    }
  }

  return {
    leftText: left,
    rightText: right,
    segments,
    unified: unifiedView.text,
    unifiedView,
    added,
    removed,
    changed
  }
}

function splitLines(text: string): string[] {
  // Java's String.split("\\R", -1) recognizes all Unicode line terminators and
  // retains the final empty line.
  return text.split(/\r\n|[\n\v\f\r\u0085\u2028\u2029]/u)
}

function buildPatch<T>(source: T[], target: T[]): PatchDelta<T>[] {
  const changes = diffArrays(source, target)
  const patch: PatchDelta<T>[] = []
  let sourcePosition = 0
  let targetPosition = 0
  let index = 0

  while (index < changes.length) {
    const change = changes[index]
    if (!change.added && !change.removed) {
      sourcePosition += change.value.length
      targetPosition += change.value.length
      index += 1
      continue
    }

    const deltaSourcePosition = sourcePosition
    const deltaTargetPosition = targetPosition
    const removed: T[] = []
    const added: T[] = []
    while (index < changes.length && (changes[index].added || changes[index].removed)) {
      const edit = changes[index]
      if (edit.removed) {
        removed.push(...edit.value)
        sourcePosition += edit.value.length
      } else if (edit.added) {
        added.push(...edit.value)
        targetPosition += edit.value.length
      }
      index += 1
    }

    patch.push({
      type: removed.length > 0 && added.length > 0 ? 'change' : removed.length > 0 ? 'delete' : 'insert',
      sourcePosition: deltaSourcePosition,
      targetPosition: deltaTargetPosition,
      source: removed,
      target: added
    })
  }

  return patch
}

function buildUiSegments(
  left: string,
  right: string,
  patch: PatchDelta<string>[],
  ignoreWhitespace: boolean
): DiffSegment[] {
  const leftLineStarts = computeLineStartOffsets(left)
  const rightLineStarts = computeLineStartOffsets(right)
  const segments: DiffSegment[] = []

  for (const delta of patch) {
    if (delta.type === 'delete') {
      delta.source.forEach((line, index) => {
        if (ignoreWhitespace && isAllWhitespace(line)) return
        const start = safeLineStart(leftLineStarts, delta.sourcePosition + index)
        segments.push(uiSegment('delete', start, start + line.length, -1, -1, true))
      })
      continue
    }

    if (delta.type === 'insert') {
      delta.target.forEach((line, index) => {
        if (ignoreWhitespace && isAllWhitespace(line)) return
        const start = safeLineStart(rightLineStarts, delta.targetPosition + index)
        segments.push(uiSegment('insert', -1, -1, start, start + line.length, true))
      })
      continue
    }

    const pairedLineCount = Math.min(delta.source.length, delta.target.length)
    for (let index = 0; index < pairedLineCount; index += 1) {
      const leftLine = delta.source[index]
      const rightLine = delta.target[index]
      const leftLineStart = safeLineStart(leftLineStarts, delta.sourcePosition + index)
      const rightLineStart = safeLineStart(rightLineStarts, delta.targetPosition + index)
      const characterPatch = buildPatch(leftLine.split(''), rightLine.split(''))

      for (const characterDelta of characterPatch) {
        const leftStart = leftLineStart + characterDelta.sourcePosition
        const leftEnd = leftStart + characterDelta.source.length
        const rightStart = rightLineStart + characterDelta.targetPosition
        const rightEnd = rightStart + characterDelta.target.length

        if (characterDelta.type === 'delete') {
          const deleted = safeSlice(leftLine, characterDelta.sourcePosition, characterDelta.source.length)
          if (!ignoreWhitespace || !isAllWhitespace(deleted)) {
            segments.push(uiSegment('delete', leftStart, leftEnd, -1, -1, false))
          }
        } else if (characterDelta.type === 'insert') {
          const inserted = safeSlice(rightLine, characterDelta.targetPosition, characterDelta.target.length)
          if (!ignoreWhitespace || !isAllWhitespace(inserted)) {
            segments.push(uiSegment('insert', -1, -1, rightStart, rightEnd, false))
          }
        } else {
          const before = safeSlice(leftLine, characterDelta.sourcePosition, characterDelta.source.length)
          const after = safeSlice(rightLine, characterDelta.targetPosition, characterDelta.target.length)
          if (!ignoreWhitespace || !equalsIgnoringWhitespace(before, after)) {
            segments.push(uiSegment('change', leftStart, leftEnd, rightStart, rightEnd, false))
          }
        }
      }
    }

    for (let index = pairedLineCount; index < delta.source.length; index += 1) {
      const line = delta.source[index]
      if (ignoreWhitespace && isAllWhitespace(line)) continue
      const start = safeLineStart(leftLineStarts, delta.sourcePosition + index)
      segments.push(uiSegment('delete', start, start + line.length, -1, -1, true))
    }
    for (let index = pairedLineCount; index < delta.target.length; index += 1) {
      const line = delta.target[index]
      if (ignoreWhitespace && isAllWhitespace(line)) continue
      const start = safeLineStart(rightLineStarts, delta.targetPosition + index)
      segments.push(uiSegment('insert', -1, -1, start, start + line.length, true))
    }
  }

  return segments
}

function uiSegment(
  type: DiffSegmentType,
  leftStart: number,
  leftEnd: number,
  rightStart: number,
  rightEnd: number,
  wholeLine: boolean
): DiffSegment {
  return { type, leftStart, leftEnd, rightStart, rightEnd, wholeLine }
}

function buildUnifiedView(
  patch: PatchDelta<string>[],
  originalLines: string[],
  ignoreWhitespace: boolean
): UnifiedDiffView {
  const lines = generateUnifiedDiffLines(originalLines, patch, 3)
  const text = lines.join('\n')
  const lineSpans: UnifiedSpan[] = []
  const characterSpans: UnifiedSpan[] = []
  let characterEventCount = 0
  let offset = 0
  let deletedStarts: number[] = []
  let deletedTexts: string[] = []
  let addedStarts: number[] = []
  let addedTexts: string[] = []

  const flushCharacterSpans = () => {
    characterEventCount += addIntralineSpans(
      deletedStarts,
      deletedTexts,
      addedStarts,
      addedTexts,
      characterSpans,
      ignoreWhitespace
    )
    deletedStarts = []
    deletedTexts = []
    addedStarts = []
    addedTexts = []
  }

  lines.forEach((line, index) => {
    const lineStart = offset
    const lineEnd = lineStart + line.length
    if (line.startsWith('@@')) {
      flushCharacterSpans()
      lineSpans.push({ start: lineStart, end: lineEnd, type: 'hunk-line' })
    } else if (line.startsWith('---') || line.startsWith('+++')) {
      lineSpans.push({ start: lineStart, end: lineEnd, type: 'header-line' })
    } else if (line.startsWith('+')) {
      const content = line.slice(1)
      if (!ignoreWhitespace || !isAllWhitespace(content)) {
        lineSpans.push({ start: lineStart, end: lineEnd, type: 'add-line' })
      }
      addedStarts.push(lineStart)
      addedTexts.push(content)
    } else if (line.startsWith('-')) {
      const content = line.slice(1)
      if (!ignoreWhitespace || !isAllWhitespace(content)) {
        lineSpans.push({ start: lineStart, end: lineEnd, type: 'delete-line' })
      }
      deletedStarts.push(lineStart)
      deletedTexts.push(content)
    }
    offset = index < lines.length - 1 ? lineEnd + 1 : lineEnd
  })
  flushCharacterSpans()

  return { text, lineSpans, characterSpans, characterEventCount }
}

function generateUnifiedDiffLines(
  originalLines: string[],
  patch: PatchDelta<string>[],
  contextSize: number
): string[] {
  if (patch.length === 0) return []
  const lines = ['--- old', '+++ new']
  let currentGroup: PatchDelta<string>[] = [patch[0]]
  let previous = patch[0]

  for (let index = 1; index < patch.length; index += 1) {
    const next = patch[index]
    const closeEnough = previous.sourcePosition + previous.source.length + contextSize >= next.sourcePosition - contextSize
    if (closeEnough) {
      currentGroup.push(next)
    } else {
      lines.push(...processUnifiedGroup(originalLines, currentGroup, contextSize))
      currentGroup = [next]
    }
    previous = next
  }
  lines.push(...processUnifiedGroup(originalLines, currentGroup, contextSize))
  return lines
}

function processUnifiedGroup(
  originalLines: string[],
  deltas: PatchDelta<string>[],
  contextSize: number
): string[] {
  const output: string[] = []
  let originalTotal = 0
  let revisedTotal = 0
  let current = deltas[0]
  const originalStart = Math.max(1, current.sourcePosition + 1 - contextSize)
  const revisedStart = Math.max(1, current.targetPosition + 1 - contextSize)
  const contextStart = Math.max(0, current.sourcePosition - contextSize)

  for (let line = contextStart; line < current.sourcePosition; line += 1) {
    output.push(` ${originalLines[line]}`)
    originalTotal += 1
    revisedTotal += 1
  }

  output.push(...deltaText(current))
  originalTotal += current.source.length
  revisedTotal += current.target.length

  for (let index = 1; index < deltas.length; index += 1) {
    const next = deltas[index]
    const intermediateStart = current.sourcePosition + current.source.length
    for (let line = intermediateStart; line < next.sourcePosition; line += 1) {
      output.push(` ${originalLines[line]}`)
      originalTotal += 1
      revisedTotal += 1
    }
    output.push(...deltaText(next))
    originalTotal += next.source.length
    revisedTotal += next.target.length
    current = next
  }

  const trailingStart = current.sourcePosition + current.source.length
  const trailingEnd = Math.min(originalLines.length, trailingStart + contextSize)
  for (let line = trailingStart; line < trailingEnd; line += 1) {
    output.push(` ${originalLines[line]}`)
    originalTotal += 1
    revisedTotal += 1
  }

  output.unshift(`@@ -${originalStart},${originalTotal} +${revisedStart},${revisedTotal} @@`)
  return output
}

function deltaText(delta: PatchDelta<string>): string[] {
  return [
    ...delta.source.map((line) => `-${line}`),
    ...delta.target.map((line) => `+${line}`)
  ]
}

function addIntralineSpans(
  deletedStarts: number[],
  deletedTexts: string[],
  addedStarts: number[],
  addedTexts: string[],
  output: UnifiedSpan[],
  ignoreWhitespace: boolean
): number {
  let events = 0
  const pairs = Math.min(deletedTexts.length, addedTexts.length)
  for (let index = 0; index < pairs; index += 1) {
    const before = deletedTexts[index]
    const after = addedTexts[index]
    const beforeBase = deletedStarts[index] + 1
    const afterBase = addedStarts[index] + 1
    for (const delta of buildPatch(before.split(''), after.split(''))) {
      const beforeText = safeSlice(before, delta.sourcePosition, delta.source.length)
      const afterText = safeSlice(after, delta.targetPosition, delta.target.length)
      if (delta.type === 'delete') {
        if (ignoreWhitespace && isAllWhitespace(beforeText)) continue
        output.push({
          start: beforeBase + delta.sourcePosition,
          end: beforeBase + delta.sourcePosition + delta.source.length,
          type: 'delete-character'
        })
      } else if (delta.type === 'insert') {
        if (ignoreWhitespace && isAllWhitespace(afterText)) continue
        output.push({
          start: afterBase + delta.targetPosition,
          end: afterBase + delta.targetPosition + delta.target.length,
          type: 'add-character'
        })
      } else {
        if (ignoreWhitespace && equalsIgnoringWhitespace(beforeText, afterText)) continue
        if (beforeText) {
          output.push({
            start: beforeBase + delta.sourcePosition,
            end: beforeBase + delta.sourcePosition + delta.source.length,
            type: 'change-character'
          })
        }
        if (afterText) {
          output.push({
            start: afterBase + delta.targetPosition,
            end: afterBase + delta.targetPosition + delta.target.length,
            type: 'change-character'
          })
        }
      }
      events += 1
    }
  }
  return events
}

function computeLineStartOffsets(text: string): number[] {
  const starts = [0]
  for (let index = 0; index < text.length; index += 1) {
    if (text.charCodeAt(index) === 10) starts.push(index + 1)
  }
  return starts
}

function safeLineStart(starts: number[], lineIndex: number): number {
  if (lineIndex < 0) return 0
  if (lineIndex >= starts.length) return starts.at(-1) ?? 0
  return starts[lineIndex]
}

function safeSlice(value: string, position: number, length: number): string {
  const start = Math.max(0, Math.min(position, value.length))
  const end = Math.max(start, Math.min(position + length, value.length))
  return value.slice(start, end)
}

function isAllWhitespace(value: string): boolean {
  for (let index = 0; index < value.length; index += 1) {
    if (!isJavaWhitespace(value.charCodeAt(index))) return false
  }
  return true
}

function equalsIgnoringWhitespace(left: string, right: string): boolean {
  let leftIndex = 0
  let rightIndex = 0
  while (leftIndex < left.length && rightIndex < right.length) {
    const leftCode = left.charCodeAt(leftIndex)
    const rightCode = right.charCodeAt(rightIndex)
    if (isJavaWhitespace(leftCode)) {
      leftIndex += 1
      continue
    }
    if (isJavaWhitespace(rightCode)) {
      rightIndex += 1
      continue
    }
    if (leftCode !== rightCode) return false
    leftIndex += 1
    rightIndex += 1
  }
  while (leftIndex < left.length) {
    if (!isJavaWhitespace(left.charCodeAt(leftIndex))) return false
    leftIndex += 1
  }
  while (rightIndex < right.length) {
    if (!isJavaWhitespace(right.charCodeAt(rightIndex))) return false
    rightIndex += 1
  }
  return true
}

function isJavaWhitespace(code: number): boolean {
  return (code >= 0x0009 && code <= 0x000d)
    || (code >= 0x001c && code <= 0x001f)
    || code === 0x0020
    || code === 0x1680
    || (code >= 0x2000 && code <= 0x2006)
    || (code >= 0x2008 && code <= 0x200a)
    || code === 0x2028
    || code === 0x2029
    || code === 0x205f
    || code === 0x3000
}

import type { Extension } from '@codemirror/state'
import type { QuickNote } from '../../platform/contracts/localData'
import { compareText, type DiffRow, type InlineSegment } from '../textDiff/textDiff'
import { quickNoteLanguage } from './quickNoteLanguage'
import type { DiffEditorDecoration } from '../../shared/DiffCodeEditor'

const syntaxByExtension: Record<string, QuickNote['syntax']> = {
  md: 'markdown',
  markdown: 'markdown',
  json: 'json',
  java: 'java',
  js: 'javascript',
  mjs: 'javascript',
  cjs: 'javascript',
  ts: 'typescript',
  tsx: 'typescript',
  py: 'python',
  html: 'html',
  htm: 'html',
  xml: 'xml',
  yaml: 'yaml',
  yml: 'yaml',
  sql: 'sql',
  txt: 'plain'
}

export function gitDiffLanguage(path: string): Extension[] {
  const extension = path.split(/[/\\]/).pop()?.split('.').pop()?.toLowerCase() ?? ''
  return quickNoteLanguage(syntaxByExtension[extension] ?? 'plain')
}

export function gitDiffDecorations(before: string, after: string): { left: DiffEditorDecoration[]; right: DiffEditorDecoration[] } {
  const rows = compareText(before, after, { ignoreCase: false, ignoreWhitespace: false }).rows
  const leftLines = lineSpans(before)
  const rightLines = lineSpans(after)
  const left: DiffEditorDecoration[] = []
  const right: DiffEditorDecoration[] = []
  let leftIndex = 0
  let rightIndex = 0
  for (const row of rows) {
    if (row.left) {
      const span = leftLines[leftIndex]
      leftIndex += 1
      if (span && row.kind !== 'equal') decorate(left, span, row, row.left.segments, 'left')
    }
    if (row.right) {
      const span = rightLines[rightIndex]
      rightIndex += 1
      if (span && row.kind !== 'equal') decorate(right, span, row, row.right.segments, 'right')
    }
  }
  return { left, right }
}

function decorate(
  target: DiffEditorDecoration[],
  span: LineSpan,
  row: DiffRow,
  segments: InlineSegment[],
  side: 'left' | 'right'
): void {
  const name = row.kind === 'added' ? 'added' : row.kind === 'removed' ? 'removed' : 'changed'
  if ((side === 'left' && row.kind === 'added') || (side === 'right' && row.kind === 'removed')) return
  target.push({ type: 'line', from: span.start, className: `cm-diff-line-${name}` })
  let cursor = span.start
  for (const segment of segments) {
    const next = cursor + segment.text.length
    if (segment.kind === 'changed' && next > cursor) {
      target.push({
        type: 'mark',
        from: cursor,
        to: Math.min(next, span.end),
        className: `cm-diff-character-${name}`
      })
    }
    cursor = next
  }
}

interface LineSpan {
  start: number
  end: number
}

function lineSpans(text: string): LineSpan[] {
  if (text === '') return []
  const spans: LineSpan[] = []
  let start = 0
  for (let index = 0; index < text.length; index += 1) {
    const carriageReturn = text[index] === '\r'
    if (text[index] !== '\n' && !carriageReturn) continue
    spans.push({ start, end: index })
    if (carriageReturn && text[index + 1] === '\n') index += 1
    start = index + 1
  }
  spans.push({ start, end: text.length })
  return spans
}

import { applyEdits, format, parse, type ParseError } from 'jsonc-parser'
import type { TextCodeEditorLanguage } from '@/shared/components/codeEditorLanguage'

export type PreviewKind = 'text' | 'json' | 'image' | 'pdf' | 'unsupported'
export type TextEncoding = 'auto' | 'utf-8' | 'utf-16le' | 'utf-16be' | 'gb18030'
export const previewBatchLimit = 100
export const previewLimits = { text: 2 * 1024 ** 2, json: 2 * 1024 ** 2, image: 32 * 1024 ** 2, pdf: 50 * 1024 ** 2, unsupported: 0 }
const languages: Record<string, TextCodeEditorLanguage> = {
  json: 'json', jsonc: 'json', md: 'markdown', markdown: 'markdown', java: 'java',
  js: 'javascript', jsx: 'javascript', mjs: 'javascript', cjs: 'javascript', ts: 'typescript', tsx: 'typescript',
  py: 'python', xml: 'xml', html: 'html', htm: 'html', yml: 'yaml', yaml: 'yaml', sql: 'sql'
}
const textExtensions = new Set(['txt', 'log', 'csv', 'tsv', 'ini', 'conf', 'config', 'properties', 'toml', 'sh', 'bat', 'ps1', 'c', 'h', 'cpp', 'hpp', 'cs', 'go', 'rs', 'rb', 'php', 'css', 'scss', 'svg', 'vue', 'svelte', 'proto', 'gradle', ...Object.keys(languages)])
export function previewExtension(name: string): string { return name.toLowerCase().split('.').at(-1) ?? '' }
export function classifyPreview(name: string, mime = ''): PreviewKind {
  const extension = previewExtension(name)
  if (extension === 'pdf') return 'pdf'
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'ico', 'avif'].includes(extension)) return 'image'
  if (extension === 'json' || extension === 'jsonc') return 'json'
  if (textExtensions.has(extension) || mime.startsWith('text/') || ['Dockerfile', 'Makefile', 'LICENSE', 'README'].some(item => item.toLowerCase() === name.toLowerCase()) || name.startsWith('.')) return 'text'
  return 'unsupported'
}
export function previewLanguage(name: string): TextCodeEditorLanguage { return languages[previewExtension(name)] ?? 'text' }

export class PreviewError extends Error {
  constructor(public readonly code: 'binary' | 'encoding' | 'tooLarge' | 'unreadable') { super(code) }
}
export function decodePreviewText(bytes: Uint8Array, encoding: TextEncoding = 'auto'): { text: string; encoding: string } {
  const selected = encoding === 'auto' ? bytes[0] === 0xff && bytes[1] === 0xfe ? 'utf-16le' : bytes[0] === 0xfe && bytes[1] === 0xff ? 'utf-16be' : 'utf-8' : encoding
  let text: string
  try { text = new TextDecoder(selected, { fatal: true }).decode(bytes) } catch { throw new PreviewError('encoding') }
  // Detect binary content after decoding so UTF-16 byte zeroes remain valid.
  if (/[\u0000-\u0008\u000e-\u001a\u001c-\u001f]/.test(text)) throw new PreviewError('binary')
  return { text, encoding: selected }
}
export function jsonPreview(text: string, name: string): { valid: boolean; formatted: string } {
  const errors: ParseError[] = []
  parse(text, errors, { allowTrailingComma: previewExtension(name) === 'jsonc', disallowComments: previewExtension(name) !== 'jsonc', allowEmptyContent: false })
  if (errors.length) return { valid: false, formatted: text }
  // Token-based formatting preserves large integers, duplicate keys and JSONC comments.
  return { valid: true, formatted: applyEdits(text, format(text, undefined, { tabSize: 2, insertSpaces: true, eol: '\n' })) }
}
export async function readPreviewBytes(file: File, kind: PreviewKind): Promise<Uint8Array> {
  if (file.size > previewLimits[kind]) throw new PreviewError('tooLarge')
  try { return new Uint8Array(await file.arrayBuffer()) } catch { throw new PreviewError('unreadable') }
}
export function formatPreviewSize(bytes: number): string {
  return bytes < 1024 ? `${bytes} B` : bytes < 1024 ** 2 ? `${(bytes / 1024).toFixed(1)} KiB` : `${(bytes / 1024 ** 2).toFixed(1)} MiB`
}

import type { Language } from '@/shared/i18n/messages'
import type { JsonFormatOptions, JsonStatus } from '@/features/json/jsonTools'
import type { DiffResult } from '@/features/diff/diffTools'

export type JsonOperation = 'format' | 'compress' | 'advancedFormat' | 'toXml' | 'toBean' | 'swap' | 'escape' | 'unescape' | 'escapeText' | 'unescapeText' | 'query' | 'fromXml' | 'fromBean'
export type TextTask =
  | { kind: 'validateJson'; input: string; language: Language }
  | { kind: 'json'; operation: JsonOperation; input: string; language: Language; options?: JsonFormatOptions; className?: string; path?: string }
  | { kind: 'diff'; left: string; right: string; ignoreWhitespace: boolean }
export type TextTaskResult<T extends TextTask> = T extends { kind: 'validateJson' } ? JsonStatus : T extends { kind: 'diff' } ? DiffResult : string
export type TextTaskReply = { ok: true; result: string | JsonStatus | DiffResult } | { ok: false; error: string }

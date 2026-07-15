import type { MessageKey } from '@/shared/i18n/messages'

export type JsonStatus =
  | { kind: 'idle'; message: string }
  | { kind: 'valid'; message: string }
  | { kind: 'error'; message: string }

type Translate = (key: MessageKey, params?: Record<string, string>) => string

export function formatJson(input: string, t: Translate, spaces = 2): string {
  return JSON.stringify(parseJson(input, t), null, spaces)
}

export function compressJson(input: string, t: Translate): string {
  return JSON.stringify(parseJson(input, t))
}

export function validateJson(input: string, t: Translate): JsonStatus {
  if (!input.trim()) {
    return { kind: 'idle', message: t('json.valid.idle') }
  }

  try {
    const value = parseJson(input, t)
    const type = Array.isArray(value) ? 'Array' : typeof value === 'object' && value !== null ? 'Object' : typeof value
    return { kind: 'valid', message: t('json.valid.ok', { type }) }
  } catch (error) {
    return { kind: 'error', message: error instanceof Error ? error.message : t('json.valid.error') }
  }
}

export function escapeJsonString(input: string): string {
  return JSON.stringify(input)
}

export function unescapeJsonString(input: string, t: Translate): string {
  const parsed = parseJson(input, t)
  if (typeof parsed !== 'string') {
    throw new Error(t('json.error.notString'))
  }
  return parsed
}

function parseJson(input: string, t: Translate): unknown {
  if (!input.trim()) {
    throw new Error(t('json.error.empty'))
  }

  return JSON.parse(input)
}

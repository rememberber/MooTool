export type JsonStatus =
  | { kind: 'idle'; message: string }
  | { kind: 'valid'; message: string }
  | { kind: 'error'; message: string }

export function formatJson(input: string, spaces = 2): string {
  return JSON.stringify(parseJson(input), null, spaces)
}

export function compressJson(input: string): string {
  return JSON.stringify(parseJson(input))
}

export function validateJson(input: string): JsonStatus {
  if (!input.trim()) {
    return { kind: 'idle', message: '等待输入 JSON' }
  }

  try {
    const value = parseJson(input)
    const type = Array.isArray(value) ? 'Array' : typeof value === 'object' && value !== null ? 'Object' : typeof value
    return { kind: 'valid', message: `有效 JSON · ${type}` }
  } catch (error) {
    return { kind: 'error', message: error instanceof Error ? error.message : 'JSON 解析失败' }
  }
}

export function escapeJsonString(input: string): string {
  return JSON.stringify(input)
}

export function unescapeJsonString(input: string): string {
  const parsed = parseJson(input)
  if (typeof parsed !== 'string') {
    throw new Error('当前内容不是 JSON 字符串')
  }
  return parsed
}

function parseJson(input: string): unknown {
  if (!input.trim()) {
    throw new Error('请输入 JSON 内容')
  }

  return JSON.parse(input)
}

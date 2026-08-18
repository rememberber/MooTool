export type RuntimeToolErrorCode = 'unclosedArguments' | 'argumentLimit'

export class RuntimeToolError extends Error {
  constructor(readonly code: RuntimeToolErrorCode) {
    super(`RUNTIME_TOOL_${code}`)
    this.name = 'RuntimeToolError'
  }
}

export function parseRuntimeArguments(value: string): string[] {
  const result: string[] = []; let current = ''; let quote = ''; let escaped = false
  const push = () => { if (current) result.push(current); current = '' }
  for (const character of value.trim()) {
    if (escaped) { current += character; escaped = false }
    else if (character === '\\' && quote !== "'") escaped = true
    else if (quote) { if (character === quote) quote = ''; else current += character }
    else if (character === '"' || character === "'") quote = character
    else if (/\s/.test(character)) push()
    else current += character
  }
  if (escaped || quote) throw new RuntimeToolError('unclosedArguments')
  push(); if (result.length > 40 || result.some((item) => item.length > 1000)) throw new RuntimeToolError('argumentLimit'); return result
}

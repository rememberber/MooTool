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

export async function formatRuntimeSource(value: string, runtime: 'java' | 'groovy' | 'python' | 'node'): Promise<string> {
  if (!value.trim()) return ''
  if (runtime === 'node') {
    const [{ format }, babel, estree] = await Promise.all([
      import('prettier/standalone'),
      import('prettier/plugins/babel'),
      import('prettier/plugins/estree')
    ])
    return (await format(value, {
      parser: 'babel',
      plugins: [babel.default, estree.default],
      printWidth: 100,
      tabWidth: 2,
      semi: false,
      singleQuote: true
    })).trimEnd()
  }
  if (runtime === 'java' || runtime === 'groovy') {
    const { default: beautify } = await import('js-beautify')
    return beautify.js(value, {
      indent_size: 4,
      indent_char: ' ',
      indent_with_tabs: false,
      brace_style: 'collapse',
      preserve_newlines: true,
      max_preserve_newlines: 2,
      wrap_line_length: 120,
      end_with_newline: false
    }).trimEnd()
  }
  return value
    .replace(/\t/g, '    ')
    .split(/\r?\n/)
    .map((line) => line.trimEnd())
    .join('\n')
    .replace(/\n{3,}/g, '\n\n')
    .trimEnd()
}

import type { CodeRuntimeId } from '@/shared/contracts/runtime'

export async function formatRuntimeSource(code: string, runtime: CodeRuntimeId): Promise<string> {
  if (!code.trim()) return ''
  if (runtime === 'java') {
    const { formatCode } = await import('@/features/reformat/reformatTools')
    return formatCode(code, 'java')
  }
  if (runtime === 'node') {
    const [{ format: prettierFormat }, { default: babelPlugin }, { default: estreePlugin }] = await Promise.all([
      import('prettier/standalone'),
      import('prettier/plugins/babel'),
      import('prettier/plugins/estree')
    ])
    return (await prettierFormat(code, {
      parser: 'babel',
      plugins: [babelPlugin, estreePlugin],
      printWidth: 100,
      tabWidth: 2,
      semi: false,
      singleQuote: true,
      endOfLine: 'lf'
    })).trimEnd()
  }
  return code.replaceAll('\t', '    ').split(/\r?\n/).map((line) => line.trimEnd()).join('\n').trimEnd()
}

export function runtimeDisplayName(runtime: CodeRuntimeId): string {
  if (runtime === 'node') return 'Node.js'
  return runtime[0].toLocaleUpperCase() + runtime.slice(1)
}

export function parseRuntimeArguments(value: string): string[] {
  const result: string[] = []
  let current = ''
  let quote = ''
  let escaped = false
  const push = (): void => {
    if (current) result.push(current)
    current = ''
  }
  for (const character of value.trim()) {
    if (escaped) {
      current += character
      escaped = false
    } else if (character === '\\' && quote !== "'") {
      escaped = true
    } else if (quote) {
      if (character === quote) quote = ''
      else current += character
    } else if (character === '"' || character === "'") {
      quote = character
    } else if (/\s/.test(character)) {
      push()
    } else {
      current += character
    }
  }
  if (escaped || quote) throw new Error('Unterminated runtime argument')
  push()
  if (result.length > 40 || result.some((argument) => argument.length > 1000)) throw new Error('Too many runtime arguments')
  return result
}

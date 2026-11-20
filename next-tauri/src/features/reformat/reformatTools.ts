export const reformatTypes = ['nginx', 'java', 'xml', 'html'] as const

export type ReformatType = (typeof reformatTypes)[number]

export const reformatSamples: Record<ReformatType, string> = {
  nginx: 'server { listen 80; location / { proxy_pass http://127.0.0.1:3000; } }',
  java: 'class Demo{public static void main(String[] args){System.out.println("MooTool");}}',
  xml: '<root><tool id="mootool"><name>MooTool</name></tool></root>',
  html: '<main><h1>MooTool</h1><p>Desktop toolbox</p></main>'
}

export async function formatCode(
  input: string,
  type: ReformatType,
  indent = 4
): Promise<string> {
  if (!input.trim()) return ''
  const tabWidth = clampIndent(indent)
  if (type === 'nginx') return formatNginx(input, tabWidth)
  if (type === 'java') {
    const [{ format }, javaPlugin] = await Promise.all([
      import('prettier/standalone'),
      import('prettier-plugin-java')
    ])
    return (await format(input, {
      parser: 'java',
      plugins: [javaPlugin.default],
      tabWidth,
      useTabs: false,
      printWidth: 120,
      endOfLine: 'lf'
    })).trimEnd()
  }

  const { format } = await import('prettier/standalone')
  const plugin = type === 'xml'
    ? (await import('@prettier/plugin-xml')).default
    : (await import('prettier/plugins/html')).default

  return (await format(input, {
    parser: type,
    plugins: [plugin],
    tabWidth,
    useTabs: false,
    printWidth: 120,
    endOfLine: 'lf',
    ...(type === 'xml' ? { xmlWhitespaceSensitivity: 'preserve' as const } : {})
  })).trimEnd()
}

export function formatNginx(input: string, indent = 4): string {
  const tokens = tokenizeNginx(input)
  const lines: string[] = []
  let level = 0
  for (const token of tokens) {
    if (token === '}') level = Math.max(0, level - 1)
    if (token) lines.push(`${' '.repeat(level * indent)}${token}`)
    if (token.endsWith('{')) level += 1
  }
  return lines.join('\n')
}

function tokenizeNginx(input: string): string[] {
  const tokens: string[] = []
  let current = ''
  let quote = ''
  let escaped = false
  let comment = false

  const flush = (): void => {
    const value = current.trim()
    if (value) tokens.push(value)
    current = ''
  }

  for (let index = 0; index < input.length; index += 1) {
    const char = input[index]
    if (comment) {
      current += char
      if (char === '\n') {
        flush()
        comment = false
      }
      continue
    }
    if (escaped) {
      current += char
      escaped = false
      continue
    }
    if (char === '\\') {
      current += char
      escaped = true
      continue
    }
    if (quote) {
      current += char
      if (char === quote) quote = ''
      continue
    }
    if (char === '"' || char === "'") {
      quote = char
      current += char
      continue
    }
    if (char === '#') {
      comment = true
      current += char
      continue
    }
    if (char === '{') {
      current = `${current.trimEnd()} {`
      flush()
    } else if (char === '}') {
      flush()
      tokens.push('}')
    } else if (char === ';') {
      current = `${current.trimEnd()};`
      flush()
    } else if (char === '\n' || char === '\r') {
      flush()
    } else {
      current += char
    }
  }
  flush()
  return tokens
}

function clampIndent(indent: number): number {
  return Math.min(8, Math.max(1, Math.round(indent)))
}

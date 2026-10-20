import type { QuickNote } from '../../platform/contracts/localData'

export function prefixSelectedLines(content: string, start: number, end: number, prefix: 'bullet' | 'numbered'): string {
  const lineStart = content.lastIndexOf('\n', Math.max(0, start - 1)) + 1
  const nextLine = content.indexOf('\n', end)
  const lineEnd = nextLine < 0 ? content.length : nextLine
  const selected = content.slice(lineStart, lineEnd)
  const transformed = selected.split('\n').map((line, index) => `${prefix === 'bullet' ? '- ' : `${index + 1}. `}${line}`).join('\n')
  return `${content.slice(0, lineStart)}${transformed}${content.slice(lineEnd)}`
}

export async function formatNoteContent(content: string, syntax: QuickNote['syntax'], sqlDialect = 'Standard SQL'): Promise<string> {
  if (!content.trim()) return ''
  if (syntax === 'json') return `${JSON.stringify(JSON.parse(content), null, 4)}\n`.trimEnd()
  if (syntax === 'java') {
    const { default: beautify } = await import('js-beautify')
    return beautify.js(content, { indent_size: 4, indent_char: ' ', brace_style: 'collapse' }).trimEnd()
  }
  if (syntax === 'sql') return formatSql(content, sqlDialect)
  if (syntax === 'plain' || syntax === 'python') {
    return content.replaceAll('\t', '    ').split(/\r?\n/).map((line) => line.trimEnd()).join('\n').trimEnd()
  }
  const { format } = await import('prettier/standalone')
  const parser = syntax === 'yaml' ? 'yaml' : syntax === 'html' ? 'html' : syntax === 'xml' ? 'xml' : syntax === 'javascript' ? 'babel' : syntax === 'typescript' ? 'typescript' : 'markdown'
  const plugin = syntax === 'yaml'
    ? (await import('prettier/plugins/yaml')).default
    : syntax === 'html'
      ? (await import('prettier/plugins/html')).default
      : syntax === 'xml'
        ? (await import('@prettier/plugin-xml')).default
        : syntax === 'javascript'
          ? (await import('prettier/plugins/babel')).default
          : syntax === 'typescript'
            ? (await import('prettier/plugins/typescript')).default
            : (await import('prettier/plugins/markdown')).default
  const estree = syntax === 'javascript' || syntax === 'typescript' ? (await import('prettier/plugins/estree')).default : null
  return (await format(content, {
    parser,
    plugins: estree ? [plugin, estree] : [plugin],
    tabWidth: 4,
    useTabs: false,
    printWidth: 80,
    endOfLine: 'lf',
    ...(syntax === 'xml' ? { xmlWhitespaceSensitivity: 'preserve' as const } : {})
  })).trimEnd()
}

const sqlLanguageByDialect: Record<string, string> = {
  'standard sql': 'sql',
  mysql: 'mysql',
  mariadb: 'mariadb',
  postgresql: 'postgresql',
  'oracle pl/sql': 'plsql',
  'sql server transact-sql': 'transactsql',
  'ibm db2': 'db2',
  'couchbase n1ql': 'n1ql',
  'amazon redshift': 'redshift',
  spark: 'spark'
}

async function formatSql(content: string, dialect: string): Promise<string> {
  const { format } = await import('sql-formatter')
  const language = sqlLanguageByDialect[dialect.trim().toLocaleLowerCase()] ?? 'sql'
  return format(content, {
    language: language as 'sql',
    tabWidth: 4,
    keywordCase: 'upper'
  }).trimEnd()
}

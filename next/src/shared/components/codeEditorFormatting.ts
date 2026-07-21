import type { Plugin } from 'prettier'
import type { SqlLanguage } from 'sql-formatter'
import { resolveTextCodeEditorLanguage, type TextCodeEditorLanguage } from './codeEditorLanguage'

type FormatCodeEditorOptions = {
  tabWidth?: number
  sqlDialect?: string
}

const sqlLanguageByDialect: Record<string, SqlLanguage> = {
  'standard sql': 'sql',
  sql: 'sql',
  mysql: 'mysql',
  mariadb: 'mariadb',
  postgresql: 'postgresql',
  'oracle pl/sql': 'plsql',
  plsql: 'plsql',
  'sql server transact-sql': 'transactsql',
  'transact-sql': 'transactsql',
  tsql: 'transactsql',
  'ibm db2': 'db2',
  db2: 'db2',
  'couchbase n1ql': 'n1ql',
  n1ql: 'n1ql',
  'amazon redshift': 'redshift',
  redshift: 'redshift',
  spark: 'spark'
}

export async function formatCodeEditorContent(
  content: string,
  languageOrMime: TextCodeEditorLanguage | string,
  options: FormatCodeEditorOptions = {}
): Promise<string> {
  if (!content.trim()) return ''
  const language = resolveTextCodeEditorLanguage(languageOrMime)
  const tabWidth = Math.min(8, Math.max(1, Math.round(options.tabWidth ?? 2)))

  if (language === 'json') return JSON.stringify(JSON.parse(content), null, tabWidth)
  if (language === 'java') return formatWithPlugin(content, 'java', () => import('prettier-plugin-java'), tabWidth)
  if (language === 'xml') return formatWithPlugin(content, 'xml', () => import('@prettier/plugin-xml'), tabWidth, { xmlWhitespaceSensitivity: 'preserve' })
  if (language === 'html') return formatWithPlugin(content, 'html', () => import('prettier/plugins/html'), tabWidth)
  if (language === 'javascript') return formatWithPlugin(content, 'babel', () => import('prettier/plugins/babel'), tabWidth, {}, true)
  if (language === 'typescript') return formatWithPlugin(content, 'typescript', () => import('prettier/plugins/typescript'), tabWidth, {}, true)
  if (language === 'markdown') return formatWithPlugin(content, 'markdown', () => import('prettier/plugins/markdown'), tabWidth)
  if (language === 'yaml') return formatWithPlugin(content, 'yaml', () => import('prettier/plugins/yaml'), tabWidth)
  if (language === 'sql') return formatSql(content, options.sqlDialect, tabWidth)
  if (language === 'python') return formatPython(content, tabWidth)
  return trimTrailingWhitespace(content)
}

async function formatSql(content: string, dialect: string | undefined, tabWidth: number): Promise<string> {
  const { format } = await import('sql-formatter')
  return format(content, {
    language: resolveSqlLanguage(dialect),
    tabWidth,
    keywordCase: 'upper'
  }).trimEnd()
}

function resolveSqlLanguage(dialect: string | undefined): SqlLanguage {
  const normalized = dialect?.trim().toLocaleLowerCase() ?? ''
  return sqlLanguageByDialect[normalized] ?? 'sql'
}

function formatPython(content: string, tabWidth: number): string {
  const indent = ' '.repeat(tabWidth)
  return content
    .replaceAll('\t', indent)
    .split(/\r?\n/)
    .map((line) => line.trimEnd())
    .join('\n')
    .trimEnd()
}

async function formatWithPlugin(
  content: string,
  parser: string,
  loadPlugin: () => Promise<{ default: Plugin }>,
  tabWidth: number,
  parserOptions: Record<string, unknown> = {},
  needsEstree = false
): Promise<string> {
  const [{ format }, parserPlugin, estreePlugin] = await Promise.all([
    import('prettier/standalone'),
    loadPlugin(),
    needsEstree ? import('prettier/plugins/estree') : Promise.resolve(null)
  ])
  return (await format(content, {
    parser,
    plugins: [parserPlugin.default, ...(estreePlugin ? [estreePlugin.default] : [])],
    printWidth: 100,
    tabWidth,
    useTabs: false,
    endOfLine: 'lf',
    ...parserOptions
  })).trimEnd()
}

function trimTrailingWhitespace(content: string): string {
  return content.split(/\r?\n/).map((line) => line.trimEnd()).join('\n').trimEnd()
}

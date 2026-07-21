import type { Plugin } from 'prettier'
import { resolveTextCodeEditorLanguage, type TextCodeEditorLanguage } from './codeEditorLanguage'

type FormatCodeEditorOptions = {
  tabWidth?: number
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
  return trimTrailingWhitespace(content)
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

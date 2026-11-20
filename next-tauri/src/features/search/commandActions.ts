import { textToBase64, base64ToText, urlDecode, urlEncode } from '../encode/encodeTools'
import { formatJson } from '../json/jsonTools'
import { runQuickReplace } from '../quickNote/quickReplace'
import { randomUuid } from '../crypto/electronCrypto'
import { timestampToLocal } from '../timestamp/timeTools'
import { clipboardHtmlToMarkdown, clipboardTableToJson } from './clipboardTransforms'
import { commandActionSearch } from './commandPaletteMessages'
import { commandActionIds, type CommandActionId } from './commandPaletteState'

export type CommandActionInput = 'none' | 'text' | 'rich-text' | 'html'

export interface CommandAction {
  id: CommandActionId
  titleKey: 'uuid' | 'timestamp' | 'jsonFormat' | 'jsonMinify' | 'base64Encode' | 'base64Decode' | 'urlEncode' | 'urlDecode' | 'deduplicate' | 'clipboardText' | 'clipboardHtml' | 'htmlMarkdown' | 'tableJson'
  keywords: string[]
  aliases: string[]
  input: CommandActionInput
}

function actionSearch(id: keyof typeof commandActionSearch): Pick<CommandAction, 'keywords' | 'aliases'> {
  const item = commandActionSearch[id]
  return { keywords: [...item.keywords], aliases: [...item.aliases] }
}

export const commandActions: CommandAction[] = [
  { id: 'uuid', titleKey: 'uuid', ...actionSearch('uuid'), input: 'none' },
  { id: 'timestamp', titleKey: 'timestamp', ...actionSearch('timestamp'), input: 'text' },
  { id: 'json-format', titleKey: 'jsonFormat', ...actionSearch('json-format'), input: 'text' },
  { id: 'json-minify', titleKey: 'jsonMinify', ...actionSearch('json-minify'), input: 'text' },
  { id: 'base64-encode', titleKey: 'base64Encode', ...actionSearch('base64-encode'), input: 'text' },
  { id: 'base64-decode', titleKey: 'base64Decode', ...actionSearch('base64-decode'), input: 'text' },
  { id: 'url-encode', titleKey: 'urlEncode', ...actionSearch('url-encode'), input: 'text' },
  { id: 'url-decode', titleKey: 'urlDecode', ...actionSearch('url-decode'), input: 'text' },
  { id: 'deduplicate-lines', titleKey: 'deduplicate', ...actionSearch('deduplicate-lines'), input: 'text' },
  { id: 'clipboard-text', titleKey: 'clipboardText', ...actionSearch('clipboard-text'), input: 'text' },
  { id: 'clipboard-html', titleKey: 'clipboardHtml', ...actionSearch('clipboard-html'), input: 'html' },
  { id: 'html-markdown', titleKey: 'htmlMarkdown', ...actionSearch('html-markdown'), input: 'rich-text' },
  { id: 'table-json', titleKey: 'tableJson', ...actionSearch('table-json'), input: 'rich-text' }
]

export interface CommandActionMatch {
  action: CommandAction
  argument?: string
}

export function searchCommandActions(query: string, title: (action: CommandAction) => string): CommandActionMatch[] {
  const source = query.trimStart()
  const normalized = source.trim().toLocaleLowerCase()
  const aliases = commandActions.flatMap((action) => action.aliases.map((alias) => ({ action, alias })))
    .sort((left, right) => right.alias.length - left.alias.length)
  const exact = aliases.find(({ alias }) => alias.toLocaleLowerCase() === normalized)
  const command = !exact && aliases.find(({ action, alias }) => action.input !== 'none' && source.toLocaleLowerCase().startsWith(`${alias.toLocaleLowerCase()} `))
  if (command) return [{ action: command.action, argument: source.slice(command.alias.length + 1) }]
  return commandActions.filter((action) => !normalized || [title(action), action.id, ...action.keywords, ...action.aliases]
    .some((keyword) => keyword.toLocaleLowerCase().includes(normalized))).map((action) => ({ action }))
}

export async function runCommandAction(match: CommandActionMatch, readText: () => Promise<string>, readHtml: () => Promise<string>, zone: string): Promise<string> {
  const action = match.action
  const html = match.argument === undefined && (action.input === 'html' || action.input === 'rich-text') ? await readHtml() : ''
  const input = action.input === 'none'
    ? ''
    : match.argument ?? (action.input === 'html' ? html : await readText())
  if (input.length > 100_000 || html.length > 100_000) throw new Error('tooLarge')
  if (action.input === 'html' && !input.trim()) throw new Error('noHtml')
  if (action.input !== 'none' && !input.trim() && !html.trim()) throw new Error('emptyInput')
  const output = execute(action.id, input, html, zone)
  if (action.id === 'html-markdown' && !output.trim()) throw new Error('emptyInput')
  return output
}

function execute(id: CommandActionId, input: string, html: string, zone: string): string {
  if (id === 'uuid') return randomUuid()
  if (id === 'timestamp') return `${timestampToLocal(input, 'second', zone).localTime} (${zone})`
  if (id === 'json-format') return formatJson(input, { indent: 2, sortKeys: false, checkDuplicateKeys: true })
  if (id === 'json-minify') return formatJson(input, { indent: 0, sortKeys: false, checkDuplicateKeys: true })
  if (id === 'base64-encode') return textToBase64(input)
  if (id === 'base64-decode') return base64ToText(input)
  if (id === 'url-encode') return urlEncode(input, 'utf-8')
  if (id === 'url-decode') {
    if (/%(?![\da-fA-F]{2})/.test(input)) throw new Error('invalidUrl')
    return urlDecode(input, 'utf-8')
  }
  if (id === 'deduplicate-lines') return runQuickReplace(input, 'deduplicateLines')
  if (id === 'clipboard-text' || id === 'clipboard-html') return input
  if (id === 'html-markdown') return html ? clipboardHtmlToMarkdown(html) : input
  if (id === 'table-json') return clipboardTableToJson(input, html)
  return commandActionIds.includes(id) ? input : ''
}

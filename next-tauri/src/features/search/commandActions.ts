import { textToBase64, base64ToText, urlDecode, urlEncode } from '../encode/encodeTools'
import { formatJson } from '../json/jsonTools'
import { runQuickReplace } from '../quickNote/quickReplace'
import { randomUuid } from '../crypto/electronCrypto'
import { timestampToLocal } from '../timestamp/timeTools'
import { clipboardHtmlToMarkdown, clipboardTableToJson } from './clipboardTransforms'
import { commandActionIds, type CommandActionId } from './commandPaletteState'

export type CommandActionInput = 'none' | 'text' | 'rich-text' | 'html'

export interface CommandAction {
  id: CommandActionId
  titleKey: 'uuid' | 'timestamp' | 'jsonFormat' | 'jsonMinify' | 'base64Encode' | 'base64Decode' | 'urlEncode' | 'urlDecode' | 'deduplicate' | 'clipboardText' | 'clipboardHtml' | 'htmlMarkdown' | 'tableJson'
  keywords: string[]
  aliases: string[]
  input: CommandActionInput
}

export const commandActions: CommandAction[] = [
  { id: 'uuid', titleKey: 'uuid', keywords: ['random', '随机'], aliases: ['uuid'], input: 'none' },
  { id: 'timestamp', titleKey: 'timestamp', keywords: ['time', '时间'], aliases: ['timestamp', '时间戳'], input: 'text' },
  { id: 'json-format', titleKey: 'jsonFormat', keywords: ['json', '剪贴板', 'clipboard'], aliases: ['json format', '格式化 JSON', 'json'], input: 'text' },
  { id: 'json-minify', titleKey: 'jsonMinify', keywords: ['json', '剪贴板', 'clipboard'], aliases: ['json minify', '压缩 JSON'], input: 'text' },
  { id: 'base64-encode', titleKey: 'base64Encode', keywords: ['base64', '剪贴板', 'clipboard'], aliases: ['base64 encode', 'base64 编码'], input: 'text' },
  { id: 'base64-decode', titleKey: 'base64Decode', keywords: ['base64', '剪贴板', 'clipboard'], aliases: ['base64 decode', 'base64 解码'], input: 'text' },
  { id: 'url-encode', titleKey: 'urlEncode', keywords: ['url', '剪贴板', 'clipboard'], aliases: ['url encode', 'url 编码'], input: 'text' },
  { id: 'url-decode', titleKey: 'urlDecode', keywords: ['url', '剪贴板', 'clipboard'], aliases: ['url decode', 'url 解码'], input: 'text' },
  { id: 'deduplicate-lines', titleKey: 'deduplicate', keywords: ['unique', '去重', '剪贴板', 'clipboard'], aliases: ['deduplicate', '按行去重'], input: 'text' },
  { id: 'clipboard-text', titleKey: 'clipboardText', keywords: ['plain', '纯文本', '剪贴板', 'clipboard'], aliases: ['clipboard text', '剪贴板文本'], input: 'text' },
  { id: 'clipboard-html', titleKey: 'clipboardHtml', keywords: ['html', '剪贴板', 'clipboard'], aliases: ['clipboard html', '剪贴板 HTML'], input: 'html' },
  { id: 'html-markdown', titleKey: 'htmlMarkdown', keywords: ['markdown', 'rich text', '富文本', '剪贴板'], aliases: ['markdown', '富文本转 Markdown'], input: 'rich-text' },
  { id: 'table-json', titleKey: 'tableJson', keywords: ['table', 'csv', 'tsv', '表格', '剪贴板'], aliases: ['table json', '表格转 JSON'], input: 'rich-text' }
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

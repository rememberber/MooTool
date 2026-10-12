import type { MessageKey } from '@/shared/i18n/messages'
import type { ClipboardTextSnapshot, CommandActionId } from '@/shared/contracts/commandPalette'

export type ActionContext = {
  t: (key: MessageKey, params?: Record<string, string>) => string
  zone: string
  html?: string
}

export type ActionDefinition = {
  id: CommandActionId
  titleKey: MessageKey
  keywords: string[]
  aliases: string[]
  input: 'none' | 'text' | 'rich-text' | 'html'
  run: (input: string, context: ActionContext) => Promise<string>
}

// Actions know nothing about windows or the clipboard. Entry points provide input
// and decide how to present/copy the result; tool pages keep their existing state.
export const actionRegistry: ActionDefinition[] = [
  {
    id: 'uuid', titleKey: 'app.command.uuid', keywords: ['random', '随机'], aliases: ['uuid'], input: 'none',
    run: async () => (await import('@/shared/utils/uuid')).randomUuid()
  },
  {
    id: 'timestamp', titleKey: 'app.command.timestamp', keywords: ['time', '时间'], aliases: ['timestamp', '时间戳'], input: 'text',
    run: async (input, { t, zone }) => {
      try {
        const { timestampToLocal } = await import('@/features/time/timeTools')
        return `${timestampToLocal(input, 'second', zone).localTime} (${zone})`
      } catch { throw new Error(t('time.error.timestamp')) }
    }
  },
  ...(['format', 'minify'] as const).map((mode): ActionDefinition => ({
    id: `json-${mode}`, titleKey: mode === 'format' ? 'app.command.jsonFormat' : 'app.command.jsonMinify',
    keywords: ['json', '剪贴板', 'clipboard'],
    aliases: mode === 'format' ? ['json format', '格式化 JSON', 'json'] : ['json minify', '压缩 JSON'], input: 'text',
    run: async (input, { t }) => (await import('@/features/json/jsonTools')).formatJsonAdvanced(input, t, {
      spaces: mode === 'format' ? 2 : 0, sortKeys: false, ignoreCase: false, checkDuplicateKeys: true
    })
  })),
  ...(['encode', 'decode'] as const).map((direction): ActionDefinition => ({
    id: `base64-${direction}`, titleKey: direction === 'encode' ? 'app.command.base64Encode' : 'app.command.base64Decode',
    keywords: ['base64', '剪贴板', 'clipboard'],
    aliases: direction === 'encode' ? ['base64 encode', 'base64 编码'] : ['base64 decode', 'base64 解码'], input: 'text',
    run: async (input, { t }) => {
      const { encodeBase64, decodeBase64 } = await import('@/features/encode/encodeTools')
      try { return direction === 'encode' ? encodeBase64(input) : decodeBase64(input) }
      catch { throw new Error(t('app.command.invalidBase64')) }
    }
  })),
  ...(['encode', 'decode'] as const).map((direction): ActionDefinition => ({
    id: `url-${direction}`, titleKey: direction === 'encode' ? 'app.command.urlEncode' : 'app.command.urlDecode',
    keywords: ['url', '剪贴板', 'clipboard'],
    aliases: direction === 'encode' ? ['url encode', 'url 编码'] : ['url decode', 'url 解码'], input: 'text',
    run: async (input, { t }) => {
      const { urlEncode, urlDecode } = await import('@/features/encode/encodeTools')
      if (direction === 'decode' && /%(?![\da-fA-F]{2})/.test(input)) throw new Error(t('app.command.invalidUrl'))
      return direction === 'encode' ? urlEncode(input, 'utf-8') : urlDecode(input, 'utf-8')
    }
  })),
  {
    id: 'deduplicate-lines', titleKey: 'app.command.deduplicate', keywords: ['unique', '去重', '剪贴板', 'clipboard'],
    aliases: ['deduplicate', '按行去重'], input: 'text',
    run: async (input) => (await import('@/features/quickNote/quickReplace')).runQuickReplace(input, 'deduplicateLines')
  },
  {
    id: 'clipboard-text', titleKey: 'app.command.clipboardText', keywords: ['plain', '纯文本', '剪贴板', 'clipboard', 'file', '文件'],
    aliases: ['clipboard text', '剪贴板文本'], input: 'text', run: async input => input
  },
  {
    id: 'clipboard-html', titleKey: 'app.command.clipboardHtml', keywords: ['html', '剪贴板', 'clipboard', 'file', '文件'],
    aliases: ['clipboard html', '剪贴板 HTML'], input: 'html', run: async input => input
  },
  {
    id: 'html-markdown', titleKey: 'app.command.htmlMarkdown', keywords: ['markdown', 'rich text', '富文本', '剪贴板', 'clipboard'],
    aliases: ['markdown', '富文本转 Markdown'], input: 'rich-text',
    run: async (input, { t, html }) => translatedClipboardTransform(async () => html
      ? (await import('@/features/search/clipboardTransforms')).clipboardHtmlToMarkdown(html) : input, t)
  },
  {
    id: 'table-json', titleKey: 'app.command.tableJson', keywords: ['table', 'csv', 'tsv', '表格', '剪贴板', 'clipboard'],
    aliases: ['table json', '表格转 JSON'], input: 'rich-text',
    run: async (input, { t, html }) => translatedClipboardTransform(async () =>
      (await import('@/features/search/clipboardTransforms')).clipboardTableToJson(input, html), t)
  }
]

async function translatedClipboardTransform(run: () => Promise<string>, t: ActionContext['t']): Promise<string> {
  try { return await run() }
  catch (error) {
    const keys: MessageKey[] = ['app.command.invalidTable', 'app.command.tableHeaders', 'app.command.tableLimit', 'app.command.unsupportedTable', 'app.command.complexHtml']
    const key = keys.find(key => error instanceof Error && error.message === key)
    if (key) throw new Error(t(key))
    throw error
  }
}

export type ActionMatch = { action: ActionDefinition; argument?: string }

export function searchActions(query: string, t: ActionContext['t']): ActionMatch[] {
  const source = query.trimStart()
  const normalized = source.trim().toLocaleLowerCase()
  // Longer aliases win, so "json minify {...}" is never consumed by "json".
  const aliases = actionRegistry.flatMap((action) => action.aliases.map((alias) => ({ action, alias })))
    .sort((left, right) => right.alias.length - left.alias.length)
  const exact = aliases.find(({ alias }) => alias.toLocaleLowerCase() === normalized)
  const command = !exact && aliases.find(({ action, alias }) => action.input !== 'none'
    && source.toLocaleLowerCase().startsWith(`${alias.toLocaleLowerCase()} `))
  if (command) return [{ action: command.action, argument: source.slice(command.alias.length + 1) }]
  return actionRegistry.filter((action) => !normalized || [t(action.titleKey), action.id, ...action.keywords, ...action.aliases]
    .some((keyword) => keyword.toLocaleLowerCase().includes(normalized))).map((action) => ({ action }))
}

export async function executeAction(match: ActionMatch, readClipboard: () => Promise<string>, context: ActionContext, readRichClipboard?: () => Promise<ClipboardTextSnapshot>): Promise<string> {
  const rich = match.argument === undefined && ['rich-text', 'html'].includes(match.action.input) && readRichClipboard ? await readRichClipboard() : null
  const input = match.action.input === 'none' ? '' : match.argument ?? (match.action.input === 'html' ? rich?.html ?? '' : rich?.text ?? await readClipboard())
  if (input.length > 100_000 || (rich?.html.length ?? 0) > 100_000 || (rich?.text.length ?? 0) > 100_000) throw new Error(context.t('app.command.tooLarge'))
  if (match.action.input === 'html' && !input.trim()) throw new Error(context.t('app.command.noHtml'))
  if (match.action.input !== 'none' && !input.trim() && !rich?.html.trim()) throw new Error(context.t('app.command.emptyInput'))
  const output = await match.action.run(input, { ...context, html: rich?.html || undefined })
  if (match.action.id === 'html-markdown' && !output.trim()) throw new Error(context.t('app.command.emptyInput'))
  return output
}

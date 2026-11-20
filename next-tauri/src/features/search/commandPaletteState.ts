export const commandActionIds = [
  'uuid', 'timestamp', 'json-format', 'json-minify', 'base64-encode', 'base64-decode',
  'url-encode', 'url-decode', 'deduplicate-lines', 'clipboard-text', 'clipboard-html',
  'html-markdown', 'table-json'
] as const

export type CommandActionId = (typeof commandActionIds)[number]
export type CommandSection = 'pinned' | 'recent' | 'all'
export type CommandFileFormat = 'text' | 'markdown' | 'json' | 'html'
export const commandFileExtensions: Record<CommandFileFormat, string> = { text: 'txt', markdown: 'md', json: 'json', html: 'html' }
export type CommandPaletteState = { pinnedActionIds: CommandActionId[]; recentActionIds: CommandActionId[] }
export const defaultCommandPaletteState: CommandPaletteState = { pinnedActionIds: [], recentActionIds: [] }
export const COMMAND_PALETTE_STORAGE_KEY = 'mootool-next-tauri:command-palette'

export type CommandPaletteOperation =
  | { type: 'pin'; actionId: CommandActionId; pinned: boolean }
  | { type: 'record'; actionId: CommandActionId }
  | { type: 'clear-recent' }

export function commandOutputFormat(actionId: CommandActionId): CommandFileFormat {
  if (actionId === 'html-markdown') return 'markdown'
  if (actionId === 'clipboard-html') return 'html'
  return actionId === 'json-format' || actionId === 'json-minify' || actionId === 'table-json' ? 'json' : 'text'
}

export function commandImportTarget(actionId: CommandActionId): 'json' | 'quick-note' {
  return commandOutputFormat(actionId) === 'json' ? 'json' : 'quick-note'
}

export function commandNoteSyntax(actionId: CommandActionId): 'text/plain' | 'text/markdown' | 'text/html' {
  const format = commandOutputFormat(actionId)
  if (format === 'markdown') return 'text/markdown'
  if (format === 'html') return 'text/html'
  return 'text/plain'
}

export function uniqueSnippetPath(paths: readonly string[]): string {
  const taken = new Set(paths)
  for (let index = 0; index < 10_000; index += 1) {
    const suffix = index === 0 ? '' : ` (${index + 1})`
    const candidate = `snippet${suffix}.json`
    if (!taken.has(candidate)) return candidate
  }
  throw new Error('importFailed')
}

export function isCommandActionId(value: unknown): value is CommandActionId {
  return typeof value === 'string' && commandActionIds.includes(value as CommandActionId)
}

export function normalizeCommandPaletteState(value: unknown): CommandPaletteState {
  const source = value && typeof value === 'object' ? value as Record<string, unknown> : {}
  const ids = (input: unknown, limit: number): CommandActionId[] => Array.isArray(input)
    ? [...new Set(input.filter(isCommandActionId))].slice(0, limit)
    : []
  return {
    pinnedActionIds: ids(source.pinnedActionIds, commandActionIds.length),
    recentActionIds: ids(source.recentActionIds, 5)
  }
}

export function updateCommandPaletteState(value: unknown, operation: CommandPaletteOperation): CommandPaletteState {
  const state = normalizeCommandPaletteState(value)
  if (operation.type === 'clear-recent') return { ...state, recentActionIds: [] }
  const actionId = operation.actionId
  if (operation.type === 'record') {
    return { ...state, recentActionIds: [actionId, ...state.recentActionIds.filter((id) => id !== actionId)].slice(0, 5) }
  }
  return {
    ...state,
    pinnedActionIds: operation.pinned
      ? [...new Set([...state.pinnedActionIds, actionId])]
      : state.pinnedActionIds.filter((id) => id !== actionId)
  }
}

export function trayCommandActions(value: unknown): CommandActionId[] {
  const state = normalizeCommandPaletteState(value)
  return state.pinnedActionIds.length ? state.pinnedActionIds : ['uuid', 'json-format', 'base64-decode']
}

export async function syncTrayQuickActions(state: unknown, label: (id: CommandActionId) => string): Promise<void> {
  if (typeof window === 'undefined' || !window.__TAURI_INTERNALS__) return
  const { invoke } = await import('@tauri-apps/api/core')
  await invoke('sync_tray_quick_actions', {
    actions: trayCommandActions(state).map((id) => ({ id, label: label(id) }))
  })
}

export function orderedCommandActions(value: unknown): Array<{ actionId: CommandActionId; section: CommandSection }> {
  const state = normalizeCommandPaletteState(value)
  const seen = new Set<CommandActionId>()
  const result: Array<{ actionId: CommandActionId; section: CommandSection }> = []
  for (const [section, ids] of [['pinned', state.pinnedActionIds], ['recent', state.recentActionIds], ['all', commandActionIds]] as const) {
    for (const actionId of ids) {
      if (seen.has(actionId)) continue
      seen.add(actionId)
      result.push({ actionId, section })
    }
  }
  return result
}

export function loadCommandPaletteState(): CommandPaletteState {
  try {
    return normalizeCommandPaletteState(JSON.parse(localStorage.getItem(COMMAND_PALETTE_STORAGE_KEY) || 'null'))
  } catch {
    return defaultCommandPaletteState
  }
}

export function saveCommandPaletteState(state: CommandPaletteState): void {
  localStorage.setItem(COMMAND_PALETTE_STORAGE_KEY, JSON.stringify(normalizeCommandPaletteState(state)))
}

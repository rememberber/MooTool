export const commandActionIds = [
  'uuid', 'timestamp', 'json-format', 'json-minify', 'base64-encode', 'base64-decode',
  'url-encode', 'url-decode', 'deduplicate-lines', 'clipboard-text', 'clipboard-html',
  'html-markdown', 'table-json'
] as const

export type CommandActionId = (typeof commandActionIds)[number]
export type CommandSection = 'pinned' | 'recent' | 'all'
export type CommandPaletteState = { pinnedActionIds: CommandActionId[]; recentActionIds: CommandActionId[] }
export const defaultCommandPaletteState: CommandPaletteState = { pinnedActionIds: [], recentActionIds: [] }
export const COMMAND_PALETTE_STORAGE_KEY = 'mootool-next-tauri:command-palette'

export type CommandPaletteOperation =
  | { type: 'pin'; actionId: CommandActionId; pinned: boolean }
  | { type: 'record'; actionId: CommandActionId }
  | { type: 'clear-recent' }

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

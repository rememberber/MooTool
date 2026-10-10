import type { MessageKey } from '../i18n/messages'

export const commandActionIds = ['uuid', 'timestamp', 'json-format', 'json-minify', 'base64-encode', 'base64-decode', 'url-encode', 'url-decode', 'deduplicate-lines'] as const
export type CommandActionId = (typeof commandActionIds)[number]
export const commandActionTitles: Record<CommandActionId, MessageKey> = {
  uuid: 'app.command.uuid', timestamp: 'app.command.timestamp',
  'json-format': 'app.command.jsonFormat', 'json-minify': 'app.command.jsonMinify',
  'base64-encode': 'app.command.base64Encode', 'base64-decode': 'app.command.base64Decode',
  'url-encode': 'app.command.urlEncode', 'url-decode': 'app.command.urlDecode',
  'deduplicate-lines': 'app.command.deduplicate'
}

export function isCommandActionId(value: unknown): value is CommandActionId {
  return typeof value === 'string' && commandActionIds.includes(value as CommandActionId)
}

export type CommandPaletteState = { pinnedActionIds: CommandActionId[]; recentActionIds: CommandActionId[] }
export const defaultCommandPaletteState: CommandPaletteState = { pinnedActionIds: [], recentActionIds: [] }
export type CommandPaletteOperation = { type: 'pin'; actionId: CommandActionId; pinned: boolean }
  | { type: 'record'; actionId: CommandActionId } | { type: 'clear-recent' }

export function normalizeCommandPaletteState(value: unknown): CommandPaletteState {
  const source = value && typeof value === 'object' ? value as Record<string, unknown> : {}
  const ids = (value: unknown, limit: number): CommandActionId[] => Array.isArray(value)
    ? [...new Set(value.filter(isCommandActionId))].slice(0, limit) : []
  return { pinnedActionIds: ids(source.pinnedActionIds, commandActionIds.length), recentActionIds: ids(source.recentActionIds, 5) }
}

export function updateCommandPaletteState(value: unknown, operation: unknown): CommandPaletteState {
  const state = normalizeCommandPaletteState(value)
  if (!operation || typeof operation !== 'object') throw new Error('Invalid command palette operation')
  const input = operation as Record<string, unknown>
  if (input.type === 'clear-recent') return { ...state, recentActionIds: [] }
  if (!isCommandActionId(input.actionId)) throw new Error('Invalid command action')
  const actionId = input.actionId
  if (input.type === 'record') return { ...state, recentActionIds: [actionId, ...state.recentActionIds.filter((id) => id !== actionId)].slice(0, 5) }
  if (input.type === 'pin' && typeof input.pinned === 'boolean') return {
    ...state, pinnedActionIds: input.pinned
      ? [...new Set([...state.pinnedActionIds, actionId])] : state.pinnedActionIds.filter((id) => id !== actionId)
  }
  throw new Error('Invalid command palette operation')
}

export type CommandSection = 'pinned' | 'recent' | 'all'
export function orderedCommandActions(value: unknown): Array<{ actionId: CommandActionId; section: CommandSection }> {
  const state = normalizeCommandPaletteState(value)
  const seen = new Set<CommandActionId>()
  const result: Array<{ actionId: CommandActionId; section: CommandSection }> = []
  for (const [section, ids] of [['pinned', state.pinnedActionIds], ['recent', state.recentActionIds], ['all', commandActionIds]] as const) {
    for (const actionId of ids) if (!seen.has(actionId)) {
      seen.add(actionId)
      result.push({ actionId, section })
    }
  }
  return result
}

export function trayCommandActions(value: unknown): CommandActionId[] {
  const state = normalizeCommandPaletteState(value)
  return state.pinnedActionIds.length ? state.pinnedActionIds : ['uuid', 'json-format', 'base64-decode']
}

export const toolIds = [
  'mootool',
  'quickNote',
  'textDiff',
  'reformat',
  'json',
  'java',
  'ymlProperties',
  'protobuf',
  'variables',
  'http',
  'host',
  'net',
  'uaParse',
  'encode',
  'crypto',
  'regex',
  'cron',
  'qrCode',
  'timeConvert',
  'translation',
  'calculator',
  'colorBoard',
  'image',
  'pdf',
  'hardware'
] as const

export type ToolId = (typeof toolIds)[number]

export function isToolId(value: unknown): value is ToolId {
  return typeof value === 'string' && toolIds.includes(value as ToolId)
}

export function isDetachableToolId(value: unknown): value is Exclude<ToolId, 'mootool'> {
  return value !== 'mootool' && isToolId(value)
}

export type WorkspaceState = {
  activeToolId: string
  recentToolIds: string[]
}

export const defaultWorkspaceState: WorkspaceState = {
  activeToolId: 'mootool',
  recentToolIds: []
}

export type AppNavigationEvent = 'focus-search'

export type ToolWorkspaceBounds = {
  x: number
  y: number
  width: number
  height: number
}

export type ToolWindowStatus = {
  toolId: Exclude<ToolId, 'mootool'>
  detached: boolean
  ready: boolean
}

export type ToolWindowSnapshot = {
  enabled: boolean
  activeToolId: ToolId
  tools: ToolWindowStatus[]
}

export const externalPageIds = [
  'home',
  'github',
  'gitee',
  'issues',
  'darcula',
  'hutool',
  'vscodeIcons',
  'wePush',
  'mooInfo',
  'contributorCassianFlorin',
  'contributorFelixcn',
  'contributorFelixnan168',
  'contributorLyp',
  'contributorSunsence',
  'contributorRememberber'
] as const

export type ExternalPageId = (typeof externalPageIds)[number]

export function isExternalPageId(value: unknown): value is ExternalPageId {
  return typeof value === 'string' && externalPageIds.includes(value as ExternalPageId)
}

export type WindowState = {
  bounds: {
    x?: number
    y?: number
    width: number
    height: number
  }
  maximized: boolean
}

export type AppPaths = {
  userData: string
  documents: string
  downloads: string
}

export type RuntimeId = 'java' | 'groovy' | 'python' | 'node'

export type RuntimeStatus = {
  id: RuntimeId
  available: boolean
  command: string
  version: string
}

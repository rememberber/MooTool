export type WorkspaceState = {
  activeToolId: string
  recentToolIds: string[]
}

export const defaultWorkspaceState: WorkspaceState = {
  activeToolId: 'mootool',
  recentToolIds: []
}

export type AppNavigationEvent = 'focus-search'

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

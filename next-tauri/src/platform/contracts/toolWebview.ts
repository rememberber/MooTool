export type ManagedToolId =
  | 'calculator'
  | 'color'
  | 'config'
  | 'cron'
  | 'crypto'
  | 'host'
  | 'http'
  | 'image'
  | 'encode'
  | 'editor-lab'
  | 'json'
  | 'message-board'
  | 'network'
  | 'pdf'
  | 'protobuf'
  | 'quick-note'
  | 'qrcode'
  | 'reformat'
  | 'regex'
  | 'runtime'
  | 'timestamp'
  | 'text-diff'
  | 'translation'
  | 'ua'
  | 'variables'
  | 'system'
  | 'webview-probe'

export interface ToolWebviewBounds {
  x: number
  y: number
  width: number
  height: number
}

export type ToolWebviewPlacement = 'closed' | 'docked' | 'detached'

export interface ToolSessionReport {
  sessionId: string
  stateRevision: number
  stateDigest: string
  stateSummary: string
}

export interface ToolWebviewSnapshot {
  toolId: ManagedToolId
  exists: boolean
  visible: boolean
  placement: ToolWebviewPlacement
  reparentOperations: number
  pageLoads: number
  sessionId: string | null
  stateRevision: number
  stateDigest: string
  stateSummary: string
  lastStressCycles: number
  lastStressPassed: boolean | null
}

export interface ToolWebviewApi {
  getSnapshot(): Promise<ToolWebviewSnapshot>
  open(bounds: ToolWebviewBounds): Promise<ToolWebviewSnapshot>
  updateBounds(bounds: ToolWebviewBounds): Promise<ToolWebviewSnapshot>
  setVisible(visible: boolean): Promise<ToolWebviewSnapshot>
  detach(): Promise<ToolWebviewSnapshot>
  dock(bounds: ToolWebviewBounds): Promise<ToolWebviewSnapshot>
  stress(bounds: ToolWebviewBounds, cycles: number): Promise<ToolWebviewSnapshot>
  close(): Promise<ToolWebviewSnapshot>
  report(report: ToolSessionReport): Promise<ToolWebviewSnapshot>
}

export function closedToolWebviewSnapshot(toolId: ManagedToolId): ToolWebviewSnapshot {
  return {
    toolId,
    exists: false,
    visible: false,
    placement: 'closed',
    reparentOperations: 0,
    pageLoads: 0,
    sessionId: null,
    stateRevision: 0,
    stateDigest: '',
    stateSummary: '',
    lastStressCycles: 0,
    lastStressPassed: null
  }
}

export interface ToolWebviewBounds {
  x: number
  y: number
  width: number
  height: number
}

export type ToolWebviewPlacement = 'closed' | 'docked' | 'detached'

export interface ToolProbeReport {
  sessionId: string
  counter: number
  draft: string
}

export interface ToolWebviewSnapshot {
  exists: boolean
  visible: boolean
  placement: ToolWebviewPlacement
  reparentOperations: number
  pageLoads: number
  sessionId: string | null
  counter: number
  draft: string
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
  report(report: ToolProbeReport): Promise<ToolWebviewSnapshot>
}

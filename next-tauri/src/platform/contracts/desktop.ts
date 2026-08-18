export type DesktopCloseDecision = 'cancel' | 'minimizeToTray' | 'quit'

export interface DesktopCloseRequest {
  canMinimizeToTray: boolean
}

export type UnlistenDesktop = () => void

export interface DesktopApi {
  resolveCloseRequest(decision: DesktopCloseDecision): Promise<void>
  subscribeCloseRequested(
    listener: (request: DesktopCloseRequest) => void
  ): Promise<UnlistenDesktop>
}

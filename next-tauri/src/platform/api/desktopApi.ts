import { invoke } from '@tauri-apps/api/core'
import { listen } from '@tauri-apps/api/event'
import type {
  DesktopApi,
  DesktopCloseDecision,
  DesktopCloseRequest,
  UnlistenDesktop
} from '../contracts/desktop'

export const CLOSE_REQUESTED_EVENT = 'mootool://close-requested'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>
type Listen = (
  event: string,
  handler: (event: { payload: DesktopCloseRequest }) => void
) => Promise<UnlistenDesktop>

export function createDesktopApi(
  invokeCommand: Invoke = invoke,
  listenEvent: Listen = listen
): DesktopApi {
  return {
    resolveCloseRequest: (decision: DesktopCloseDecision) => invokeCommand<void>(
      'resolve_close_request',
      { decision }
    ),
    subscribeCloseRequested: (listener) => listenEvent(
      CLOSE_REQUESTED_EVENT,
      (event) => listener(event.payload)
    )
  }
}

const browserDesktopApi: DesktopApi = {
  resolveCloseRequest: async (decision) => {
    if (decision === 'quit') window.close()
  },
  subscribeCloseRequested: async () => () => undefined
}

export const desktopApi: DesktopApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createDesktopApi()
  : browserDesktopApi

import { invoke } from '@tauri-apps/api/core'
import type {
  ToolProbeReport,
  ToolWebviewApi,
  ToolWebviewBounds,
  ToolWebviewSnapshot
} from '../contracts/toolWebview'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

const closedSnapshot: ToolWebviewSnapshot = {
  exists: false,
  visible: false,
  placement: 'closed',
  reparentOperations: 0,
  pageLoads: 0,
  sessionId: null,
  counter: 0,
  draft: '',
  lastStressCycles: 0,
  lastStressPassed: null
}

export function createToolWebviewApi(invokeCommand: Invoke = invoke): ToolWebviewApi {
  return {
    getSnapshot: () => invokeCommand<ToolWebviewSnapshot>('get_tool_webview_snapshot'),
    open: (bounds) => invokeCommand<ToolWebviewSnapshot>('open_tool_webview', { bounds }),
    updateBounds: (bounds) => invokeCommand<ToolWebviewSnapshot>('update_tool_webview_bounds', { bounds }),
    setVisible: (visible) => invokeCommand<ToolWebviewSnapshot>('set_tool_webview_visible', { visible }),
    detach: () => invokeCommand<ToolWebviewSnapshot>('detach_tool_webview'),
    dock: (bounds) => invokeCommand<ToolWebviewSnapshot>('dock_tool_webview', { bounds }),
    stress: (bounds, cycles) => invokeCommand<ToolWebviewSnapshot>(
      'stress_tool_webview_reparent',
      { bounds, cycles }
    ),
    close: () => invokeCommand<ToolWebviewSnapshot>('close_tool_webview'),
    report: (report) => invokeCommand<ToolWebviewSnapshot>('report_tool_webview_probe', { report })
  }
}

function createBrowserPreviewApi(): ToolWebviewApi {
  let snapshot = { ...closedSnapshot }

  const withBounds = async (_bounds: ToolWebviewBounds): Promise<ToolWebviewSnapshot> => snapshot
  return {
    getSnapshot: async () => snapshot,
    open: async () => {
      snapshot = { ...snapshot, exists: true, visible: true, placement: 'docked', pageLoads: 1 }
      return snapshot
    },
    updateBounds: withBounds,
    setVisible: async (visible) => {
      snapshot = { ...snapshot, visible }
      return snapshot
    },
    detach: async () => {
      snapshot = {
        ...snapshot,
        placement: 'detached',
        visible: true,
        reparentOperations: snapshot.reparentOperations + 1
      }
      return snapshot
    },
    dock: async () => {
      snapshot = {
        ...snapshot,
        placement: 'docked',
        visible: true,
        reparentOperations: snapshot.reparentOperations + 1
      }
      return snapshot
    },
    stress: async (_bounds, cycles) => {
      snapshot = {
        ...snapshot,
        placement: 'docked',
        reparentOperations: snapshot.reparentOperations + cycles * 2,
        lastStressCycles: cycles,
        lastStressPassed: snapshot.sessionId !== null
      }
      return snapshot
    },
    close: async () => {
      snapshot = { ...closedSnapshot }
      return snapshot
    },
    report: async (report: ToolProbeReport) => {
      snapshot = { ...snapshot, ...report }
      return snapshot
    }
  }
}

export const toolWebviewApi: ToolWebviewApi =
  typeof window !== 'undefined' && window.__TAURI_INTERNALS__
    ? createToolWebviewApi()
    : createBrowserPreviewApi()

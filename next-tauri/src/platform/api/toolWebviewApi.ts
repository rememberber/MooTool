import { invoke } from '@tauri-apps/api/core'
import {
  closedToolWebviewSnapshot,
  type ManagedToolId,
  type ToolSessionReport,
  type ToolWebviewApi,
  type ToolWebviewBounds,
  type ToolWebviewSnapshot
} from '../contracts/toolWebview'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

export function createToolWebviewApi(
  toolId: ManagedToolId,
  invokeCommand: Invoke = invoke
): ToolWebviewApi {
  const toolArg = { toolId }
  return {
    getSnapshot: () => invokeCommand<ToolWebviewSnapshot>('get_tool_webview_snapshot', toolArg),
    open: (bounds) => invokeCommand<ToolWebviewSnapshot>('open_tool_webview', { ...toolArg, bounds }),
    updateBounds: (bounds) => invokeCommand<ToolWebviewSnapshot>(
      'update_tool_webview_bounds',
      { ...toolArg, bounds }
    ),
    setVisible: (visible) => invokeCommand<ToolWebviewSnapshot>(
      'set_tool_webview_visible',
      { ...toolArg, visible }
    ),
    detach: () => invokeCommand<ToolWebviewSnapshot>('detach_tool_webview', toolArg),
    dock: (bounds) => invokeCommand<ToolWebviewSnapshot>(
      'dock_tool_webview',
      { ...toolArg, bounds }
    ),
    stress: (bounds, cycles) => invokeCommand<ToolWebviewSnapshot>(
      'stress_tool_webview_reparent',
      { ...toolArg, bounds, cycles }
    ),
    close: () => invokeCommand<ToolWebviewSnapshot>('close_tool_webview', toolArg),
    report: (report) => invokeCommand<ToolWebviewSnapshot>(
      'report_tool_webview_session',
      { report }
    )
  }
}

function createBrowserPreviewApi(toolId: ManagedToolId): ToolWebviewApi {
  let snapshot = closedToolWebviewSnapshot(toolId)

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
      snapshot = closedToolWebviewSnapshot(toolId)
      return snapshot
    },
    report: async (report: ToolSessionReport) => {
      snapshot = { ...snapshot, ...report }
      return snapshot
    }
  }
}

function platformApi(toolId: ManagedToolId): ToolWebviewApi {
  return typeof window !== 'undefined' && window.__TAURI_INTERNALS__
    ? createToolWebviewApi(toolId)
    : createBrowserPreviewApi(toolId)
}

export const toolWebviewApis: Record<ManagedToolId, ToolWebviewApi> = {
  calculator: platformApi('calculator'),
  color: platformApi('color'),
  config: platformApi('config'),
  cron: platformApi('cron'),
  crypto: platformApi('crypto'),
  host: platformApi('host'),
  http: platformApi('http'),
  image: platformApi('image'),
  encode: platformApi('encode'),
  'editor-lab': platformApi('editor-lab'),
  json: platformApi('json'),
  'message-board': platformApi('message-board'),
  network: platformApi('network'),
  pdf: platformApi('pdf'),
  protobuf: platformApi('protobuf'),
  'quick-note': platformApi('quick-note'),
  qrcode: platformApi('qrcode'),
  reformat: platformApi('reformat'),
  regex: platformApi('regex'),
  runtime: platformApi('runtime'),
  timestamp: platformApi('timestamp'),
  'text-diff': platformApi('text-diff'),
  translation: platformApi('translation'),
  ua: platformApi('ua'),
  variables: platformApi('variables'),
  system: platformApi('system'),
  'webview-probe': platformApi('webview-probe')
}

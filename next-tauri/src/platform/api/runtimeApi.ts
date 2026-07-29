import { invoke } from '@tauri-apps/api/core'
import type { RuntimeApi, RuntimeInfo } from '../contracts/runtime'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

export function createRuntimeApi(invokeCommand: Invoke = invoke): RuntimeApi {
  return {
    getInfo: () => invokeCommand<RuntimeInfo>('get_runtime_info')
  }
}

const browserPreviewInfo: RuntimeInfo = {
  productId: 'next-tauri',
  productName: 'MooTool Next Tauri',
  version: 'web-preview',
  platform: typeof navigator === 'undefined' ? 'browser' : navigator.platform || 'browser',
  architecture: 'browser',
  runtime: 'tauri'
}

export const runtimeApi: RuntimeApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createRuntimeApi()
  : { getInfo: async () => browserPreviewInfo }

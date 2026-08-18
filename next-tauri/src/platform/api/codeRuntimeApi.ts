import { Channel, invoke } from '@tauri-apps/api/core'
import type { CodeOutputEvent, CodeRunResult, CodeRuntimeApi, CodeRuntimeStatus } from '../contracts/codeRuntime'

export const codeRuntimeApi: CodeRuntimeApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__ ? {
  detect: () => invoke<CodeRuntimeStatus[]>('detect_code_runtimes'),
  run: (spec, onOutput) => { const output = new Channel<CodeOutputEvent>(); output.onmessage = onOutput; return invoke<CodeRunResult>('run_code', { spec, output }) },
  cancel: (requestId) => invoke<boolean>('cancel_code_run', { requestId })
} : {
  detect: async () => [{ id: 'java', available: false, command: 'java', version: '' }, { id: 'groovy', available: false, command: 'groovy', version: '' }, { id: 'python', available: false, command: 'python3', version: '' }, { id: 'node', available: false, command: 'node', version: '' }],
  run: async () => { throw new Error('浏览器预览不能运行本地代码，请使用 Tauri 桌面应用') },
  cancel: async () => false
}

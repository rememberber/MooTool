export type CodeRuntimeId = 'java' | 'groovy' | 'python' | 'node'

export interface CodeRuntimeStatus {
  id: CodeRuntimeId
  available: boolean
  command: string
  version: string
}

export interface RuntimeInfo {
  productId: 'next-tauri'
  productName: string
  version: string
  platform: string
  architecture: string
  runtime: 'tauri'
}

export interface RuntimeApi {
  getInfo(): Promise<RuntimeInfo>
  detectRuntimes(): Promise<CodeRuntimeStatus[]>
}

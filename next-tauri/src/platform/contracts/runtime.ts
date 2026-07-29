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
}

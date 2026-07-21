export type UpdateStatus = 'latest' | 'available'

export const updateProductIds = ['java', 'next-electron', 'next-tauri', 'next-macos-native'] as const

export type UpdateProductId = (typeof updateProductIds)[number]

export type UpdateTarget = {
  platform: string
  architecture: string
}

export type UpdateDownload = {
  fileName: string
  packageType: string
  url: string
  sha512: string
  size: number
}

export type UpdateCheckResult = {
  status: UpdateStatus
  productId: UpdateProductId
  productName: string
  currentVersion: string
  latestVersion: string
  releaseUrl: string
  releaseNotes: string
  target: UpdateTarget
  download: UpdateDownload | null
  checkedAt: string
}

export type UpdateCheckEvent =
  | { type: 'result'; result: UpdateCheckResult }
  | { type: 'error'; message: string }

export type UpdateDownloadStatus = 'idle' | 'available' | 'downloading' | 'ready' | 'error'

export type UpdateInstallMode = 'automatic' | 'manual'

export type UpdateDownloadState = {
  status: UpdateDownloadStatus
  installMode: UpdateInstallMode
  version: string | null
  fileName: string | null
  percent: number | null
  transferred: number | null
  total: number | null
  message: string | null
  releaseNotes: string | null
}

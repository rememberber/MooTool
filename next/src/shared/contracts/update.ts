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

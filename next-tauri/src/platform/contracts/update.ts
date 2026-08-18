export type ProductUpdateStatus = 'available' | 'upToDate' | 'inactive'

export interface ProductUpdateCheck {
  status: ProductUpdateStatus
  currentVersion: string
  latestVersion: string | null
  releaseNotes: string | null
  publishedAt: string | null
  releaseUrl: string | null
}

export type ProductUpdateEvent =
  | { event: 'started' }
  | {
      event: 'progress'
      data: {
        chunkLength: number
        downloadedBytes: number
        contentLength: number | null
      }
    }
  | { event: 'finished' }
  | { event: 'cancelled' }
  | { event: 'installed' }

export interface ProductUpdateApi {
  check(): Promise<ProductUpdateCheck>
  install(onEvent: (event: ProductUpdateEvent) => void): Promise<void>
  cancel(): Promise<boolean>
  relaunch(): Promise<void>
}

import { basename } from 'node:path'
import type { UpdateCheckResult, UpdateDownloadState } from '../../src/shared/contracts/update'

type UpdateProgress = {
  percent?: number
  transferred?: number
  total?: number
}

export type UpdateAdapter = {
  autoDownload: boolean
  autoInstallOnAppQuit: boolean
  allowDowngrade: boolean
  setFeedURL: (options: { provider: 'generic'; url: string; channel: string }) => void
  checkForUpdates: () => Promise<unknown>
  downloadUpdate: () => Promise<string[]>
  quitAndInstall: (isSilent?: boolean, isForceRunAfter?: boolean) => void
  on: (event: string, listener: (payload: unknown) => void) => unknown
}

const idleState: UpdateDownloadState = {
  status: 'idle',
  version: null,
  fileName: null,
  percent: null,
  transferred: null,
  total: null,
  message: null
}

export class UpdateManager {
  private state: UpdateDownloadState = idleState
  private activeResult: UpdateCheckResult | null = null
  private checkPromise: Promise<unknown> | null = null
  private downloadPromise: Promise<UpdateDownloadState> | null = null
  private metadataReady = false

  constructor(
    private readonly updater: UpdateAdapter,
    private readonly enabled: boolean,
    private readonly onStateChange: (state: UpdateDownloadState) => void
  ) {
    updater.autoInstallOnAppQuit = false
    updater.allowDowngrade = false
    this.registerEvents()
  }

  getState(): UpdateDownloadState {
    return { ...this.state }
  }

  setAutoDownload(enabled: boolean): void {
    this.updater.autoDownload = enabled
    if (enabled && this.enabled && this.activeResult && this.state.status === 'available' && this.metadataReady) {
      void this.download().catch((error) => this.fail(error))
    }
  }

  prepare(result: UpdateCheckResult, autoDownload: boolean): void {
    this.setAutoDownload(autoDownload)
    if (result.status !== 'available' || !result.download) {
      this.activeResult = null
      this.metadataReady = false
      this.publish(idleState)
      return
    }

    if (this.activeResult?.latestVersion === result.latestVersion
      && (this.state.status === 'downloading' || this.state.status === 'ready')) {
      return
    }

    this.activeResult = result
    this.metadataReady = false
    this.publish({
      status: 'available',
      version: result.latestVersion,
      fileName: result.download.fileName,
      percent: null,
      transferred: null,
      total: null,
      message: null
    })

    if (!this.enabled) return

    this.updater.setFeedURL({
      provider: 'generic',
      url: updateFeedDirectory(result.download.url),
      channel: updateMetadataChannel(result.target.architecture)
    })
    this.checkPromise = this.updater.checkForUpdates()
      .then((value) => {
        this.metadataReady = true
        return value
      })
      .catch((error) => {
        this.fail(error)
        throw error
      })
    void this.checkPromise.catch(() => undefined)
  }

  async download(): Promise<UpdateDownloadState> {
    if (this.downloadPromise) return this.downloadPromise
    this.downloadPromise = this.performDownload().finally(() => {
      this.downloadPromise = null
    })
    return this.downloadPromise
  }

  private async performDownload(): Promise<UpdateDownloadState> {
    if (!this.enabled) throw new Error('Updates can only be downloaded by a packaged application')
    if (!this.activeResult?.download) throw new Error('No compatible update download is available')
    if (this.getState().status === 'ready') return this.getState()
    if (this.checkPromise && !this.metadataReady) await this.checkPromise
    if (['downloading', 'ready'].includes(this.getState().status)) return this.getState()

    this.publish({ ...this.state, status: 'downloading', percent: this.state.percent ?? 0, message: null })
    await this.updater.downloadUpdate()
    return this.getState()
  }

  install(): void {
    if (!this.enabled) throw new Error('Updates can only be installed by a packaged application')
    if (this.state.status !== 'ready') throw new Error('The update installer is not ready')
    this.updater.quitAndInstall(true, true)
  }

  private registerEvents(): void {
    this.updater.on('update-available', () => {
      if (!this.activeResult?.download || this.state.status === 'downloading' || this.state.status === 'ready') return
      this.publish({ ...this.state, status: 'available', message: null })
    })
    this.updater.on('update-not-available', () => {
      if (!this.activeResult) return
      this.fail(new Error(`Update metadata does not contain version ${this.activeResult.latestVersion}`))
    })
    this.updater.on('download-progress', (payload) => {
      if (!this.activeResult) return
      const progress = isRecord(payload) ? payload as UpdateProgress : {}
      this.publish({
        ...this.state,
        status: 'downloading',
        percent: clampProgress(progress.percent),
        transferred: validByteCount(progress.transferred),
        total: validByteCount(progress.total),
        message: null
      })
    })
    this.updater.on('update-downloaded', (payload) => {
      if (!this.activeResult) return
      const downloadedFile = isRecord(payload) && typeof payload.downloadedFile === 'string'
        ? basename(payload.downloadedFile)
        : this.state.fileName
      this.publish({
        ...this.state,
        status: 'ready',
        fileName: downloadedFile,
        percent: 100,
        message: null
      })
    })
    this.updater.on('error', (payload) => {
      if (this.activeResult) this.fail(payload)
    })
  }

  private fail(error: unknown): void {
    this.publish({
      ...this.state,
      status: 'error',
      message: (error instanceof Error ? error.message : String(error)).slice(0, 500)
    })
  }

  private publish(state: UpdateDownloadState): void {
    this.state = { ...state }
    this.onStateChange(this.getState())
  }
}

export function updateFeedDirectory(downloadUrl: string): string {
  const url = new URL(downloadUrl)
  url.pathname = url.pathname.slice(0, url.pathname.lastIndexOf('/') + 1)
  url.search = ''
  url.hash = ''
  return url.toString()
}

export function updateMetadataChannel(architecture: string): string {
  const normalized = architecture.trim().toLowerCase()
  const channel = normalized === 'aarch64' ? 'arm64' : normalized === 'amd64' ? 'x64' : normalized
  if (!/^[a-z0-9-]+$/.test(channel)) throw new Error(`Invalid update architecture: ${architecture}`)
  return channel
}

function clampProgress(value: number | undefined): number | null {
  return typeof value === 'number' && Number.isFinite(value)
    ? Math.min(100, Math.max(0, Number(value.toFixed(1))))
    : null
}

function validByteCount(value: number | undefined): number | null {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? Math.round(value) : null
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

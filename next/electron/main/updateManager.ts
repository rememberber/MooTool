import { basename } from 'node:path'
import type { UpdateCheckResult, UpdateDownload, UpdateDownloadState, UpdateInstallMode } from '../../src/shared/contracts/update'

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

export type UpdateManagerOptions = {
  installMode?: UpdateInstallMode
  downloadFile?: (download: UpdateDownload, onProgress: (progress: UpdateProgress) => void) => Promise<string>
  openDownloadedFile?: (filePath: string) => Promise<void> | void
}

function idleState(installMode: UpdateInstallMode): UpdateDownloadState {
  return {
    status: 'idle',
    installMode,
    version: null,
    fileName: null,
    percent: null,
    transferred: null,
    total: null,
    message: null,
    releaseNotes: null
  }
}

export class UpdateManager {
  private state: UpdateDownloadState
  private activeResult: UpdateCheckResult | null = null
  private checkPromise: Promise<unknown> | null = null
  private downloadPromise: Promise<UpdateDownloadState> | null = null
  private metadataReady = false
  private downloadedFilePath: string | null = null
  private readonly installMode: UpdateInstallMode
  private readonly downloadFile: UpdateManagerOptions['downloadFile']
  private readonly openDownloadedFile: UpdateManagerOptions['openDownloadedFile']

  constructor(
    private readonly updater: UpdateAdapter,
    private readonly enabled: boolean,
    private readonly onStateChange: (state: UpdateDownloadState) => void,
    options: UpdateManagerOptions = {}
  ) {
    this.installMode = options.installMode ?? 'automatic'
    this.downloadFile = options.downloadFile
    this.openDownloadedFile = options.openDownloadedFile
    this.state = idleState(this.installMode)
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
      void this.download().catch(() => undefined)
    }
  }

  prepare(result: UpdateCheckResult, autoDownload: boolean): void {
    this.setAutoDownload(autoDownload)
    if (result.status !== 'available' || !result.download) {
      this.activeResult = null
      this.metadataReady = false
      this.downloadedFilePath = null
      this.publish(idleState(this.installMode))
      return
    }

    if (this.activeResult?.latestVersion === result.latestVersion
      && (this.state.status === 'downloading' || this.state.status === 'ready')) {
      return
    }

    this.activeResult = result
    this.metadataReady = false
    this.downloadedFilePath = null
    this.publish({
      status: 'available',
      installMode: this.installMode,
      version: result.latestVersion,
      fileName: result.download.fileName,
      percent: null,
      transferred: null,
      total: null,
      message: null,
      releaseNotes: result.releaseNotes || null
    })

    if (!this.enabled) return

    if (this.installMode === 'manual') {
      this.metadataReady = true
      if (autoDownload) void this.download().catch(() => undefined)
      return
    }

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
    this.downloadPromise = this.performDownload()
      .catch((error) => {
        this.fail(error)
        throw error
      })
      .finally(() => {
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
    if (this.installMode === 'manual') {
      if (!this.downloadFile) throw new Error('Downloading a manual update is unavailable')
      const downloadedFilePath = await this.downloadFile(this.activeResult.download, (progress) => {
        this.publish({
          ...this.state,
          status: 'downloading',
          percent: clampProgress(progress.percent),
          transferred: validByteCount(progress.transferred),
          total: validByteCount(progress.total),
          message: null
        })
      })
      this.downloadedFilePath = downloadedFilePath
      this.publish({
        ...this.state,
        status: 'ready',
        fileName: basename(downloadedFilePath),
        percent: 100,
        message: null
      })
    } else {
      await this.updater.downloadUpdate()
    }
    return this.getState()
  }

  async install(): Promise<void> {
    if (!this.enabled) throw new Error('Updates can only be installed by a packaged application')
    if (this.state.status !== 'ready') throw new Error('The update installer is not ready')
    if (this.installMode === 'manual') {
      if (!this.downloadedFilePath) throw new Error('The downloaded update file is unavailable')
      if (!this.openDownloadedFile) throw new Error('Opening the downloaded update is unavailable')
      await this.openDownloadedFile(this.downloadedFilePath)
      return
    }
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
      const downloadedFilePath = isRecord(payload) && typeof payload.downloadedFile === 'string'
        ? payload.downloadedFile
        : null
      this.downloadedFilePath = downloadedFilePath
      const downloadedFile = downloadedFilePath ? basename(downloadedFilePath) : this.state.fileName
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
    this.state = {
      ...state,
      releaseNotes: this.activeResult?.releaseNotes || null
    }
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

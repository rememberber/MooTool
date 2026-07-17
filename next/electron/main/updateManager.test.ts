// @vitest-environment node
import { EventEmitter } from 'node:events'
import { describe, expect, it, vi } from 'vitest'
import type { UpdateCheckResult, UpdateDownloadState } from '../../src/shared/contracts/update'
import { UpdateManager, updateFeedDirectory, type UpdateAdapter } from './updateManager'

describe('UpdateManager', () => {
  it('silently downloads by default and publishes a ready state', async () => {
    const adapter = new FakeUpdater()
    const states: UpdateDownloadState[] = []
    const manager = new UpdateManager(adapter.asAdapter(), true, (state) => states.push(state))

    manager.prepare(updateResult(), true)
    await adapter.finished

    expect(adapter.autoDownload).toBe(true)
    expect(adapter.feedUrl).toBe('https://github.com/rememberber/MooTool/releases/download/next-electron-v1.1.0/')
    expect(states.map((state) => state.status)).toEqual(expect.arrayContaining(['available', 'downloading', 'ready']))
    expect(manager.getState()).toMatchObject({ status: 'ready', version: '1.1.0', percent: 100 })
  })

  it('waits for a manual download when automatic downloads are disabled', async () => {
    const adapter = new FakeUpdater()
    const manager = new UpdateManager(adapter.asAdapter(), true, () => undefined)

    manager.prepare(updateResult(), false)
    await adapter.checked
    expect(manager.getState().status).toBe('available')
    expect(adapter.downloadCalls).toBe(0)

    await manager.download()
    expect(adapter.downloadCalls).toBe(1)
    expect(manager.getState().status).toBe('ready')
  })

  it('only installs a fully downloaded update', async () => {
    const adapter = new FakeUpdater()
    const manager = new UpdateManager(adapter.asAdapter(), true, () => undefined)
    expect(() => manager.install()).toThrow('not ready')

    manager.prepare(updateResult(), true)
    await adapter.finished
    manager.install()

    expect(adapter.quitAndInstall).toHaveBeenCalledWith(true, true)
  })
})

it('derives a generic updater directory from the selected release asset', () => {
  expect(updateFeedDirectory('https://example.test/releases/v1/MooTool-1.1.0.dmg?token=ignored')).toBe('https://example.test/releases/v1/')
})

class FakeUpdater extends EventEmitter {
  autoDownload = true
  autoInstallOnAppQuit = true
  allowDowngrade = true
  feedUrl = ''
  downloadCalls = 0
  checked: Promise<void> = Promise.resolve()
  finished: Promise<void> = Promise.resolve()
  quitAndInstall = vi.fn()

  setFeedURL(options: { provider: 'generic'; url: string }): void {
    this.feedUrl = options.url
  }

  checkForUpdates(): Promise<void> {
    this.checked = Promise.resolve().then(() => {
      this.emit('update-available', {})
      if (this.autoDownload) this.finished = this.downloadUpdate().then(() => undefined)
    })
    return this.checked
  }

  async downloadUpdate(): Promise<string[]> {
    this.downloadCalls += 1
    this.emit('download-progress', { percent: 47.25, transferred: 47, total: 100 })
    this.emit('update-downloaded', { downloadedFile: '/tmp/MooTool-1.1.0.zip' })
    return ['/tmp/MooTool-1.1.0.zip']
  }

  asAdapter(): UpdateAdapter {
    return this as unknown as UpdateAdapter
  }
}

function updateResult(): UpdateCheckResult {
  return {
    status: 'available',
    productId: 'next-electron',
    productName: 'MooTool Next Electron',
    currentVersion: '1.0.0',
    latestVersion: '1.1.0',
    releaseUrl: 'https://github.com/rememberber/MooTool/releases/tag/next-electron-v1.1.0',
    releaseNotes: 'Update',
    target: { platform: 'darwin', architecture: 'arm64' },
    download: {
      fileName: 'MooTool-Next-Electron-1.1.0-mac-arm64.dmg',
      packageType: 'dmg',
      url: 'https://github.com/rememberber/MooTool/releases/download/next-electron-v1.1.0/MooTool-Next-Electron-1.1.0-mac-arm64.dmg'
    },
    checkedAt: '2026-07-17T00:00:00.000Z'
  }
}

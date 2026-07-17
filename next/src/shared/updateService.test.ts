// @vitest-environment node
import { readFile } from 'node:fs/promises'
import { describe, expect, it, vi } from 'vitest'
import { compareVersions, type UpdateClientIdentity, UpdateService } from '../../electron/main/updateService'

const macArmIdentity = {
  productId: 'next-electron',
  platform: 'darwin',
  architecture: 'arm64'
} satisfies UpdateClientIdentity

describe('UpdateService', () => {
  it('isolates the Electron product and selects the exact macOS architecture', async () => {
    const fetcher = vi.fn(async () => response(manifest([
      release('1.7.8', 'Current', 'old'),
      release('1.8.0', 'Next', 'rewritten', [
        asset('darwin', 'x64', 'dmg', 'MooTool-intel.dmg', 1),
        asset('darwin', 'arm64', 'zip', 'MooTool-arm64.zip', 20),
        asset('darwin', 'arm64', 'dmg', 'MooTool-arm64.dmg', 10)
      ]),
      release('1.7.9', 'Patch', 'fixed')
    ], {
      java: {
        displayName: 'MooTool Java',
        status: 'active',
        releases: [release('99.0.0', 'Java only', 'must be ignored')]
      }
    })))
    const result = await new UpdateService('https://feed.test', macArmIdentity, fetcher).check('1.7.8')

    expect(result).toMatchObject({
      status: 'available',
      productId: 'next-electron',
      productName: 'MooTool Next Electron',
      currentVersion: '1.7.8',
      latestVersion: '1.8.0',
      target: { platform: 'darwin', architecture: 'arm64' },
      download: { fileName: 'MooTool-arm64.dmg', packageType: 'dmg' }
    })
    expect(result.releaseNotes).toContain('1.7.9\nPatch\nfixed')
    expect(result.releaseNotes).toContain('1.8.0\nNext\nrewritten')
    expect(result.releaseNotes).not.toContain('Current')
    expect(result.releaseNotes).not.toContain('Java only')
  })

  it('reports the current Electron version as latest regardless of other products', async () => {
    const result = await new UpdateService('https://feed.test', macArmIdentity, async () => response(manifest([
      release('v1.7.8', 'Current', '')
    ], {
      java: { displayName: 'MooTool Java', status: 'active', releases: [release('20.0.0', 'Java', '')] }
    }))).check('v1.7.8')

    expect(result.status).toBe('latest')
    expect(result.latestVersion).toBe('1.7.8')
    expect(result.releaseNotes).toBe('')
    expect(result.download).toBeNull()
  })

  it('normalizes architecture aliases and prefers the Windows installer', async () => {
    const result = await new UpdateService('https://feed.test', {
      productId: 'next-electron',
      platform: 'win32',
      architecture: 'amd64'
    }, async () => response(manifest([
      release('2.0.0', 'Windows', '', [
        asset('win32', 'x64', 'portable', 'MooTool-portable.exe', 20),
        asset('win32', 'x64', 'nsis', 'MooTool-setup.exe', 10)
      ])
    ]))).check('1.0.0')

    expect(result.target.architecture).toBe('x64')
    expect(result.download?.fileName).toBe('MooTool-setup.exe')
  })

  it('returns the product release page when no package matches the device', async () => {
    const result = await new UpdateService('https://feed.test', {
      productId: 'next-electron',
      platform: 'linux',
      architecture: 'arm64'
    }, async () => response(manifest([
      release('2.0.0', 'Linux', '', [asset('linux', 'x64', 'appimage', 'MooTool.AppImage', 10)])
    ]))).check('1.0.0')

    expect(result.status).toBe('available')
    expect(result.download).toBeNull()
    expect(result.releaseUrl).toBe('https://github.com/rememberber/MooTool/releases/tag/next-electron-v2.0.0')
  })

  it('accepts the repository update manifest for the current Electron channel', async () => {
    const raw = await readFile(new URL('../../../update-manifest.json', import.meta.url), 'utf8')
    const result = await new UpdateService('https://feed.test', macArmIdentity, async () => ({
      ok: true,
      status: 200,
      text: async () => raw
    })).check('0.9.9')

    expect(result.latestVersion).toBe('1.0.0')
    expect(result.download?.fileName).toBe('MooTool-Next-Electron-1.0.0-mac-arm64.dmg')
  })

  it('rejects invalid, insecure, oversized, and unsuccessful responses', async () => {
    await expect(new UpdateService('', macArmIdentity, async () => ({ ok: false, status: 503, text: async () => '' })).check('1.0.0')).rejects.toThrow('HTTP 503')
    await expect(new UpdateService('', macArmIdentity, async () => ({ ok: true, status: 200, text: async () => '{' })).check('1.0.0')).rejects.toThrow('valid JSON')
    await expect(new UpdateService('', macArmIdentity, async () => ({ ok: true, status: 200, text: async () => 'x'.repeat(2 * 1024 * 1024 + 1) })).check('1.0.0')).rejects.toThrow('Invalid update response')
    await expect(new UpdateService('', macArmIdentity, async () => response({ currentVersion: '9.9.9' })).check('1.0.0')).rejects.toThrow('Unsupported update manifest')
    await expect(new UpdateService('', macArmIdentity, async () => response(manifest([
      release('2.0.0', 'Unsafe', '', [{ ...asset('darwin', 'arm64', 'dmg', 'Unsafe.dmg', 1), url: 'http://example.test/Unsafe.dmg' }])
    ]))).check('1.0.0')).rejects.toThrow('must use HTTPS')
  })
})

describe('compareVersions', () => {
  it('compares semantic versions and prereleases', () => {
    expect(compareVersions('1.7.9', '1.7.8')).toBe(1)
    expect(compareVersions('2.0.0-beta.2', '2.0.0-beta.1')).toBe(1)
    expect(compareVersions('2.0.0', '2.0.0-beta.2')).toBe(1)
    expect(compareVersions('v1.7', '1.7.0')).toBe(0)
  })
})

function manifest(releases: unknown[], additionalProducts: Record<string, unknown> = {}): unknown {
  return {
    schemaVersion: 1,
    products: {
      ...additionalProducts,
      'next-electron': {
        displayName: 'MooTool Next Electron',
        status: 'active',
        releases
      }
    }
  }
}

function release(version: string, title: string, notes: string, assets: unknown[] = []): unknown {
  return {
    version,
    title,
    notes,
    releaseUrl: `https://github.com/rememberber/MooTool/releases/tag/next-electron-v${version.replace(/^v/, '')}`,
    assets
  }
}

function asset(platform: string, architecture: string, packageType: string, fileName: string, priority: number): Record<string, unknown> {
  return {
    platform,
    architecture,
    packageType,
    priority,
    fileName,
    url: `https://downloads.example.test/${fileName}`
  }
}

function response(value: unknown): Pick<Response, 'ok' | 'status' | 'text'> {
  return { ok: true, status: 200, text: async () => JSON.stringify(value) }
}

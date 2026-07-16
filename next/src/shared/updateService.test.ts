// @vitest-environment node
import { describe, expect, it, vi } from 'vitest'
import { compareVersions, UpdateService } from '../../electron/main/updateService'

describe('UpdateService', () => {
  it('detects a newer version and returns only newer release notes', async () => {
    const fetcher = vi.fn(async () => response({
      currentVersion: 'v1.8.0',
      versionDetailList: [
        { version: 'v1.7.8', title: 'Current', log: 'old' },
        { version: 'v1.7.9', title: 'Patch', log: 'fixed' },
        { version: 'v1.8.0', title: 'Next', log: 'rewritten' }
      ]
    }))
    const result = await new UpdateService('https://feed.test', 'https://release.test', fetcher).check('1.7.8')

    expect(result).toMatchObject({ status: 'available', currentVersion: '1.7.8', latestVersion: '1.8.0', releaseUrl: 'https://release.test' })
    expect(result.releaseNotes).toContain('1.7.9\nPatch\nfixed')
    expect(result.releaseNotes).toContain('1.8.0\nNext\nrewritten')
    expect(result.releaseNotes).not.toContain('Current')
  })

  it('reports the current version as latest', async () => {
    const result = await new UpdateService('https://feed.test', 'https://release.test', async () => response({ currentVersion: 'v1.7.8' })).check('v1.7.8')
    expect(result.status).toBe('latest')
    expect(result.releaseNotes).toBe('')
  })

  it('rejects invalid, oversized, and unsuccessful responses', async () => {
    await expect(new UpdateService('', '', async () => ({ ok: false, status: 503, text: async () => '' })).check('1.0.0')).rejects.toThrow('HTTP 503')
    await expect(new UpdateService('', '', async () => ({ ok: true, status: 200, text: async () => '{' })).check('1.0.0')).rejects.toThrow('valid JSON')
    await expect(new UpdateService('', '', async () => ({ ok: true, status: 200, text: async () => 'x'.repeat(2 * 1024 * 1024 + 1) })).check('1.0.0')).rejects.toThrow('Invalid update response')
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

function response(value: unknown): Pick<Response, 'ok' | 'status' | 'text'> {
  return { ok: true, status: 200, text: async () => JSON.stringify(value) }
}

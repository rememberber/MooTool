import { createHash } from 'node:crypto'
import { mkdir, mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { gzipSync } from 'node:zlib'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ComponentManifest } from '../../src/shared/contracts/optionalComponent'
import { OptionalOcrComponent, parseComponentManifest, unpackComponent } from './optionalOcrComponent'
import { downloadUpdateFile } from './updateDownloader'

const digest = (value: Buffer | string) => createHash('sha256').update(value).digest('hex')
const content = Buffer.from('verified OCR runtime fixture')
const payload = gzipSync(content)
const version = digest(content)
const fileName = `MooTool-OCR-${version}-linux-x64.bin.gz`
const fixture: ComponentManifest = {
  schema: 1, target: 'linux-x64', version,
  files: [{ path: 'manifest.json', bytes: content.length, sha256: version }],
  download: { fileName, packageType: 'component', size: payload.length, sha512: createHash('sha512').update(payload).digest('base64'), url: `https://github.com/rememberber/MooTool/releases/download/next-electron-v1.3.3/${fileName}` }
}
let directory: string
beforeEach(async () => { directory = await mkdtemp(join(tmpdir(), 'mootool-optional-component-')) })
afterEach(async () => { await rm(directory, { recursive: true, force: true }) })
const download = (body = payload) => vi.fn((descriptor, cache, progress) => downloadUpdateFile(descriptor, cache, progress, async () => new Response(new Uint8Array(body))))
const verify = vi.fn(async (path: string) => { expect(await readFile(join(path, 'manifest.json'))).toEqual(content) })

describe('optional OCR components', () => {
  it('downloads only on request, shares concurrent requests, verifies and activates after restart', async () => {
    const fetcher = download()
    const service = new OptionalOcrComponent(fixture, directory, fetcher, verify)
    await service.initialize()
    expect(service.status().status).toBe('missing'); expect(fetcher).not.toHaveBeenCalled()
    const pending = service.install()
    expect(service.install()).toBe(pending)
    expect(service.status().status).toBe('downloading')
    expect((await pending).status).toBe('restart')
    expect(fetcher).toHaveBeenCalledTimes(1)
    expect(await readFile(join(service.directory, '.complete'), 'utf8')).toBe(version)
    expect((await readdir(directory)).some(name => name.startsWith('.ocr-download-'))).toBe(false)
    const restarted = new OptionalOcrComponent(fixture, directory, fetcher, verify)
    await restarted.initialize()
    expect(restarted.status().status).toBe('installed')
    await restarted.install(); expect(fetcher).toHaveBeenCalledTimes(1)
  })
  it('cleans a failed download and supports retry without activating partial files', async () => {
    const fetcher = download(Buffer.from('corrupted bundle'))
    const service = new OptionalOcrComponent(fixture, directory, fetcher, verify)
    expect((await service.install()).status).toBe('error')
    expect(await readdir(directory)).toEqual([])
    fetcher.mockImplementation(download())
    expect((await service.install()).status).toBe('restart')
  })
  it('rejects native/model verification failure before installing a component', async () => {
    const service = new OptionalOcrComponent(fixture, directory, download(), async () => { throw new Error('Invalid OCR bundle') })
    expect((await service.install()).status).toBe('error')
    expect(await readdir(directory)).toEqual([])
  })
  it('cleans interrupted staging and offers repair for damaged installations', async () => {
    const service = new OptionalOcrComponent(fixture, directory, download(), verify)
    await service.install()
    await writeFile(join(service.directory, '.complete'), 'invalid')
    await mkdir(join(directory, '.ocr-download-interrupted'))
    await writeFile(join(directory, '.ocr-download-interrupted/partial'), 'incomplete')
    const restarted = new OptionalOcrComponent(fixture, directory, download(), verify)
    await restarted.initialize()
    expect(restarted.status().status).toBe('missing')
    expect(await readdir(directory)).not.toContain('.ocr-download-interrupted')
    expect((await restarted.install()).status).toBe('restart')
  })
  it('rejects truncated, oversized and substituted decompressed payloads', async () => {
    for (const value of [content.subarray(1), Buffer.concat([content, Buffer.from('extra')]), Buffer.alloc(content.length)]) {
      const path = join(directory, 'bundle.gz')
      await writeFile(path, gzipSync(value))
      const output = await mkdtemp(join(directory, 'extract-'))
      await expect(unpackComponent(path, output, fixture)).rejects.toThrow()
    }
  })
  it('validates target, download origin, checksums, file paths and resource limits', () => {
    expect(parseComponentManifest(fixture, 'linux-x64')).toEqual(fixture)
    for (const change of [
      { target: 'win32-x64' },
      { download: { ...fixture.download, url: 'https://example.com/runtime.gz' } },
      { download: { ...fixture.download, size: 300 * 1024 * 1024 } },
      { files: [{ ...fixture.files[0], path: '../escape' }] },
      { files: [{ ...fixture.files[0], path: '/escape' }] },
      { files: [fixture.files[0], fixture.files[0]] },
      { files: [{ ...fixture.files[0], sha256: 'bad' }] }
    ]) expect(() => parseComponentManifest({ ...fixture, ...change }, 'linux-x64')).toThrow()
  })
})

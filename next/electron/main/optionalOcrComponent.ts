import { createHash } from 'node:crypto'
import { createReadStream } from 'node:fs'
import { mkdir, mkdtemp, open, readFile, readdir, rename, rm, stat, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { createGunzip } from 'node:zlib'
import type { ComponentManifest, ComponentState } from '../../src/shared/contracts/optionalComponent'
import { downloadUpdateFile } from './updateDownloader'
import { verifyOcrResources } from './ocrResources'

// Metadata is shipped inside the app, never obtained from the download server.
export function parseComponentManifest(value: unknown, target: string): ComponentManifest {
  const manifest = value as ComponentManifest
  if (!manifest || manifest.schema !== 1 || manifest.target !== target || !/^[a-f0-9]{64}$/.test(manifest.version)
    || !Array.isArray(manifest.files) || !manifest.files.length || manifest.files.length > 257) throw new Error('Invalid component manifest')
  const download = manifest.download
  if (!download || download.fileName !== `MooTool-OCR-${manifest.version}-${target}.bin.gz`
    || !Number.isSafeInteger(download.size) || download.size < 1 || download.size > 256 * 1024 * 1024
    || !/^[A-Za-z0-9+/]{86}==$/.test(download.sha512)
    || typeof download.url !== 'string' || !/^https:\/\/github\.com\/rememberber\/MooTool\/releases\/download\/next-electron-v\d+\.\d+\.\d+(?:-[\w.-]+)?\/[^/?#]+$/.test(download.url)
    || download.url.slice(download.url.lastIndexOf('/') + 1) !== download.fileName) throw new Error('Invalid component download')
  const seen = new Set<string>(); let total = 0
  for (const file of manifest.files) {
    if (!file || typeof file.path !== 'string' || file.path.length > 200 || !/^[\w@./-]+$/.test(file.path)
      || file.path.split('/').some(part => !part || part === '.' || part === '..') || seen.has(file.path)
      || !Number.isSafeInteger(file.bytes) || file.bytes < 1 || file.bytes > 128 * 1024 * 1024 || !/^[a-f0-9]{64}$/.test(file.sha256)) throw new Error('Invalid component file')
    seen.add(file.path); total += file.bytes
  }
  if (total > 256 * 1024 * 1024 || !seen.has('manifest.json')) throw new Error('Invalid component size')
  return manifest
}

// A gzip stream of concatenated files in manifest order. Paths come exclusively
// from the app's trusted manifest; the downloaded data cannot introduce entries.
export async function unpackComponent(bundle: string, root: string, manifest: ComponentManifest): Promise<void> {
  const source = createReadStream(bundle)
  const stream = source.pipe(createGunzip())
  source.on('error', error => stream.destroy(error))
  let index = 0, received = 0, file: Awaited<ReturnType<typeof open>> | undefined
  let hash = createHash('sha256')
  try {
    for await (const chunk of stream) {
      let offset = 0
      while (offset < chunk.length) {
        const entry = manifest.files[index]
        if (!entry) throw new Error('Component exceeds expected size')
        if (!file) { await mkdir(dirname(join(root, entry.path)), { recursive: true }); file = await open(join(root, entry.path), 'wx', 0o600) }
        const part = chunk.subarray(offset, offset + Math.min(entry.bytes - received, chunk.length - offset))
        let written = 0
        while (written < part.length) {
          const result = await file.write(part, written, part.length - written)
          if (!result.bytesWritten) throw new Error('Could not write component')
          written += result.bytesWritten
        }
        hash.update(part); received += part.length; offset += part.length
        if (received === entry.bytes) {
          await file.close(); file = undefined
          if (hash.digest('hex') !== entry.sha256) throw new Error('Component checksum mismatch')
          index++; received = 0; hash = createHash('sha256')
        }
      }
    }
    if (index !== manifest.files.length || received) throw new Error('Incomplete component')
  } finally { source.destroy(); stream.destroy(); await file?.close() }
}

export class OptionalOcrComponent {
  readonly directory: string
  private state: ComponentState
  private pending: Promise<ComponentState> | null = null
  private initialization: Promise<void> | null = null
  constructor(readonly manifest: ComponentManifest, private readonly cache: string,
    private readonly download = downloadUpdateFile, private readonly verify = verifyOcrResources) {
    this.directory = join(cache, manifest.version, manifest.target)
    this.state = { status: 'missing', percent: 0, total: manifest.download.size }
  }
  initialize(): Promise<void> { return this.initialization ??= this.probeInstallation() }
  private async probeInstallation(): Promise<void> {
    for (const entry of await readdir(this.cache).catch(() => [])) {
      if (entry.startsWith('.ocr-download-')) await rm(join(this.cache, entry), { recursive: true, force: true }).catch(() => {})
    }
    try {
      if ((await stat(join(this.directory, '.complete'))).size !== 64 || await readFile(join(this.directory, '.complete'), 'utf8') !== this.manifest.version) return
      const [platform, arch] = this.manifest.target.split('-')
      await this.verify(this.directory, platform as NodeJS.Platform, arch as NodeJS.Architecture)
      this.state.status = 'installed'
    } catch { /* Missing or damaged components can be downloaded again. */ }
  }
  status(): ComponentState { return { ...this.state } }
  install(): Promise<ComponentState> {
    if (this.pending) return this.pending
    if (this.state.status === 'installed' || this.state.status === 'restart') return Promise.resolve(this.status())
    this.state = { ...this.state, status: 'downloading', percent: 0 }
    this.pending = this.performInstall().finally(() => { this.pending = null })
    return this.pending
  }
  private async performInstall(): Promise<ComponentState> {
    let staging: string | undefined
    try {
      await mkdir(this.cache, { recursive: true })
      staging = await mkdtemp(join(this.cache, '.ocr-download-'))
      const bundle = await this.download(this.manifest.download, staging, progress => { this.state.percent = progress.percent })
      const extracted = join(staging, 'files')
      await unpackComponent(bundle, extracted, this.manifest)
      const [platform, arch] = this.manifest.target.split('-')
      await this.verify(extracted, platform as NodeJS.Platform, arch as NodeJS.Architecture)
      await writeFile(join(extracted, '.complete'), this.manifest.version)
      await mkdir(dirname(this.directory), { recursive: true })
      await rm(this.directory, { recursive: true, force: true })
      await rename(extracted, this.directory)
      this.state = { ...this.state, status: 'restart', percent: 100 }
    } catch { this.state = { ...this.state, status: 'error', percent: 0 } }
    finally { if (staging) await rm(staging, { recursive: true, force: true }).catch(() => {}) }
    return this.status()
  }
}

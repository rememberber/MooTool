import { nativeImage } from 'electron'
import { copyFile, mkdir, readdir, rename, rm, stat, writeFile } from 'node:fs/promises'
import { basename, extname, join, parse } from 'node:path'
import type { ImageAsset, ImageAssetSummary, RenameImageAssetInput, SaveImageAssetInput } from '../../src/shared/contracts/images'

const imageExtensions = new Set(['.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'])

export class ImageRepository {
  constructor(private readonly root: string) {}

  async list(): Promise<ImageAssetSummary[]> {
    await this.ensureRoot()
    const entries = await readdir(this.root, { withFileTypes: true })
    const records = await Promise.all(entries.filter((entry) => entry.isFile() && imageExtensions.has(extname(entry.name).toLowerCase())).map((entry) => this.summary(entry.name)))
    return records.sort((left, right) => right.modifiedTime.localeCompare(left.modifiedTime))
  }

  async read(name: string): Promise<ImageAsset> {
    const summary = await this.summary(name)
    const image = nativeImage.createFromPath(this.resolveName(name))
    if (image.isEmpty()) throw new Error('Unable to read image')
    return { ...summary, dataUrl: image.toDataURL() }
  }

  async import(paths: string[]): Promise<ImageAssetSummary[]> {
    await this.ensureRoot()
    const imported: ImageAssetSummary[] = []
    for (const source of paths) {
      const extension = extname(source).toLowerCase()
      if (!imageExtensions.has(extension)) continue
      const image = nativeImage.createFromPath(source)
      if (image.isEmpty()) continue
      const name = await this.uniqueName(basename(source))
      await copyFile(source, this.resolveName(name))
      imported.push(await this.summary(name))
    }
    return imported
  }

  async save(input: SaveImageAssetInput): Promise<ImageAssetSummary> {
    await this.ensureRoot()
    const image = nativeImage.createFromDataURL(input.dataUrl)
    if (image.isEmpty()) throw new Error('Invalid image data')
    const requested = sanitizeImageName(input.name)
    const extension = extname(requested).toLowerCase()
    const name = extension === '.jpg' || extension === '.jpeg' ? requested : `${parse(requested).name}.png`
    const bytes = extension === '.jpg' || extension === '.jpeg' ? image.toJPEG(92) : image.toPNG()
    await writeFile(this.resolveName(name), bytes)
    return this.summary(name)
  }

  async rename(input: RenameImageAssetInput): Promise<ImageAssetSummary> {
    const source = this.resolveName(input.name)
    const sourceExtension = extname(input.name).toLowerCase() || '.png'
    const requested = sanitizeImageName(input.nextName)
    const nextName = extname(requested) ? requested : `${requested}${sourceExtension}`
    await rename(source, this.resolveName(nextName))
    return this.summary(nextName)
  }

  async delete(names: string[]): Promise<void> {
    await Promise.all(names.map((name) => rm(this.resolveName(name), { force: true })))
  }

  async export(names: string[], directory: string): Promise<void> {
    await mkdir(directory, { recursive: true })
    await Promise.all(names.map((name) => copyFile(this.resolveName(name), join(directory, sanitizeImageName(name)))))
  }

  pathFor(name: string): string {
    return this.resolveName(name)
  }

  private async summary(name: string): Promise<ImageAssetSummary> {
    const path = this.resolveName(name)
    const fileStat = await stat(path)
    const image = nativeImage.createFromPath(path)
    if (image.isEmpty()) throw new Error(`Unable to read image: ${name}`)
    const size = image.getSize()
    return { name: sanitizeImageName(name), size: fileStat.size, width: size.width, height: size.height, modifiedTime: fileStat.mtime.toISOString() }
  }

  private async uniqueName(requested: string): Promise<string> {
    const safe = sanitizeImageName(requested)
    const parts = parse(safe)
    let candidate = safe
    let index = 2
    while (await exists(this.resolveName(candidate))) {
      candidate = `${parts.name}-${index}${parts.ext}`
      index += 1
    }
    return candidate
  }

  private resolveName(name: string): string {
    const safe = sanitizeImageName(name)
    const path = join(this.root, safe)
    if (basename(path) !== safe) throw new Error('Invalid image name')
    return path
  }

  private async ensureRoot(): Promise<void> {
    await mkdir(this.root, { recursive: true })
  }
}

function sanitizeImageName(value: string): string {
  const name = basename(String(value || '').trim()).replace(/[^a-zA-Z0-9._\-\u4e00-\u9fff\u3040-\u30ff]/g, '_').slice(0, 160)
  if (!name || name === '.' || name === '..') throw new Error('Invalid image name')
  const extension = extname(name).toLowerCase()
  if (extension && !imageExtensions.has(extension)) throw new Error('Unsupported image format')
  return name
}

async function exists(path: string): Promise<boolean> {
  try { await stat(path); return true } catch { return false }
}

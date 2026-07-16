import { copyFile, lstat, mkdir, readFile, readdir, realpath, rename, rm, stat, writeFile } from 'node:fs/promises'
import { basename, dirname, extname, isAbsolute, relative, resolve, sep } from 'node:path'
import ignore, { type Ignore } from 'ignore'
import type {
  JsonVaultFile,
  JsonVaultListInput,
  JsonVaultNode,
  MoveJsonVaultEntryInput,
  RenameJsonVaultEntryInput,
  SaveJsonVaultFileInput
} from '../../src/shared/contracts/jsonVault'

const maxFileSize = 20 * 1024 * 1024
const maxEntries = 2000
const maxDepth = 16

export class JsonVaultRepository {
  constructor(private readonly rootDirectory: string) {}

  async list(input: JsonVaultListInput = {}): Promise<JsonVaultNode[]> {
    const root = await this.ensureRoot()
    const matcher = input.hideIgnored ? await this.loadIgnoreMatcher(root) : null
    return this.listDirectory(root, root, 0, { value: 0 }, matcher, input.sort ?? 'name')
  }

  async read(relativePath: string): Promise<JsonVaultFile> {
    const root = await this.ensureRoot()
    const target = await this.resolveExisting(root, normalizeJsonPath(relativePath), 'file')
    const fileStat = await stat(target)
    if (fileStat.size > maxFileSize) throw new Error('Vault file exceeds 20 MB limit')
    return {
      relativePath: toPortablePath(relative(root, target)),
      content: await readFile(target, 'utf8'),
      modifiedAt: fileStat.mtime.toISOString()
    }
  }

  async save(input: SaveJsonVaultFileInput): Promise<JsonVaultFile> {
    if (typeof input.content !== 'string' || Buffer.byteLength(input.content, 'utf8') > maxFileSize) {
      throw new Error('Invalid Vault file content')
    }
    const root = await this.ensureRoot()
    const normalizedPath = normalizeJsonPath(input.relativePath)
    const target = resolve(root, ...splitPath(normalizedPath))
    assertInsideRoot(root, target)
    await mkdir(dirname(target), { recursive: true })
    await this.assertRealDirectoryInside(root, dirname(target))
    if (await exists(target)) {
      const targetStat = await lstat(target)
      if (targetStat.isSymbolicLink() || !targetStat.isFile()) throw new Error('Invalid Vault file')
    }
    await writeFile(target, input.content, 'utf8')
    return this.read(normalizedPath)
  }

  async createFolder(relativePath: string): Promise<string> {
    const root = await this.ensureRoot()
    const normalized = normalizeDirectoryPath(relativePath)
    const target = resolve(root, ...splitPath(normalized))
    assertInsideRoot(root, target)
    await mkdir(target, { recursive: false })
    await this.assertRealDirectoryInside(root, target)
    return normalized
  }

  async renameEntry(input: RenameJsonVaultEntryInput): Promise<string> {
    const root = await this.ensureRoot()
    const sourcePath = normalizeEntryPath(input.relativePath)
    const source = await this.resolveExisting(root, sourcePath)
    const sourceStat = await lstat(source)
    const name = sanitizeName(input.name, sourceStat.isFile())
    const target = resolve(dirname(source), sourceStat.isFile() ? `${name}.json` : name)
    assertInsideRoot(root, target)
    if (await exists(target)) throw new Error('An entry with that name already exists')
    await rename(source, target)
    return toPortablePath(relative(root, target))
  }

  async moveEntry(input: MoveJsonVaultEntryInput): Promise<string> {
    const root = await this.ensureRoot()
    const sourcePath = normalizeEntryPath(input.relativePath)
    const targetDirectoryPath = normalizeDirectoryPath(input.targetDirectory, true)
    if (targetDirectoryPath === sourcePath || targetDirectoryPath.startsWith(`${sourcePath}/`)) {
      throw new Error('Cannot move a folder into itself')
    }
    const source = await this.resolveExisting(root, sourcePath)
    const targetDirectory = resolve(root, ...splitPath(targetDirectoryPath))
    await mkdir(targetDirectory, { recursive: true })
    await this.assertRealDirectoryInside(root, targetDirectory)
    const target = resolve(targetDirectory, basename(source))
    if (await exists(target)) throw new Error('Target folder already contains this entry')
    await rename(source, target)
    return toPortablePath(relative(root, target))
  }

  async duplicate(relativePath: string): Promise<JsonVaultFile> {
    const root = await this.ensureRoot()
    const normalized = normalizeJsonPath(relativePath)
    const source = await this.resolveExisting(root, normalized, 'file')
    const parent = toPortablePath(dirname(normalized)).replace(/^\.$/, '')
    const title = basename(normalized, extname(normalized))
    const targetPath = await this.uniqueJsonPath(root, parent, `${title} Copy`)
    const target = resolve(root, ...splitPath(targetPath))
    await copyFile(source, target)
    return this.read(targetPath)
  }

  async delete(relativePath: string): Promise<void> {
    const root = await this.ensureRoot()
    const normalized = normalizeEntryPath(relativePath)
    const target = await this.resolveExisting(root, normalized)
    const targetStat = await lstat(target)
    if (targetStat.isDirectory() && (await readdir(target)).length > 0) {
      throw new Error('Folder must be empty before it can be deleted')
    }
    await rm(target, { force: true, recursive: targetStat.isDirectory() })
  }

  private async ensureRoot(): Promise<string> {
    await mkdir(this.rootDirectory, { recursive: true })
    return realpath(this.rootDirectory)
  }

  private async resolveExisting(root: string, normalizedPath: string, expected?: 'file' | 'directory'): Promise<string> {
    const unresolved = resolve(root, ...splitPath(normalizedPath))
    assertInsideRoot(root, unresolved)
    const unresolvedStat = await lstat(unresolved)
    if (unresolvedStat.isSymbolicLink()) throw new Error('Symbolic links are not supported')
    const target = await realpath(unresolved)
    assertInsideRoot(root, target)
    const targetStat = await lstat(target)
    if (expected === 'file' && !targetStat.isFile()) throw new Error('Vault entry is not a regular file')
    if (expected === 'directory' && !targetStat.isDirectory()) throw new Error('Vault entry is not a directory')
    if (!targetStat.isFile() && !targetStat.isDirectory()) throw new Error('Unsupported Vault entry')
    return target
  }

  private async assertRealDirectoryInside(root: string, directory: string): Promise<void> {
    const directoryStat = await lstat(directory)
    if (directoryStat.isSymbolicLink() || !directoryStat.isDirectory()) throw new Error('Invalid Vault directory')
    assertInsideRoot(root, await realpath(directory), true)
  }

  private async listDirectory(
    root: string,
    directory: string,
    depth: number,
    counter: { value: number },
    matcher: Ignore | null,
    sort: 'name' | 'modified'
  ): Promise<JsonVaultNode[]> {
    if (depth > maxDepth || counter.value >= maxEntries) return []
    const nodes: JsonVaultNode[] = []
    for (const entry of await readdir(directory, { withFileTypes: true })) {
      if (counter.value >= maxEntries || entry.name.startsWith('.') || entry.isSymbolicLink()) continue
      const target = resolve(directory, entry.name)
      const relativePath = toPortablePath(relative(root, target))
      if (matcher?.ignores(entry.isDirectory() ? `${relativePath}/` : relativePath)) continue
      if (entry.isDirectory()) {
        const children = await this.listDirectory(root, target, depth + 1, counter, matcher, sort)
        nodes.push({ name: entry.name, relativePath, kind: 'directory', children })
        counter.value += 1
      } else if (entry.isFile() && entry.name.toLocaleLowerCase().endsWith('.json')) {
        const fileStat = await stat(target)
        nodes.push({ name: entry.name, relativePath, kind: 'file', modifiedAt: fileStat.mtime.toISOString() })
        counter.value += 1
      }
    }
    return sortNodes(nodes, sort)
  }

  private async loadIgnoreMatcher(root: string): Promise<Ignore | null> {
    try {
      return ignore().add(await readFile(resolve(root, '.gitignore'), 'utf8'))
    } catch {
      return null
    }
  }

  private async uniqueJsonPath(root: string, parent: string, name: string): Promise<string> {
    for (let index = 0; index < 10_000; index += 1) {
      const suffix = index === 0 ? '' : ` (${index + 1})`
      const candidate = [parent, `${name}${suffix}.json`].filter(Boolean).join('/')
      if (!await exists(resolve(root, ...splitPath(candidate)))) return candidate
    }
    throw new Error('Unable to allocate a unique Vault path')
  }
}

function normalizeJsonPath(value: unknown): string {
  const normalized = normalizeEntryPath(value)
  return normalized.toLocaleLowerCase().endsWith('.json') ? normalized : `${normalized}.json`
}

function normalizeDirectoryPath(value: unknown, allowRoot = false): string {
  if (allowRoot && (value === '' || value == null)) return ''
  const normalized = normalizeEntryPath(value)
  if (normalized.toLocaleLowerCase().endsWith('.json')) throw new Error('Invalid Vault directory')
  return normalized
}

function normalizeEntryPath(value: unknown): string {
  if (typeof value !== 'string' || !value.trim() || isAbsolute(value) || value.includes('\0')) throw new Error('Invalid Vault path')
  const normalized = value.trim().replaceAll('\\', '/').replace(/^\.\//, '').replace(/\/$/, '')
  const parts = normalized.split('/')
  if (normalized.length > 768 || parts.some((part) => !part || part === '.' || part === '..' || part.startsWith('.'))) {
    throw new Error('Invalid Vault path')
  }
  if (parts.some((part) => part.length > 180)) throw new Error('Vault path is too long')
  return normalized
}

function sanitizeName(value: unknown, file: boolean): string {
  if (typeof value !== 'string' || /[\r\n\0]/.test(value)) throw new Error('Invalid Vault name')
  const withoutExtension = file ? value.trim().replace(/\.json$/i, '') : value.trim()
  const sanitized = withoutExtension.replace(/[\\/:*?"<>|]/g, '_').replace(/^\.+/, '').trim()
  if (!sanitized) throw new Error('Invalid Vault name')
  return sanitized.slice(0, 180)
}

function sortNodes(nodes: JsonVaultNode[], sort: 'name' | 'modified'): JsonVaultNode[] {
  return nodes.sort((left, right) => {
    if (left.kind !== right.kind) return left.kind === 'directory' ? -1 : 1
    if (left.kind === 'directory' || sort === 'name') return left.name.localeCompare(right.name)
    return (right.modifiedAt ?? '').localeCompare(left.modifiedAt ?? '')
  })
}

function assertInsideRoot(root: string, target: string, allowRoot = false): void {
  if ((allowRoot && target === root) || target.startsWith(`${root}${sep}`)) return
  throw new Error('Vault path escapes the configured directory')
}

function splitPath(value: string): string[] {
  return value ? value.split('/') : []
}

function toPortablePath(value: string): string {
  return value.split(sep).join('/')
}

async function exists(path: string): Promise<boolean> {
  try {
    await lstat(path)
    return true
  } catch {
    return false
  }
}

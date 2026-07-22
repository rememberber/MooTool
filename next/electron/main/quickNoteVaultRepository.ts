import { lstat, mkdir, readFile, readdir, realpath, rename, rm, stat, writeFile } from 'node:fs/promises'
import { randomUUID } from 'node:crypto'
import { basename, dirname, extname, isAbsolute, relative, resolve, sep } from 'node:path'
import ignore, { type Ignore } from 'ignore'
import { parse as parseYaml, stringify as stringifyYaml } from 'yaml'
import type {
  CreateQuickNoteInput,
  MoveQuickNoteEntryInput,
  QuickNoteAttachment,
  QuickNoteFile,
  QuickNoteListInput,
  QuickNoteMetadata,
  QuickNoteNode,
  RenameQuickNoteEntryInput,
  SaveQuickNoteInput
} from '../../src/shared/contracts/quickNote'

const maxFileSize = 20 * 1024 * 1024
const maxEntries = 5000
const maxDepth = 24
const attachmentExtensions = new Set(['.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'])

export class QuickNoteVaultRepository {
  constructor(private readonly rootDirectory: string) {}

  async list(input: QuickNoteListInput = {}): Promise<QuickNoteNode[]> {
    const root = await this.ensureRoot()
    await this.seedWelcomeNote(root)
    const matcher = input.hideIgnored ? await this.loadIgnoreMatcher(root) : null
    const keyword = input.keyword?.trim().toLocaleLowerCase() ?? ''
    return this.listDirectory(root, root, 0, { value: 0 }, matcher, {
      keyword,
      includeContent: input.includeContent ?? true,
      sort: input.sort ?? 'modified'
    })
  }

  async read(relativePath: string): Promise<QuickNoteFile> {
    const root = await this.ensureRoot()
    const target = await this.resolveExisting(root, normalizeNotePath(relativePath), 'file')
    const fileStat = await stat(target)
    if (fileStat.size > maxFileSize) throw new Error('Quick Note exceeds 20 MB limit')
    const parsed = parseNote(await readFile(target, 'utf8'), basename(target, extname(target)), fileStat)
    return {
      relativePath: toPortablePath(relative(root, target)),
      content: parsed.content,
      metadata: parsed.metadata,
      modifiedAt: fileStat.mtime.toISOString()
    }
  }

  async create(input: CreateQuickNoteInput): Promise<QuickNoteFile> {
    const title = normalizeTitle(input.title)
    const root = await this.ensureRoot()
    const parent = normalizeDirectoryPath(input.parentPath ?? '', true)
    const parentTarget = resolve(root, ...splitPath(parent))
    assertInsideRoot(root, parentTarget, true)
    await mkdir(parentTarget, { recursive: true })
    await this.assertRealDirectoryInside(root, parentTarget)
    const relativePath = await this.uniqueNotePath(root, parent, sanitizeName(title))
    const now = new Date().toISOString()
    return this.save({
      relativePath,
      content: '',
      metadata: {
        title,
        style: '',
        syntax: 'text/plain',
        fontName: 'ui-monospace',
        fontSize: clampFontSize(input.fontSize ?? 14),
        color: 'default',
        lineWrap: input.lineWrap ?? true,
        createdAt: now,
        modifiedAt: now
      }
    })
  }

  async save(input: SaveQuickNoteInput): Promise<QuickNoteFile> {
    if (typeof input.content !== 'string' || Buffer.byteLength(input.content, 'utf8') > maxFileSize) {
      throw new Error('Invalid Quick Note content')
    }
    const root = await this.ensureRoot()
    const normalizedPath = normalizeNotePath(input.relativePath)
    const target = resolve(root, ...splitPath(normalizedPath))
    assertInsideRoot(root, target)
    await mkdir(dirname(target), { recursive: true })
    await this.assertRealDirectoryInside(root, dirname(target))
    let previousAttachments = new Set<string>()
    if (await exists(target)) {
      const targetStat = await lstat(target)
      if (targetStat.isSymbolicLink() || !targetStat.isFile()) throw new Error('Invalid Quick Note file')
      previousAttachments = extractAttachmentPaths(parseNote(await readFile(target, 'utf8'), basename(normalizedPath, '.txt'), await stat(target)).content)
    }
    const metadata = normalizeMetadata(input.metadata, basename(normalizedPath, '.txt'))
    metadata.modifiedAt = new Date().toISOString()
    await writeFile(target, serializeNote(metadata, input.content), 'utf8')
    const currentAttachments = extractAttachmentPaths(input.content)
    await this.cleanupOrphanedAttachments(root, [...previousAttachments].filter((path) => !currentAttachments.has(path)))
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

  async renameEntry(input: RenameQuickNoteEntryInput): Promise<string> {
    const root = await this.ensureRoot()
    const sourcePath = normalizeEntryPath(input.relativePath)
    const source = await this.resolveExisting(root, sourcePath)
    const sourceStat = await lstat(source)
    const name = sanitizeName(normalizeTitle(input.name))
    const nextName = sourceStat.isFile() ? `${name}.txt` : name
    const target = resolve(dirname(source), nextName)
    assertInsideRoot(root, target)
    if (await exists(target)) throw new Error('An entry with that name already exists')
    await rename(source, target)
    const nextPath = toPortablePath(relative(root, target))
    if (sourceStat.isFile()) {
      const note = await this.read(nextPath)
      await this.save({ ...note, metadata: { ...note.metadata, title: input.name.trim() } })
    }
    return nextPath
  }

  async moveEntry(input: MoveQuickNoteEntryInput): Promise<string> {
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
    let target = resolve(targetDirectory, basename(source))
    if (await exists(target)) {
      if ((await lstat(source)).isDirectory()) throw new Error('Target folder already contains this entry')
      const title = basename(source, '.txt')
      const unique = await this.uniqueNotePath(root, targetDirectoryPath, title)
      target = resolve(root, ...splitPath(unique))
    }
    await rename(source, target)
    return toPortablePath(relative(root, target))
  }

  async duplicate(relativePath: string): Promise<QuickNoteFile> {
    const source = await this.read(relativePath)
    const root = await this.ensureRoot()
    const parent = toPortablePath(dirname(normalizeNotePath(relativePath))).replace(/^\.$/, '')
    const copyTitle = `${source.metadata.title} Copy`
    const targetPath = await this.uniqueNotePath(root, parent, sanitizeName(copyTitle))
    const now = new Date().toISOString()
    return this.save({
      relativePath: targetPath,
      content: source.content,
      metadata: { ...source.metadata, title: copyTitle, createdAt: now, modifiedAt: now }
    })
  }

  async delete(relativePath: string): Promise<void> {
    const root = await this.ensureRoot()
    const normalized = normalizeEntryPath(relativePath)
    const target = await this.resolveExisting(root, normalized)
    const targetStat = await lstat(target)
    let deletedAttachments = new Set<string>()
    if (targetStat.isFile()) {
      const parsed = parseNote(await readFile(target, 'utf8'), basename(target, '.txt'), await stat(target))
      deletedAttachments = extractAttachmentPaths(parsed.content)
    }
    if (targetStat.isDirectory()) {
      const entries = await readdir(target)
      if (entries.length > 0) throw new Error('Folder must be empty before it can be deleted')
    }
    await rm(target, { force: true })
    await this.cleanupOrphanedAttachments(root, [...deletedAttachments])
  }

  async importAttachment(sourcePath: string): Promise<QuickNoteAttachment> {
    const extension = extname(sourcePath).toLocaleLowerCase()
    if (!attachmentExtensions.has(extension)) throw new Error('Unsupported image attachment')
    const sourceStat = await stat(sourcePath)
    if (!sourceStat.isFile() || sourceStat.size > maxFileSize) throw new Error('Attachment exceeds 20 MB limit')
    return this.storeAttachment(await readFile(sourcePath), extension)
  }

  async importAttachmentBuffer(data: Uint8Array, extension = '.png'): Promise<QuickNoteAttachment> {
    const normalizedExtension = extension.toLocaleLowerCase()
    if (!attachmentExtensions.has(normalizedExtension)) throw new Error('Unsupported image attachment')
    if (data.byteLength === 0 || data.byteLength > maxFileSize) throw new Error('Attachment exceeds 20 MB limit')
    return this.storeAttachment(data, normalizedExtension)
  }

  private async storeAttachment(data: Uint8Array, extension: string): Promise<QuickNoteAttachment> {
    const root = await this.ensureRoot()
    const directory = resolve(root, 'attachments')
    await mkdir(directory, { recursive: true })
    await this.assertRealDirectoryInside(root, directory)
    const filename = `${compactTimestamp()}_${randomUUID().slice(0, 8)}${extension === '.jpeg' ? '.jpg' : extension}`
    const target = resolve(directory, filename)
    await writeFile(target, data)
    const relativePath = `attachments/${filename}`
    return {
      relativePath,
      markdown: `![image](${relativePath})`,
      dataUrl: await this.readAttachment(relativePath)
    }
  }

  async readAttachment(relativePath: string): Promise<string> {
    const root = await this.ensureRoot()
    const normalized = normalizeAttachmentPath(relativePath)
    const target = await this.resolveExisting(root, normalized, 'file')
    const targetStat = await stat(target)
    if (targetStat.size > maxFileSize) throw new Error('Attachment exceeds 20 MB limit')
    const mime = imageMime(extname(target).toLocaleLowerCase())
    return `data:${mime};base64,${(await readFile(target)).toString('base64')}`
  }

  private async ensureRoot(): Promise<string> {
    await mkdir(this.rootDirectory, { recursive: true })
    return realpath(this.rootDirectory)
  }

  private async seedWelcomeNote(root: string): Promise<void> {
    const entries = await readdir(root, { withFileTypes: true })
    if (entries.some((entry) => entry.isFile() && entry.name.toLocaleLowerCase().endsWith('.txt'))) return
    const nested = await this.containsNote(root, 0)
    if (nested) return
    const now = new Date().toISOString()
    const metadata: QuickNoteMetadata = {
      title: 'Welcome to MooTool',
      style: '',
      syntax: 'text/markdown',
      fontName: 'ui-monospace',
      fontSize: 14,
      color: 'default',
      lineWrap: true,
      createdAt: now,
      modifiedAt: now
    }
    await writeFile(resolve(root, 'Welcome to MooTool.txt'), serializeNote(metadata, '# MooTool Quick Note\n\nYour notes are plain text files and can be managed with Git.'), 'utf8')
  }

  private async containsNote(directory: string, depth: number): Promise<boolean> {
    if (depth > maxDepth) return false
    for (const entry of await readdir(directory, { withFileTypes: true })) {
      if (entry.name.startsWith('.') || entry.name === 'attachments' || entry.isSymbolicLink()) continue
      if (entry.isFile() && entry.name.toLocaleLowerCase().endsWith('.txt')) return true
      if (entry.isDirectory() && await this.containsNote(resolve(directory, entry.name), depth + 1)) return true
    }
    return false
  }

  private async listDirectory(
    root: string,
    directory: string,
    depth: number,
    counter: { value: number },
    matcher: Ignore | null,
    options: { keyword: string; includeContent: boolean; sort: 'name' | 'modified' | 'created' }
  ): Promise<QuickNoteNode[]> {
    if (depth > maxDepth || counter.value >= maxEntries) return []
    const nodes: QuickNoteNode[] = []
    for (const entry of await readdir(directory, { withFileTypes: true })) {
      if (counter.value >= maxEntries || entry.name.startsWith('.') || entry.name === 'attachments' || entry.isSymbolicLink()) continue
      const target = resolve(directory, entry.name)
      const relativePath = toPortablePath(relative(root, target))
      if (matcher?.ignores(entry.isDirectory() ? `${relativePath}/` : relativePath)) continue
      if (entry.isDirectory()) {
        const children = await this.listDirectory(root, target, depth + 1, counter, matcher, options)
        const directoryMatches = options.keyword && relativePath.toLocaleLowerCase().includes(options.keyword)
        if (!options.keyword || directoryMatches || children.length > 0) {
          nodes.push({ name: entry.name, relativePath, kind: 'directory', children })
          counter.value += 1
        }
      } else if (entry.isFile() && entry.name.toLocaleLowerCase().endsWith('.txt')) {
        const fileStat = await stat(target)
        if (fileStat.size > maxFileSize) continue
        const raw = await readFile(target, 'utf8')
        const parsed = parseNote(raw, basename(entry.name, '.txt'), fileStat)
        const matches = !options.keyword || [parsed.metadata.title, relativePath, options.includeContent ? parsed.content : '']
          .some((value) => value.toLocaleLowerCase().includes(options.keyword))
        if (!matches) continue
        nodes.push({
          name: entry.name,
          relativePath,
          kind: 'file',
          title: parsed.metadata.title,
          color: parsed.metadata.color,
          createdAt: parsed.metadata.createdAt,
          modifiedAt: parsed.metadata.modifiedAt
        })
        counter.value += 1
      }
    }
    return sortNodes(nodes, options.sort)
  }

  private async loadIgnoreMatcher(root: string): Promise<Ignore | null> {
    try {
      return ignore().add(await readFile(resolve(root, '.gitignore'), 'utf8'))
    } catch {
      return null
    }
  }

  private async cleanupOrphanedAttachments(root: string, candidates: string[]): Promise<void> {
    if (!candidates.length) return
    const referenced = new Set<string>()
    await this.collectReferences(root, root, 0, referenced)
    for (const candidate of candidates) {
      if (referenced.has(candidate)) continue
      try {
        const normalized = normalizeAttachmentPath(candidate)
        const target = resolve(root, ...splitPath(normalized))
        const targetStat = await lstat(target)
        if (!targetStat.isSymbolicLink() && targetStat.isFile()) await rm(target, { force: true })
      } catch {
        // Missing or malformed attachment paths do not block note persistence.
      }
    }
  }

  private async collectReferences(root: string, directory: string, depth: number, referenced: Set<string>): Promise<void> {
    if (depth > maxDepth) return
    for (const entry of await readdir(directory, { withFileTypes: true })) {
      if (entry.name.startsWith('.') || entry.name === 'attachments' || entry.isSymbolicLink()) continue
      const target = resolve(directory, entry.name)
      if (entry.isDirectory()) {
        await this.collectReferences(root, target, depth + 1, referenced)
      } else if (entry.isFile() && entry.name.toLocaleLowerCase().endsWith('.txt')) {
        const fileStat = await stat(target)
        if (fileStat.size > maxFileSize) continue
        const parsed = parseNote(await readFile(target, 'utf8'), basename(entry.name, '.txt'), fileStat)
        for (const path of extractAttachmentPaths(parsed.content)) referenced.add(path)
      }
    }
  }

  private async resolveExisting(root: string, normalizedPath: string, expected?: 'file' | 'directory'): Promise<string> {
    const unresolved = resolve(root, ...splitPath(normalizedPath))
    assertInsideRoot(root, unresolved)
    const unresolvedStat = await lstat(unresolved)
    if (unresolvedStat.isSymbolicLink()) throw new Error('Symbolic links are not supported')
    const target = await realpath(unresolved)
    assertInsideRoot(root, target)
    const targetStat = await lstat(target)
    if (expected === 'file' && !targetStat.isFile()) throw new Error('Vault entry is not a file')
    if (expected === 'directory' && !targetStat.isDirectory()) throw new Error('Vault entry is not a directory')
    if (!targetStat.isFile() && !targetStat.isDirectory()) throw new Error('Unsupported Vault entry')
    return target
  }

  private async assertRealDirectoryInside(root: string, directory: string): Promise<void> {
    const directoryStat = await lstat(directory)
    if (directoryStat.isSymbolicLink() || !directoryStat.isDirectory()) throw new Error('Invalid Vault directory')
    assertInsideRoot(root, await realpath(directory), true)
  }

  private async uniqueNotePath(root: string, parent: string, name: string): Promise<string> {
    for (let index = 0; index < 10_000; index += 1) {
      const suffix = index === 0 ? '' : ` (${index + 1})`
      const candidate = [parent, `${name}${suffix}.txt`].filter(Boolean).join('/')
      if (!await exists(resolve(root, ...splitPath(candidate)))) return candidate
    }
    throw new Error('Unable to allocate a unique note path')
  }
}

function parseNote(raw: string, fallbackTitle: string, fileStat: { birthtime: Date; mtime: Date }): { content: string; metadata: QuickNoteMetadata } {
  const match = /^---\r?\n([\s\S]*?)\r?\n---(?:\r?\n)?/.exec(raw)
  let values: Record<string, unknown> = {}
  if (match) {
    try {
      const parsed = parseYaml(match[1])
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) values = parsed as Record<string, unknown>
    } catch {
      values = {}
    }
  }
  const fallbackCreated = fileStat.birthtime.toISOString()
  const fallbackModified = fileStat.mtime.toISOString()
  return {
    content: match ? raw.slice(match[0].length) : raw,
    metadata: {
      title: stringValue(values.title, fallbackTitle),
      style: stringValue(values.style, ''),
      syntax: stringValue(values.syntax, 'text/plain'),
      fontName: stringValue(values.font_name, ''),
      fontSize: clampFontSize(numberValue(values.font_size, 14)),
      color: stringValue(values.color, 'default'),
      lineWrap: booleanValue(values.line_wrap, true),
      createdAt: stringValue(values.created_at, fallbackCreated),
      modifiedAt: stringValue(values.modified_at, fallbackModified)
    }
  }
}

function serializeNote(metadata: QuickNoteMetadata, content: string): string {
  const values = {
    title: metadata.title,
    style: metadata.style,
    syntax: metadata.syntax,
    font_name: metadata.fontName,
    font_size: String(metadata.fontSize),
    color: metadata.color,
    line_wrap: metadata.lineWrap ? '1' : '0',
    created_at: metadata.createdAt,
    modified_at: metadata.modifiedAt
  }
  const yaml = stringifyYaml(values, { lineWidth: 0 }).trim()
  return `---\n${yaml}\n---${content && !content.startsWith('\n') ? '\n' : ''}${content}`
}

function normalizeMetadata(value: QuickNoteMetadata, fallbackTitle: string): QuickNoteMetadata {
  if (!value || typeof value !== 'object') throw new Error('Invalid Quick Note metadata')
  return {
    title: normalizeTitle(value.title || fallbackTitle),
    style: stringValue(value.style, '').slice(0, 64),
    syntax: stringValue(value.syntax, 'text/plain').slice(0, 80),
    fontName: stringValue(value.fontName, '').slice(0, 120),
    fontSize: clampFontSize(value.fontSize),
    color: stringValue(value.color, 'default').slice(0, 32),
    lineWrap: Boolean(value.lineWrap),
    createdAt: validDate(value.createdAt) ? value.createdAt : new Date().toISOString(),
    modifiedAt: validDate(value.modifiedAt) ? value.modifiedAt : new Date().toISOString()
  }
}

function normalizeNotePath(value: unknown): string {
  const normalized = normalizeEntryPath(value)
  return normalized.toLocaleLowerCase().endsWith('.txt') ? normalized : `${normalized}.txt`
}

function normalizeAttachmentPath(value: unknown): string {
  const normalized = normalizeEntryPath(value)
  if (!normalized.startsWith('attachments/') || normalized.split('/').length !== 2 || !attachmentExtensions.has(extname(normalized).toLocaleLowerCase())) {
    throw new Error('Invalid attachment path')
  }
  return normalized
}

function normalizeEntryPath(value: unknown): string {
  if (typeof value !== 'string' || !value.trim() || isAbsolute(value) || value.includes('\0')) throw new Error('Invalid Vault path')
  const normalized = value.trim().replaceAll('\\', '/').replace(/^\.\//, '').replace(/\/$/, '')
  const parts = normalized.split('/')
  if (normalized.length > 768 || parts.some((part) => !part || part === '.' || part === '..' || part.startsWith('.'))) {
    throw new Error('Invalid Vault path')
  }
  return normalized
}

function normalizeDirectoryPath(value: unknown, allowRoot = false): string {
  if (allowRoot && (value === '' || value == null)) return ''
  const normalized = normalizeEntryPath(value)
  if (normalized.toLocaleLowerCase().endsWith('.txt')) throw new Error('Invalid Vault directory')
  return normalized
}

function normalizeTitle(value: unknown): string {
  if (typeof value !== 'string') throw new Error('Invalid title')
  const title = value.trim()
  if (!title || title.length > 180 || /[\r\n\0]/.test(title)) throw new Error('Invalid title')
  return title
}

function sanitizeName(value: string): string {
  const sanitized = value.replace(/[\\/:*?"<>|]/g, '_').replace(/^\.+/, '').trim()
  if (!sanitized) throw new Error('Invalid file name')
  return sanitized.slice(0, 180)
}

function sortNodes(nodes: QuickNoteNode[], sort: 'name' | 'modified' | 'created'): QuickNoteNode[] {
  return nodes.sort((left, right) => {
    if (left.kind !== right.kind) return left.kind === 'directory' ? -1 : 1
    if (left.kind === 'directory' || sort === 'name') return left.name.localeCompare(right.name)
    const field = sort === 'created' ? 'createdAt' : 'modifiedAt'
    return (right[field] ?? '').localeCompare(left[field] ?? '')
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

function stringValue(value: unknown, fallback: string): string {
  return value == null ? fallback : String(value)
}

function numberValue(value: unknown, fallback: number): number {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function booleanValue(value: unknown, fallback: boolean): boolean {
  if (value == null || value === '') return fallback
  return value === true || value === 1 || value === '1' || value === 'true'
}

function validDate(value: unknown): value is string {
  return typeof value === 'string' && value.length <= 64 && !Number.isNaN(Date.parse(value))
}

function clampFontSize(value: number): number {
  return Math.min(48, Math.max(8, Math.round(Number.isFinite(value) ? value : 14)))
}

function compactTimestamp(): string {
  return new Date().toISOString().replace(/\D/g, '').slice(0, 14)
}

function imageMime(extension: string): string {
  if (extension === '.jpg' || extension === '.jpeg') return 'image/jpeg'
  if (extension === '.svg') return 'image/svg+xml'
  return `image/${extension.slice(1)}`
}

function extractAttachmentPaths(content: string): Set<string> {
  const paths = new Set<string>()
  for (const match of content.matchAll(/(?:\(|src=["'])(attachments\/[A-Za-z0-9_.-]+)(?:\)|["'])/g)) {
    try {
      paths.add(normalizeAttachmentPath(match[1]))
    } catch {
      // Ignore malformed attachment references in note content.
    }
  }
  return paths
}

async function exists(path: string): Promise<boolean> {
  try {
    await lstat(path)
    return true
  } catch {
    return false
  }
}

import { constants } from 'node:fs'
import { lstat, open, readFile, readdir, realpath } from 'node:fs/promises'
import { basename, extname, isAbsolute, join, relative, resolve, sep } from 'node:path'
import ignore from 'ignore'
import { z } from 'zod'
import { parseNote } from '../main/quickNoteVaultRepository'

export const accessSchema = z.strictObject({ version: z.literal(1), notes: z.string().nullable(), json: z.string().nullable() })
export type VaultKind = 'notes' | 'json'
const noteExtensions = new Set(['.txt', '.md', '.json', '.java', '.js', '.ts', '.py', '.xml', '.yaml', '.yml', '.sql'])
const maxBytes = 2_000_000

export async function readAccess(path?: string) {
  if (!path) return { version: 1 as const, notes: null, json: null }
  try {
    const info = await lstat(path)
    if (!info.isFile() || info.isSymbolicLink() || info.size > 16_000) throw new Error('Invalid MooTool access settings')
    return accessSchema.parse(JSON.parse(await readFile(path, 'utf8')))
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return { version: 1 as const, notes: null, json: null }
    throw new Error('Cannot read MooTool access settings. Check AI integration settings.')
  }
}

export class VaultReadService {
  constructor(private readonly accessFile?: string) {}

  private async root(kind: VaultKind): Promise<string> {
    const path = (await readAccess(this.accessFile))[kind]
    if (!path) throw new Error(`Read access to ${kind} is disabled. Enable it in MooTool Settings → AI integration.`)
    if (!isAbsolute(path) || await realpath(path) !== path || !(await lstat(path)).isDirectory()) throw new Error('The granted vault moved. Grant access again in MooTool settings.')
    return path
  }

  async search(kind: VaultKind, query: string, limit: number, offset: number) {
    const root = await this.root(kind)
    const matcher = await this.matcher(root)
    const found: Array<{ path: string; title: string; excerpt: string; modifiedAt: string }> = []
    let visited = 0
    let bytes = 0
    let truncated = false
    const needle = query.toLocaleLowerCase()
    const walk = async (directory: string, depth: number): Promise<void> => {
      if (depth > 24) { truncated = true; return }
      if (!inside(root, await realpath(directory))) return
      const entries = (await readdir(directory, { withFileTypes: true })).sort((a, b) => a.name.localeCompare(b.name, 'en'))
      for (const entry of entries) {
        if (++visited > 4000 || bytes > 10_000_000 || found.length > offset + limit) { truncated = true; return }
        if (entry.name.startsWith('.') || entry.isSymbolicLink()) continue
        const target = join(directory, entry.name)
        const path = relative(root, target).split(sep).join('/')
        if (matcher.ignores(path + (entry.isDirectory() ? '/' : ''))) continue
        if (entry.isDirectory()) await walk(target, depth + 1)
        else if (entry.isFile() && allowed(kind, target)) {
          try {
            const document = await this.document(root, kind, path)
            bytes += document.bytes
            const haystack = `${document.title}\n${path}\n${document.content}`.toLocaleLowerCase()
            if (!needle || haystack.includes(needle)) {
              const index = document.content.toLocaleLowerCase().indexOf(needle)
              found.push({ path, title: document.title, excerpt: document.content.slice(Math.max(0, index - 60), Math.max(0, index - 60) + 240), modifiedAt: document.modifiedAt })
            }
          } catch { /* Files removed, oversized or made inaccessible during search are omitted. */ }
        }
      }
    }
    await walk(root, 0)
    const hasMore = found.length > offset + limit
    return { entries: found.slice(offset, offset + limit), nextOffset: hasMore ? offset + limit : null, truncated }
  }

  async read(kind: VaultKind, path: string, offset: number, length: number) {
    const root = await this.root(kind)
    const normalized = normalizePath(path)
    if ((await this.matcher(root)).ignores(normalized)) throw new Error('Gitignored documents are not exposed to AI')
    const document = await this.document(root, kind, normalized)
    return { path: normalized, title: document.title, content: document.content.slice(offset, offset + length), modifiedAt: document.modifiedAt,
      totalCharacters: document.content.length, nextOffset: offset + length < document.content.length ? offset + length : null }
  }

  private async document(root: string, kind: VaultKind, path: string) {
    const normalized = normalizePath(path)
    if (!allowed(kind, normalized)) throw new Error('Unsupported document type')
    let target = root
    for (const part of normalized.split('/')) {
      target = join(target, part)
      if ((await lstat(target)).isSymbolicLink()) throw new Error('Symbolic links are not exposed to AI')
    }
    const canonical = await realpath(target)
    if (!inside(root, canonical)) throw new Error('Document must be inside the granted vault')
    const file = await open(canonical, constants.O_RDONLY | (constants.O_NOFOLLOW ?? 0))
    try {
      const info = await file.stat()
      if (!info.isFile() || info.size > maxBytes) throw new Error('Document exceeds the 2 MB read limit')
      // Read at most the permitted bytes even if another process grows the file.
      const buffer = Buffer.alloc(maxBytes + 1)
      let bytesRead = 0
      while (bytesRead < buffer.length) {
        const chunk = await file.read(buffer, bytesRead, buffer.length - bytesRead, bytesRead)
        if (chunk.bytesRead === 0) break
        bytesRead += chunk.bytesRead
      }
      if (bytesRead > maxBytes) throw new Error('Document exceeds the 2 MB read limit')
      const raw = new TextDecoder('utf-8', { fatal: true }).decode(buffer.subarray(0, bytesRead))
      const note = kind === 'notes' ? parseNote(raw, basename(path, extname(path)), info) : null
      return { content: note?.content ?? raw, title: note?.metadata.title ?? basename(path), modifiedAt: info.mtime.toISOString(), bytes: bytesRead }
    } finally { await file.close() }
  }

  private async matcher(root: string) {
    const matcher = ignore()
    try {
      const path = join(root, '.gitignore')
      const info = await lstat(path)
      if (!info.isFile() || info.isSymbolicLink() || info.size > 100_000) throw new Error('Cannot safely read the vault .gitignore; use a regular file under 100 KB')
      matcher.add(new TextDecoder('utf-8', { fatal: true }).decode(await readFile(path)))
    } catch (error) { if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error }
    return matcher
  }
}

function allowed(kind: VaultKind, path: string): boolean {
  return kind === 'json' ? extname(path).toLowerCase() === '.json' : noteExtensions.has(extname(path).toLowerCase())
}
function normalizePath(path: string): string {
  const parts = path.split('/')
  if (!path || isAbsolute(path) || path.includes('\\') || path.includes(':') || path.includes('\0') || parts.some((part) => !part || part.startsWith('.'))) throw new Error('Use a relative document path returned by MooTool search')
  return parts.join('/')
}
function inside(root: string, path: string): boolean {
  const suffix = relative(resolve(root), resolve(path))
  return suffix === '' || (!isAbsolute(suffix) && suffix !== '..' && !suffix.startsWith(`..${sep}`))
}

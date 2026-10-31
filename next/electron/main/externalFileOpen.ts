import { open, stat } from 'node:fs/promises'
import { basename, extname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

export const externalFileSyntax: Record<string, string> = {
  json: 'application/json', jsonc: 'application/json', txt: 'text/plain', log: 'text/plain',
  md: 'text/markdown', markdown: 'text/markdown', xml: 'text/xml',
  yaml: 'text/yaml', yml: 'text/yaml', html: 'text/html', htm: 'text/html',
  js: 'text/javascript', ts: 'text/typescript', java: 'text/java', py: 'text/python', sql: 'text/sql'
}
export const externalFileSizeLimit = 20 * 1024 * 1024
export type ExternalTextFile = { path: string; name: string; content: string; syntax: string }
export type ExternalFileTarget = 'json' | 'quickNote'

// Electron's executable and (in development) app path are not documents.
export function externalFilesFromArgv(argv: string[], cwd: string, defaultApp: boolean): string[] {
  const paths: string[] = []
  for (const arg of argv.slice(defaultApp ? 2 : 1)) {
    if (arg.startsWith('-') || arg.includes('\0')) continue
    let path = arg
    try {
      // Linux desktop launchers may supply file: URLs through Exec's %U.
      if (arg.startsWith('file:')) path = fileURLToPath(arg)
      else if (/^[a-z][a-z\d+.-]*:\/\//i.test(arg)) continue
    } catch { continue }
    if (Object.hasOwn(externalFileSyntax, extname(path).slice(1).toLowerCase())) paths.push(resolve(cwd, path))
  }
  return [...new Set(paths)]
}

export async function readExternalTextFile(path: string): Promise<ExternalTextFile> {
  const extension = extname(path).slice(1).toLowerCase()
  if (!Object.hasOwn(externalFileSyntax, extension)) throw new Error('fileOpen.unsupported')
  const syntax = externalFileSyntax[extension]
  // Reject devices/FIFOs before opening so a named pipe cannot block the queue.
  if (!(await stat(path)).isFile()) throw new Error('fileOpen.unreadable')
  const handle = await open(path, 'r')
  try {
    const info = await handle.stat()
    if (!info.isFile()) throw new Error('fileOpen.unreadable')
    if (info.size > externalFileSizeLimit) throw new Error('fileOpen.tooLarge')
    const chunks: Buffer[] = []
    let size = 0
    // Bound reads even when the source grows after stat().
    for await (const chunk of handle.createReadStream({ autoClose: false })) {
      size += chunk.length
      if (size > externalFileSizeLimit) throw new Error('fileOpen.tooLarge')
      chunks.push(chunk)
    }
    const bytes = Buffer.concat(chunks)
    const encoding = bytes[0] === 0xff && bytes[1] === 0xfe ? 'utf-16le'
      : bytes[0] === 0xfe && bytes[1] === 0xff ? 'utf-16be' : 'utf-8'
    let content: string
    try { content = new TextDecoder(encoding, { fatal: true }).decode(bytes) }
    catch { throw new Error('fileOpen.encoding') }
    if (content.includes('\0')) throw new Error('fileOpen.encoding')
    return { path, name: basename(path), content, syntax }
  } finally {
    await handle.close()
  }
}

/** OS events may arrive before app/renderer readiness. Keep them ordered and
 * avoid duplicate imports while the same document is already being handled. */
export class ExternalFileOpenQueue {
  private paths = new Set<string>()
  private active: string | null = null
  private ready = false
  private running = false

  constructor(private readonly processFile: (path: string) => Promise<void>,
    private readonly reportError: (path: string, error: unknown) => Promise<void>) {}

  enqueue(paths: string[]): void {
    for (const path of paths) {
      const absolute = resolve(path)
      if (absolute !== this.active) this.paths.add(absolute)
    }
    void this.drain()
  }

  setReady(ready: boolean): void {
    this.ready = ready
    void this.drain()
  }

  private async drain(): Promise<void> {
    if (!this.ready || this.running) return
    this.running = true
    try {
      while (this.ready && this.paths.size) {
        const path = this.paths.values().next().value!
        this.paths.delete(path)
        this.active = path
        try { await this.processFile(path) }
        catch (error) { await this.reportError(path, error).catch(() => undefined) }
        finally { this.active = null }
      }
    } finally { this.running = false }
  }
}

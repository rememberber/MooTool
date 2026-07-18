import { createHash } from 'node:crypto'
import { lstat, readFile, readdir, realpath, stat } from 'node:fs/promises'
import { basename, dirname, isAbsolute, join, relative, resolve, sep } from 'node:path'
import type { AiNativeMemoryArtifact, AiNativeMemorySnapshot } from '../../../src/shared/contracts/aiNativeMemory'
import { redactSensitiveContent, scanSensitiveContent } from './securityScanner'

type NativeMemoryServiceOptions = {
  homeDirectory: string
  maxArtifacts?: number
  maxFileBytes?: number
  maxExcerptBytes?: number
}

export class NativeMemoryService {
  private readonly homeDirectory: string
  private readonly maxArtifacts: number
  private readonly maxFileBytes: number
  private readonly maxExcerptBytes: number

  constructor(options: NativeMemoryServiceOptions) {
    this.homeDirectory = resolve(options.homeDirectory)
    this.maxArtifacts = options.maxArtifacts ?? 500
    this.maxFileBytes = options.maxFileBytes ?? 256 * 1024
    this.maxExcerptBytes = options.maxExcerptBytes ?? 25 * 1024
  }

  async scan(): Promise<AiNativeMemorySnapshot> {
    const diagnostics: AiNativeMemorySnapshot['diagnostics'] = []
    const roots = await this.discoverRoots(diagnostics)
    const artifacts: AiNativeMemoryArtifact[] = []
    for (const root of roots) {
      if (artifacts.length >= this.maxArtifacts) break
      await this.scanRoot(root, artifacts, diagnostics)
    }
    if (artifacts.length >= this.maxArtifacts) diagnostics.push({ severity: 'warning', message: `Native memory scan stopped at the ${this.maxArtifacts}-file safety limit.` })
    return {
      scannedAt: new Date().toISOString(),
      readOnly: true,
      roots,
      artifacts: artifacts.sort((left, right) => Number(right.entrypoint) - Number(left.entrypoint) || right.modifiedAt.localeCompare(left.modifiedAt) || left.path.localeCompare(right.path)),
      diagnostics
    }
  }

  private async discoverRoots(diagnostics: AiNativeMemorySnapshot['diagnostics']): Promise<string[]> {
    const candidates: string[] = []
    const settingsPath = join(this.homeDirectory, '.claude', 'settings.json')
    try {
      const info = await stat(settingsPath)
      if (info.isFile() && info.size <= 1024 * 1024) {
        const parsed = JSON.parse(await readFile(settingsPath, 'utf8')) as unknown
        const configured = isRecord(parsed) && typeof parsed.autoMemoryDirectory === 'string' ? parsed.autoMemoryDirectory.trim() : ''
        if (configured) {
          const expanded = configured.startsWith('~/') ? join(this.homeDirectory, configured.slice(2)) : configured
          if (isAbsolute(expanded)) candidates.push(resolve(expanded))
          else diagnostics.push({ severity: 'warning', message: 'Claude autoMemoryDirectory is not absolute and was ignored.' })
        }
      }
    } catch (error) {
      if (!isMissing(error)) diagnostics.push({ severity: 'warning', message: 'Claude settings could not be parsed while discovering auto memory.' })
    }

    const projectRoot = join(this.homeDirectory, '.claude', 'projects')
    try {
      for (const entry of await readdir(projectRoot, { withFileTypes: true })) {
        if (!entry.isDirectory() || entry.isSymbolicLink()) continue
        candidates.push(join(projectRoot, entry.name, 'memory'))
      }
    } catch (error) {
      if (!isMissing(error)) diagnostics.push({ severity: 'warning', message: 'Claude project memory directories could not be listed.' })
    }

    const roots: string[] = []
    for (const candidate of [...new Set(candidates)]) {
      try {
        const link = await lstat(candidate)
        if (link.isSymbolicLink() || !link.isDirectory()) continue
        const resolved = await realpath(candidate)
        roots.push(resolved)
      } catch (error) {
        if (!isMissing(error)) diagnostics.push({ severity: 'warning', message: `Native memory root could not be inspected: ${candidate}` })
      }
    }
    return [...new Set(roots)].sort()
  }

  private async scanRoot(root: string, output: AiNativeMemoryArtifact[], diagnostics: AiNativeMemorySnapshot['diagnostics']): Promise<void> {
    let entries
    try { entries = await readdir(root, { withFileTypes: true }) } catch {
      diagnostics.push({ severity: 'warning', message: `Native memory root could not be read: ${root}` })
      return
    }
    for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
      if (output.length >= this.maxArtifacts || !entry.isFile() || entry.isSymbolicLink() || !entry.name.toLowerCase().endsWith('.md')) continue
      const path = join(root, entry.name)
      try {
        const resolved = await realpath(path)
        if (!isInside(root, resolved)) continue
        const info = await stat(resolved)
        if (!info.isFile()) continue
        if (info.size > this.maxFileBytes) {
          diagnostics.push({ severity: 'warning', message: `Native memory file exceeds ${this.maxFileBytes} bytes and was skipped: ${path}` })
          continue
        }
        const content = await readFile(resolved, 'utf8')
        const excerptSource = entry.name === 'MEMORY.md' ? content.split(/\r?\n/).slice(0, 200).join('\n') : content
        const excerptBuffer = Buffer.from(excerptSource, 'utf8')
        const excerptTruncated = excerptBuffer.length > this.maxExcerptBytes || excerptSource.length < content.length
        const excerpt = excerptBuffer.subarray(0, this.maxExcerptBytes).toString('utf8')
        const findings = scanSensitiveContent(excerpt)
        output.push({
          id: `sha256:${createHash('sha256').update(resolved).digest('hex')}`,
          clientId: 'claudeCode',
          source: 'claudeAutoMemory',
          projectKey: projectKey(root, this.homeDirectory),
          path: resolved,
          root,
          name: entry.name,
          entrypoint: entry.name === 'MEMORY.md',
          sizeBytes: info.size,
          modifiedAt: info.mtime.toISOString(),
          estimatedTokens: estimateTokens(excerptSource),
          contentExcerpt: redactSensitiveContent(excerpt),
          excerptTruncated,
          sensitiveFindings: findings.length
        })
      } catch {
        diagnostics.push({ severity: 'warning', message: `Native memory file could not be read: ${path}` })
      }
    }
  }
}

function projectKey(root: string, homeDirectory: string): string {
  const standardRoot = join(homeDirectory, '.claude', 'projects')
  const relation = relative(standardRoot, root)
  if (relation && relation !== '..' && !relation.startsWith(`..${sep}`) && !isAbsolute(relation)) return relation.split(sep)[0]
  return basename(dirname(root)) || 'custom'
}

function isInside(root: string, target: string): boolean {
  const relation = relative(root, target)
  return relation === '' || (relation !== '..' && !relation.startsWith(`..${sep}`) && !isAbsolute(relation))
}

function estimateTokens(value: string): number {
  const cjk = (value.match(/[\u3400-\u9fff\uf900-\ufaff]/g) ?? []).length
  return Math.max(1, Math.ceil((value.length - cjk) / 4 + cjk))
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isMissing(error: unknown): boolean {
  return isRecord(error) && error.code === 'ENOENT'
}

import { createHash } from 'node:crypto'
import { lstat, readFile, readdir, stat } from 'node:fs/promises'
import { extname, join, relative, resolve, sep } from 'node:path'
import { parseDocument } from 'yaml'
import { scanSensitiveContent } from './securityScanner'

export type SkillInspectionFindingCode =
  | 'SKILL_ENTRY_INVALID'
  | 'SKILL_REFERENCE_MISSING'
  | 'SKILL_DANGEROUS_PATTERN'
  | 'SKILL_ENTRY_TOO_LARGE'
  | 'PLAINTEXT_SECRET_RISK'

export type SkillInspectionFinding = {
  code: SkillInspectionFindingCode
  severity: 'warning' | 'error'
  path: string
}

export type SkillInspection = {
  metadata: {
    entryPath: string
    description: string
    declaredName: string
    fileCount: number
    totalSizeBytes: number
    estimatedTokens: number
    hasScripts: boolean
    referenceCount: number
    contentDigest: string
  }
  findings: SkillInspectionFinding[]
}

const maximumEntryBytes = 512 * 1024
const recommendedEntryBytes = 100 * 1024
const maximumFiles = 500
const maximumDepth = 8
const riskyPatterns = [
  /\bcurl\b[^\n|]*\|\s*(?:sh|bash|zsh)\b/i,
  /\bwget\b[^\n|]*\|\s*(?:sh|bash|zsh)\b/i,
  /\brm\s+-[a-z]*r[a-z]*f\b/i,
  /\bsudo\b/i,
  /\beval\s+["'$`]/i,
  /\b(?:base64\s+(?:-d|--decode)|openssl\s+enc\s+-d)[^\n|]*\|\s*(?:sh|bash|zsh)\b/i
]

export async function inspectSkillPackage(directory: string): Promise<SkillInspection> {
  const entryPath = join(directory, 'SKILL.md')
  const entryInfo = await stat(entryPath)
  if (!entryInfo.isFile() || entryInfo.size > maximumEntryBytes) throw new Error('Skill entry exceeds the 512 KB inspection limit')
  const source = await readFile(entryPath, 'utf8')
  const findings: SkillInspectionFinding[] = []
  const frontmatter = parseFrontmatter(source)
  if (!frontmatter.valid) findings.push({ code: 'SKILL_ENTRY_INVALID', severity: 'error', path: entryPath })
  if (entryInfo.size > recommendedEntryBytes) findings.push({ code: 'SKILL_ENTRY_TOO_LARGE', severity: 'warning', path: entryPath })
  if (scanSensitiveContent(source).length > 0) findings.push({ code: 'PLAINTEXT_SECRET_RISK', severity: 'warning', path: entryPath })

  const inventory = await inspectFiles(directory)
  if (inventory.riskyPath) findings.push({ code: 'SKILL_DANGEROUS_PATTERN', severity: 'warning', path: inventory.riskyPath })
  const references = extractRelativeReferences(source)
  for (const reference of references) {
    const resolved = resolve(directory, reference)
    if (!isInside(directory, resolved) || !(await isRegularPath(resolved))) {
      findings.push({ code: 'SKILL_REFERENCE_MISSING', severity: 'warning', path: resolved })
    }
  }

  return {
    metadata: {
      entryPath,
      description: frontmatter.description,
      declaredName: frontmatter.name,
      fileCount: inventory.fileCount,
      totalSizeBytes: inventory.totalSizeBytes,
      estimatedTokens: estimateTokens(source),
      hasScripts: inventory.hasScripts,
      referenceCount: references.length,
      contentDigest: createHash('sha256').update(source).digest('hex')
    },
    findings
  }
}

async function inspectFiles(root: string): Promise<{ fileCount: number; totalSizeBytes: number; hasScripts: boolean; riskyPath?: string }> {
  const queue: Array<{ directory: string; depth: number }> = [{ directory: root, depth: 0 }]
  let fileCount = 0
  let totalSizeBytes = 0
  let hasScripts = false
  let riskyPath: string | undefined
  while (queue.length > 0 && fileCount < maximumFiles) {
    const current = queue.shift()!
    const entries = await readdir(current.directory, { withFileTypes: true })
    for (const entry of entries) {
      if (fileCount >= maximumFiles) break
      const path = join(current.directory, entry.name)
      if (entry.isSymbolicLink()) continue
      if (entry.isDirectory()) {
        if (current.depth < maximumDepth) queue.push({ directory: path, depth: current.depth + 1 })
        continue
      }
      if (!entry.isFile()) continue
      const info = await stat(path)
      fileCount += 1
      totalSizeBytes += info.size
      const relativePath = relative(root, path).split(sep).join('/')
      const script = relativePath.startsWith('scripts/') || ['.sh', '.bash', '.zsh', '.ps1', '.bat', '.cmd'].includes(extname(path).toLowerCase())
      hasScripts ||= script
      if (script && !riskyPath && info.size <= 256 * 1024) {
        const content = await readFile(path, 'utf8').catch(() => '')
        if (riskyPatterns.some((pattern) => pattern.test(content))) riskyPath = path
        if (scanSensitiveContent(content).length > 0) riskyPath ??= path
      }
    }
  }
  return { fileCount, totalSizeBytes, hasScripts, riskyPath }
}

function parseFrontmatter(source: string): { valid: boolean; name: string; description: string } {
  const match = source.match(/^---\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)/)
  if (!match) return { valid: false, name: '', description: '' }
  try {
    const document = parseDocument(match[1], { strict: true })
    if (document.errors.length > 0) return { valid: false, name: '', description: '' }
    const value = document.toJSON() as unknown
    if (!isRecord(value)) return { valid: false, name: '', description: '' }
    const name = typeof value.name === 'string' ? value.name.trim().slice(0, 200) : ''
    const description = typeof value.description === 'string' ? value.description.trim().slice(0, 1_000) : ''
    return { valid: Boolean(name && description), name, description }
  } catch {
    return { valid: false, name: '', description: '' }
  }
}

function extractRelativeReferences(source: string): string[] {
  const references = [...source.matchAll(/!?(?:\[[^\]]*])\(([^)\s]+)(?:\s+["'][^"']*["'])?\)/g)]
    .map((match) => match[1].split('#')[0])
    .filter((value) => value && !/^(?:https?:|mailto:|data:|#)/i.test(value))
    .map((value) => decodeURIComponent(value))
  return [...new Set(references)].slice(0, 200)
}

function estimateTokens(value: string): number {
  const cjk = (value.match(/[\u3400-\u9fff\uf900-\ufaff]/g) ?? []).length
  return Math.max(1, Math.ceil((value.length - cjk) / 4 + cjk))
}

async function isRegularPath(path: string): Promise<boolean> {
  try {
    const info = await lstat(path)
    return !info.isSymbolicLink() && (info.isFile() || info.isDirectory())
  } catch {
    return false
  }
}

function isInside(root: string, target: string): boolean {
  const relation = relative(root, target)
  return relation === '' || (!relation.startsWith(`..${sep}`) && relation !== '..' && !relation.startsWith(sep))
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

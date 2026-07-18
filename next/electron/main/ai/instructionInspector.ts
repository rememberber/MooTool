import { createHash } from 'node:crypto'
import { readFile, stat } from 'node:fs/promises'
import { parseDocument } from 'yaml'
import type { AiArtifact, AiClientId } from '../../../src/shared/contracts/ai'

export type InstructionInspection = {
  metadata: {
    appliesTo: string
    lineCount: number
    estimatedTokens: number
    contentDigest: string
    conflictTags: string
    pathPatterns: string
  }
  tooLarge: boolean
}

export type InstructionAnalysisFinding = {
  code: 'INSTRUCTION_DUPLICATE' | 'INSTRUCTION_CONFLICT'
  path: string
  clientId: AiClientId
}

const maximumInstructionBytes = 1024 * 1024
const recommendedTokens = 8_000

export async function inspectInstruction(path: string, appliesTo: string): Promise<InstructionInspection> {
  const info = await stat(path)
  if (!info.isFile() || info.size > maximumInstructionBytes) throw new Error('Instruction file exceeds the 1 MB inspection limit')
  const source = await readFile(path, 'utf8')
  const estimatedTokens = estimateTokens(source)
  return {
    metadata: {
      appliesTo,
      lineCount: source ? source.split(/\r?\n/).length : 0,
      estimatedTokens,
      contentDigest: createHash('sha256').update(normalize(source)).digest('hex'),
      conflictTags: detectConflictTags(source).join(','),
      pathPatterns: readPathPatterns(source).join('\n')
    },
    tooLarge: estimatedTokens > recommendedTokens
  }
}

function readPathPatterns(source: string): string[] {
  const match = source.match(/^---\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)/)
  if (!match) return []
  try {
    const document = parseDocument(match[1], { strict: true })
    if (document.errors.length > 0) return []
    const value = document.toJSON() as unknown
    if (!isRecord(value)) return []
    const candidates = [value.paths, value.globs, value.applyTo].flatMap((item) => {
      if (typeof item === 'string') return item.split(',')
      return Array.isArray(item) ? item : []
    })
    return candidates.filter((path): path is string => typeof path === 'string').map((path) => path.trim()).filter(Boolean).slice(0, 100)
  } catch {
    return []
  }
}

export function analyzeInstructions(artifacts: AiArtifact[]): InstructionAnalysisFinding[] {
  const instructions = artifacts.filter((artifact) => artifact.kind === 'instruction')
  const findings: InstructionAnalysisFinding[] = []
  const digestOwners = new Map<string, AiArtifact>()
  const tagsByCategory = new Map<string, AiArtifact>()
  for (const artifact of instructions) {
    const digest = readMetadataString(artifact, 'contentDigest')
    if (digest) {
      const owner = digestOwners.get(digest)
      if (owner && owner.path !== artifact.path) findings.push({ code: 'INSTRUCTION_DUPLICATE', path: artifact.path, clientId: artifact.clientId })
      else digestOwners.set(digest, artifact)
    }
    for (const tag of readMetadataString(artifact, 'conflictTags').split(',').filter(Boolean)) {
      const [category] = tag.split(':')
      const existing = tagsByCategory.get(`${artifact.clientId}:${category}`)
      const existingTags = existing ? readMetadataString(existing, 'conflictTags').split(',') : []
      if (existing && existing.path !== artifact.path && !existingTags.includes(tag)) {
        findings.push({ code: 'INSTRUCTION_CONFLICT', path: artifact.path, clientId: artifact.clientId })
      } else {
        tagsByCategory.set(`${artifact.clientId}:${category}`, artifact)
      }
    }
  }
  return findings
}

function detectConflictTags(source: string): string[] {
  const tags: string[] = []
  if (/\b(?:use|indent with)\s+tabs\b/i.test(source)) tags.push('indent:tabs')
  if (/\b(?:use\s+)?2[- ]space indentation\b|\bindent(?:ation)?\s*[:=]\s*2\b/i.test(source)) tags.push('indent:spaces2')
  if (/\b(?:use\s+)?4[- ]space indentation\b|\bindent(?:ation)?\s*[:=]\s*4\b/i.test(source)) tags.push('indent:spaces4')
  if (/\buse\s+npm\b|\bnpm\s+(?:install|run)\b/i.test(source)) tags.push('packageManager:npm')
  if (/\buse\s+yarn\b|\byarn\s+(?:install|run)\b/i.test(source)) tags.push('packageManager:yarn')
  if (/\buse\s+pnpm\b|\bpnpm\s+(?:install|run)\b/i.test(source)) tags.push('packageManager:pnpm')
  if (/\b(?:must|always|required to)\s+(?:run\s+)?(?:the\s+)?tests?\b/i.test(source)) tags.push('tests:required')
  if (/\b(?:skip|do not run|never run)\s+(?:the\s+)?tests?\b/i.test(source)) tags.push('tests:skip')
  return [...new Set(tags)]
}

function readMetadataString(artifact: AiArtifact, key: string): string {
  const value = artifact.metadata?.[key]
  return typeof value === 'string' ? value : ''
}

function normalize(source: string): string {
  return source.replace(/\r\n/g, '\n').trim().replace(/[ \t]+/g, ' ')
}

function estimateTokens(value: string): number {
  const cjk = (value.match(/[\u3400-\u9fff\uf900-\ufaff]/g) ?? []).length
  return Math.max(1, Math.ceil((value.length - cjk) / 4 + cjk))
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

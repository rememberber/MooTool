import { realpath } from 'node:fs/promises'
import { isAbsolute, relative, resolve, sep } from 'node:path'
import type { AiArtifact } from '../../../src/shared/contracts/ai'
import type { AiEffectiveInstruction, AiInstructionPreview, AiInstructionPreviewInput } from '../../../src/shared/contracts/aiInstructions'
import { AiDiscoveryService } from './discoveryService'

export class InstructionScopeService {
  constructor(private readonly discovery: AiDiscoveryService) {}

  async preview(input: AiInstructionPreviewInput): Promise<AiInstructionPreview> {
    const projectRoot = await realpath(resolve(input.projectRoot))
    const targetPath = await realpath(resolve(input.targetPath))
    ensureInside(projectRoot, targetPath)
    const snapshot = await this.discovery.scan({ projectRoot })
    const instructions = snapshot.artifacts
      .filter((artifact) => artifact.kind === 'instruction' && (!input.clientId || artifact.clientId === input.clientId))
      .flatMap((artifact) => effectiveInstruction(artifact, projectRoot, targetPath))
      .sort((left, right) => left.order - right.order || clientOrder(left.clientId) - clientOrder(right.clientId) || left.path.localeCompare(right.path))
      .map((instruction, index) => ({ ...instruction, order: index + 1 }))
    const instructionPaths = new Set(instructions.map((instruction) => instruction.path))
    return {
      projectRoot,
      targetPath,
      instructions,
      totalEstimatedTokens: instructions.reduce((sum, instruction) => sum + instruction.estimatedTokens, 0),
      diagnostics: snapshot.diagnostics.filter((diagnostic) => diagnostic.code.startsWith('INSTRUCTION_') && (!diagnostic.path || instructionPaths.has(diagnostic.path)))
    }
  }
}

function clientOrder(clientId: AiArtifact['clientId']): number {
  if (clientId === 'codex') return 0
  if (clientId === 'claudeCode') return 1
  if (clientId === 'cursor') return 2
  if (clientId === 'geminiCli') return 3
  return 4
}

function effectiveInstruction(artifact: AiArtifact, projectRoot: string, targetPath: string): AiEffectiveInstruction[] {
  const appliesTo = metadataString(artifact, 'appliesTo')
  const estimatedTokens = metadataNumber(artifact, 'estimatedTokens')
  if (artifact.scope === 'user') return [entry('userScope', 0)]

  const directoryScoped = artifact.clientId === 'codex'
    || artifact.clientId === 'geminiCli'
    || (artifact.clientId === 'githubCopilot' && (artifact.name === 'AGENTS.md' || artifact.path.endsWith(`${sep}AGENTS.md`)))
  if (directoryScoped) {
    if (!appliesTo || !isInside(appliesTo, targetPath)) return []
    return [entry('directoryAncestor', 100 + pathDepth(relative(projectRoot, appliesTo)))]
  }

  const patterns = metadataString(artifact, 'pathPatterns').split('\n').map((value) => value.trim()).filter(Boolean)
  if (patterns.length > 0) {
    const targetRelative = relative(projectRoot, targetPath).split(sep).join('/')
    if (!patterns.some((pattern) => matchesGlob(targetRelative, pattern))) return []
    return [entry('pathPattern', 250 + pathDepth(relative(projectRoot, artifact.path)))]
  }
  if (!appliesTo || !isInside(appliesTo, targetPath)) return []
  return [entry('projectScope', 200 + pathDepth(relative(projectRoot, appliesTo)))]

  function entry(reason: AiEffectiveInstruction['reason'], order: number): AiEffectiveInstruction {
    return {
      artifactId: artifact.id,
      name: artifact.name,
      path: artifact.path,
      clientId: artifact.clientId,
      scope: artifact.scope,
      appliesTo,
      estimatedTokens,
      reason,
      order
    }
  }
}

function matchesGlob(path: string, pattern: string): boolean {
  const normalized = pattern.replace(/^\.\//, '').replaceAll('\\', '/')
  let expression = '^'
  for (let index = 0; index < normalized.length; index += 1) {
    const character = normalized[index]
    if (character === '*' && normalized[index + 1] === '*') {
      expression += '.*'
      index += 1
    } else if (character === '*') expression += '[^/]*'
    else if (character === '?') expression += '[^/]'
    else expression += character.replace(/[|\\{}()[\]^$+?.]/g, '\\$&')
  }
  return new RegExp(`${expression}(?:/.*)?$`).test(path)
}

function metadataString(artifact: AiArtifact, key: string): string {
  const value = artifact.metadata?.[key]
  return typeof value === 'string' ? value : ''
}

function metadataNumber(artifact: AiArtifact, key: string): number {
  const value = artifact.metadata?.[key]
  return typeof value === 'number' ? value : 0
}

function pathDepth(value: string): number {
  return value ? value.split(sep).length : 0
}

function ensureInside(root: string, target: string): void {
  if (!isInside(root, target)) throw new Error('Instruction preview target escapes the selected project')
}

function isInside(root: string, target: string): boolean {
  const relation = relative(root, target)
  return relation === '' || (!relation.startsWith(`..${sep}`) && relation !== '..' && !isAbsolute(relation))
}

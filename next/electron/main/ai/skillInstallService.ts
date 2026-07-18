import { createHash } from 'node:crypto'
import { lstat, readFile, readdir, realpath } from 'node:fs/promises'
import { join, relative, resolve, sep } from 'node:path'
import type { AiClientId } from '../../../src/shared/contracts/ai'
import type { AiChangeApplyResult, AiChangeRollbackResult } from '../../../src/shared/contracts/aiChanges'
import type { AiSkillInstallInput, AiSkillInstallPreview } from '../../../src/shared/contracts/aiSkills'
import { ConfigChangeService } from './configChangeService'
import { inspectSkillPackage, type SkillInspectionFinding } from './skillInspector'

type SkillInstallServiceOptions = {
  homeDirectory: string
  changes: ConfigChangeService
}

type SourceFile = {
  path: string
  relativePath: string
  content: Buffer
  hash: string
  binary: boolean
  executable: boolean
}

type PendingInstall = {
  sourceDirectory: string
  targetDirectory: string
  files: Array<Pick<SourceFile, 'path' | 'relativePath' | 'hash' | 'executable'>>
  requiresRiskConfirmation: boolean
}

const maximumFiles = 500
const maximumFileBytes = 5 * 1024 * 1024
const maximumPackageBytes = 25 * 1024 * 1024

export class SkillInstallService {
  private readonly homeDirectory: string
  private readonly changes: ConfigChangeService
  private readonly pending = new Map<string, PendingInstall>()

  constructor(options: SkillInstallServiceOptions) {
    this.homeDirectory = resolve(options.homeDirectory)
    this.changes = options.changes
  }

  async preview(input: AiSkillInstallInput): Promise<AiSkillInstallPreview> {
    validateInput(input)
    const sourceDirectory = await realpath(resolve(input.sourceDirectory))
    const sourceInfo = await lstat(sourceDirectory)
    if (sourceInfo.isSymbolicLink() || !sourceInfo.isDirectory()) throw new Error('Skill source must be a regular directory')
    const inspection = await inspectSkillPackage(sourceDirectory)
    const blocking = inspection.findings.find((finding) => finding.severity === 'error' || finding.code === 'PLAINTEXT_SECRET_RISK')
    if (blocking) throw new Error(`Skill install blocked: ${blocking.code}`)
    const name = inspection.metadata.declaredName
    if (!/^[a-z0-9][a-z0-9._-]{0,127}$/i.test(name)) throw new Error('Skill name is not safe for installation')
    const root = input.scope === 'project' ? await requireProjectRoot(input.projectRoot) : await realpath(this.homeDirectory)
    const targetRelativeRoot = input.targetClientId === 'codex'
      ? join('.agents', 'skills', name)
      : join('.claude', 'skills', name)
    const targetDirectory = join(root, targetRelativeRoot)
    if (await pathExists(targetDirectory)) throw new Error('Target Skill directory already exists')

    const files = await collectSourceFiles(sourceDirectory)
    const plan = await this.changes.createPlan(root, files.map((file) => ({
      targetPath: join(targetRelativeRoot, file.relativePath),
      ...(file.binary ? { nextContentBase64: file.content.toString('base64') } : { nextContent: file.content.toString('utf8') }),
      summary: `Install Skill file ${file.relativePath}`,
      expectedState: 'missing' as const,
      mode: file.executable ? 0o700 as const : 0o600 as const
    })))
    const requiresRiskConfirmation = inspection.findings.some((finding) => finding.severity === 'warning')
    this.pending.set(plan.id, {
      sourceDirectory,
      targetDirectory,
      files: files.map(({ path, relativePath, hash, executable }) => ({ path, relativePath, hash, executable })),
      requiresRiskConfirmation
    })
    return {
      plan,
      name,
      description: inspection.metadata.description,
      sourceDirectory,
      targetDirectory,
      targetClientId: input.targetClientId,
      scope: input.scope,
      files: files.map((file) => ({ relativePath: file.relativePath, sizeBytes: file.content.byteLength, binary: file.binary, executable: file.executable })),
      findings: inspection.findings.map((finding) => publicFinding(sourceDirectory, finding)),
      totalSizeBytes: files.reduce((sum, file) => sum + file.content.byteLength, 0),
      estimatedTokens: inspection.metadata.estimatedTokens,
      requiresRiskConfirmation
    }
  }

  async apply(planId: string, confirmRisks: boolean): Promise<AiChangeApplyResult> {
    const pending = this.pending.get(planId)
    if (!pending) throw new Error('Unknown or expired Skill install plan')
    if (pending.requiresRiskConfirmation && confirmRisks !== true) throw new Error('Skill risks must be confirmed before installation')
    const currentFiles = await collectSourceFiles(pending.sourceDirectory)
    if (!sameManifest(pending.files, currentFiles)) throw new Error('Skill source changed after preview')
    const result = await this.changes.apply(planId)
    try {
      const installedFiles = await collectSourceFiles(pending.targetDirectory)
      if (!sameManifest(pending.files, installedFiles)) throw new Error('Installed Skill verification failed')
      this.pending.delete(planId)
      return result
    } catch (error) {
      await this.changes.rollback(result.snapshotId)
      this.pending.delete(planId)
      throw error
    }
  }

  rollback(snapshotId: string): Promise<AiChangeRollbackResult> {
    return this.changes.rollback(snapshotId)
  }
}

async function collectSourceFiles(root: string): Promise<SourceFile[]> {
  const files: SourceFile[] = []
  const queue = [root]
  let totalBytes = 0
  while (queue.length > 0) {
    const directory = queue.shift()!
    const entries = await readdir(directory, { withFileTypes: true })
    for (const entry of entries) {
      if (entry.name === '.git' || entry.name === '.DS_Store') continue
      const path = join(directory, entry.name)
      if (entry.isSymbolicLink()) throw new Error(`Skill package contains a symbolic link: ${relative(root, path)}`)
      if (entry.isDirectory()) {
        queue.push(path)
        continue
      }
      if (!entry.isFile()) throw new Error(`Skill package contains an unsupported filesystem entry: ${relative(root, path)}`)
      if (files.length >= maximumFiles) throw new Error('Skill package exceeds the 500-file limit')
      const info = await lstat(path)
      if (info.size > maximumFileBytes) throw new Error(`Skill file exceeds 5 MB: ${relative(root, path)}`)
      totalBytes += info.size
      if (totalBytes > maximumPackageBytes) throw new Error('Skill package exceeds 25 MB')
      const content = await readFile(path)
      const relativePath = relative(root, path).split(sep).join('/')
      const binary = !Buffer.from(content.toString('utf8'), 'utf8').equals(content) || content.includes(0)
      files.push({
        path,
        relativePath,
        content,
        hash: createHash('sha256').update(content).digest('hex'),
        binary,
        executable: Boolean(info.mode & 0o111)
      })
    }
  }
  return files.sort((left, right) => left.relativePath.localeCompare(right.relativePath))
}

function sameManifest(expected: PendingInstall['files'], actual: SourceFile[]): boolean {
  if (expected.length !== actual.length) return false
  return expected.every((file, index) => file.relativePath === actual[index]?.relativePath
    && file.hash === actual[index]?.hash
    && file.executable === actual[index]?.executable)
}

function publicFinding(root: string, finding: SkillInspectionFinding): AiSkillInstallPreview['findings'][number] {
  const path = resolve(finding.path)
  const relation = relative(root, path)
  return { code: finding.code, severity: finding.severity, relativePath: relation.startsWith('..') ? '' : relation.split(sep).join('/') }
}

async function requireProjectRoot(value?: string): Promise<string> {
  if (!value?.trim()) throw new Error('A project must be selected for project-scope Skill installation')
  const root = await realpath(resolve(value))
  if (!(await lstat(root)).isDirectory()) throw new Error('Skill project root must be a directory')
  return root
}

async function pathExists(path: string): Promise<boolean> {
  try {
    await lstat(path)
    return true
  } catch (error) {
    if (typeof error === 'object' && error !== null && 'code' in error && error.code === 'ENOENT') return false
    throw error
  }
}

function validateInput(value: AiSkillInstallInput): void {
  if (!value || typeof value !== 'object' || typeof value.sourceDirectory !== 'string' || value.sourceDirectory.length > 4096) throw new Error('Invalid Skill install input')
  if (!['codex', 'claudeCode'].includes(value.targetClientId)) throw new Error('Unsupported Skill target client')
  if (!['user', 'project'].includes(value.scope)) throw new Error('Invalid Skill target scope')
  if (value.projectRoot !== undefined && (typeof value.projectRoot !== 'string' || value.projectRoot.length > 4096)) throw new Error('Invalid Skill project root')
}

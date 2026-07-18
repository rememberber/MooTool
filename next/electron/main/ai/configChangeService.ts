import { createHash, randomUUID } from 'node:crypto'
import { lstat, mkdir, open, readFile, realpath, rename, rmdir, stat, unlink } from 'node:fs/promises'
import { basename, dirname, isAbsolute, join, relative, resolve, sep } from 'node:path'
import { createTwoFilesPatch } from 'diff'
import type {
  AiChangeApplyResult,
  AiChangeOperationInput,
  AiChangeOperationPreview,
  AiChangePlan,
  AiChangeRollbackResult
} from '../../../src/shared/contracts/aiChanges'
import { redactSensitiveContent } from './securityScanner'

export type SnapshotProtector = {
  isAvailable(): boolean
  encrypt(value: Buffer): Buffer
  decrypt(value: Buffer): Buffer
}

type AtomicWriter = (path: string, content: Buffer, mode: number) => Promise<void>

type ConfigChangeServiceOptions = {
  snapshotDirectory: string
  protector: SnapshotProtector
  planTtlMs?: number
  clock?: () => Date
  atomicWriter?: AtomicWriter
}

type TargetState = {
  targetPath: string
  existed: boolean
  content: Buffer
  hash?: string
  mode: number
}

type InternalOperation = {
  preview: AiChangeOperationPreview
  nextContent: Buffer
  before: TargetState
  nextMode: number
}

type InternalPlan = {
  publicPlan: AiChangePlan
  operations: InternalOperation[]
  applied: boolean
}

type SnapshotFile = {
  targetPath: string
  existed: boolean
  contentBase64: string
  beforeHash?: string
  afterHash: string
  mode: number
}

type SnapshotEnvelope = {
  version: 2
  id: string
  planId: string
  rootPath: string
  createdAt: string
  files: SnapshotFile[]
  createdDirectories: string[]
}

const maximumConfigBytes = 5 * 1024 * 1024
const maximumPlanBytes = 25 * 1024 * 1024
const lockedTargets = new Set<string>()

export class ConfigChangeService {
  private readonly snapshotDirectory: string
  private readonly protector: SnapshotProtector
  private readonly planTtlMs: number
  private readonly clock: () => Date
  private readonly atomicWriter: AtomicWriter
  private readonly plans = new Map<string, InternalPlan>()

  constructor(options: ConfigChangeServiceOptions) {
    this.snapshotDirectory = resolve(options.snapshotDirectory)
    this.protector = options.protector
    this.planTtlMs = options.planTtlMs ?? 15 * 60 * 1_000
    this.clock = options.clock ?? (() => new Date())
    this.atomicWriter = options.atomicWriter ?? writeFileAtomically
  }

  async createPlan(rootPath: string, inputs: AiChangeOperationInput[]): Promise<AiChangePlan> {
    if (!Array.isArray(inputs) || inputs.length === 0 || inputs.length > 500) throw new Error('A change plan requires 1-500 operations')
    const root = await resolveDirectory(rootPath)
    const targets = new Set<string>()
    const operations: InternalOperation[] = []
    let planBytes = 0

    for (const input of inputs) {
      validateOperationInput(input)
      const targetPath = await resolveTarget(root, input.targetPath)
      if (targets.has(targetPath)) throw new Error('A change plan cannot contain duplicate targets')
      targets.add(targetPath)
      const before = await inspectTarget(root, targetPath)
      if (input.expectedState === 'missing' && before.existed) throw new Error(`AI configuration target already exists: ${targetPath}`)
      if (input.expectedState === 'existing' && !before.existed) throw new Error(`AI configuration target does not exist: ${targetPath}`)
      const binary = typeof input.nextContentBase64 === 'string'
      const nextContent = binary ? Buffer.from(input.nextContentBase64!, 'base64') : Buffer.from(input.nextContent!, 'utf8')
      if (nextContent.byteLength > maximumConfigBytes) throw new Error('AI configuration content exceeds 5 MB')
      planBytes += nextContent.byteLength
      if (planBytes > maximumPlanBytes) throw new Error('AI change plan exceeds 25 MB')
      const preview: AiChangeOperationPreview = {
        id: stableId(targetPath, before.hash ?? '', hash(nextContent)),
        kind: before.existed ? 'update' : 'create',
        targetPath,
        summary: input.summary.trim(),
        expectedHash: before.hash,
        nextHash: hash(nextContent),
        beforeSizeBytes: before.content.byteLength,
        afterSizeBytes: nextContent.byteLength,
        redactedDiff: binary
          ? `Binary file: ${before.content.byteLength} bytes -> ${nextContent.byteLength} bytes`
          : createTwoFilesPatch(
              basename(targetPath),
              basename(targetPath),
              redactSensitiveContent(before.content.toString('utf8')),
              redactSensitiveContent(nextContent.toString('utf8')),
              'before',
              'after',
              { context: 3 }
            ),
        binary,
        executable: input.mode === 0o700
      }
      operations.push({ preview, nextContent, before, nextMode: input.mode ?? before.mode })
    }

    const createdAt = this.clock()
    const publicPlan: AiChangePlan = {
      id: randomUUID(),
      rootPath: root,
      createdAt: createdAt.toISOString(),
      expiresAt: new Date(createdAt.getTime() + this.planTtlMs).toISOString(),
      state: 'pending',
      operations: operations.map((operation) => operation.preview)
    }
    this.plans.set(publicPlan.id, { publicPlan, operations, applied: false })
    return publicPlan
  }

  async apply(planId: string): Promise<AiChangeApplyResult> {
    const plan = this.getPendingPlan(planId)
    this.assertNotExpired(plan.publicPlan)
    const targets = plan.operations.map((operation) => operation.preview.targetPath)
    return this.withTargetLocks(targets, async () => {
      if (!this.protector.isAvailable()) throw new Error('Secure snapshot storage is unavailable')
      for (const operation of plan.operations) {
        const current = await inspectTarget(plan.publicPlan.rootPath, operation.preview.targetPath)
        if (current.existed !== operation.before.existed || current.hash !== operation.before.hash) {
          throw new Error(`AI configuration changed after preview: ${operation.preview.targetPath}`)
        }
      }

      const snapshotId = randomUUID()
      const directoriesToCreate = await collectMissingDirectories(plan.publicPlan.rootPath, targets)
      const envelope: SnapshotEnvelope = {
        version: 2,
        id: snapshotId,
        planId,
        rootPath: plan.publicPlan.rootPath,
        createdAt: this.clock().toISOString(),
        files: plan.operations.map((operation) => ({
          targetPath: operation.preview.targetPath,
          existed: operation.before.existed,
          contentBase64: operation.before.content.toString('base64'),
          beforeHash: operation.before.hash,
          afterHash: operation.preview.nextHash,
          mode: operation.before.mode
        })),
        createdDirectories: directoriesToCreate
      }
      await this.saveSnapshot(envelope)

      const applied: InternalOperation[] = []
      const createdDirectories: string[] = []
      try {
        for (const directory of directoriesToCreate) {
          await mkdir(directory, { mode: 0o755 })
          createdDirectories.push(directory)
        }
        for (const operation of plan.operations) {
          await this.atomicWriter(operation.preview.targetPath, operation.nextContent, operation.nextMode)
          applied.push(operation)
        }
      } catch (error) {
        await this.restoreOperations(applied.reverse().map((operation) => operation.before))
        await removeCreatedDirectories(createdDirectories)
        throw error
      }

      plan.applied = true
      return {
        planId,
        snapshotId,
        appliedAt: this.clock().toISOString(),
        operationCount: plan.operations.length
      }
    })
  }

  async rollback(snapshotId: string): Promise<AiChangeRollbackResult> {
    const envelope = await this.readSnapshot(snapshotId)
    const root = await resolveDirectory(envelope.rootPath)
    const targets = envelope.files.map((file) => file.targetPath)
    return this.withTargetLocks(targets, async () => {
      for (const file of envelope.files) {
        const current = await inspectTarget(root, file.targetPath)
        if (!current.existed || current.hash !== file.afterHash) {
          throw new Error(`AI configuration changed after apply: ${file.targetPath}`)
        }
      }
      await this.restoreOperations(envelope.files.slice().reverse().map((file) => ({
        targetPath: file.targetPath,
        existed: file.existed,
        content: Buffer.from(file.contentBase64, 'base64'),
        hash: file.beforeHash,
        mode: file.mode
      })))
      await removeCreatedDirectories(envelope.createdDirectories)
      return {
        snapshotId,
        rolledBackAt: this.clock().toISOString(),
        operationCount: envelope.files.length
      }
    })
  }

  private getPendingPlan(planId: string): InternalPlan {
    if (typeof planId !== 'string') throw new Error('Invalid change plan id')
    const plan = this.plans.get(planId)
    if (!plan) throw new Error('Unknown change plan')
    if (plan.applied) throw new Error('Change plan has already been applied')
    return plan
  }

  private assertNotExpired(plan: AiChangePlan): void {
    if (this.clock().getTime() > Date.parse(plan.expiresAt)) throw new Error('Change plan has expired')
  }

  private async saveSnapshot(envelope: SnapshotEnvelope): Promise<void> {
    await mkdir(this.snapshotDirectory, { recursive: true, mode: 0o700 })
    const cleartext = Buffer.from(JSON.stringify(envelope), 'utf8')
    const encrypted = this.protector.encrypt(cleartext)
    await writeFileAtomically(this.snapshotPath(envelope.id), encrypted, 0o600)
  }

  private async readSnapshot(snapshotId: string): Promise<SnapshotEnvelope> {
    if (!/^[0-9a-f-]{36}$/i.test(snapshotId)) throw new Error('Invalid snapshot id')
    if (!this.protector.isAvailable()) throw new Error('Secure snapshot storage is unavailable')
    const encrypted = await readFile(this.snapshotPath(snapshotId))
    const parsed = JSON.parse(this.protector.decrypt(encrypted).toString('utf8')) as unknown
    if (!isSnapshotEnvelope(parsed) || parsed.id !== snapshotId) throw new Error('Invalid configuration snapshot')
    return parsed
  }

  private snapshotPath(snapshotId: string): string {
    return join(this.snapshotDirectory, `${snapshotId}.snapshot`)
  }

  private async restoreOperations(states: TargetState[]): Promise<void> {
    for (const state of states) {
      if (state.existed) {
        await this.atomicWriter(state.targetPath, state.content, state.mode)
      } else {
        await unlink(state.targetPath).catch((error) => {
          if (!isMissing(error)) throw error
        })
      }
    }
  }

  private async withTargetLocks<T>(targets: string[], task: () => Promise<T>): Promise<T> {
    if (targets.some((target) => lockedTargets.has(target))) throw new Error('Another AI configuration change is already in progress')
    targets.forEach((target) => lockedTargets.add(target))
    try {
      return await task()
    } finally {
      targets.forEach((target) => lockedTargets.delete(target))
    }
  }
}

export async function writeFileAtomically(path: string, content: Buffer, mode: number): Promise<void> {
  const temporaryPath = join(dirname(path), `.${randomUUID()}.mootool.tmp`)
  const handle = await open(temporaryPath, 'wx', mode || 0o600)
  try {
    await handle.writeFile(content)
    await handle.sync()
  } catch (error) {
    await handle.close().catch(() => undefined)
    await unlink(temporaryPath).catch(() => undefined)
    throw error
  }
  await handle.close()
  try {
    await rename(temporaryPath, path)
  } catch (error) {
    await unlink(temporaryPath).catch(() => undefined)
    throw error
  }
}

async function inspectTarget(root: string, targetPath: string): Promise<TargetState> {
  ensureInside(root, targetPath)
  try {
    const info = await lstat(targetPath)
    if (info.isSymbolicLink()) throw new Error(`Symbolic-link AI configuration targets are not allowed: ${targetPath}`)
    if (!info.isFile()) throw new Error(`AI configuration target must be a file: ${targetPath}`)
    const canonical = await realpath(targetPath)
    ensureInside(root, canonical)
    if (info.size > maximumConfigBytes) throw new Error('AI configuration content exceeds 5 MB')
    const content = await readFile(canonical)
    return { targetPath: canonical, existed: true, content, hash: hash(content), mode: info.mode & 0o777 }
  } catch (error) {
    if (!isMissing(error)) throw error
    const canonical = await canonicalizeTarget(root, targetPath)
    if (canonical !== targetPath) throw new Error(`AI configuration target path changed through a symbolic link: ${targetPath}`)
    return { targetPath, existed: false, content: Buffer.alloc(0), mode: 0o600 }
  }
}

async function resolveTarget(root: string, value: string): Promise<string> {
  if (typeof value !== 'string' || !value.trim() || value.length > 4096) throw new Error('Invalid AI configuration target')
  const requested = resolve(isAbsolute(value) ? value : join(root, value))
  const target = await canonicalizeTarget(root, requested)
  ensureInside(root, target)
  return target
}

function ensureInside(root: string, target: string): void {
  const relation = relative(root, target)
  if (relation === '' || (!relation.startsWith(`..${sep}`) && relation !== '..' && !isAbsolute(relation))) return
  throw new Error('AI configuration target escapes the allowed root')
}

async function resolveDirectory(value: string): Promise<string> {
  if (typeof value !== 'string' || !value.trim()) throw new Error('Invalid AI configuration root')
  const canonical = await realpath(resolve(value))
  if (!(await stat(canonical)).isDirectory()) throw new Error('AI configuration root must be a directory')
  return canonical
}

function validateOperationInput(value: AiChangeOperationInput): void {
  if (!value || typeof value !== 'object' || typeof value.targetPath !== 'string') {
    throw new Error('Invalid AI change operation')
  }
  const hasText = typeof value.nextContent === 'string'
  const hasBinary = typeof value.nextContentBase64 === 'string' && /^[A-Za-z0-9+/]*={0,2}$/.test(value.nextContentBase64)
  if (hasText === hasBinary) throw new Error('AI change operation requires exactly one content encoding')
  if (typeof value.summary !== 'string' || !value.summary.trim() || value.summary.length > 500) throw new Error('AI change summary is required')
  if (value.expectedState && !['missing', 'existing', 'any'].includes(value.expectedState)) throw new Error('Invalid AI change target state')
  if (value.mode !== undefined && value.mode !== 0o600 && value.mode !== 0o700) throw new Error('Invalid AI change file mode')
}

function hash(value: Buffer): string {
  return createHash('sha256').update(value).digest('hex')
}

function stableId(...parts: string[]): string {
  return createHash('sha256').update(parts.join('\0')).digest('hex').slice(0, 24)
}

function isMissing(error: unknown): boolean {
  return isRecord(error) && error.code === 'ENOENT'
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isSnapshotEnvelope(value: unknown): value is SnapshotEnvelope {
  if (!isRecord(value) || value.version !== 2 || typeof value.id !== 'string' || typeof value.planId !== 'string' || typeof value.rootPath !== 'string' || !Array.isArray(value.files) || !Array.isArray(value.createdDirectories) || !value.createdDirectories.every((item) => typeof item === 'string')) return false
  return value.files.every((file) => isRecord(file)
    && typeof file.targetPath === 'string'
    && typeof file.existed === 'boolean'
    && typeof file.contentBase64 === 'string'
    && typeof file.afterHash === 'string'
    && typeof file.mode === 'number')
}

async function canonicalizeTarget(root: string, requested: string): Promise<string> {
  let cursor = dirname(requested)
  const missingSegments: string[] = []
  while (true) {
    try {
      const info = await lstat(cursor)
      if (!info.isDirectory()) throw new Error(`AI configuration parent must be a directory: ${cursor}`)
      break
    } catch (error) {
      if (!isMissing(error)) throw error
      const parent = dirname(cursor)
      if (parent === cursor) throw new Error('AI configuration target has no existing parent')
      missingSegments.unshift(basename(cursor))
      cursor = parent
    }
  }
  const canonicalParent = await realpath(cursor)
  ensureInside(root, canonicalParent)
  return join(canonicalParent, ...missingSegments, basename(requested))
}

async function collectMissingDirectories(root: string, targets: string[]): Promise<string[]> {
  const missing = new Set<string>()
  for (const target of targets) {
    let cursor = dirname(target)
    while (cursor !== root) {
      ensureInside(root, cursor)
      try {
        const info = await lstat(cursor)
        if (info.isSymbolicLink() || !info.isDirectory()) throw new Error(`Unsafe AI configuration parent: ${cursor}`)
        break
      } catch (error) {
        if (!isMissing(error)) throw error
        missing.add(cursor)
        cursor = dirname(cursor)
      }
    }
  }
  return [...missing].sort((left, right) => left.split(sep).length - right.split(sep).length || left.localeCompare(right))
}

async function removeCreatedDirectories(directories: string[]): Promise<void> {
  for (const directory of directories.slice().reverse()) {
    await rmdir(directory).catch((error) => {
      if (!isMissing(error) && !(isRecord(error) && error.code === 'ENOTEMPTY')) throw error
    })
  }
}

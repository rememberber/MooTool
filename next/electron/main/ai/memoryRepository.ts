import { createHash, randomUUID } from 'node:crypto'
import { mkdirSync } from 'node:fs'
import { dirname, isAbsolute, relative, resolve, sep } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import type {
  AiEffectiveMemory,
  AiMemory,
  AiMemoryCandidate,
  AiMemoryCandidateReviewInput,
  AiMemoryCandidateSaveInput,
  AiMemoryListInput,
  AiMemoryPreview,
  AiMemoryPreviewInput,
  AiMemorySaveInput,
  AiMemorySnapshot,
  AiMemoryStats
} from '../../../src/shared/contracts/aiMemory'
import type { AiMemoryEmbeddingStatus, AiMemorySemanticPreview, AiMemorySemanticPreviewInput } from '../../../src/shared/contracts/aiMemoryEmbedding'
import type { AiModelRuntimeId } from '../../../src/shared/contracts/aiModelRuntime'
import { initializeAiMemorySchema } from './memorySchema'
import { scanSensitiveContent } from './securityScanner'

export class AiMemoryRepository {
  private readonly database: DatabaseSync
  private readonly clock: () => Date

  constructor(databasePath: string, clock: () => Date = () => new Date()) {
    mkdirSync(dirname(databasePath), { recursive: true })
    this.database = new DatabaseSync(databasePath)
    this.database.exec('PRAGMA journal_mode = WAL; PRAGMA busy_timeout = 3000;')
    initializeAiMemorySchema(this.database)
    this.clock = clock
  }

  snapshot(input: AiMemoryListInput = {}): AiMemorySnapshot {
    return { memories: this.list(input), candidates: this.listCandidates(), stats: this.stats() }
  }

  list(input: AiMemoryListInput = {}): AiMemory[] {
    const conditions: string[] = []
    const parameters: Array<string | number> = []
    if (!input.includeArchived) conditions.push('m.archived_at IS NULL')
    if (input.kind) { conditions.push('m.kind = ?'); parameters.push(input.kind) }
    if (input.scope) { conditions.push('m.scope = ?'); parameters.push(input.scope) }
    const keyword = input.keyword?.trim()
    const join = keyword ? 'JOIN ai_memory_fts ON ai_memory_fts.memory_id = m.id' : ''
    if (keyword) { conditions.push('ai_memory_fts MATCH ?'); parameters.push(ftsQuery(keyword)) }
    const rows = this.database.prepare(`
      SELECT m.* FROM ai_memories m ${join}
      ${conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : ''}
      ORDER BY m.archived_at IS NOT NULL, m.updated_at DESC, m.id
      LIMIT 1000
    `).all(...parameters)
    return rows.map(mapMemory)
  }

  save(input: AiMemorySaveInput): AiMemory {
    assertMemoryContent(input.content)
    const now = this.clock().toISOString()
    const id = input.id ?? randomUUID()
    const scopeValue = normalizedScopeValue(input.scope, input.scopeValue)
    const content = input.content.trim()
    const fingerprint = memoryFingerprint(content)
    try {
      if (input.id) {
        const result = this.database.prepare(`
          UPDATE ai_memories SET
            kind = ?, scope = ?, scope_value = ?, content = ?, source_kind = ?, source_ref = ?,
            confidence = ?, sensitivity = ?, updated_at = ?, expires_at = ?, fingerprint = ?
          WHERE id = ?
        `).run(input.kind, input.scope, scopeValue, content, input.sourceKind, input.sourceRef?.trim() || null,
          input.confidence, input.sensitivity, now, input.expiresAt ?? null, fingerprint, id)
        if (Number(result.changes) === 0) throw new Error('Agent memory no longer exists')
      } else {
        this.database.prepare(`
          INSERT INTO ai_memories (
            id, kind, scope, scope_value, content, source_kind, source_ref, confidence, sensitivity,
            created_by, created_at, updated_at, expires_at, fingerprint
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'user', ?, ?, ?, ?)
        `).run(id, input.kind, input.scope, scopeValue, content, input.sourceKind, input.sourceRef?.trim() || null,
          input.confidence, input.sensitivity, now, now, input.expiresAt ?? null, fingerprint)
      }
    } catch (error) {
      if (isUniqueConstraint(error)) throw new Error('An identical memory already exists in this scope')
      throw error
    }
    return this.getRequired(id)
  }

  archive(id: string): AiMemory {
    const result = this.database.prepare('UPDATE ai_memories SET archived_at = ?, updated_at = ? WHERE id = ?').run(this.clock().toISOString(), this.clock().toISOString(), id)
    if (Number(result.changes) === 0) throw new Error('Agent memory no longer exists')
    return this.getRequired(id)
  }

  restore(id: string): AiMemory {
    const result = this.database.prepare('UPDATE ai_memories SET archived_at = NULL, updated_at = ? WHERE id = ?').run(this.clock().toISOString(), id)
    if (Number(result.changes) === 0) throw new Error('Agent memory no longer exists')
    return this.getRequired(id)
  }

  delete(id: string): void {
    const result = this.database.prepare('DELETE FROM ai_memories WHERE id = ?').run(id)
    if (Number(result.changes) === 0) throw new Error('Agent memory no longer exists')
  }

  createCandidate(input: AiMemoryCandidateSaveInput): AiMemoryCandidate {
    assertMemoryContent(input.content)
    const id = randomUUID()
    this.database.prepare(`
      INSERT INTO ai_memory_candidates (
        id, kind, proposed_scope, proposed_scope_value, content, source_kind, source_ref,
        evidence_summary, confidence, sensitivity, status, created_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?)
    `).run(id, input.kind, input.proposedScope, normalizedScopeValue(input.proposedScope, input.proposedScopeValue), input.content.trim(),
      input.sourceKind, input.sourceRef.trim(), input.evidenceSummary.trim(), input.confidence, input.sensitivity, this.clock().toISOString())
    return this.getCandidateRequired(id)
  }

  reviewCandidate(input: AiMemoryCandidateReviewInput): AiMemoryCandidate {
    const candidate = this.getCandidateRequired(input.candidateId)
    if (candidate.status !== 'pending') throw new Error('Agent memory candidate has already been reviewed')
    const now = this.clock().toISOString()
    if (input.action === 'reject') {
      this.database.prepare("UPDATE ai_memory_candidates SET status = 'rejected', reviewed_at = ? WHERE id = ?").run(now, candidate.id)
      return this.getCandidateRequired(candidate.id)
    }

    const memoryId = randomUUID()
    const fingerprint = memoryFingerprint(candidate.content)
    this.database.exec('BEGIN IMMEDIATE')
    try {
      this.database.prepare(`
        INSERT INTO ai_memories (
          id, kind, scope, scope_value, content, source_kind, source_ref, confidence, sensitivity,
          created_by, created_at, updated_at, fingerprint
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'agentCandidate', ?, ?, ?)
      `).run(memoryId, candidate.kind, candidate.proposedScope, normalizedScopeValue(candidate.proposedScope, candidate.proposedScopeValue),
        candidate.content, candidate.sourceKind, candidate.sourceRef, candidate.confidence, candidate.sensitivity, now, now, fingerprint)
      this.database.prepare("UPDATE ai_memory_candidates SET status = 'approved', reviewed_at = ?, approved_memory_id = ? WHERE id = ?").run(now, memoryId, candidate.id)
      this.database.exec('COMMIT')
    } catch (error) {
      this.database.exec('ROLLBACK')
      if (isUniqueConstraint(error)) throw new Error('An identical memory already exists in this scope')
      throw error
    }
    return this.getCandidateRequired(candidate.id)
  }

  preview(input: AiMemoryPreviewInput): AiMemoryPreview {
    const now = this.clock().toISOString()
    const candidates = this.list({ keyword: input.query }).filter((memory) => !memory.archivedAt && !memory.supersededBy && (!memory.expiresAt || memory.expiresAt > now))
    const effective = candidates.flatMap((memory) => effectiveMemory(memory, input)).sort((left, right) =>
      reasonPriority(left.reason) - reasonPriority(right.reason)
      || right.memory.confidence - left.memory.confidence
      || right.memory.updatedAt.localeCompare(left.memory.updatedAt)
    )
    const selected: AiEffectiveMemory[] = []
    let tokens = 0
    for (const item of effective) {
      if (selected.length >= input.maxItems || tokens + item.estimatedTokens > input.tokenBudget) continue
      tokens += item.estimatedTokens
      selected.push({ ...item, rank: selected.length + 1 })
    }
    return { memories: selected, totalEstimatedTokens: tokens, omittedByBudget: effective.length - selected.length }
  }

  embeddingCandidates(): AiMemory[] {
    const now = this.clock().toISOString()
    return this.database.prepare(`
      SELECT * FROM ai_memories
      WHERE archived_at IS NULL
        AND superseded_by IS NULL
        AND (expires_at IS NULL OR expires_at > ?)
        AND sensitivity IN ('public', 'internal')
      ORDER BY updated_at DESC, id
      LIMIT 1000
    `).all(now).map(mapMemory)
  }

  embeddingStatus(): AiMemoryEmbeddingStatus {
    const now = this.clock().toISOString()
    const counts = this.database.prepare(`
      SELECT
        sum(CASE WHEN m.archived_at IS NULL AND m.superseded_by IS NULL AND (m.expires_at IS NULL OR m.expires_at > ?) AND m.sensitivity IN ('public', 'internal') THEN 1 ELSE 0 END) AS eligible,
        sum(CASE WHEN m.archived_at IS NULL AND m.superseded_by IS NULL AND (m.expires_at IS NULL OR m.expires_at > ?) AND m.sensitivity IN ('private', 'restricted') THEN 1 ELSE 0 END) AS skipped_sensitive
      FROM ai_memories m
    `).get(now, now) as Record<string, unknown>
    const metadata = this.database.prepare(`
      SELECT runtime_id, model, model_version, dimensions, generated_at
      FROM ai_memory_embeddings ORDER BY generated_at DESC LIMIT 1
    `).get() as Record<string, unknown> | undefined
    const current = metadata ? this.database.prepare(`
      SELECT count(*) AS count FROM ai_memory_embeddings e
      JOIN ai_memories m ON m.id = e.memory_id
      WHERE e.runtime_id = ? AND e.model = ? AND e.content_fingerprint = m.fingerprint
        AND m.archived_at IS NULL AND m.superseded_by IS NULL
        AND (m.expires_at IS NULL OR m.expires_at > ?)
        AND m.sensitivity IN ('public', 'internal')
    `).get(String(metadata.runtime_id), String(metadata.model), now) as Record<string, unknown> : { count: 0 }
    const indexed = Number(current.count ?? 0)
    const totalEmbeddings = Number((this.database.prepare('SELECT count(*) AS count FROM ai_memory_embeddings').get() as Record<string, unknown>).count ?? 0)
    const eligible = Number(counts.eligible ?? 0)
    return {
      available: true,
      eligible,
      indexed,
      stale: Math.max(0, totalEmbeddings - indexed),
      skippedSensitive: Number(counts.skipped_sensitive ?? 0),
      coverage: eligible > 0 ? indexed / eligible : 1,
      ...(metadata ? {
        runtimeId: String(metadata.runtime_id) as AiModelRuntimeId,
        model: String(metadata.model),
        ...(metadata.model_version == null ? {} : { modelVersion: String(metadata.model_version) }),
        dimensions: Number(metadata.dimensions),
        generatedAt: String(metadata.generated_at)
      } : {})
    }
  }

  replaceEmbeddings(entries: Array<{
    memoryId: string
    runtimeId: AiModelRuntimeId
    model: string
    modelVersion?: string
    contentFingerprint: string
    vector: number[]
    generatedAt: string
  }>): void {
    this.database.exec('BEGIN IMMEDIATE')
    try {
      this.database.exec('DELETE FROM ai_memory_embeddings')
      const insert = this.database.prepare(`
        INSERT INTO ai_memory_embeddings (
          memory_id, runtime_id, model, model_version, dimensions, embedding, content_fingerprint, generated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      `)
      for (const entry of entries) {
        if (entry.vector.length === 0 || entry.vector.length > 16_384 || entry.vector.some((value) => !Number.isFinite(value))) throw new Error('Embedding vector is invalid')
        const values = Float32Array.from(entry.vector)
        insert.run(entry.memoryId, entry.runtimeId, entry.model, entry.modelVersion ?? null, values.length,
          Buffer.from(values.buffer, values.byteOffset, values.byteLength), entry.contentFingerprint, entry.generatedAt)
      }
      this.database.exec('COMMIT')
    } catch (error) {
      this.database.exec('ROLLBACK')
      throw error
    }
  }

  semanticPreview(input: AiMemorySemanticPreviewInput, queryVector: number[]): AiMemorySemanticPreview {
    if (queryVector.length === 0 || queryVector.length > 16_384 || queryVector.some((value) => !Number.isFinite(value))) throw new Error('Semantic query embedding is invalid')
    const now = this.clock().toISOString()
    const effective: AiEffectiveMemory[] = []
    const rows = this.database.prepare(`
      SELECT m.*, e.dimensions AS embedding_dimensions, e.embedding AS embedding_blob
      FROM ai_memory_embeddings e
      JOIN ai_memories m ON m.id = e.memory_id
      WHERE e.runtime_id = ? AND e.model = ? AND e.content_fingerprint = m.fingerprint
        AND m.archived_at IS NULL AND m.superseded_by IS NULL
        AND (m.expires_at IS NULL OR m.expires_at > ?)
        AND m.sensitivity IN ('public', 'internal')
    `).iterate(input.runtimeId, input.model, now)
    for (const value of rows) {
      const row = value as Record<string, unknown>
      if (Number(row.embedding_dimensions) !== queryVector.length || !(row.embedding_blob instanceof Uint8Array)) continue
      const memory = mapMemory(row)
      const scoped = effectiveMemory(memory, input)[0]
      if (!scoped) continue
      scoped.semanticScore = cosineSimilarity(queryVector, readVector(row.embedding_blob, queryVector.length))
      effective.push(scoped)
    }
    effective.sort((left, right) => (right.semanticScore ?? -1) - (left.semanticScore ?? -1)
      || reasonPriority(left.reason) - reasonPriority(right.reason)
      || right.memory.confidence - left.memory.confidence)
    const selected: AiEffectiveMemory[] = []
    let tokens = 0
    for (const item of effective) {
      if (selected.length >= input.maxItems || tokens + item.estimatedTokens > input.tokenBudget) continue
      tokens += item.estimatedTokens
      selected.push({ ...item, rank: selected.length + 1 })
    }
    return {
      mode: 'semantic', runtimeId: input.runtimeId, model: input.model,
      indexedCandidates: effective.length, memories: selected, totalEstimatedTokens: tokens,
      omittedByBudget: effective.length - selected.length
    }
  }

  close(): void {
    if (this.database.isOpen) this.database.close()
  }

  private listCandidates(): AiMemoryCandidate[] {
    return this.database.prepare("SELECT * FROM ai_memory_candidates WHERE status = 'pending' ORDER BY created_at DESC, id").all().map(mapCandidate)
  }

  private stats(): AiMemoryStats {
    const now = this.clock()
    const soon = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1_000).toISOString()
    const current = now.toISOString()
    const memory = this.database.prepare(`
      SELECT
        sum(CASE WHEN archived_at IS NULL AND (expires_at IS NULL OR expires_at > ?) THEN 1 ELSE 0 END) AS active,
        sum(CASE WHEN archived_at IS NOT NULL THEN 1 ELSE 0 END) AS archived,
        sum(CASE WHEN archived_at IS NULL AND expires_at > ? AND expires_at <= ? THEN 1 ELSE 0 END) AS expiring_soon
      FROM ai_memories
    `).get(current, current, soon) as Record<string, unknown>
    const candidate = this.database.prepare("SELECT count(*) AS count FROM ai_memory_candidates WHERE status = 'pending'").get() as Record<string, unknown>
    return {
      active: Number(memory.active ?? 0),
      archived: Number(memory.archived ?? 0),
      expiringSoon: Number(memory.expiring_soon ?? 0),
      pendingCandidates: Number(candidate.count ?? 0)
    }
  }

  private getRequired(id: string): AiMemory {
    const row = this.database.prepare('SELECT * FROM ai_memories WHERE id = ?').get(id)
    if (!row) throw new Error('Agent memory no longer exists')
    return mapMemory(row)
  }

  private getCandidateRequired(id: string): AiMemoryCandidate {
    const row = this.database.prepare('SELECT * FROM ai_memory_candidates WHERE id = ?').get(id)
    if (!row) throw new Error('Agent memory candidate no longer exists')
    return mapCandidate(row)
  }
}

function effectiveMemory(memory: AiMemory, input: AiMemoryPreviewInput): AiEffectiveMemory[] {
  const estimatedTokens = estimateTokens(memory.content)
  if (memory.scope === 'user') return [entry('userScope')]
  if (memory.scope === 'project' && input.projectRoot && samePath(memory.scopeValue, input.projectRoot)) return [entry('projectScope')]
  if (memory.scope === 'directory' && input.targetPath && isInside(memory.scopeValue, input.targetPath)) return [entry('directoryScope')]
  if (memory.scope === 'branch' && memory.scopeValue && memory.scopeValue === input.branch) return [entry('branchScope')]
  if (memory.scope === 'agentProfile' && memory.scopeValue && memory.scopeValue === input.agentProfileId) return [entry('agentProfileScope')]
  if (memory.scope === 'task' && memory.scopeValue && memory.scopeValue === input.taskRef) return [entry('taskScope')]
  return []

  function entry(reason: AiEffectiveMemory['reason']): AiEffectiveMemory {
    return { memory, estimatedTokens, reason, rank: 0 }
  }
}

function normalizedScopeValue(scope: AiMemory['scope'], value?: string): string | null {
  if (scope === 'user') return null
  const normalized = value?.trim()
  if (!normalized) throw new Error(`A scope value is required for ${scope} memory`)
  if ((scope === 'project' || scope === 'directory') && !isAbsolute(normalized)) throw new Error(`${scope} memory scope must be an absolute path`)
  return scope === 'project' || scope === 'directory' ? resolve(normalized) : normalized
}

function mapMemory(row: Record<string, unknown>): AiMemory {
  return {
    id: String(row.id),
    kind: row.kind as AiMemory['kind'],
    scope: row.scope as AiMemory['scope'],
    ...(row.scope_value == null ? {} : { scopeValue: String(row.scope_value) }),
    content: String(row.content),
    sourceKind: row.source_kind as AiMemory['sourceKind'],
    ...(row.source_ref == null ? {} : { sourceRef: String(row.source_ref) }),
    confidence: Number(row.confidence),
    sensitivity: row.sensitivity as AiMemory['sensitivity'],
    createdBy: row.created_by as AiMemory['createdBy'],
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at),
    ...(row.last_used_at == null ? {} : { lastUsedAt: String(row.last_used_at) }),
    ...(row.expires_at == null ? {} : { expiresAt: String(row.expires_at) }),
    ...(row.archived_at == null ? {} : { archivedAt: String(row.archived_at) }),
    ...(row.superseded_by == null ? {} : { supersededBy: String(row.superseded_by) }),
    fingerprint: String(row.fingerprint)
  }
}

function mapCandidate(row: Record<string, unknown>): AiMemoryCandidate {
  return {
    id: String(row.id),
    kind: row.kind as AiMemoryCandidate['kind'],
    proposedScope: row.proposed_scope as AiMemoryCandidate['proposedScope'],
    ...(row.proposed_scope_value == null ? {} : { proposedScopeValue: String(row.proposed_scope_value) }),
    content: String(row.content),
    sourceKind: row.source_kind as AiMemoryCandidate['sourceKind'],
    sourceRef: String(row.source_ref),
    evidenceSummary: String(row.evidence_summary),
    confidence: Number(row.confidence),
    sensitivity: row.sensitivity as AiMemoryCandidate['sensitivity'],
    status: row.status as AiMemoryCandidate['status'],
    createdAt: String(row.created_at),
    ...(row.reviewed_at == null ? {} : { reviewedAt: String(row.reviewed_at) }),
    ...(row.approved_memory_id == null ? {} : { approvedMemoryId: String(row.approved_memory_id) })
  }
}

function assertMemoryContent(content: string): void {
  if (scanSensitiveContent(content).length > 0) throw new Error('Agent memory cannot contain plaintext credentials or private keys')
}

function memoryFingerprint(content: string): string {
  return createHash('sha256').update(content.toLowerCase().replace(/\s+/g, ' ').trim()).digest('hex')
}

function estimateTokens(value: string): number {
  const cjk = (value.match(/[\u3400-\u9fff\uf900-\ufaff]/g) ?? []).length
  return Math.max(1, Math.ceil((value.length - cjk) / 4 + cjk))
}

function readVector(value: Uint8Array, dimensions: number): number[] {
  if (value.byteLength !== dimensions * Float32Array.BYTES_PER_ELEMENT) throw new Error('Stored embedding vector has an invalid byte length')
  const view = new DataView(value.buffer, value.byteOffset, value.byteLength)
  return Array.from({ length: dimensions }, (_, index) => view.getFloat32(index * Float32Array.BYTES_PER_ELEMENT, true))
}

function cosineSimilarity(left: number[], right: number[]): number {
  let dot = 0
  let leftMagnitude = 0
  let rightMagnitude = 0
  for (let index = 0; index < left.length; index += 1) {
    dot += left[index] * right[index]
    leftMagnitude += left[index] * left[index]
    rightMagnitude += right[index] * right[index]
  }
  if (leftMagnitude === 0 || rightMagnitude === 0) return 0
  return dot / Math.sqrt(leftMagnitude * rightMagnitude)
}

function ftsQuery(value: string): string {
  const tokens = value.trim().split(/\s+/).filter(Boolean).slice(0, 20)
  return tokens.map((token) => `"${token.replaceAll('"', '""')}"`).join(' AND ')
}

function reasonPriority(reason: AiEffectiveMemory['reason']): number {
  return ['taskScope', 'branchScope', 'directoryScope', 'projectScope', 'agentProfileScope', 'userScope'].indexOf(reason)
}

function samePath(left: string | undefined, right: string): boolean {
  return Boolean(left) && resolve(left!) === resolve(right)
}

function isInside(root: string | undefined, target: string): boolean {
  if (!root) return false
  const relation = relative(resolve(root), resolve(target))
  return relation === '' || (relation !== '..' && !relation.startsWith(`..${sep}`) && !isAbsolute(relation))
}

function isUniqueConstraint(error: unknown): boolean {
  return error instanceof Error && /UNIQUE constraint failed/i.test(error.message)
}

import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import { afterEach, describe, expect, it } from 'vitest'
import { AiMemoryRepository } from '../../electron/main/ai/memoryRepository'
import type { AiMemorySaveInput } from './contracts/aiMemory'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('AiMemoryRepository', () => {
  it('creates, updates, searches, archives, restores, and rejects duplicates and plaintext secrets', async () => {
    const fixture = await createFixture()
    const memory = fixture.repository.save(memoryInput({ content: 'The project stores durable state in SQLite.', scope: 'project', scopeValue: '/repo' }))
    fixture.repository.save(memoryInput({ content: 'Prefer concise Chinese responses.', scope: 'user' }))

    expect(fixture.repository.snapshot({ keyword: 'SQLite' }).memories.map((item) => item.id)).toEqual([memory.id])
    expect(fixture.repository.snapshot().stats).toMatchObject({ active: 2, archived: 0 })
    const updated = fixture.repository.save({ ...memoryInput({ content: 'The project uses SQLite through one repository.', scope: 'project', scopeValue: '/repo' }), id: memory.id })
    expect(updated.content).toContain('one repository')

    await expect(() => fixture.repository.save(memoryInput({ content: updated.content, scope: 'project', scopeValue: '/repo' }))).toThrow('identical memory')
    expect(() => fixture.repository.save(memoryInput({ content: 'api_key = "sk-this_is_a_test_token_value_123456"', scope: 'user' }))).toThrow('plaintext credentials')

    expect(fixture.repository.archive(memory.id).archivedAt).toBeTruthy()
    expect(fixture.repository.snapshot().memories.some((item) => item.id === memory.id)).toBe(false)
    expect(fixture.repository.snapshot({ includeArchived: true }).memories.some((item) => item.id === memory.id)).toBe(true)
    expect(fixture.repository.restore(memory.id).archivedAt).toBeUndefined()
    fixture.repository.delete(memory.id)
    expect(fixture.repository.snapshot({ includeArchived: true }).memories.some((item) => item.id === memory.id)).toBe(false)
    fixture.repository.close()
  })

  it('keeps task summaries in a review inbox until explicit approval or rejection', async () => {
    const fixture = await createFixture()
    const approved = fixture.repository.createCandidate({
      kind: 'technicalDecision', proposedScope: 'project', proposedScopeValue: '/repo', content: 'Use one SQLite database.',
      sourceKind: 'taskSummary', sourceRef: 'task-42', evidenceSummary: 'The accepted implementation selected SQLite.', confidence: 0.9, sensitivity: 'internal'
    })
    const rejected = fixture.repository.createCandidate({
      kind: 'projectFact', proposedScope: 'project', proposedScopeValue: '/repo', content: 'The temporary prototype uses Redis.',
      sourceKind: 'taskSummary', sourceRef: 'task-43', evidenceSummary: 'Prototype-only observation.', confidence: 0.4, sensitivity: 'internal'
    })
    expect(fixture.repository.snapshot().stats.pendingCandidates).toBe(2)

    const reviewed = fixture.repository.reviewCandidate({ candidateId: approved.id, action: 'approve' })
    expect(reviewed).toMatchObject({ status: 'approved', approvedMemoryId: expect.any(String) })
    expect(fixture.repository.snapshot().memories).toContainEqual(expect.objectContaining({ content: 'Use one SQLite database.', createdBy: 'agentCandidate', sourceRef: 'task-42' }))
    expect(fixture.repository.reviewCandidate({ candidateId: rejected.id, action: 'reject' }).status).toBe('rejected')
    expect(fixture.repository.snapshot().stats.pendingCandidates).toBe(0)
    fixture.repository.close()
  })

  it('previews only matching active scopes in narrow-first order and enforces token and item budgets', async () => {
    let now = new Date('2026-07-18T00:00:00.000Z')
    const fixture = await createFixture(() => now)
    const inputs: AiMemorySaveInput[] = [
      memoryInput({ kind: 'taskSummary', scope: 'task', scopeValue: 'task-1', content: 'Task-specific completion note.' }),
      memoryInput({ kind: 'projectFact', scope: 'branch', scopeValue: 'feature/memory', content: 'Branch-specific migration is active.' }),
      memoryInput({ kind: 'projectFact', scope: 'directory', scopeValue: '/repo/src', content: 'Source code uses strict TypeScript.' }),
      memoryInput({ kind: 'technicalDecision', scope: 'project', scopeValue: '/repo', content: 'The project uses SQLite.' }),
      memoryInput({ kind: 'agentPrivate', scope: 'agentProfile', scopeValue: 'reviewer', content: 'Reviewer focuses on regressions.' }),
      memoryInput({ kind: 'userPreference', scope: 'user', content: 'Prefer concise responses.' }),
      memoryInput({ kind: 'temporary', scope: 'project', scopeValue: '/other', content: 'Other project fact.' }),
      memoryInput({ kind: 'temporary', scope: 'project', scopeValue: '/repo', content: 'Expired project fact.', expiresAt: '2026-07-17T00:00:00.000Z' })
    ]
    inputs.forEach((input) => fixture.repository.save(input))

    const preview = fixture.repository.preview({
      projectRoot: '/repo', targetPath: '/repo/src/components', branch: 'feature/memory', agentProfileId: 'reviewer', taskRef: 'task-1', tokenBudget: 1000, maxItems: 20
    })
    expect(preview.memories.map((item) => item.reason)).toEqual(['taskScope', 'branchScope', 'directoryScope', 'projectScope', 'agentProfileScope', 'userScope'])
    expect(preview.memories.some((item) => item.memory.content.includes('Expired'))).toBe(false)
    expect(preview.memories.some((item) => item.memory.content.includes('Other'))).toBe(false)

    const limited = fixture.repository.preview({ projectRoot: '/repo', targetPath: '/repo/src', tokenBudget: 6, maxItems: 1 })
    expect(limited.memories.length).toBeLessThanOrEqual(1)
    expect(limited.totalEstimatedTokens).toBeLessThanOrEqual(6)
    expect(limited.omittedByBudget).toBeGreaterThan(0)

    now = new Date('2026-07-20T00:00:00.000Z')
    fixture.repository.save(memoryInput({ kind: 'temporary', scope: 'user', content: 'Expires soon.', expiresAt: '2026-07-21T00:00:00.000Z' }))
    expect(fixture.repository.snapshot().stats.expiringSoon).toBe(1)
    fixture.repository.close()
  })

  it('cascades hard deletion through FTS, embeddings, and injection audit rows', async () => {
    const fixture = await createFixture()
    const memory = fixture.repository.save(memoryInput({ content: 'Cascade test memory.', scope: 'user' }))
    fixture.repository.close()
    const database = new DatabaseSync(fixture.databasePath)
    database.exec('PRAGMA foreign_keys = ON')
    database.prepare('INSERT INTO ai_memory_embeddings (memory_id, runtime_id, model, model_version, dimensions, embedding, content_fingerprint, generated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)')
      .run(memory.id, 'ollama', 'fixture', '1', 2, Buffer.from([1, 2]), memory.fingerprint, '2026-07-18T00:00:00.000Z')
    database.prepare('INSERT INTO ai_memory_injection_events VALUES (?, ?, ?, ?, ?, ?)').run('event-1', memory.id, 'task-1', 'Scope match', 4, '2026-07-18T00:00:00.000Z')
    database.close()

    const reopened = new AiMemoryRepository(fixture.databasePath)
    reopened.delete(memory.id)
    reopened.close()
    const verified = new DatabaseSync(fixture.databasePath, { readOnly: true })
    expect(verified.prepare('SELECT count(*) AS count FROM ai_memory_fts WHERE memory_id = ?').get(memory.id)).toEqual({ count: 0 })
    expect(verified.prepare('SELECT count(*) AS count FROM ai_memory_embeddings').get()).toEqual({ count: 0 })
    expect(verified.prepare('SELECT count(*) AS count FROM ai_memory_injection_events').get()).toEqual({ count: 0 })
    verified.close()
  })
})

async function createFixture(clock?: () => Date): Promise<{ root: string; databasePath: string; repository: AiMemoryRepository }> {
  const root = await mkdtemp(join(tmpdir(), 'mootool-ai-memory-repository-'))
  temporaryDirectories.push(root)
  const databasePath = join(root, 'memory.db')
  return { root, databasePath, repository: new AiMemoryRepository(databasePath, clock) }
}

function memoryInput(overrides: Partial<AiMemorySaveInput> = {}): AiMemorySaveInput {
  return {
    kind: 'projectFact',
    scope: 'user',
    content: 'Default memory content.',
    sourceKind: 'user',
    confidence: 1,
    sensitivity: 'internal',
    ...overrides
  }
}

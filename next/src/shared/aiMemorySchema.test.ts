import { DatabaseSync } from 'node:sqlite'
import { afterEach, describe, expect, it } from 'vitest'
import { aiMemorySchemaVersion, initializeAiMemorySchema } from '../../electron/main/ai/memorySchema'

const databases: DatabaseSync[] = []

afterEach(() => {
  databases.splice(0).forEach((database) => database.close())
})

describe('AI memory schema', () => {
  it('stores scoped, reviewable memories and searches them without any conversation table', () => {
    const database = openDatabase()
    insertMemory(database, { id: 'memory-project', content: 'The project uses SQLite for local storage.', scope: 'project', scopeValue: '/repo' })
    insertMemory(database, { id: 'memory-user', content: 'Prefer concise Chinese responses.', scope: 'user', scopeValue: null })
    database.prepare(`
      INSERT INTO ai_memory_candidates (
        id, kind, proposed_scope, proposed_scope_value, content, source_kind, source_ref,
        evidence_summary, confidence, sensitivity, status, created_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      'candidate-1', 'technicalDecision', 'project', '/repo', 'Use a single database implementation.',
      'taskSummary', 'task-42', 'Approved task summary states the selected database.', 0.8, 'internal', 'pending', '2026-07-18T00:00:00.000Z'
    )

    expect(database.prepare('SELECT version FROM ai_memory_schema').get()).toEqual({ version: aiMemorySchemaVersion })
    expect(database.prepare("SELECT memory_id FROM ai_memory_fts WHERE ai_memory_fts MATCH 'SQLite'").all()).toEqual([{ memory_id: 'memory-project' }])
    expect(database.prepare("SELECT id FROM ai_memory_candidates WHERE status = 'pending'").all()).toEqual([{ id: 'candidate-1' }])
    expect(database.prepare("SELECT name FROM sqlite_master WHERE type = 'table' AND name LIKE '%conversation%'").all()).toEqual([])
  })

  it('filters expired and archived memories and cascades deletion through audit and vector indexes', () => {
    const database = openDatabase()
    insertMemory(database, { id: 'active', content: 'Active project fact', scope: 'project', scopeValue: '/repo' })
    insertMemory(database, { id: 'expired', content: 'Expired temporary fact', scope: 'project', scopeValue: '/repo', expiresAt: '2026-01-01T00:00:00.000Z' })
    insertMemory(database, { id: 'archived', content: 'Archived decision', scope: 'project', scopeValue: '/repo', archivedAt: '2026-07-01T00:00:00.000Z' })
    database.prepare('INSERT INTO ai_memory_injection_events VALUES (?, ?, ?, ?, ?, ?)').run('event-1', 'active', 'task-7', 'Project scope match', 5, '2026-07-18T00:00:00.000Z')
    database.prepare('INSERT INTO ai_memory_embeddings (memory_id, runtime_id, model, model_version, dimensions, embedding, content_fingerprint, generated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)')
      .run('active', 'ollama', 'nomic-embed-text', '1', 3, Buffer.from([1, 2, 3]), 'active', '2026-07-18T00:00:00.000Z')

    const active = database.prepare(`
      SELECT id FROM ai_memories
      WHERE archived_at IS NULL AND superseded_by IS NULL AND (expires_at IS NULL OR expires_at > ?)
      ORDER BY id
    `).all('2026-07-18T00:00:00.000Z')
    expect(active).toEqual([{ id: 'active' }])

    database.prepare('DELETE FROM ai_memories WHERE id = ?').run('active')
    expect(database.prepare("SELECT count(*) AS count FROM ai_memory_fts WHERE memory_id = 'active'").get()).toEqual({ count: 0 })
    expect(database.prepare('SELECT count(*) AS count FROM ai_memory_embeddings').get()).toEqual({ count: 0 })
    expect(database.prepare('SELECT count(*) AS count FROM ai_memory_injection_events').get()).toEqual({ count: 0 })
  })

  it('keeps conflicting topics as separate records instead of silently overwriting them', () => {
    const database = openDatabase()
    insertMemory(database, { id: 'decision-old', content: 'Use PostgreSQL.', scope: 'project', scopeValue: '/repo', fingerprint: 'old' })
    insertMemory(database, { id: 'decision-new', content: 'Use SQLite.', scope: 'project', scopeValue: '/repo', fingerprint: 'new' })
    database.prepare('UPDATE ai_memories SET superseded_by = ? WHERE id = ?').run('decision-new', 'decision-old')

    expect(database.prepare('SELECT id, superseded_by FROM ai_memories ORDER BY id').all()).toEqual([
      { id: 'decision-new', superseded_by: null },
      { id: 'decision-old', superseded_by: 'decision-new' }
    ])
  })
})

function openDatabase(): DatabaseSync {
  const database = new DatabaseSync(':memory:')
  initializeAiMemorySchema(database)
  databases.push(database)
  return database
}

function insertMemory(database: DatabaseSync, value: {
  id: string
  content: string
  scope: string
  scopeValue: string | null
  expiresAt?: string
  archivedAt?: string
  fingerprint?: string
}): void {
  database.prepare(`
    INSERT INTO ai_memories (
      id, kind, scope, scope_value, content, source_kind, source_ref, confidence, sensitivity,
      created_by, created_at, updated_at, expires_at, archived_at, fingerprint
    ) VALUES (?, 'projectFact', ?, ?, ?, 'user', NULL, 1, 'internal', 'user', ?, ?, ?, ?, ?)
  `).run(
    value.id,
    value.scope,
    value.scopeValue,
    value.content,
    '2026-07-18T00:00:00.000Z',
    '2026-07-18T00:00:00.000Z',
    value.expiresAt ?? null,
    value.archivedAt ?? null,
    value.fingerprint ?? value.id
  )
}

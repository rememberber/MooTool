import type { DatabaseSync } from 'node:sqlite'

export const aiMemorySchemaVersion = 2

export function initializeAiMemorySchema(database: DatabaseSync): void {
  database.exec(`
    PRAGMA foreign_keys = ON;

    CREATE TABLE IF NOT EXISTS ai_memory_schema (
      version INTEGER NOT NULL
    );

    INSERT INTO ai_memory_schema(version)
    SELECT ${aiMemorySchemaVersion}
    WHERE NOT EXISTS (SELECT 1 FROM ai_memory_schema);

    CREATE TABLE IF NOT EXISTS ai_memories (
      id TEXT PRIMARY KEY,
      kind TEXT NOT NULL CHECK (kind IN ('userPreference', 'projectFact', 'technicalDecision', 'taskSummary', 'agentPrivate', 'temporary')),
      scope TEXT NOT NULL CHECK (scope IN ('task', 'branch', 'directory', 'project', 'agentProfile', 'user')),
      scope_value TEXT,
      content TEXT NOT NULL CHECK (length(content) BETWEEN 1 AND 32768),
      source_kind TEXT NOT NULL CHECK (source_kind IN ('user', 'taskSummary', 'decisionRecord', 'document', 'clientAdapter', 'import')),
      source_ref TEXT,
      confidence REAL NOT NULL CHECK (confidence BETWEEN 0 AND 1),
      sensitivity TEXT NOT NULL CHECK (sensitivity IN ('public', 'internal', 'private', 'restricted')),
      created_by TEXT NOT NULL CHECK (created_by IN ('user', 'import', 'agentCandidate')),
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL,
      last_used_at TEXT,
      expires_at TEXT,
      archived_at TEXT,
      superseded_by TEXT REFERENCES ai_memories(id) ON DELETE SET NULL,
      fingerprint TEXT NOT NULL,
      UNIQUE(scope, scope_value, fingerprint)
    );

    CREATE INDEX IF NOT EXISTS idx_ai_memories_scope ON ai_memories(scope, scope_value);
    CREATE INDEX IF NOT EXISTS idx_ai_memories_active ON ai_memories(archived_at, expires_at);
    CREATE INDEX IF NOT EXISTS idx_ai_memories_superseded ON ai_memories(superseded_by);

    CREATE TABLE IF NOT EXISTS ai_memory_candidates (
      id TEXT PRIMARY KEY,
      kind TEXT NOT NULL CHECK (kind IN ('userPreference', 'projectFact', 'technicalDecision', 'taskSummary', 'agentPrivate', 'temporary')),
      proposed_scope TEXT NOT NULL CHECK (proposed_scope IN ('task', 'branch', 'directory', 'project', 'agentProfile', 'user')),
      proposed_scope_value TEXT,
      content TEXT NOT NULL CHECK (length(content) BETWEEN 1 AND 32768),
      source_kind TEXT NOT NULL CHECK (source_kind IN ('user', 'taskSummary', 'decisionRecord', 'document', 'import')),
      source_ref TEXT NOT NULL,
      evidence_summary TEXT NOT NULL CHECK (length(evidence_summary) <= 4096),
      confidence REAL NOT NULL CHECK (confidence BETWEEN 0 AND 1),
      sensitivity TEXT NOT NULL CHECK (sensitivity IN ('public', 'internal', 'private', 'restricted')),
      status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
      created_at TEXT NOT NULL,
      reviewed_at TEXT,
      approved_memory_id TEXT REFERENCES ai_memories(id) ON DELETE SET NULL
    );

    CREATE INDEX IF NOT EXISTS idx_ai_memory_candidates_status ON ai_memory_candidates(status, created_at);

    CREATE TABLE IF NOT EXISTS ai_memory_injection_events (
      id TEXT PRIMARY KEY,
      memory_id TEXT NOT NULL REFERENCES ai_memories(id) ON DELETE CASCADE,
      target_task_ref TEXT NOT NULL,
      selection_reason TEXT NOT NULL CHECK (length(selection_reason) <= 2048),
      token_count INTEGER NOT NULL CHECK (token_count >= 0),
      injected_at TEXT NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_ai_memory_injection_memory ON ai_memory_injection_events(memory_id, injected_at);

    CREATE TABLE IF NOT EXISTS ai_memory_embeddings (
      memory_id TEXT PRIMARY KEY REFERENCES ai_memories(id) ON DELETE CASCADE,
      runtime_id TEXT NOT NULL,
      model TEXT NOT NULL,
      model_version TEXT,
      dimensions INTEGER NOT NULL CHECK (dimensions > 0),
      embedding BLOB NOT NULL,
      content_fingerprint TEXT NOT NULL,
      generated_at TEXT NOT NULL
    );

    CREATE VIRTUAL TABLE IF NOT EXISTS ai_memory_fts USING fts5(
      memory_id UNINDEXED,
      content,
      tokenize = 'unicode61'
    );

    CREATE TRIGGER IF NOT EXISTS ai_memories_fts_insert AFTER INSERT ON ai_memories BEGIN
      INSERT INTO ai_memory_fts(memory_id, content) VALUES (new.id, new.content);
    END;

    CREATE TRIGGER IF NOT EXISTS ai_memories_fts_update AFTER UPDATE OF content ON ai_memories BEGIN
      DELETE FROM ai_memory_fts WHERE memory_id = old.id;
      INSERT INTO ai_memory_fts(memory_id, content) VALUES (new.id, new.content);
    END;

    CREATE TRIGGER IF NOT EXISTS ai_memories_fts_delete AFTER DELETE ON ai_memories BEGIN
      DELETE FROM ai_memory_fts WHERE memory_id = old.id;
    END;
  `)

  const columns = new Set((database.prepare('PRAGMA table_info(ai_memory_embeddings)').all() as Array<Record<string, unknown>>).map((row) => String(row.name)))
  if (!columns.has('runtime_id')) database.exec("ALTER TABLE ai_memory_embeddings ADD COLUMN runtime_id TEXT NOT NULL DEFAULT 'ollama'")
  if (!columns.has('content_fingerprint')) database.exec("ALTER TABLE ai_memory_embeddings ADD COLUMN content_fingerprint TEXT NOT NULL DEFAULT ''")
  database.exec(`
    CREATE INDEX IF NOT EXISTS idx_ai_memory_embeddings_runtime_model ON ai_memory_embeddings(runtime_id, model);
    UPDATE ai_memory_schema SET version = ${aiMemorySchemaVersion};
  `)
}

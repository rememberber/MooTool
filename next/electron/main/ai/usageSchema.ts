import type { DatabaseSync } from 'node:sqlite'

export const aiUsageSchemaVersion = 1

export function initializeAiUsageSchema(database: DatabaseSync): void {
  database.exec(`
    CREATE TABLE IF NOT EXISTS ai_usage_schema (
      version INTEGER NOT NULL
    );

    INSERT INTO ai_usage_schema(version)
    SELECT ${aiUsageSchemaVersion}
    WHERE NOT EXISTS (SELECT 1 FROM ai_usage_schema);

    CREATE TABLE IF NOT EXISTS ai_usage_events (
      id TEXT PRIMARY KEY,
      source TEXT NOT NULL CHECK (source IN ('localLog', 'cli', 'providerApi', 'import', 'localRuntime')),
      provider TEXT NOT NULL,
      client_id TEXT NOT NULL,
      project_id TEXT,
      agent_profile_id TEXT,
      model_runtime_id TEXT,
      local_model_digest TEXT,
      session_id TEXT,
      model TEXT NOT NULL,
      started_at TEXT NOT NULL,
      input_tokens INTEGER NOT NULL CHECK (input_tokens >= 0),
      output_tokens INTEGER NOT NULL CHECK (output_tokens >= 0),
      cached_input_tokens INTEGER CHECK (cached_input_tokens >= 0),
      cache_write_tokens INTEGER CHECK (cache_write_tokens >= 0),
      reasoning_tokens INTEGER CHECK (reasoning_tokens >= 0),
      request_count INTEGER CHECK (request_count >= 0),
      estimated_cost_micros INTEGER CHECK (estimated_cost_micros >= 0),
      estimated_cost_currency TEXT,
      billed_cost_micros INTEGER CHECK (billed_cost_micros >= 0),
      billed_cost_currency TEXT,
      source_fingerprint TEXT NOT NULL UNIQUE,
      source_revision TEXT NOT NULL,
      imported_at TEXT NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_ai_usage_started ON ai_usage_events(started_at);
    CREATE INDEX IF NOT EXISTS idx_ai_usage_model ON ai_usage_events(model, started_at);
    CREATE INDEX IF NOT EXISTS idx_ai_usage_client ON ai_usage_events(client_id, started_at);
    CREATE INDEX IF NOT EXISTS idx_ai_usage_project ON ai_usage_events(project_id, started_at);

    CREATE TABLE IF NOT EXISTS ai_usage_budgets (
      period TEXT PRIMARY KEY CHECK (period IN ('daily', 'weekly', 'monthly')),
      token_limit INTEGER CHECK (token_limit > 0),
      cost_limit_micros INTEGER CHECK (cost_limit_micros >= 0),
      cost_currency TEXT,
      enabled INTEGER NOT NULL CHECK (enabled IN (0, 1)),
      updated_at TEXT NOT NULL,
      CHECK (token_limit IS NOT NULL OR cost_limit_micros IS NOT NULL)
    );

    CREATE TABLE IF NOT EXISTS ai_usage_budget_notifications (
      period TEXT NOT NULL CHECK (period IN ('daily', 'weekly', 'monthly')),
      period_key TEXT NOT NULL,
      threshold INTEGER NOT NULL CHECK (threshold IN (50, 80, 100)),
      notified_at TEXT NOT NULL,
      PRIMARY KEY (period, period_key, threshold)
    );
  `)
}

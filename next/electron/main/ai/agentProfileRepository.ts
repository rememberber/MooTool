import { randomUUID } from 'node:crypto'
import { mkdirSync } from 'node:fs'
import { dirname, isAbsolute, resolve } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import type { AiAgentProfile, AiAgentProfileSaveInput } from '../../../src/shared/contracts/aiAgents'
import type { AiPrimaryClientId } from '../../../src/shared/contracts/ai'

export class AiAgentProfileRepository {
  private readonly database: DatabaseSync
  private readonly clock: () => Date

  constructor(databasePath: string, clock: () => Date = () => new Date()) {
    mkdirSync(dirname(databasePath), { recursive: true })
    this.database = new DatabaseSync(databasePath)
    this.database.exec('PRAGMA journal_mode = WAL; PRAGMA busy_timeout = 3000;')
    this.database.exec(`
      CREATE TABLE IF NOT EXISTS ai_agent_profiles (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        client_id TEXT NOT NULL CHECK (client_id IN ('codex', 'claudeCode')),
        model TEXT,
        model_runtime_id TEXT CHECK (model_runtime_id IS NULL OR model_runtime_id IN ('ollama', 'lmStudio', 'llamaCpp', 'vllm', 'localAi')),
        runtime_adapter_id TEXT,
        local_model_digest TEXT,
        working_directory TEXT NOT NULL,
        config_profile TEXT,
        permission_mode TEXT NOT NULL CHECK (permission_mode IN ('readOnly', 'default', 'workspaceWrite', 'plan', 'acceptEdits', 'dontAsk')),
        mcp_server_names TEXT NOT NULL,
        skill_names TEXT NOT NULL,
        environment_variable_refs TEXT NOT NULL,
        optional_flags TEXT NOT NULL,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
      );
      CREATE INDEX IF NOT EXISTS idx_ai_agent_profiles_client ON ai_agent_profiles(client_id, updated_at);
      CREATE TABLE IF NOT EXISTS ai_agent_config_observations (
        scope_key TEXT NOT NULL,
        client_id TEXT NOT NULL CHECK (client_id IN ('codex', 'claudeCode')),
        fingerprint TEXT NOT NULL,
        observed_at TEXT NOT NULL,
        PRIMARY KEY (scope_key, client_id)
      );
    `)
    ensureColumn(this.database, 'ai_agent_profiles', 'model_runtime_id', "TEXT CHECK (model_runtime_id IS NULL OR model_runtime_id = 'ollama')")
    ensureColumn(this.database, 'ai_agent_profiles', 'runtime_adapter_id', 'TEXT')
    ensureColumn(this.database, 'ai_agent_profiles', 'local_model_digest', 'TEXT')
    this.clock = clock
  }

  list(): AiAgentProfile[] {
    return this.database.prepare('SELECT * FROM ai_agent_profiles ORDER BY updated_at DESC, name').all().map(mapProfile)
  }

  save(input: AiAgentProfileSaveInput): AiAgentProfile {
    if (!isAbsolute(input.workingDirectory)) throw new Error('Agent Profile working directory must be absolute')
    const directory = resolve(input.workingDirectory)
    validateProfileCompatibility(input)
    const now = this.clock().toISOString()
    const id = input.id ?? randomUUID()
    const existing = input.id ? this.database.prepare('SELECT created_at FROM ai_agent_profiles WHERE id = ?').get(input.id) as { created_at?: unknown } | undefined : undefined
    if (input.id && !existing) throw new Error('Agent Profile no longer exists')
    this.database.prepare(`
      INSERT INTO ai_agent_profiles (
        id, name, client_id, model, model_runtime_id, runtime_adapter_id, local_model_digest, working_directory, config_profile, permission_mode,
        mcp_server_names, skill_names, environment_variable_refs, optional_flags, created_at, updated_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(id) DO UPDATE SET name = excluded.name, client_id = excluded.client_id, model = excluded.model,
        model_runtime_id = excluded.model_runtime_id, runtime_adapter_id = excluded.runtime_adapter_id, local_model_digest = excluded.local_model_digest,
        working_directory = excluded.working_directory, config_profile = excluded.config_profile,
        permission_mode = excluded.permission_mode, mcp_server_names = excluded.mcp_server_names,
        skill_names = excluded.skill_names, environment_variable_refs = excluded.environment_variable_refs,
        optional_flags = excluded.optional_flags, updated_at = excluded.updated_at
    `).run(id, input.name.trim(), input.clientId, nullable(input.model), input.modelRuntimeId === 'ollama' ? 'ollama' : null, nullable(input.modelRuntimeId), nullable(input.localModelDigest), directory, nullable(input.configProfile), input.permissionMode,
      serialize(input.mcpServerNames), serialize(input.skillNames), serialize(input.environmentVariableRefs), serialize(input.optionalFlags),
      typeof existing?.created_at === 'string' ? existing.created_at : now, now)
    return this.getRequired(id)
  }

  delete(id: string): void {
    if (Number(this.database.prepare('DELETE FROM ai_agent_profiles WHERE id = ?').run(id).changes) === 0) throw new Error('Agent Profile no longer exists')
  }

  getRequired(id: string): AiAgentProfile {
    const row = this.database.prepare('SELECT * FROM ai_agent_profiles WHERE id = ?').get(id)
    if (!row) throw new Error('Agent Profile no longer exists')
    return mapProfile(row)
  }

  observeConfiguration(scopeKey: string, clientId: AiPrimaryClientId, fingerprint: string): { previousFingerprint?: string; changed: boolean } {
    const row = this.database.prepare('SELECT fingerprint FROM ai_agent_config_observations WHERE scope_key = ? AND client_id = ?').get(scopeKey, clientId) as { fingerprint?: unknown } | undefined
    const previousFingerprint = typeof row?.fingerprint === 'string' ? row.fingerprint : undefined
    this.database.prepare(`
      INSERT INTO ai_agent_config_observations(scope_key, client_id, fingerprint, observed_at) VALUES (?, ?, ?, ?)
      ON CONFLICT(scope_key, client_id) DO UPDATE SET fingerprint = excluded.fingerprint, observed_at = excluded.observed_at
    `).run(scopeKey, clientId, fingerprint, this.clock().toISOString())
    return { ...(previousFingerprint ? { previousFingerprint } : {}), changed: Boolean(previousFingerprint && previousFingerprint !== fingerprint) }
  }

  close(): void {
    if (this.database.isOpen) this.database.close()
  }
}

function mapProfile(row: Record<string, unknown>): AiAgentProfile {
  return {
    id: String(row.id),
    name: String(row.name),
    clientId: row.client_id as AiAgentProfile['clientId'],
    ...(row.model == null ? {} : { model: String(row.model) }),
    ...(row.runtime_adapter_id == null && row.model_runtime_id == null ? {} : { modelRuntimeId: String(row.runtime_adapter_id ?? row.model_runtime_id) as AiAgentProfile['modelRuntimeId'] }),
    ...(row.local_model_digest == null ? {} : { localModelDigest: String(row.local_model_digest) }),
    workingDirectory: String(row.working_directory),
    ...(row.config_profile == null ? {} : { configProfile: String(row.config_profile) }),
    permissionMode: row.permission_mode as AiAgentProfile['permissionMode'],
    mcpServerNames: parseArray(row.mcp_server_names),
    skillNames: parseArray(row.skill_names),
    environmentVariableRefs: parseArray(row.environment_variable_refs),
    optionalFlags: parseArray(row.optional_flags),
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at)
  }
}

function validateProfileCompatibility(input: AiAgentProfileSaveInput): void {
  const permissions = input.clientId === 'codex' ? ['readOnly', 'default', 'workspaceWrite'] : ['default', 'plan', 'acceptEdits', 'dontAsk']
  if (!permissions.includes(input.permissionMode)) throw new Error(`Permission mode is not supported by ${input.clientId}`)
  if (input.clientId === 'claudeCode' && input.configProfile?.trim()) throw new Error('Claude Code Agent Profiles do not support Codex config profiles')
  if (input.localModelDigest && !input.modelRuntimeId) throw new Error('A local model Digest requires a model runtime binding')
  if (input.modelRuntimeId && (!input.model?.trim() || !input.localModelDigest?.trim())) throw new Error('A model runtime binding requires a model name and Digest')
  const flags = input.clientId === 'codex' ? new Set(['--search', '--no-alt-screen']) : new Set(['--ide', '--no-chrome', '--disable-slash-commands'])
  if (input.optionalFlags.some((flag) => !flags.has(flag))) throw new Error(`Agent Profile contains a flag outside the ${input.clientId} allowlist`)
}

function ensureColumn(database: DatabaseSync, table: string, column: string, definition: string): void {
  const columns = database.prepare(`PRAGMA table_info(${table})`).all() as Array<{ name?: unknown }>
  if (!columns.some((item) => item.name === column)) database.exec(`ALTER TABLE ${table} ADD COLUMN ${column} ${definition}`)
}

function serialize(values: string[]): string {
  return JSON.stringify([...new Set(values.map((value) => value.trim()).filter(Boolean))])
}

function parseArray(value: unknown): string[] {
  try {
    const parsed = JSON.parse(String(value)) as unknown
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : []
  } catch { return [] }
}

function nullable(value?: string): string | null {
  return value?.trim() || null
}

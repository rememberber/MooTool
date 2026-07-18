import { randomUUID } from 'node:crypto'
import { mkdirSync } from 'node:fs'
import { dirname } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import type { AiPromptLabCase, AiPromptLabSuite, AiPromptLabSuiteSaveInput } from '../../../src/shared/contracts/aiPromptLab'

export class AiPromptLabRepository {
  private readonly database: DatabaseSync

  constructor(databasePath: string, private readonly clock: () => Date = () => new Date()) {
    mkdirSync(dirname(databasePath), { recursive: true })
    this.database = new DatabaseSync(databasePath)
    this.database.exec('PRAGMA journal_mode = WAL; PRAGMA busy_timeout = 3000;')
    this.database.exec(`
      CREATE TABLE IF NOT EXISTS ai_prompt_lab_suites (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        system_prompt TEXT NOT NULL,
        prompt_template TEXT NOT NULL,
        test_cases TEXT NOT NULL,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
      );
      CREATE INDEX IF NOT EXISTS idx_ai_prompt_lab_suites_updated ON ai_prompt_lab_suites(updated_at DESC, name);
    `)
  }

  list(): AiPromptLabSuite[] {
    return this.database.prepare('SELECT * FROM ai_prompt_lab_suites ORDER BY updated_at DESC, name').all().map(mapSuite)
  }

  save(input: AiPromptLabSuiteSaveInput): AiPromptLabSuite {
    const id = input.id ?? randomUUID()
    const existing = input.id
      ? this.database.prepare('SELECT created_at FROM ai_prompt_lab_suites WHERE id = ?').get(input.id) as { created_at?: unknown } | undefined
      : undefined
    if (input.id && !existing) throw new Error('Prompt Lab suite no longer exists')
    const now = this.clock().toISOString()
    this.database.prepare(`
      INSERT INTO ai_prompt_lab_suites(id, name, system_prompt, prompt_template, test_cases, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(id) DO UPDATE SET name = excluded.name, system_prompt = excluded.system_prompt,
        prompt_template = excluded.prompt_template, test_cases = excluded.test_cases, updated_at = excluded.updated_at
    `).run(
      id,
      input.name.trim(),
      input.systemPrompt,
      input.promptTemplate,
      JSON.stringify(input.testCases.map(normalizeCase)),
      typeof existing?.created_at === 'string' ? existing.created_at : now,
      now
    )
    return this.getRequired(id)
  }

  delete(id: string): void {
    if (Number(this.database.prepare('DELETE FROM ai_prompt_lab_suites WHERE id = ?').run(id).changes) === 0) throw new Error('Prompt Lab suite no longer exists')
  }

  close(): void {
    if (this.database.isOpen) this.database.close()
  }

  private getRequired(id: string): AiPromptLabSuite {
    const row = this.database.prepare('SELECT * FROM ai_prompt_lab_suites WHERE id = ?').get(id)
    if (!row) throw new Error('Prompt Lab suite no longer exists')
    return mapSuite(row)
  }
}

function mapSuite(row: Record<string, unknown>): AiPromptLabSuite {
  return {
    id: String(row.id),
    name: String(row.name),
    systemPrompt: String(row.system_prompt),
    promptTemplate: String(row.prompt_template),
    testCases: parseCases(row.test_cases),
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at)
  }
}

function parseCases(value: unknown): AiPromptLabCase[] {
  try {
    const parsed = JSON.parse(String(value)) as unknown
    return Array.isArray(parsed) ? parsed.filter(isCase).map(normalizeCase) : []
  } catch {
    return []
  }
}

function normalizeCase(value: AiPromptLabCase): AiPromptLabCase {
  return { id: value.id, name: value.name.trim(), input: value.input, expectedContains: value.expectedContains }
}

function isCase(value: unknown): value is AiPromptLabCase {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return false
  const item = value as Record<string, unknown>
  return typeof item.id === 'string' && typeof item.name === 'string' && typeof item.input === 'string' && typeof item.expectedContains === 'string'
}

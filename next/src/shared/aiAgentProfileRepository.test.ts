import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import { afterEach, describe, expect, it } from 'vitest'
import { AiAgentProfileRepository } from '../../electron/main/ai/agentProfileRepository'
import type { AiAgentProfileSaveInput } from './contracts/aiAgents'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('AiAgentProfileRepository', () => {
  it('persists reusable profiles and updates them without storing environment values', async () => {
    const root = await fixtureRoot()
    const repository = new AiAgentProfileRepository(join(root, 'agents.db'), () => new Date('2026-07-18T10:00:00.000Z'))
    const saved = repository.save(profileInput(root))

    expect(saved).toMatchObject({
      clientId: 'codex',
      permissionMode: 'readOnly',
      model: 'qwen3:8b',
      modelRuntimeId: 'ollama',
      localModelDigest: 'sha256:qwen3-8b',
      environmentVariableRefs: ['OPENAI_API_KEY']
    })
    expect(JSON.stringify(saved)).not.toContain('secret-value')
    const updated = repository.save({ ...profileInput(root), id: saved.id, name: 'Updated profile', optionalFlags: ['--search', '--search'] })
    expect(updated).toMatchObject({ id: saved.id, name: 'Updated profile', optionalFlags: ['--search'] })
    expect(repository.list()).toHaveLength(1)

    expect(repository.observeConfiguration('/project', 'codex', 'sha256:first')).toEqual({ changed: false })
    expect(repository.observeConfiguration('/project', 'codex', 'sha256:second')).toEqual({ previousFingerprint: 'sha256:first', changed: true })
    expect(repository.observeConfiguration('/project', 'codex', 'sha256:second')).toEqual({ previousFingerprint: 'sha256:second', changed: false })

    repository.delete(saved.id)
    expect(repository.list()).toEqual([])
    repository.close()
  })

  it('rejects relative paths, incompatible permissions, unsupported flags, and Claude config profiles', async () => {
    const root = await fixtureRoot()
    const repository = new AiAgentProfileRepository(join(root, 'agents.db'))

    expect(() => repository.save({ ...profileInput(root), workingDirectory: 'relative/path' })).toThrow('must be absolute')
    expect(() => repository.save({ ...profileInput(root), permissionMode: 'dontAsk' })).toThrow('not supported')
    expect(() => repository.save({ ...profileInput(root), optionalFlags: ['--dangerously-bypass-approvals-and-sandbox'] })).toThrow('allowlist')
    expect(() => repository.save({ ...profileInput(root), clientId: 'claudeCode', permissionMode: 'plan', configProfile: 'work', optionalFlags: [] })).toThrow('do not support')
    expect(() => repository.save({ ...profileInput(root), modelRuntimeId: undefined, localModelDigest: 'sha256:orphaned' })).toThrow('requires a model runtime')
    expect(() => repository.save({ ...profileInput(root), localModelDigest: undefined })).toThrow('requires a model name and Digest')

    repository.close()
  })

  it('adds local model binding columns to databases created by an earlier version', async () => {
    const root = await fixtureRoot()
    const databasePath = join(root, 'agents.db')
    const legacy = new DatabaseSync(databasePath)
    legacy.exec(`
      CREATE TABLE ai_agent_profiles (
        id TEXT PRIMARY KEY, name TEXT NOT NULL, client_id TEXT NOT NULL, model TEXT,
        working_directory TEXT NOT NULL, config_profile TEXT, permission_mode TEXT NOT NULL,
        mcp_server_names TEXT NOT NULL, skill_names TEXT NOT NULL, environment_variable_refs TEXT NOT NULL,
        optional_flags TEXT NOT NULL, created_at TEXT NOT NULL, updated_at TEXT NOT NULL
      );
    `)
    legacy.close()

    const repository = new AiAgentProfileRepository(databasePath)
    expect(repository.save(profileInput(root))).toMatchObject({ modelRuntimeId: 'ollama', localModelDigest: 'sha256:qwen3-8b' })
    expect(repository.save({
      ...profileInput(root),
      name: 'Remote vLLM reviewer',
      model: 'Qwen/Qwen3-8B',
      modelRuntimeId: 'vllm',
      localModelDigest: 'sha256:vllm-qwen'
    })).toMatchObject({ modelRuntimeId: 'vllm', localModelDigest: 'sha256:vllm-qwen' })
    repository.close()
  })
})

async function fixtureRoot(): Promise<string> {
  const root = await mkdtemp(join(tmpdir(), 'mootool-agent-profile-'))
  temporaryDirectories.push(root)
  return root
}

function profileInput(root: string): AiAgentProfileSaveInput {
  return {
    name: 'Read-only reviewer',
    clientId: 'codex',
    model: 'qwen3:8b',
    modelRuntimeId: 'ollama',
    localModelDigest: 'sha256:qwen3-8b',
    workingDirectory: root,
    configProfile: 'work',
    permissionMode: 'readOnly',
    mcpServerNames: ['docs'],
    skillNames: ['code-review'],
    environmentVariableRefs: ['OPENAI_API_KEY'],
    optionalFlags: ['--search']
  }
}

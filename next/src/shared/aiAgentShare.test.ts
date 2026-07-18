import { describe, expect, it } from 'vitest'
import { createAiAgentProfileShareDocument, isAiAgentProfileShareDocument } from './contracts/aiAgentShare'
import type { AiAgentProfile } from './contracts/aiAgents'

describe('Agent Profile sharing', () => {
  it('exports portable configuration without machine paths, ids, timestamps, or environment values', () => {
    const profile: AiAgentProfile = {
      id: crypto.randomUUID(),
      name: 'Shared reviewer',
      clientId: 'codex',
      model: 'Qwen/Qwen3-8B',
      modelRuntimeId: 'vllm',
      localModelDigest: 'sha256:model',
      workingDirectory: '/Users/example/private-project',
      permissionMode: 'readOnly',
      mcpServerNames: ['docs'],
      skillNames: ['review'],
      environmentVariableRefs: ['MODEL_API_KEY'],
      optionalFlags: ['--search'],
      createdAt: '2026-07-18T00:00:00.000Z',
      updatedAt: '2026-07-18T00:00:00.000Z'
    }

    const document = createAiAgentProfileShareDocument(profile, '2026-07-18T12:00:00.000Z')
    const serialized = JSON.stringify(document)

    expect(isAiAgentProfileShareDocument(document)).toBe(true)
    expect(serialized).not.toContain(profile.workingDirectory)
    expect(serialized).not.toContain(profile.id)
    expect(document.profile.environmentVariableRefs).toEqual(['MODEL_API_KEY'])
  })

  it('rejects unsupported formats and invalid permission combinations', () => {
    const valid = {
      format: 'mootool.agent-profile', version: 1, exportedAt: '2026-07-18T12:00:00.000Z',
      profile: { name: 'Test', clientId: 'codex', permissionMode: 'readOnly', mcpServerNames: [], skillNames: [], environmentVariableRefs: [], optionalFlags: [] }
    }
    expect(isAiAgentProfileShareDocument(valid)).toBe(true)
    expect(isAiAgentProfileShareDocument({ ...valid, version: 2 })).toBe(false)
    expect(isAiAgentProfileShareDocument({ ...valid, profile: { ...valid.profile, environmentVariableRefs: ['KEY=value'] } })).toBe(false)
  })
})

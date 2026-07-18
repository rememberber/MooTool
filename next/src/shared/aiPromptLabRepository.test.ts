import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { AiPromptLabRepository } from '../../electron/main/ai/promptLabRepository'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('AiPromptLabRepository', () => {
  it('persists explicitly saved templates and test sets without storing run outputs', async () => {
    const root = await mkdtemp(join(tmpdir(), 'mootool-prompt-lab-'))
    temporaryDirectories.push(root)
    const repository = new AiPromptLabRepository(join(root, 'prompt-lab.db'), () => new Date('2026-07-18T12:00:00.000Z'))
    const testCase = { id: crypto.randomUUID(), name: 'Greeting', input: 'MooTool', expectedContains: 'hello' }
    const saved = repository.save({ name: 'Greeting suite', systemPrompt: 'Be concise.', promptTemplate: 'Say hello to {{input}}.', testCases: [testCase] })

    expect(saved).toMatchObject({ name: 'Greeting suite', promptTemplate: 'Say hello to {{input}}.', testCases: [testCase] })
    expect(repository.list()).toEqual([saved])
    const updated = repository.save({ ...saved, name: 'Updated suite' })
    expect(updated).toMatchObject({ id: saved.id, name: 'Updated suite', createdAt: saved.createdAt })

    repository.delete(saved.id)
    expect(repository.list()).toEqual([])
    repository.close()
  })
})

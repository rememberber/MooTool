import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { randomUUID } from 'node:crypto'
import { afterEach, describe, expect, it } from 'vitest'
import { AiMemoryRepository } from '../../electron/main/ai/memoryRepository'
import { MemoryEmbeddingService } from '../../electron/main/ai/memoryEmbeddingService'
import type { AiModelRuntimeId, AiModelRuntimeSnapshot } from './contracts/aiModelRuntime'
import { isAiMemoryEmbeddingRebuildInput, isAiMemorySemanticPreviewInput } from './contracts/aiMemoryEmbedding'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('MemoryEmbeddingService', () => {
  it('builds a local Ollama index, excludes sensitive memories, and ranks scope-matched semantic previews', async () => {
    const repository = await createRepository()
    const sqlite = repository.save(memory('SQLite stores durable local state.', 'internal'))
    repository.save(memory('The dashboard uses purple accents.', 'public'))
    repository.save(memory('Private personal preference.', 'private'))
    const requests: Array<{ url: string; body: string }> = []
    const service = new MemoryEmbeddingService({
      repository: () => repository,
      runtimes: runtimeService('ollama', 'http://127.0.0.1:11434', 'nomic-embed-text'),
      fetcher: async (url, init) => {
        requests.push({ url, body: init.body })
        const input = (JSON.parse(init.body) as { input: string[] }).input
        return response({ embeddings: input.map((text) => text.includes('SQLite') ? [1, 0] : text.includes('purple') ? [0, 1] : [0.9, 0.1]) })
      }
    })

    const rebuilt = await service.rebuild({ requestId: randomUUID(), runtimeId: 'ollama', model: 'nomic-embed-text', confirmLocalProcessing: true })
    expect(rebuilt).toMatchObject({ status: 'completed', indexed: 2, eligible: 2, skippedSensitive: 1, dimensions: 2 })
    expect(requests[0]).toMatchObject({ url: 'http://127.0.0.1:11434/api/embed' })
    expect(repository.embeddingStatus()).toMatchObject({ indexed: 2, stale: 0, coverage: 1, runtimeId: 'ollama', model: 'nomic-embed-text' })

    const preview = await service.semanticPreview({
      requestId: randomUUID(), runtimeId: 'ollama', model: 'nomic-embed-text', query: 'database',
      confirmLocalProcessing: true, tokenBudget: 1_000, maxItems: 10
    })
    expect(preview.memories.map((item) => item.memory.id)).toEqual([sqlite.id, expect.any(String)])
    expect(preview.memories[0].semanticScore).toBeGreaterThan(0.99)

    repository.save({ ...memory('SQLite stores durable state through one repository.', 'internal'), id: sqlite.id })
    expect(repository.embeddingStatus()).toMatchObject({ indexed: 1, stale: 1 })
    repository.close()
  })

  it('uses the LM Studio OpenAI-compatible endpoint without exposing its token', async () => {
    const repository = await createRepository()
    repository.save(memory('Embedding fixture.', 'internal'))
    const token = 'lm-secret-fixture-token'
    let authorization = ''
    const service = new MemoryEmbeddingService({
      repository: () => repository,
      runtimes: runtimeService('lmStudio', 'http://0.0.0.0:1234', 'text-embedding-nomic'),
      credentialProvider: () => token,
      fetcher: async (url, init) => {
        expect(url).toBe('http://127.0.0.1:1234/v1/embeddings')
        authorization = init.headers.authorization
        return response({ data: [{ index: 0, embedding: [0.25, 0.75] }] })
      }
    })
    const result = await service.rebuild({ requestId: randomUUID(), runtimeId: 'lmStudio', model: 'text-embedding-nomic', confirmLocalProcessing: true })
    expect(result.status).toBe('completed')
    expect(authorization).toBe(`Bearer ${token}`)
    expect(JSON.stringify(result)).not.toContain(token)
    repository.close()
  })

  it('preserves the previous index on cancellation and blocks network endpoints or missing confirmation', async () => {
    const repository = await createRepository()
    repository.save(memory('Cancellation fixture.', 'internal'))
    const first = new MemoryEmbeddingService({
      repository: () => repository,
      runtimes: runtimeService('ollama', 'http://127.0.0.1:11434', 'embed'),
      fetcher: async () => response({ embeddings: [[1, 0]] })
    })
    await first.rebuild({ requestId: randomUUID(), runtimeId: 'ollama', model: 'embed', confirmLocalProcessing: true })

    const pending = new MemoryEmbeddingService({
      repository: () => repository,
      runtimes: runtimeService('ollama', 'http://127.0.0.1:11434', 'embed'),
      fetcher: (_url, init) => new Promise((_resolve, reject) => init.signal.addEventListener('abort', () => reject(Object.assign(new Error('aborted'), { name: 'AbortError' })), { once: true }))
    })
    const requestId = randomUUID()
    const running = pending.rebuild({ requestId, runtimeId: 'ollama', model: 'embed', confirmLocalProcessing: true })
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(pending.cancel(requestId)).toBe(true)
    expect(await running).toMatchObject({ status: 'cancelled', indexed: 0 })
    expect(repository.embeddingStatus().indexed).toBe(1)

    const remote = new MemoryEmbeddingService({ repository: () => repository, runtimes: runtimeService('ollama', 'http://192.168.1.8:11434', 'embed', 'localNetwork') })
    await expect(remote.rebuild({ requestId: randomUUID(), runtimeId: 'ollama', model: 'embed', confirmLocalProcessing: true })).rejects.toThrow('blocks LAN and remote')
    await expect(first.rebuild({ requestId: randomUUID(), runtimeId: 'ollama', model: 'embed', confirmLocalProcessing: false })).rejects.toThrow('explicit')
    repository.close()
  })

  it('validates rebuild and semantic preview IPC payloads', () => {
    expect(isAiMemoryEmbeddingRebuildInput({ requestId: randomUUID(), runtimeId: 'ollama', model: 'embed', confirmLocalProcessing: true })).toBe(true)
    expect(isAiMemoryEmbeddingRebuildInput({ requestId: 'bad', runtimeId: 'ollama', model: 'embed', confirmLocalProcessing: true })).toBe(false)
    expect(isAiMemorySemanticPreviewInput({
      requestId: randomUUID(), runtimeId: 'lmStudio', model: 'embed', query: 'sqlite', confirmLocalProcessing: true,
      tokenBudget: 1000, maxItems: 10
    })).toBe(true)
    expect(isAiMemorySemanticPreviewInput({ requestId: randomUUID(), runtimeId: 'lmStudio', model: 'embed', query: '', confirmLocalProcessing: true, tokenBudget: 1000, maxItems: 10 })).toBe(false)
  })
})

async function createRepository(): Promise<AiMemoryRepository> {
  const root = await mkdtemp(join(tmpdir(), 'mootool-memory-embedding-'))
  temporaryDirectories.push(root)
  return new AiMemoryRepository(join(root, 'memory.db'))
}

function memory(content: string, sensitivity: 'public' | 'internal' | 'private') {
  return { kind: 'projectFact' as const, scope: 'user' as const, content, sourceKind: 'user' as const, confidence: 1, sensitivity }
}

function runtimeService(id: AiModelRuntimeId, endpoint: string, model: string, exposure: 'loopback' | 'allInterfaces' | 'localNetwork' = endpoint.includes('0.0.0.0') ? 'allInterfaces' : 'loopback') {
  return { scan: async (): Promise<AiModelRuntimeSnapshot> => ({
    scannedAt: '2026-07-18T00:00:00.000Z', readOnly: true,
    runtime: installation(id, endpoint, model, exposure),
    runtimes: [installation(id, endpoint, model, exposure)],
    resources: { platform: 'darwin', architecture: 'x64', cpuModel: 'Intel', totalMemoryBytes: 1, freeMemoryBytes: 1, cpuOnly: true, modelDirectory: '/models', modelDirectoryExists: true },
    stats: { models: 1, runningModels: 1, totalModelBytes: 1, loadedBytes: 1, vramBytes: 0 }
  }) }
}

function installation(id: AiModelRuntimeId, endpoint: string, model: string, exposure: 'loopback' | 'allInterfaces' | 'localNetwork') {
  return {
    id, name: id === 'ollama' ? 'Ollama' : 'LM Studio', detected: true as const, health: 'healthy' as const,
    endpoint, exposure, protocols: id === 'ollama' ? ['ollamaNative' as const] : ['openAICompatible' as const],
    models: [{ name: model, digest: `sha256:${model}`, sizeBytes: 1, running: true }], diagnostics: []
  }
}

function response(value: unknown) {
  return { ok: true, status: 200, text: async () => JSON.stringify(value) }
}

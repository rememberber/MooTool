import { describe, expect, it } from 'vitest'
import { PromptLabService } from '../../electron/main/ai/promptLabService'
import type { ModelRuntimeService } from '../../electron/main/ai/modelRuntimeService'
import type { AiModelRuntimeInstallation, AiModelRuntimeSnapshot } from './contracts/aiModelRuntime'

describe('PromptLabService', () => {
  it('runs explicit test cases, substitutes variables, scores outputs, and records usage without persisting them', async () => {
    const requests: Array<{ url: string; body: string }> = []
    const service = new PromptLabService({
      runtimes: runtimeService(runtime()),
      fetcher: async (url, init) => {
        requests.push({ url, body: init.body })
        const input = JSON.parse(init.body) as { messages: Array<{ content: string }> }
        return response({
          choices: [{ message: { content: `Hello from ${input.messages.at(-1)?.content ?? ''}` } }],
          usage: { prompt_tokens: 12, completion_tokens: 5, total_tokens: 17 }
        })
      }
    })
    const result = await service.run({
      requestId: crypto.randomUUID(),
      runtimeId: 'lmStudio',
      model: 'local-model',
      systemPrompt: 'Be concise.',
      promptTemplate: 'Target={{input}}',
      testCases: [
        { id: crypto.randomUUID(), name: 'passes', input: 'MooTool', expectedContains: 'mootool' },
        { id: crypto.randomUUID(), name: 'fails', input: 'Codex', expectedContains: 'missing' }
      ],
      temperature: 0.2,
      maxTokens: 128,
      confirmNetworkEndpoint: false
    })

    expect(requests).toHaveLength(2)
    expect(requests[0].url).toBe('http://127.0.0.1:1234/v1/chat/completions')
    expect(requests[0].body).toContain('Target=MooTool')
    expect(result.results.map((item) => item.passed)).toEqual([true, false])
    expect(result.summary).toMatchObject({ cases: 2, completed: 2, passed: 1, scored: 2, passRate: 0.5, promptTokens: 24, completionTokens: 10, totalTokens: 34 })
  })

  it('requires confirmation for network endpoints and blocks remote plaintext HTTP', async () => {
    const network = runtime({ endpoint: 'https://192.168.1.20:1234', exposure: 'localNetwork' })
    const service = new PromptLabService({ runtimes: runtimeService(network), fetcher: async () => response({ choices: [{ message: { content: 'ok' } }] }) })
    await expect(service.run(input())).rejects.toThrow('explicit confirmation')

    const remote = runtime({ endpoint: 'http://models.example.test:1234', exposure: 'remote' })
    const blocked = new PromptLabService({ runtimes: runtimeService(remote), fetcher: async () => response({}) })
    await expect(blocked.run({ ...input(), confirmNetworkEndpoint: true })).rejects.toThrow('unencrypted remote')
  })

  it('cancels an in-flight model request', async () => {
    const service = new PromptLabService({
      runtimes: runtimeService(runtime()),
      fetcher: async (_url, init) => await new Promise((resolve, reject) => {
        init.signal.addEventListener('abort', () => reject(new Error('aborted')), { once: true })
        void resolve
      })
    })
    const request = input()
    const running = service.run(request)
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(service.cancel(request.requestId)).toBe(true)
    await expect(running).resolves.toMatchObject({ cancelled: true, results: [] })
  })
})

function input() {
  return {
    requestId: crypto.randomUUID(),
    runtimeId: 'lmStudio' as const,
    model: 'local-model',
    systemPrompt: '',
    promptTemplate: '{{input}}',
    testCases: [{ id: crypto.randomUUID(), name: 'case', input: 'hello', expectedContains: '' }],
    temperature: 0,
    maxTokens: 32,
    confirmNetworkEndpoint: false
  }
}

function runtime(overrides: Partial<AiModelRuntimeInstallation> = {}): AiModelRuntimeInstallation {
  return {
    id: 'lmStudio',
    name: 'LM Studio',
    detected: true,
    health: 'healthy',
    endpoint: 'http://127.0.0.1:1234',
    exposure: 'loopback',
    protocols: ['lmStudioNative', 'openAICompatible'],
    models: [{ name: 'local-model', digest: 'sha256:model', sizeBytes: 100, running: true }],
    diagnostics: [],
    ...overrides
  }
}

function runtimeService(selected: AiModelRuntimeInstallation): ModelRuntimeService {
  const snapshot = {
    scannedAt: new Date().toISOString(),
    readOnly: true,
    runtime: selected,
    runtimes: [selected],
    resources: { platform: 'darwin', architecture: 'x64', cpuModel: 'Intel', totalMemoryBytes: 1, freeMemoryBytes: 1, cpuOnly: true, modelDirectory: '/models', modelDirectoryExists: false },
    stats: { models: 1, runningModels: 1, totalModelBytes: 100, loadedBytes: 0, vramBytes: 0 }
  } satisfies AiModelRuntimeSnapshot
  return { scan: async () => snapshot } as unknown as ModelRuntimeService
}

function response(value: unknown) {
  return { ok: true, status: 200, text: async () => JSON.stringify(value) }
}

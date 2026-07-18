import { randomUUID } from 'node:crypto'
import { describe, expect, it } from 'vitest'
import { ModelRuntimeActionService } from '../../electron/main/ai/modelRuntimeActionService'
import type { AiAgentProfile } from './contracts/aiAgents'
import type { AiLocalModelRuntimeModel, AiModelRuntimeId, AiModelRuntimeInstallation, AiModelRuntimeSnapshot } from './contracts/aiModelRuntime'
import { isAiModelRuntimeActionExecuteInput, isAiModelRuntimeActionPlanInput, type AiModelRuntimeActionExecuteInput } from './contracts/aiModelRuntimeActions'

describe('controlled model runtime lifecycle actions', () => {
  it('plans and verifies destructive Ollama deletion with Digest and affected Profile revalidation', async () => {
    const model = runtimeModel({ name: 'qwen3:8b', digest: 'sha256:qwen', sizeBytes: 5_000, running: false })
    const state = runtimeState('ollama', [model])
    const profile = agentProfile({ model: model.name, modelRuntimeId: 'ollama', localModelDigest: model.digest })
    const requests: Array<{ url: string; method: string; body?: string }> = []
    const service = new ModelRuntimeActionService({
      runtimes: { scan: async () => snapshot(state.runtime) },
      profiles: { list: () => [profile] },
      fetcher: async (url, init) => {
        requests.push({ url, method: init.method, body: init.body })
        state.runtime.models = []
        return response({})
      },
      pollIntervalMs: 1
    })

    const plan = await service.plan({ runtimeId: 'ollama', action: 'delete', modelName: model.name })
    expect(plan).toMatchObject({ destructive: true, modelDigest: model.digest, modelSizeBytes: 5_000, affectedAgentProfiles: [{ id: profile.id, name: profile.name }] })
    await expect(service.execute(approval(plan.planId, { confirmDestructive: false }))).rejects.toThrow('destructive-action confirmation')
    const result = await service.execute(approval(plan.planId))

    expect(result.status).toBe('completed')
    expect(requests).toEqual([{ url: 'http://127.0.0.1:11434/api/delete', method: 'DELETE', body: JSON.stringify({ model: model.name }) }])
  })

  it('streams bounded Ollama pull progress and verifies the downloaded inventory', async () => {
    const state = runtimeState('ollama', [])
    const progress: string[] = []
    const service = new ModelRuntimeActionService({
      runtimes: { scan: async () => snapshot(state.runtime) },
      profiles: { list: () => [] },
      fetcher: async () => {
        state.runtime.models = [runtimeModel({ name: 'gemma3:4b', digest: 'sha256:gemma', sizeBytes: 100, running: false })]
        return textResponse([
          JSON.stringify({ status: 'pulling manifest' }),
          JSON.stringify({ status: 'downloading', completed: 50, total: 100 }),
          JSON.stringify({ status: 'success', completed: 100, total: 100 })
        ].join('\n'))
      },
      pollIntervalMs: 1
    })

    const plan = await service.plan({ runtimeId: 'ollama', action: 'pull', modelName: 'gemma3:4b' })
    const result = await service.execute(approval(plan.planId), (event) => progress.push(JSON.stringify(event)))

    expect(result.status).toBe('completed')
    expect(progress.join('\n')).toContain('"percent":50')
    expect(progress.join('\n')).toContain('"percent":100')
  })

  it('uses a secure LM Studio token for load/unload without returning it through plans, progress, or results', async () => {
    const secret = 'lm-secret-token-123456'
    const model = runtimeModel({ name: 'ibm/granite-4-micro', digest: 'sha256:granite', sizeBytes: 2_000, running: false, runtimeInstanceIds: [] })
    const state = runtimeState('lmStudio', [model])
    const requests: Array<{ url: string; authorization?: string; body?: string }> = []
    const service = new ModelRuntimeActionService({
      runtimes: { scan: async () => snapshot(state.runtime) },
      profiles: { list: () => [] },
      credentialProvider: () => secret,
      fetcher: async (url, init) => {
        requests.push({ url, authorization: init.headers.authorization, body: init.body })
        if (url.endsWith('/load')) {
          state.runtime.models[0] = { ...state.runtime.models[0], running: true, runtimeInstanceIds: ['instance-granite'] }
          return response({ status: 'loaded', instance_id: 'instance-granite' })
        }
        state.runtime.models[0] = { ...state.runtime.models[0], running: false, runtimeInstanceIds: [] }
        return response({ instance_id: 'instance-granite' })
      },
      pollIntervalMs: 1
    })

    const loadPlan = await service.plan({ runtimeId: 'lmStudio', action: 'load', modelName: model.name })
    const loadResult = await service.execute(approval(loadPlan.planId))
    const unloadPlan = await service.plan({ runtimeId: 'lmStudio', action: 'unload', modelName: model.name })
    const unloadResult = await service.execute(approval(unloadPlan.planId))

    expect(loadPlan.authenticationConfigured).toBe(true)
    expect(requests).toEqual([
      { url: 'http://127.0.0.1:1234/api/v1/models/load', authorization: `Bearer ${secret}`, body: JSON.stringify({ model: model.name }) },
      { url: 'http://127.0.0.1:1234/api/v1/models/unload', authorization: `Bearer ${secret}`, body: JSON.stringify({ instance_id: 'instance-granite' }) }
    ])
    expect(JSON.stringify([loadPlan, loadResult, unloadPlan, unloadResult])).not.toContain(secret)
  })

  it('cancels an active download and blocks unverified or non-local runtime APIs', async () => {
    const state = runtimeState('ollama', [])
    const service = new ModelRuntimeActionService({
      runtimes: { scan: async () => snapshot(state.runtime) },
      profiles: { list: () => [] },
      fetcher: async (_url, init) => new Promise((_resolve, reject) => {
        init.signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')), { once: true })
      }),
      pollIntervalMs: 1
    })
    const plan = await service.plan({ runtimeId: 'ollama', action: 'pull', modelName: 'slow:latest' })
    const requestId = randomUUID()
    const running = service.execute(approval(plan.planId, { requestId }))
    await new Promise((resolve) => setTimeout(resolve, 10))
    expect(service.cancel(requestId)).toBe(true)
    expect(await running).toMatchObject({ status: 'cancelled' })

    const remote = runtimeState('ollama', [])
    remote.runtime.endpoint = 'https://models.example.test'
    remote.runtime.exposure = 'remote'
    const remoteService = new ModelRuntimeActionService({ runtimes: { scan: async () => snapshot(remote.runtime) }, profiles: { list: () => [] } })
    await expect(remoteService.plan({ runtimeId: 'ollama', action: 'pull', modelName: 'qwen3' })).rejects.toThrow('limited to local')
    await expect(remoteService.plan({ runtimeId: 'vllm', action: 'load', modelName: 'qwen3' })).rejects.toThrow('verified lifecycle API')
  })

  it('validates model names, UUIDs, and explicit confirmation flags at typed IPC boundaries', () => {
    expect(isAiModelRuntimeActionPlanInput({ runtimeId: 'ollama', action: 'pull', modelName: 'qwen3:8b' })).toBe(true)
    expect(isAiModelRuntimeActionPlanInput({ runtimeId: 'ollama', action: 'pull', modelName: 'bad\nname' })).toBe(false)
    expect(isAiModelRuntimeActionExecuteInput(approval(randomUUID()))).toBe(true)
    expect(isAiModelRuntimeActionExecuteInput({ ...approval(randomUUID()), confirmAction: 'yes' })).toBe(false)
  })
})

function runtimeState(id: AiModelRuntimeId, models: AiLocalModelRuntimeModel[]): { runtime: AiModelRuntimeInstallation } {
  return { runtime: {
    id,
    name: id === 'ollama' ? 'Ollama' : id === 'lmStudio' ? 'LM Studio' : id,
    detected: true,
    health: 'healthy',
    endpoint: id === 'lmStudio' ? 'http://127.0.0.1:1234' : id === 'ollama' ? 'http://127.0.0.1:11434' : 'http://127.0.0.1:8000',
    exposure: 'loopback',
    protocols: ['openAICompatible'],
    modelDirectory: '/models',
    modelDirectoryExists: true,
    modelDirectoryAvailableBytes: 10_000,
    models,
    diagnostics: []
  } }
}

function snapshot(runtime: AiModelRuntimeInstallation): AiModelRuntimeSnapshot {
  return {
    scannedAt: new Date().toISOString(),
    readOnly: true,
    runtime: runtime.id === 'ollama' ? runtime : runtimeState('ollama', []).runtime,
    runtimes: runtime.id === 'ollama' ? [runtime] : [runtimeState('ollama', []).runtime, runtime],
    resources: {
      platform: process.platform,
      architecture: process.arch,
      cpuModel: 'Test CPU',
      totalMemoryBytes: 16_000,
      freeMemoryBytes: 8_000,
      cpuOnly: false,
      modelDirectory: '/models',
      modelDirectoryExists: true,
      modelDirectoryAvailableBytes: 10_000
    },
    stats: { models: runtime.models.length, runningModels: runtime.models.filter((model) => model.running).length, totalModelBytes: 0, loadedBytes: 0, vramBytes: 0 }
  }
}

function runtimeModel(overrides: Partial<AiLocalModelRuntimeModel> = {}): AiLocalModelRuntimeModel {
  return { name: 'model', digest: 'sha256:model', sizeBytes: 1_000, running: false, ...overrides }
}

function agentProfile(overrides: Partial<AiAgentProfile> = {}): AiAgentProfile {
  const now = new Date(0).toISOString()
  return {
    id: randomUUID(), name: 'Local profile', clientId: 'codex', workingDirectory: '/project', permissionMode: 'readOnly',
    mcpServerNames: [], skillNames: [], environmentVariableRefs: [], optionalFlags: [], createdAt: now, updatedAt: now, ...overrides
  }
}

function approval(planId: string, overrides: Partial<AiModelRuntimeActionExecuteInput> = {}): AiModelRuntimeActionExecuteInput {
  return { ...approvalBase(planId), ...overrides }
}

function approvalBase(planId: string = randomUUID()): AiModelRuntimeActionExecuteInput {
  return { requestId: randomUUID(), planId, confirmAction: true, confirmDestructive: true, confirmRemoteEndpoint: true }
}

function response(value: unknown) {
  return textResponse(JSON.stringify(value))
}

function textResponse(value: string) {
  return { ok: true, status: 200, text: async () => value, body: null }
}

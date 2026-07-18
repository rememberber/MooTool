import { chmod, mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { ModelRuntimeService } from '../../electron/main/ai/modelRuntimeService'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('ModelRuntimeService', () => {
  it('discovers Ollama, downloaded and running models, resources, and endpoint exposure without sending a prompt', async () => {
    const root = await fixtureRoot()
    const binaryDirectory = join(root, 'bin')
    const modelDirectory = join(root, 'models')
    await Promise.all([mkdir(binaryDirectory), mkdir(modelDirectory)])
    const binaryPath = join(binaryDirectory, 'ollama')
    await writeFile(binaryPath, '#!/bin/sh\nexit 0\n')
    await chmod(binaryPath, 0o755)
    const requests: Array<{ url: string; method?: string }> = []
    const service = new ModelRuntimeService({
      homeDirectory: root,
      pathValue: binaryDirectory,
      includeDefaultExecutablePaths: false,
      endpoint: '0.0.0.0:11434',
      modelDirectory,
      platform: 'darwin',
      architecture: 'x64',
      versionReader: async () => '0.13.3',
      systemResources: () => ({ cpuModel: 'Intel Core i7', totalMemoryBytes: 16_000, freeMemoryBytes: 8_000 }),
      fetcher: async (url, init) => {
        requests.push({ url, method: init.method })
        if (url.endsWith('/api/version')) return jsonResponse({ version: '0.13.3' })
        if (url.endsWith('/api/ps')) return jsonResponse({ models: [{
          name: 'qwen3:8b', digest: 'sha256:qwen', size: 6_000, size_vram: 0, context_length: 32_768, expires_at: '2026-07-18T12:00:00.000Z'
        }] })
        return jsonResponse({ models: [{
          name: 'qwen3:8b', digest: 'sha256:qwen', size: 5_000, modified_at: '2026-07-17T12:00:00.000Z',
          details: { format: 'gguf', family: 'qwen3', parameter_size: '8B', quantization_level: 'Q4_K_M' }
        }] })
      }
    })

    const snapshot = await service.scan()

    expect(snapshot.readOnly).toBe(true)
    expect(snapshot.runtime).toMatchObject({
      detected: true,
      health: 'healthy',
      binaryPath,
      cliVersion: '0.13.3',
      apiVersion: '0.13.3',
      endpoint: 'http://0.0.0.0:11434',
      exposure: 'allInterfaces',
      protocols: ['ollamaNative', 'openAICompatible', 'anthropicCompatible']
    })
    expect(snapshot.runtime.models).toEqual([expect.objectContaining({
      name: 'qwen3:8b', running: true, format: 'gguf', parameterSize: '8B', quantization: 'Q4_K_M', contextLength: 32_768
    })])
    expect(snapshot.resources).toMatchObject({ architecture: 'x64', cpuModel: 'Intel Core i7', cpuOnly: true, modelDirectoryExists: true })
    expect(snapshot.stats).toMatchObject({ models: 1, runningModels: 1, totalModelBytes: 5_000, loadedBytes: 6_000, vramBytes: 0 })
    expect(snapshot.runtime.diagnostics).toContainEqual(expect.objectContaining({ code: 'RUNTIME_ENDPOINT_EXPOSED' }))
    expect(requests.map((request) => request.url)).toEqual(expect.arrayContaining([
      'http://127.0.0.1:11434/api/version',
      'http://127.0.0.1:11434/api/tags',
      'http://127.0.0.1:11434/api/ps'
    ]))
    expect(requests.every((request) => request.method === undefined)).toBe(true)
  })

  it('reports a missing installation without making other AI workbench features depend on Ollama', async () => {
    const root = await fixtureRoot()
    const service = new ModelRuntimeService({
      homeDirectory: root,
      pathValue: join(root, 'empty'),
      includeDefaultExecutablePaths: false,
      modelDirectory: join(root, 'missing-models'),
      requestTimeoutMs: 10,
      fetcher: async () => { throw new Error('connection refused') }
    })

    const snapshot = await service.scan()

    expect(snapshot.runtime).toMatchObject({ detected: false, health: 'notInstalled', models: [] })
    expect(snapshot.runtime.diagnostics).toContainEqual(expect.objectContaining({ code: 'RUNTIME_NOT_INSTALLED', severity: 'info' }))
  })

  it('keeps the runtime visible as degraded when an optional inventory response is malformed', async () => {
    const root = await fixtureRoot()
    const service = new ModelRuntimeService({
      homeDirectory: root,
      pathValue: '',
      includeDefaultExecutablePaths: false,
      fetcher: async (url) => url.endsWith('/api/version') ? jsonResponse({ version: '1.0.0' }) : url.endsWith('/api/tags') ? textResponse('{broken') : jsonResponse({ models: [] })
    })

    const snapshot = await service.scan()

    expect(snapshot.runtime).toMatchObject({ detected: true, health: 'degraded', apiVersion: '1.0.0' })
    expect(snapshot.runtime.diagnostics).toContainEqual(expect.objectContaining({ code: 'RUNTIME_API_INVALID' }))
  })

  it('reads model metadata without loading the model or returning its prompt template', async () => {
    const root = await fixtureRoot()
    let request: { method?: string; body?: string } | undefined
    const service = new ModelRuntimeService({
      homeDirectory: root,
      pathValue: '',
      includeDefaultExecutablePaths: false,
      fetcher: async (_url, init) => {
        request = init
        return jsonResponse({
          modified_at: '2026-07-17T12:00:00.000Z',
          details: { format: 'gguf', family: 'gemma3', parameter_size: '4.3B', quantization_level: 'Q4_K_M' },
          capabilities: ['completion', 'vision'],
          parameters: 'temperature 0.7',
          license: 'Example license',
          template: 'PRIVATE TEMPLATE THAT MUST NOT CROSS IPC',
          model_info: { 'gemma3.context_length': 131_072 }
        })
      }
    })

    const detail = await service.inspectModel({ runtimeId: 'ollama', modelName: 'gemma3' })

    expect(request).toMatchObject({ method: 'POST', body: JSON.stringify({ model: 'gemma3', verbose: false }) })
    expect(detail).toMatchObject({ modelName: 'gemma3', contextLength: 131_072, capabilities: ['completion', 'vision'], parameterText: 'temperature 0.7' })
    expect(JSON.stringify(detail)).not.toContain('PRIVATE TEMPLATE')
  })

  it('discovers LM Studio, llama.cpp, vLLM, and LocalAI through their read-only model APIs', async () => {
    const root = await fixtureRoot()
    const binaryDirectory = join(root, 'bin')
    await mkdir(binaryDirectory)
    const binaries = ['lms', 'llama-server', 'vllm', 'local-ai'].map((name) => join(binaryDirectory, name))
    await Promise.all(binaries.map((path) => writeFile(path, '#!/bin/sh\nexit 0\n')))
    await Promise.all(binaries.map((path) => chmod(path, 0o755)))
    const requests: Array<{ url: string; method?: string }> = []
    const service = new ModelRuntimeService({
      homeDirectory: root,
      pathValue: binaryDirectory,
      includeDefaultExecutablePaths: false,
      modelDirectory: join(root, 'missing-ollama-models'),
      endpoints: {
        lmStudio: 'http://127.0.0.1:1234',
        llamaCpp: 'http://127.0.0.1:18080',
        vllm: 'http://127.0.0.1:18000',
        localAi: 'http://127.0.0.1:18081'
      },
      versionReader: async (path) => path.endsWith('lms') ? '0.4.0' : '1.0.0',
      fetcher: async (url, init) => {
        requests.push({ url, method: init.method })
        if (url.includes(':11434')) throw new Error('Ollama offline')
        if (url.endsWith(':1234/api/v1/models')) return jsonResponse({ models: [{
          type: 'llm', key: 'google/gemma-4-4b', display_name: 'Gemma 4', architecture: 'gemma4',
          quantization: { name: 'Q4_K_M' }, size_bytes: 3_000, params_string: '4B', format: 'gguf',
          loaded_instances: [{ id: 'gemma', config: { context_length: 8192 } }], max_context_length: 131072,
          capabilities: { vision: true, trained_for_tool_use: true }
        }] })
        if (url.endsWith(':18080/health')) return jsonResponse({ status: 'ok' })
        if (url.endsWith(':18080/v1/models')) return jsonResponse({ data: [{
          id: 'gemma.gguf', owned_by: 'llamacpp', meta: { size: 2_000, n_ctx_train: 4096, n_params: 4_000_000_000 }
        }] })
        if (url.endsWith(':18000/version')) return jsonResponse({ version: '0.11.0' })
        if (url.endsWith(':18000/v1/models')) return jsonResponse({ data: [{ id: 'Qwen/Qwen3-8B', owned_by: 'vllm' }] })
        if (url.endsWith(':18081/.well-known/localai.json')) return jsonResponse({ version: '3.0.0', endpoints: { models: '/v1/models' } })
        if (url.endsWith(':18081/v1/models')) return jsonResponse({ data: [{ id: 'local-qwen', owned_by: 'localai' }, { id: 'local-embed', owned_by: 'localai' }] })
        if (url.endsWith(':18081/system')) return jsonResponse({ loaded_models: [{ id: 'local-qwen' }] })
        throw new Error(`Unexpected request: ${url}`)
      }
    })

    const snapshot = await service.scan()

    expect(snapshot.runtimes.map((runtime) => runtime.id)).toEqual(['ollama', 'lmStudio', 'llamaCpp', 'vllm', 'localAi'])
    expect(snapshot.runtimes.find((runtime) => runtime.id === 'lmStudio')).toMatchObject({
      detected: true,
      health: 'healthy',
      protocols: ['lmStudioNative', 'openAICompatible', 'anthropicCompatible'],
      models: [expect.objectContaining({ name: 'google/gemma-4-4b', running: true, contextLength: 8192, capabilities: ['llm', 'vision', 'toolUse'] })]
    })
    expect(snapshot.runtimes.find((runtime) => runtime.id === 'llamaCpp')?.models[0]).toMatchObject({ name: 'gemma.gguf', running: true, parameterSize: '4B' })
    expect(snapshot.runtimes.find((runtime) => runtime.id === 'vllm')).toMatchObject({ apiVersion: '0.11.0', health: 'healthy' })
    expect(snapshot.runtimes.find((runtime) => runtime.id === 'localAi')?.models).toEqual([
      expect.objectContaining({ name: 'local-qwen', running: true }),
      expect.objectContaining({ name: 'local-embed', running: false })
    ])
    expect(requests.every((request) => request.method === undefined)).toBe(true)
    expect(requests.some((request) => /chat|completion|response/.test(request.url))).toBe(false)
  })
})

async function fixtureRoot(): Promise<string> {
  const root = await mkdtemp(join(tmpdir(), 'mootool-model-runtime-'))
  temporaryDirectories.push(root)
  return root
}

function jsonResponse(value: unknown) {
  return textResponse(JSON.stringify(value))
}

function textResponse(value: string) {
  return { ok: true, status: 200, text: async () => value }
}

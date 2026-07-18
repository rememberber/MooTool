import { execFile } from 'node:child_process'
import { createHash } from 'node:crypto'
import { constants } from 'node:fs'
import { access, stat, statfs } from 'node:fs/promises'
import { arch as systemArch, cpus, freemem, platform as systemPlatform, totalmem } from 'node:os'
import { delimiter, join, resolve } from 'node:path'
import { promisify } from 'node:util'
import type {
  AiLocalModelRuntimeModel,
  AiModelRuntimeDetailInput,
  AiModelRuntimeDiagnostic,
  AiModelRuntimeExposure,
  AiModelRuntimeId,
  AiModelRuntimeInstallation,
  AiModelRuntimeModelDetail,
  AiModelRuntimeSnapshot
} from '../../../src/shared/contracts/aiModelRuntime'

type RuntimeHttpResponse = {
  ok: boolean
  status: number
  text(): Promise<string>
}

type RuntimeFetcher = (url: string, init: { method?: string; body?: string; headers?: Record<string, string>; signal: AbortSignal }) => Promise<RuntimeHttpResponse>

type ModelRuntimeServiceOptions = {
  homeDirectory: string
  pathValue?: string
  endpoint?: string
  endpoints?: Partial<Record<AiModelRuntimeId, string>>
  modelDirectory?: string
  platform?: NodeJS.Platform
  architecture?: string
  includeDefaultExecutablePaths?: boolean
  requestTimeoutMs?: number
  fetcher?: RuntimeFetcher
  versionReader?: (binaryPath: string) => Promise<string | undefined>
  systemResources?: () => { cpuModel: string; totalMemoryBytes: number; freeMemoryBytes: number }
  credentialProvider?: (runtimeId: AiModelRuntimeId) => string
}

const execFileAsync = promisify(execFile)
const maximumResponseBytes = 8 * 1024 * 1024
const maximumModels = 2_000

type RuntimeAdapter = {
  id: Exclude<AiModelRuntimeId, 'ollama'>
  name: string
  binaryNames: string[]
  defaultEndpoint: string
  environmentEndpoint?: string
  protocols: AiModelRuntimeInstallation['protocols']
}

const runtimeAdapters: RuntimeAdapter[] = [
  { id: 'lmStudio', name: 'LM Studio', binaryNames: ['lms'], defaultEndpoint: 'http://127.0.0.1:1234', environmentEndpoint: process.env.LM_STUDIO_ENDPOINT, protocols: ['lmStudioNative', 'openAICompatible', 'anthropicCompatible'] },
  { id: 'llamaCpp', name: 'llama.cpp', binaryNames: ['llama-server'], defaultEndpoint: 'http://127.0.0.1:8080', environmentEndpoint: process.env.LLAMA_CPP_ENDPOINT, protocols: ['openAICompatible', 'anthropicCompatible'] },
  { id: 'vllm', name: 'vLLM', binaryNames: ['vllm'], defaultEndpoint: 'http://127.0.0.1:8000', environmentEndpoint: process.env.VLLM_ENDPOINT, protocols: ['openAICompatible'] },
  { id: 'localAi', name: 'LocalAI', binaryNames: ['local-ai'], defaultEndpoint: 'http://127.0.0.1:8080', environmentEndpoint: process.env.LOCALAI_ENDPOINT, protocols: ['openAICompatible', 'anthropicCompatible'] }
]

export class ModelRuntimeService {
  private readonly homeDirectory: string
  private readonly pathValue: string
  private readonly endpoint: EndpointDescription
  private readonly runtimeEndpoints: Record<Exclude<AiModelRuntimeId, 'ollama'>, EndpointDescription>
  private readonly modelDirectory: string
  private readonly platform: NodeJS.Platform
  private readonly architecture: string
  private readonly includeDefaultExecutablePaths: boolean
  private readonly requestTimeoutMs: number
  private readonly fetcher: RuntimeFetcher
  private readonly versionReader: (binaryPath: string) => Promise<string | undefined>
  private readonly systemResources: () => { cpuModel: string; totalMemoryBytes: number; freeMemoryBytes: number }
  private readonly credentialProvider: (runtimeId: AiModelRuntimeId) => string

  constructor(options: ModelRuntimeServiceOptions) {
    this.homeDirectory = resolve(options.homeDirectory)
    this.pathValue = options.pathValue ?? process.env.PATH ?? ''
    this.platform = options.platform ?? systemPlatform()
    this.architecture = options.architecture ?? systemArch()
    this.includeDefaultExecutablePaths = options.includeDefaultExecutablePaths ?? true
    this.requestTimeoutMs = options.requestTimeoutMs ?? 1_500
    this.endpoint = describeEndpoint(options.endpoint ?? process.env.OLLAMA_HOST ?? 'http://127.0.0.1:11434')
    this.runtimeEndpoints = Object.fromEntries(runtimeAdapters.map((adapter) => [
      adapter.id,
      describeEndpoint(options.endpoints?.[adapter.id] ?? adapter.environmentEndpoint ?? adapter.defaultEndpoint)
    ])) as Record<Exclude<AiModelRuntimeId, 'ollama'>, EndpointDescription>
    this.modelDirectory = resolve(options.modelDirectory ?? process.env.OLLAMA_MODELS ?? defaultModelDirectory(this.homeDirectory, this.platform))
    this.fetcher = options.fetcher ?? ((url, init) => fetch(url, init))
    this.versionReader = options.versionReader ?? readCliVersion
    this.systemResources = options.systemResources ?? (() => ({
      cpuModel: cpus()[0]?.model ?? 'Unknown CPU',
      totalMemoryBytes: totalmem(),
      freeMemoryBytes: freemem()
    }))
    this.credentialProvider = options.credentialProvider ?? (() => '')
  }

  async scan(): Promise<AiModelRuntimeSnapshot> {
    const diagnostics: AiModelRuntimeDiagnostic[] = []
    const additionalRuntimesPromise = Promise.all(runtimeAdapters.map((adapter) => this.scanCompatibleRuntime(adapter)))
    const binaryPath = await this.findExecutable(['ollama'])
    const modelDirectoryState = await this.readModelDirectoryState(diagnostics)
    const cliVersion = binaryPath ? await this.versionReader(binaryPath).catch(() => undefined) : undefined
    const startedAt = performance.now()
    let apiVersion: string | undefined
    let responseTimeMs: number | undefined
    let models: AiLocalModelRuntimeModel[] = []
    let apiHealthy = false
    let apiDegraded = false

    try {
      const versionPayload = await this.fetchJson('/api/version')
      apiVersion = readString(versionPayload, 'version') || undefined
      if (!apiVersion) {
        apiDegraded = true
        diagnostics.push(diagnostic('RUNTIME_API_INVALID', 'warning', 'Ollama returned a version response without a valid version field.'))
      }
      responseTimeMs = Math.max(0, Math.round(performance.now() - startedAt))
      apiHealthy = true
      const [tagsResult, runningResult] = await Promise.allSettled([this.fetchJson('/api/tags'), this.fetchJson('/api/ps')])
      if (tagsResult.status === 'fulfilled') {
        try {
          models = normalizeModels(tagsResult.value, runningResult.status === 'fulfilled' ? runningResult.value : undefined)
        } catch (error) {
          apiDegraded = true
          diagnostics.push(diagnostic('RUNTIME_API_INVALID', 'warning', errorMessage(error)))
        }
      } else {
        apiDegraded = true
        diagnostics.push(diagnostic('RUNTIME_API_INVALID', 'warning', `Ollama model inventory failed: ${errorMessage(tagsResult.reason)}`))
      }
      if (runningResult.status === 'rejected') {
        apiDegraded = true
        diagnostics.push(diagnostic('RUNTIME_API_INVALID', 'warning', `Ollama running-model inventory failed: ${errorMessage(runningResult.reason)}`))
      }
    } catch {
      // A stopped or absent local service is an expected state.
    }

    const detected = Boolean(binaryPath) || modelDirectoryState.exists || apiHealthy
    if (!detected) diagnostics.push(diagnostic('RUNTIME_NOT_INSTALLED', 'info', 'Ollama was not found. MooTool will not install it automatically.'))
    else if (!apiHealthy) diagnostics.push(diagnostic('RUNTIME_SERVICE_STOPPED', 'warning', 'Ollama is installed, but its API is not responding.'))
    if (this.endpoint.exposure !== 'loopback') {
      diagnostics.push(diagnostic('RUNTIME_ENDPOINT_EXPOSED', 'warning', 'The Ollama endpoint is not limited to loopback. Verify network exposure and authentication before using sensitive prompts.'))
    }
    if (this.endpoint.displayUrl.startsWith('http://') && this.endpoint.exposure === 'remote') {
      diagnostics.push(diagnostic('RUNTIME_REMOTE_HTTP', 'error', 'The remote Ollama endpoint uses unencrypted HTTP.'))
    }

    const system = this.systemResources()
    const stats = summarizeModels(models)
    const runtime: AiModelRuntimeInstallation = {
      id: 'ollama',
      name: 'Ollama',
      detected,
      health: apiHealthy ? apiDegraded ? 'degraded' : 'healthy' : detected ? 'stopped' : 'notInstalled',
      binaryPath,
      cliVersion,
      apiVersion,
      endpoint: this.endpoint.displayUrl,
      exposure: this.endpoint.exposure,
      protocols: ['ollamaNative', 'openAICompatible', 'anthropicCompatible'],
      responseTimeMs,
      modelDirectory: this.modelDirectory,
      modelDirectoryExists: modelDirectoryState.exists,
      modelDirectoryAvailableBytes: modelDirectoryState.availableBytes,
      models,
      diagnostics
    }
    return {
      scannedAt: new Date().toISOString(),
      readOnly: true,
      runtime,
      runtimes: [runtime, ...await additionalRuntimesPromise],
      resources: {
        platform: this.platform,
        architecture: this.architecture,
        cpuModel: system.cpuModel,
        totalMemoryBytes: system.totalMemoryBytes,
        freeMemoryBytes: system.freeMemoryBytes,
        cpuOnly: this.platform === 'darwin' && this.architecture === 'x64',
        modelDirectory: this.modelDirectory,
        modelDirectoryExists: modelDirectoryState.exists,
        modelDirectoryAvailableBytes: modelDirectoryState.availableBytes
      },
      stats
    }
  }

  async inspectModel(input: AiModelRuntimeDetailInput): Promise<AiModelRuntimeModelDetail> {
    if (input.runtimeId !== 'ollama') {
      const runtime = (await this.scan()).runtimes.find((candidate) => candidate.id === input.runtimeId)
      const model = runtime?.models.find((candidate) => candidate.name === input.modelName)
      if (!runtime || !model) throw new Error('The selected runtime model is no longer available')
      return {
        runtimeId: input.runtimeId,
        modelName: model.name,
        modifiedAt: model.modifiedAt,
        format: model.format,
        family: model.family,
        parameterSize: model.parameterSize,
        quantization: model.quantization,
        contextLength: model.contextLength,
        capabilities: model.capabilities ?? []
      }
    }
    const payload = await this.fetchJson('/api/show', { method: 'POST', body: JSON.stringify({ model: input.modelName, verbose: false }) })
    if (!isRecord(payload)) throw new Error('Ollama returned an invalid model detail response')
    const details = isRecord(payload.details) ? payload.details : {}
    const modelInfo = isRecord(payload.model_info) ? payload.model_info : {}
    const contextLengths = Object.entries(modelInfo)
      .filter(([key, value]) => key.endsWith('.context_length') && isNonNegativeNumber(value))
      .map(([, value]) => Number(value))
    return {
      runtimeId: 'ollama',
      modelName: input.modelName,
      modifiedAt: validIsoDate(readString(payload, 'modified_at')),
      format: readString(details, 'format') || undefined,
      family: readString(details, 'family') || undefined,
      parameterSize: readString(details, 'parameter_size') || undefined,
      quantization: readString(details, 'quantization_level') || undefined,
      contextLength: contextLengths.length > 0 ? Math.max(...contextLengths) : undefined,
      capabilities: Array.isArray(payload.capabilities) ? payload.capabilities.filter((item): item is string => typeof item === 'string').slice(0, 100) : [],
      parameterText: truncateText(readString(payload, 'parameters'), 16_384),
      licenseExcerpt: truncateText(readString(payload, 'license'), 4_096)
    }
  }

  private async scanCompatibleRuntime(adapter: RuntimeAdapter): Promise<AiModelRuntimeInstallation> {
    const endpoint = this.runtimeEndpoints[adapter.id]
    const diagnostics: AiModelRuntimeDiagnostic[] = []
    const binaryPath = await this.findExecutable(adapter.binaryNames)
    const cliVersion = binaryPath ? await this.versionReader(binaryPath).catch(() => undefined) : undefined
    const startedAt = performance.now()
    let apiVersion: string | undefined
    let models: AiLocalModelRuntimeModel[] = []
    let apiHealthy = false
    let apiResponded = false
    const headers = this.authorizationHeaders(adapter.id)

    try {
      if (adapter.id === 'lmStudio') {
        const payload = await this.fetchJsonAt(endpoint, '/api/v1/models', adapter.name, { headers })
        apiResponded = true
        models = normalizeLmStudioModels(payload, Boolean(binaryPath))
      } else if (adapter.id === 'llamaCpp') {
        const health = await this.fetchJsonAt(endpoint, '/health', adapter.name, { headers })
        apiResponded = true
        if (!isRecord(health) || readString(health, 'status') !== 'ok') throw new Error('llama.cpp returned an invalid health response')
        const payload = await this.fetchJsonAt(endpoint, '/v1/models', adapter.name, { headers })
        if (!binaryPath && !hasOpenAiRuntimeSignature(payload, 'llamaCpp')) throw new Error('The endpoint does not identify itself as llama.cpp')
        models = normalizeOpenAiModels(payload, adapter.id)
      } else if (adapter.id === 'vllm') {
        const version = await this.fetchJsonAt(endpoint, '/version', adapter.name, { headers })
        apiResponded = true
        apiVersion = readString(version, 'version') || undefined
        if (!apiVersion) throw new Error('vLLM returned an invalid version response')
        const payload = await this.fetchJsonAt(endpoint, '/v1/models', adapter.name, { headers })
        if (!binaryPath && !hasOpenAiRuntimeSignature(payload, 'vllm')) throw new Error('The endpoint does not identify itself as vLLM')
        models = normalizeOpenAiModels(payload, adapter.id)
      } else {
        const discovery = await this.fetchJsonAt(endpoint, '/.well-known/localai.json', adapter.name, { headers })
        apiResponded = true
        if (!isRecord(discovery) || !isRecord(discovery.endpoints)) throw new Error('LocalAI returned an invalid discovery response')
        apiVersion = readString(discovery, 'version') || undefined
        const [modelsPayload, systemPayload] = await Promise.all([
          this.fetchJsonAt(endpoint, '/v1/models', adapter.name, { headers }),
          this.fetchJsonAt(endpoint, '/system', adapter.name, { headers }).catch(() => undefined)
        ])
        const loaded = readLoadedModelNames(systemPayload)
        models = normalizeOpenAiModels(modelsPayload, adapter.id, loaded)
      }
      apiHealthy = true
    } catch (error) {
      if (apiResponded) diagnostics.push(diagnostic('RUNTIME_API_INVALID', 'warning', errorMessage(error)))
    }

    const detected = Boolean(binaryPath) || apiHealthy
    if (!detected) diagnostics.push(diagnostic('RUNTIME_NOT_INSTALLED', 'info', `${adapter.name} was not found. MooTool will not install it automatically.`))
    else if (!apiHealthy && !apiResponded) diagnostics.push(diagnostic('RUNTIME_SERVICE_STOPPED', 'warning', `${adapter.name} is installed, but its API is not responding.`))
    if (endpoint.exposure !== 'loopback') diagnostics.push(diagnostic('RUNTIME_ENDPOINT_EXPOSED', 'warning', `The ${adapter.name} endpoint is not limited to loopback. Verify network exposure and authentication before using sensitive prompts.`))
    if (endpoint.displayUrl.startsWith('http://') && endpoint.exposure === 'remote') diagnostics.push(diagnostic('RUNTIME_REMOTE_HTTP', 'error', `The remote ${adapter.name} endpoint uses unencrypted HTTP.`))

    return {
      id: adapter.id,
      name: adapter.name,
      detected,
      health: apiHealthy ? 'healthy' : apiResponded && detected ? 'degraded' : detected ? 'stopped' : 'notInstalled',
      binaryPath,
      cliVersion,
      apiVersion,
      endpoint: endpoint.displayUrl,
      exposure: endpoint.exposure,
      protocols: adapter.protocols,
      responseTimeMs: apiHealthy ? Math.max(0, Math.round(performance.now() - startedAt)) : undefined,
      models,
      diagnostics
    }
  }

  private async fetchJson(pathname: string, init: { method?: string; body?: string } = {}): Promise<unknown> {
    return this.fetchJsonAt(this.endpoint, pathname, 'Ollama', init)
  }

  private async fetchJsonAt(endpoint: EndpointDescription, pathname: string, runtimeName: string, init: { method?: string; body?: string; headers?: Record<string, string> } = {}): Promise<unknown> {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), this.requestTimeoutMs)
    try {
      const response = await this.fetcher(`${endpoint.probeUrl}${pathname}`, {
        ...init,
        ...((init.body || init.headers) ? { headers: { ...(init.body ? { 'content-type': 'application/json' } : {}), ...init.headers } } : {}),
        signal: controller.signal
      })
      if (!response.ok) throw new Error(`${runtimeName} API returned HTTP ${response.status}`)
      const source = await response.text()
      if (Buffer.byteLength(source, 'utf8') > maximumResponseBytes) throw new Error(`${runtimeName} API response exceeds 8 MB`)
      try {
        return JSON.parse(source) as unknown
      } catch {
        throw new Error(`${runtimeName} API returned malformed JSON`)
      }
    } finally {
      clearTimeout(timer)
    }
  }

  private authorizationHeaders(runtimeId: AiModelRuntimeId): Record<string, string> {
    const token = this.credentialProvider(runtimeId)
    return token ? { authorization: `Bearer ${token}` } : {}
  }

  private async findExecutable(names: string[]): Promise<string | undefined> {
    const directories = new Set(this.pathValue.split(delimiter).filter(Boolean).map((entry) => resolve(entry)))
    if (this.includeDefaultExecutablePaths) {
      directories.add(join(this.homeDirectory, '.local', 'bin'))
      directories.add(join(this.homeDirectory, '.lmstudio', 'bin'))
      directories.add('/usr/local/bin')
      directories.add('/opt/homebrew/bin')
    }
    const candidates = [...directories].flatMap((directory) => names.flatMap((name) => this.platform === 'win32'
      ? [join(directory, `${name}.exe`), join(directory, `${name}.cmd`), join(directory, name)]
      : [join(directory, name)]))
    if (this.includeDefaultExecutablePaths && this.platform === 'darwin' && names.includes('ollama')) candidates.push('/Applications/Ollama.app/Contents/Resources/ollama')
    for (const candidate of candidates) {
      try {
        const info = await stat(candidate)
        if (!info.isFile()) continue
        if (this.platform !== 'win32') await access(candidate, constants.X_OK)
        return candidate
      } catch {
        // Missing executable candidates are expected.
      }
    }
    return undefined
  }

  private async readModelDirectoryState(diagnostics: AiModelRuntimeDiagnostic[]): Promise<{ exists: boolean; availableBytes?: number }> {
    try {
      if (!(await stat(this.modelDirectory)).isDirectory()) return { exists: false }
      const filesystem = await statfs(this.modelDirectory)
      return { exists: true, availableBytes: Number(filesystem.bavail) * Number(filesystem.bsize) }
    } catch (error) {
      if (isMissing(error)) return { exists: false }
      diagnostics.push(diagnostic('RUNTIME_MODEL_DIRECTORY_UNREADABLE', 'warning', 'The Ollama model directory could not be inspected.'))
      return { exists: false }
    }
  }
}

type EndpointDescription = {
  displayUrl: string
  probeUrl: string
  exposure: AiModelRuntimeExposure
}

function describeEndpoint(value: string): EndpointDescription {
  const withProtocol = /^[a-z][a-z\d+.-]*:\/\//i.test(value.trim()) ? value.trim() : `http://${value.trim()}`
  const url = new URL(withProtocol)
  if (url.username || url.password) throw new Error('Ollama endpoint must not contain credentials')
  if (url.protocol !== 'http:' && url.protocol !== 'https:') throw new Error('Ollama endpoint must use HTTP or HTTPS')
  url.pathname = url.pathname.replace(/\/$/, '')
  url.search = ''
  url.hash = ''
  const exposure = endpointExposure(url.hostname)
  const probe = new URL(url)
  if (exposure === 'allInterfaces') probe.hostname = url.hostname.includes(':') ? '[::1]' : '127.0.0.1'
  return { displayUrl: url.toString().replace(/\/$/, ''), probeUrl: probe.toString().replace(/\/$/, ''), exposure }
}

function endpointExposure(hostname: string): AiModelRuntimeExposure {
  const host = hostname.replace(/^\[|]$/g, '').toLowerCase()
  if (host === 'localhost' || host === '127.0.0.1' || host === '::1') return 'loopback'
  if (host === '0.0.0.0' || host === '::') return 'allInterfaces'
  if (/^10\./.test(host) || /^192\.168\./.test(host) || /^172\.(1[6-9]|2\d|3[01])\./.test(host) || /^169\.254\./.test(host)) return 'localNetwork'
  return 'remote'
}

function normalizeModels(tagsPayload: unknown, runningPayload: unknown): AiLocalModelRuntimeModel[] {
  if (!isRecord(tagsPayload) || !Array.isArray(tagsPayload.models)) throw new Error('Ollama returned an invalid model inventory')
  const runningModels = isRecord(runningPayload) && Array.isArray(runningPayload.models) ? runningPayload.models.filter(isRecord) : []
  const runningByDigest = new Map(runningModels.map((model) => [readString(model, 'digest'), model]))
  const runningByName = new Map(runningModels.map((model) => [readString(model, 'name') || readString(model, 'model'), model]))
  return tagsPayload.models.filter(isRecord).slice(0, maximumModels).map((model) => {
    const name = readString(model, 'name') || readString(model, 'model')
    if (!name) throw new Error('Ollama returned a model without a name')
    const digest = readString(model, 'digest')
    const running = runningByDigest.get(digest) ?? runningByName.get(name)
    const details = isRecord(model.details) ? model.details : {}
    return {
      name,
      digest,
      sizeBytes: readNonNegativeNumber(model, 'size') ?? 0,
      modifiedAt: validIsoDate(readString(model, 'modified_at')),
      format: readString(details, 'format') || undefined,
      family: readString(details, 'family') || undefined,
      parameterSize: readString(details, 'parameter_size') || undefined,
      quantization: readString(details, 'quantization_level') || undefined,
      running: Boolean(running),
      loadedSizeBytes: running ? readNonNegativeNumber(running, 'size') : undefined,
      vramSizeBytes: running ? readNonNegativeNumber(running, 'size_vram') : undefined,
      contextLength: running ? readNonNegativeNumber(running, 'context_length') : undefined,
      expiresAt: running ? validIsoDate(readString(running, 'expires_at')) : undefined
    }
  }).sort((left, right) => Number(right.running) - Number(left.running) || left.name.localeCompare(right.name))
}

function normalizeLmStudioModels(payload: unknown, binaryDetected: boolean): AiLocalModelRuntimeModel[] {
  if (!isRecord(payload) || !Array.isArray(payload.models)) throw new Error('LM Studio returned an invalid model inventory')
  const candidates = payload.models.filter(isRecord).slice(0, maximumModels)
  const models = candidates.flatMap((model): AiLocalModelRuntimeModel[] => {
    const name = readString(model, 'key')
    if (!name) return []
    const loadedInstances = Array.isArray(model.loaded_instances) ? model.loaded_instances.filter(isRecord) : []
    const runtimeInstanceIds = loadedInstances.map((instance) => readString(instance, 'id')).filter(Boolean).slice(0, 100)
    const quantization = isRecord(model.quantization) ? readString(model.quantization, 'name') : ''
    const contexts = loadedInstances.map((instance) => isRecord(instance.config) ? readNonNegativeNumber(instance.config, 'context_length') : undefined).filter((value): value is number => value !== undefined)
    const capabilities = ['type', 'vision', 'toolUse', 'reasoning'].flatMap((capability) => {
      if (capability === 'type') return readString(model, 'type') || []
      const values = isRecord(model.capabilities) ? model.capabilities : {}
      if (capability === 'vision' && values.vision === true) return ['vision']
      if (capability === 'toolUse' && values.trained_for_tool_use === true) return ['toolUse']
      if (capability === 'reasoning' && isRecord(values.reasoning)) return ['reasoning']
      return []
    })
    return [{
      name,
      digest: stableModelDigest('lmStudio', name, readString(model, 'selected_variant')),
      sizeBytes: readNonNegativeNumber(model, 'size_bytes') ?? 0,
      format: readString(model, 'format') || undefined,
      family: readString(model, 'architecture') || undefined,
      parameterSize: readString(model, 'params_string') || undefined,
      quantization: quantization || undefined,
      running: loadedInstances.length > 0,
      loadedSizeBytes: loadedInstances.length > 0 ? readNonNegativeNumber(model, 'size_bytes') : undefined,
      contextLength: contexts.length > 0 ? Math.max(...contexts) : readNonNegativeNumber(model, 'max_context_length'),
      capabilities,
      runtimeInstanceIds
    }]
  })
  if (candidates.length > 0 && models.length === 0) throw new Error('LM Studio returned models without stable keys')
  if (models.length === 0 && !binaryDetected) throw new Error('The endpoint does not provide enough information to identify LM Studio')
  return models.sort((left, right) => Number(right.running) - Number(left.running) || left.name.localeCompare(right.name))
}

function normalizeOpenAiModels(payload: unknown, runtimeId: 'llamaCpp' | 'vllm' | 'localAi', loadedModelNames?: Set<string>): AiLocalModelRuntimeModel[] {
  if (!isRecord(payload) || !Array.isArray(payload.data)) throw new Error('The OpenAI-compatible endpoint returned an invalid model inventory')
  return payload.data.filter(isRecord).slice(0, maximumModels).map((model) => {
    const name = readString(model, 'id')
    if (!name) throw new Error('The OpenAI-compatible endpoint returned a model without an id')
    const meta = isRecord(model.meta) ? model.meta : {}
    const created = readNonNegativeNumber(model, 'created')
    const running = loadedModelNames ? loadedModelNames.has(name) : true
    const capabilities = Array.isArray(meta.capabilities)
      ? meta.capabilities.filter((item): item is string => typeof item === 'string').slice(0, 100)
      : []
    return {
      name,
      digest: readString(model, 'digest') || stableModelDigest(runtimeId, name),
      sizeBytes: readNonNegativeNumber(meta, 'size') ?? readNonNegativeNumber(model, 'size') ?? 0,
      modifiedAt: created === undefined ? undefined : new Date(created * 1_000).toISOString(),
      format: readString(meta, 'format') || undefined,
      family: readString(meta, 'architecture') || readString(model, 'owned_by') || undefined,
      parameterSize: formatParameterCount(readNonNegativeNumber(meta, 'n_params')),
      quantization: readString(meta, 'quantization') || undefined,
      running,
      loadedSizeBytes: running ? readNonNegativeNumber(meta, 'size') ?? readNonNegativeNumber(model, 'size') : undefined,
      contextLength: readNonNegativeNumber(meta, 'n_ctx_train') ?? readNonNegativeNumber(meta, 'context_length'),
      capabilities
    }
  }).sort((left, right) => Number(right.running) - Number(left.running) || left.name.localeCompare(right.name))
}

function hasOpenAiRuntimeSignature(payload: unknown, runtimeId: 'llamaCpp' | 'vllm'): boolean {
  if (!isRecord(payload) || !Array.isArray(payload.data) || payload.data.length === 0) return false
  return payload.data.filter(isRecord).some((model) => {
    const owner = readString(model, 'owned_by').toLowerCase()
    if (runtimeId === 'llamaCpp') return owner.includes('llama') || isRecord(model.meta)
    return owner.includes('vllm')
  })
}

function readLoadedModelNames(payload: unknown): Set<string> {
  if (!isRecord(payload) || !Array.isArray(payload.loaded_models)) return new Set()
  return new Set(payload.loaded_models.filter(isRecord).map((model) => readString(model, 'id')).filter(Boolean))
}

function stableModelDigest(runtimeId: AiModelRuntimeId, ...parts: string[]): string {
  return `sha256:${createHash('sha256').update([runtimeId, ...parts].join('\0')).digest('hex')}`
}

function formatParameterCount(value?: number): string | undefined {
  if (value === undefined || value <= 0) return undefined
  if (value >= 1_000_000_000) return `${Number((value / 1_000_000_000).toPrecision(3))}B`
  if (value >= 1_000_000) return `${Number((value / 1_000_000).toPrecision(3))}M`
  return value.toLocaleString('en-US')
}

function summarizeModels(models: AiLocalModelRuntimeModel[]): AiModelRuntimeSnapshot['stats'] {
  return {
    models: models.length,
    runningModels: models.filter((model) => model.running).length,
    totalModelBytes: models.reduce((sum, model) => sum + model.sizeBytes, 0),
    loadedBytes: models.reduce((sum, model) => sum + (model.loadedSizeBytes ?? 0), 0),
    vramBytes: models.reduce((sum, model) => sum + (model.vramSizeBytes ?? 0), 0)
  }
}

async function readCliVersion(binaryPath: string): Promise<string | undefined> {
  const result = await execFileAsync(binaryPath, ['--version'], { timeout: 1_500, maxBuffer: 64 * 1024, windowsHide: true })
  const match = `${result.stdout}\n${result.stderr}`.match(/(?:ollama\s+version\s+)?v?(\d+(?:\.\d+){1,3}(?:[-+][\w.-]+)?)/i)
  return match?.[1]
}

function defaultModelDirectory(homeDirectory: string, platform: NodeJS.Platform): string {
  if (platform === 'win32') return join(homeDirectory, '.ollama', 'models')
  return join(homeDirectory, '.ollama', 'models')
}

function diagnostic(code: AiModelRuntimeDiagnostic['code'], severity: AiModelRuntimeDiagnostic['severity'], message: string): AiModelRuntimeDiagnostic {
  return { code, severity, message }
}

function readString(value: unknown, key: string): string {
  return isRecord(value) && typeof value[key] === 'string' ? value[key].slice(0, maximumResponseBytes) : ''
}

function readNonNegativeNumber(value: unknown, key: string): number | undefined {
  return isRecord(value) && isNonNegativeNumber(value[key]) ? Number(value[key]) : undefined
}

function isNonNegativeNumber(value: unknown): boolean {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0
}

function validIsoDate(value: string): string | undefined {
  return value && Number.isFinite(Date.parse(value)) ? value : undefined
}

function truncateText(value: string, maximum: number): string | undefined {
  if (!value) return undefined
  return value.length <= maximum ? value : `${value.slice(0, maximum)}\n…`
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isMissing(error: unknown): boolean {
  return isRecord(error) && error.code === 'ENOENT'
}

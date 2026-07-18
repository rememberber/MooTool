import type {
  AiMemoryEmbeddingProgressEvent,
  AiMemoryEmbeddingRebuildInput,
  AiMemoryEmbeddingRebuildResult,
  AiMemoryEmbeddingStatus,
  AiMemorySemanticPreview,
  AiMemorySemanticPreviewInput
} from '../../../src/shared/contracts/aiMemoryEmbedding'
import type { AiModelRuntimeId, AiModelRuntimeInstallation } from '../../../src/shared/contracts/aiModelRuntime'
import type { AiMemoryRepository } from './memoryRepository'
import type { ModelRuntimeService } from './modelRuntimeService'
import { redactSensitiveContent } from './securityScanner'

type EmbeddingResponse = {
  ok: boolean
  status: number
  text(): Promise<string>
}

type EmbeddingFetcher = (url: string, init: {
  method: 'POST'
  headers: Record<string, string>
  body: string
  signal: AbortSignal
}) => Promise<EmbeddingResponse>

type MemoryEmbeddingServiceOptions = {
  runtimes: Pick<ModelRuntimeService, 'scan'>
  repository: () => AiMemoryRepository
  credentialProvider?: (runtimeId: AiModelRuntimeId) => string
  fetcher?: EmbeddingFetcher
  clock?: () => Date
  requestTimeoutMs?: number
}

const supportedRuntimes: AiModelRuntimeId[] = ['ollama', 'lmStudio']
const maximumResponseBytes = 16 * 1024 * 1024
const batchSize = 8

export class MemoryEmbeddingService {
  private readonly runtimes: MemoryEmbeddingServiceOptions['runtimes']
  private readonly repository: MemoryEmbeddingServiceOptions['repository']
  private readonly credentialProvider: NonNullable<MemoryEmbeddingServiceOptions['credentialProvider']>
  private readonly fetcher: EmbeddingFetcher
  private readonly clock: () => Date
  private readonly requestTimeoutMs: number
  private readonly active = new Map<string, AbortController>()

  constructor(options: MemoryEmbeddingServiceOptions) {
    this.runtimes = options.runtimes
    this.repository = options.repository
    this.credentialProvider = options.credentialProvider ?? (() => '')
    this.fetcher = options.fetcher ?? ((url, init) => fetch(url, init))
    this.clock = options.clock ?? (() => new Date())
    this.requestTimeoutMs = options.requestTimeoutMs ?? 120_000
  }

  status(): AiMemoryEmbeddingStatus {
    return this.repository().embeddingStatus()
  }

  async rebuild(input: AiMemoryEmbeddingRebuildInput, onProgress: (event: AiMemoryEmbeddingProgressEvent) => void = () => undefined): Promise<AiMemoryEmbeddingRebuildResult> {
    if (!input.confirmLocalProcessing) throw new Error('Local embedding requires explicit processing confirmation')
    if (this.active.has(input.requestId)) throw new Error('Memory embedding request is already running')
    const runtime = await this.resolveRuntime(input.runtimeId, input.model)
    const controller = new AbortController()
    this.active.set(input.requestId, controller)
    const startedAt = this.clock()
    const candidates = this.repository().embeddingCandidates()
    const skippedSensitive = this.repository().embeddingStatus().skippedSensitive
    const entries: Parameters<AiMemoryRepository['replaceEmbeddings']>[0] = []
    let dimensions: number | undefined
    const emit = (status: AiMemoryEmbeddingProgressEvent['status'], completed: number, message: string): void => {
      onProgress({ requestId: input.requestId, status, completed, total: candidates.length, skippedSensitive, message, timestamp: this.clock().toISOString() })
    }
    emit('preparing', 0, `Using ${runtime.name} on the local machine. Private and restricted memories are excluded.`)
    let status: AiMemoryEmbeddingRebuildResult['status'] = 'completed'
    let message = 'Local memory embedding index rebuilt'
    try {
      for (let offset = 0; offset < candidates.length; offset += batchSize) {
        if (controller.signal.aborted) throw abortError()
        const batch = candidates.slice(offset, offset + batchSize)
        const vectors = await this.embed(runtime, input.model, batch.map((memory) => memory.content), controller.signal)
        for (let index = 0; index < batch.length; index += 1) {
          const vector = vectors[index]
          dimensions ??= vector.length
          if (vector.length !== dimensions) throw new Error('Embedding dimensions changed during index rebuild')
          entries.push({
            memoryId: batch[index].id,
            runtimeId: runtime.id,
            model: input.model,
            ...(runtime.models.find((model) => model.name === input.model)?.digest ? { modelVersion: runtime.models.find((model) => model.name === input.model)!.digest } : {}),
            contentFingerprint: batch[index].fingerprint,
            vector,
            generatedAt: this.clock().toISOString()
          })
        }
        emit('embedding', entries.length, `Embedded ${entries.length} of ${candidates.length} eligible memories.`)
      }
      if (controller.signal.aborted) throw abortError()
      this.repository().replaceEmbeddings(entries)
      emit('completed', entries.length, message)
    } catch (error) {
      status = controller.signal.aborted || isAbortError(error) ? 'cancelled' : 'failed'
      message = status === 'cancelled' ? 'Memory embedding rebuild was cancelled; the previous index was preserved.' : sanitizeError(error, this.credentialProvider(runtime.id))
      emit(status, entries.length, message)
    } finally {
      this.active.delete(input.requestId)
    }
    return {
      requestId: input.requestId,
      status,
      indexed: status === 'completed' ? entries.length : 0,
      eligible: candidates.length,
      skippedSensitive,
      ...(dimensions === undefined ? {} : { dimensions }),
      startedAt: startedAt.toISOString(),
      finishedAt: this.clock().toISOString(),
      message
    }
  }

  async semanticPreview(input: AiMemorySemanticPreviewInput): Promise<AiMemorySemanticPreview> {
    if (!input.confirmLocalProcessing) throw new Error('Semantic preview requires explicit local processing confirmation')
    if (this.active.has(input.requestId)) throw new Error('Memory embedding request is already running')
    const runtime = await this.resolveRuntime(input.runtimeId, input.model)
    const status = this.repository().embeddingStatus()
    if (status.indexed === 0 || status.runtimeId !== input.runtimeId || status.model !== input.model) throw new Error('Rebuild the local embedding index with the selected runtime and model first')
    const controller = new AbortController()
    this.active.set(input.requestId, controller)
    try {
      const [vector] = await this.embed(runtime, input.model, [input.query.trim()], controller.signal)
      return this.repository().semanticPreview(input, vector)
    } finally {
      this.active.delete(input.requestId)
    }
  }

  cancel(requestId: string): boolean {
    const controller = this.active.get(requestId)
    if (!controller) return false
    controller.abort()
    return true
  }

  cancelAll(): void {
    for (const controller of this.active.values()) controller.abort()
  }

  private async resolveRuntime(runtimeId: AiModelRuntimeId, modelName: string): Promise<AiModelRuntimeInstallation> {
    if (!supportedRuntimes.includes(runtimeId)) throw new Error('MooTool currently verifies local embeddings only for Ollama and LM Studio')
    const snapshot = await this.runtimes.scan()
    const runtime = snapshot.runtimes.find((candidate) => candidate.id === runtimeId)
    if (!runtime || !['healthy', 'degraded'].includes(runtime.health)) throw new Error('The selected local model runtime is not ready')
    if (runtime.exposure !== 'loopback' && runtime.exposure !== 'allInterfaces') throw new Error('Memory embedding blocks LAN and remote model endpoints')
    if (!runtime.models.some((model) => model.name === modelName)) throw new Error('The selected embedding model is no longer available')
    return runtime
  }

  private async embed(runtime: AiModelRuntimeInstallation, model: string, inputs: string[], parentSignal: AbortSignal): Promise<number[][]> {
    if (inputs.length === 0) return []
    const controller = new AbortController()
    const abort = () => controller.abort()
    if (parentSignal.aborted) controller.abort()
    parentSignal.addEventListener('abort', abort, { once: true })
    const timer = setTimeout(() => controller.abort(), this.requestTimeoutMs)
    const endpoint = localEndpoint(runtime.endpoint)
    const credential = this.credentialProvider(runtime.id)
    try {
      const response = await this.fetcher(`${endpoint}${runtime.id === 'ollama' ? '/api/embed' : '/v1/embeddings'}`, {
        method: 'POST',
        headers: { 'content-type': 'application/json', ...(credential ? { authorization: `Bearer ${credential}` } : {}) },
        body: JSON.stringify({ model, input: inputs }),
        signal: controller.signal
      })
      if (!response.ok) throw new Error(`Local embedding runtime returned HTTP ${response.status}`)
      const source = await response.text()
      if (Buffer.byteLength(source, 'utf8') > maximumResponseBytes) throw new Error('Embedding response exceeds the 16 MB safety limit')
      const payload = JSON.parse(source) as unknown
      const vectors = runtime.id === 'ollama' ? readOllamaVectors(payload) : readOpenAiVectors(payload)
      if (vectors.length !== inputs.length) throw new Error('Embedding runtime returned a different number of vectors than requested')
      const dimensions = vectors[0]?.length ?? 0
      if (dimensions < 1 || dimensions > 16_384 || vectors.some((vector) => vector.length !== dimensions || vector.some((value) => !Number.isFinite(value)))) {
        throw new Error('Embedding runtime returned invalid or inconsistent vectors')
      }
      return vectors
    } catch (error) {
      if (controller.signal.aborted) throw abortError()
      if (error instanceof SyntaxError) throw new Error('Embedding runtime returned malformed JSON')
      throw error
    } finally {
      clearTimeout(timer)
      parentSignal.removeEventListener('abort', abort)
    }
  }
}

function readOllamaVectors(payload: unknown): number[][] {
  if (!isRecord(payload) || !Array.isArray(payload.embeddings)) throw new Error('Ollama returned an invalid embedding response')
  return payload.embeddings.map(readVector)
}

function readOpenAiVectors(payload: unknown): number[][] {
  if (!isRecord(payload) || !Array.isArray(payload.data)) throw new Error('LM Studio returned an invalid embedding response')
  return payload.data.filter(isRecord).sort((left, right) => Number(left.index) - Number(right.index)).map((item) => readVector(item.embedding))
}

function readVector(value: unknown): number[] {
  if (!Array.isArray(value)) throw new Error('Embedding runtime returned a non-vector item')
  return value.map((item) => typeof item === 'number' ? item : Number.NaN)
}

function localEndpoint(value: string): string {
  const url = new URL(value)
  if (url.hostname === '0.0.0.0') url.hostname = '127.0.0.1'
  if (url.hostname === '[::]' || url.hostname === '::') url.hostname = '[::1]'
  url.username = ''
  url.password = ''
  url.search = ''
  url.hash = ''
  return url.toString().replace(/\/$/, '')
}

function abortError(): Error {
  const error = new Error('Memory embedding request was cancelled')
  error.name = 'AbortError'
  return error
}

function isAbortError(error: unknown): boolean {
  return error instanceof Error && error.name === 'AbortError'
}

function sanitizeError(error: unknown, credential: string): string {
  const source = error instanceof Error ? error.message : String(error)
  return redactSensitiveContent(credential ? source.replaceAll(credential, '[REDACTED]') : source).slice(0, 2_000)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

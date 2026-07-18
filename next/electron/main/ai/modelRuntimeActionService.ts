import { randomUUID } from 'node:crypto'
import type {
  AiModelRuntimeActionExecuteInput,
  AiModelRuntimeActionPlan,
  AiModelRuntimeActionPlanInput,
  AiModelRuntimeActionProgressEvent,
  AiModelRuntimeActionResult
} from '../../../src/shared/contracts/aiModelRuntimeActions'
import { supportedAiModelRuntimeActions } from '../../../src/shared/contracts/aiModelRuntimeActions'
import type { AiLocalModelRuntimeModel, AiModelRuntimeId, AiModelRuntimeInstallation } from '../../../src/shared/contracts/aiModelRuntime'
import { redactSensitiveContent } from './securityScanner'
import type { ModelRuntimeService } from './modelRuntimeService'
import type { AiAgentProfileRepository } from './agentProfileRepository'

type ActionResponse = {
  ok: boolean
  status: number
  text(): Promise<string>
  body?: ReadableStream<Uint8Array> | null
}

type ActionFetcher = (url: string, init: {
  method: string
  headers: Record<string, string>
  body?: string
  signal: AbortSignal
}) => Promise<ActionResponse>

type ModelRuntimeActionServiceOptions = {
  runtimes: Pick<ModelRuntimeService, 'scan'>
  profiles: Pick<AiAgentProfileRepository, 'list'>
  credentialProvider?: (runtimeId: AiModelRuntimeId) => string
  fetcher?: ActionFetcher
  clock?: () => Date
  actionTimeoutMs?: number
  planTtlMs?: number
  pollIntervalMs?: number
}

const maximumResponseBytes = 8 * 1024 * 1024

export class ModelRuntimeActionService {
  private readonly runtimes: ModelRuntimeActionServiceOptions['runtimes']
  private readonly profiles: ModelRuntimeActionServiceOptions['profiles']
  private readonly credentialProvider: NonNullable<ModelRuntimeActionServiceOptions['credentialProvider']>
  private readonly fetcher: ActionFetcher
  private readonly clock: () => Date
  private readonly actionTimeoutMs: number
  private readonly planTtlMs: number
  private readonly pollIntervalMs: number
  private readonly plans = new Map<string, AiModelRuntimeActionPlan>()
  private readonly active = new Map<string, AbortController>()

  constructor(options: ModelRuntimeActionServiceOptions) {
    this.runtimes = options.runtimes
    this.profiles = options.profiles
    this.credentialProvider = options.credentialProvider ?? (() => '')
    this.fetcher = options.fetcher ?? ((url, init) => fetch(url, init))
    this.clock = options.clock ?? (() => new Date())
    this.actionTimeoutMs = options.actionTimeoutMs ?? 2 * 60 * 60 * 1_000
    this.planTtlMs = options.planTtlMs ?? 5 * 60 * 1_000
    this.pollIntervalMs = options.pollIntervalMs ?? 750
  }

  async plan(input: AiModelRuntimeActionPlanInput): Promise<AiModelRuntimeActionPlan> {
    this.removeExpiredPlans()
    if (!supportedAiModelRuntimeActions(input.runtimeId).includes(input.action)) {
      throw new Error('This runtime does not expose a verified lifecycle API in MooTool')
    }
    const snapshot = await this.runtimes.scan()
    const runtime = snapshot.runtimes.find((candidate) => candidate.id === input.runtimeId)
    if (!runtime || !['healthy', 'degraded'].includes(runtime.health)) throw new Error('The selected model runtime is not ready')
    assertLifecycleEndpoint(runtime)
    const modelName = input.modelName.trim()
    const model = runtime.models.find((candidate) => candidate.name === modelName)
    if (input.action !== 'pull' && !model) throw new Error('The selected model is no longer available')
    if (input.action === 'unload' && !model?.running) throw new Error('The selected model is not loaded')
    const runtimeInstanceId = input.runtimeId === 'lmStudio' ? model?.runtimeInstanceIds?.[0] : undefined
    if (input.action === 'unload' && input.runtimeId === 'lmStudio' && !runtimeInstanceId) throw new Error('LM Studio did not report a model instance id to unload')
    const affectedAgentProfiles = input.action === 'delete' && model
      ? this.profiles.list()
        .filter((profile) => profile.modelRuntimeId === runtime.id && (profile.localModelDigest === model.digest || profile.model === model.name))
        .map((profile) => ({ id: profile.id, name: profile.name }))
      : []
    const warnings = buildWarnings(runtime, input, model, affectedAgentProfiles.length)
    const createdAt = this.clock()
    const plan: AiModelRuntimeActionPlan = {
      planId: randomUUID(),
      createdAt: createdAt.toISOString(),
      expiresAt: new Date(createdAt.getTime() + this.planTtlMs).toISOString(),
      runtimeId: runtime.id,
      runtimeName: runtime.name,
      action: input.action,
      modelName,
      endpoint: redactEndpoint(runtime.endpoint),
      exposure: runtime.exposure,
      destructive: input.action === 'delete',
      modelExists: Boolean(model),
      modelRunning: Boolean(model?.running),
      ...(model?.digest ? { modelDigest: model.digest } : {}),
      ...(model ? { modelSizeBytes: model.sizeBytes } : {}),
      ...(runtime.modelDirectoryAvailableBytes === undefined ? {} : { modelDirectoryAvailableBytes: runtime.modelDirectoryAvailableBytes }),
      ...(runtimeInstanceId ? { runtimeInstanceId } : {}),
      affectedAgentProfiles,
      authenticationConfigured: Boolean(this.credentialProvider(runtime.id)),
      requiresRemoteConfirmation: runtime.exposure !== 'loopback',
      warnings
    }
    if (this.plans.size >= 50) this.plans.delete(this.plans.keys().next().value as string)
    this.plans.set(plan.planId, plan)
    return plan
  }

  async execute(input: AiModelRuntimeActionExecuteInput, onProgress: (event: AiModelRuntimeActionProgressEvent) => void = () => undefined): Promise<AiModelRuntimeActionResult> {
    if (!input.confirmAction) throw new Error('Model lifecycle actions require explicit confirmation')
    if (this.active.has(input.requestId)) throw new Error('Model runtime action request is already active')
    this.removeExpiredPlans()
    const plan = this.plans.get(input.planId)
    if (!plan) throw new Error('Model runtime action plan expired or no longer exists')
    if (plan.destructive && !input.confirmDestructive) throw new Error('Deleting a model requires destructive-action confirmation')
    if (plan.requiresRemoteConfirmation && !input.confirmRemoteEndpoint) throw new Error('An exposed model endpoint requires additional confirmation')
    this.plans.delete(input.planId)
    await this.revalidate(plan)

    const controller = new AbortController()
    this.active.set(input.requestId, controller)
    const startedAt = this.clock()
    const timer = setTimeout(() => controller.abort(), this.actionTimeoutMs)
    const emit = (event: Omit<AiModelRuntimeActionProgressEvent, 'requestId' | 'planId' | 'runtimeId' | 'action' | 'timestamp'>): void => {
      onProgress({ requestId: input.requestId, planId: plan.planId, runtimeId: plan.runtimeId, action: plan.action, timestamp: this.clock().toISOString(), ...event })
    }
    emit({ status: 'preparing', message: `Revalidated ${plan.runtimeName} and ${plan.modelName}.` })
    let status: AiModelRuntimeActionResult['status'] = 'completed'
    let message = `${plan.action} completed`
    try {
      await this.perform(plan, controller.signal, emit)
      emit({ status: 'verifying', message: 'Verifying the runtime inventory after the action.' })
      await this.verify(plan, controller.signal)
      emit({ status: 'completed', message })
    } catch (error) {
      const cancelled = controller.signal.aborted
      status = cancelled ? 'cancelled' : 'failed'
      message = cancelled ? 'Model runtime action was cancelled' : sanitizeError(error, this.credentialProvider(plan.runtimeId))
      emit({ status: cancelled ? 'cancelled' : 'failed', message })
    } finally {
      clearTimeout(timer)
      this.active.delete(input.requestId)
    }
    const finishedAt = this.clock()
    return {
      requestId: input.requestId,
      planId: plan.planId,
      runtimeId: plan.runtimeId,
      action: plan.action,
      modelName: plan.modelName,
      status,
      startedAt: startedAt.toISOString(),
      finishedAt: finishedAt.toISOString(),
      durationMs: Math.max(0, finishedAt.getTime() - startedAt.getTime()),
      message
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

  private async revalidate(plan: AiModelRuntimeActionPlan): Promise<void> {
    const snapshot = await this.runtimes.scan()
    const runtime = snapshot.runtimes.find((candidate) => candidate.id === plan.runtimeId)
    if (!runtime || !['healthy', 'degraded'].includes(runtime.health)) throw new Error('The model runtime changed or stopped after planning')
    if (redactEndpoint(runtime.endpoint) !== plan.endpoint) throw new Error('The model runtime endpoint changed after planning')
    const model = runtime.models.find((candidate) => candidate.name === plan.modelName)
    if (plan.action !== 'pull' && !model) throw new Error('The selected model disappeared after planning')
    if (plan.modelDigest && model?.digest !== plan.modelDigest) throw new Error('The selected model Digest changed after planning')
    if (plan.action === 'unload' && !model?.running) throw new Error('The selected model is no longer loaded')
  }

  private async perform(plan: AiModelRuntimeActionPlan, signal: AbortSignal, emit: (event: Omit<AiModelRuntimeActionProgressEvent, 'requestId' | 'planId' | 'runtimeId' | 'action' | 'timestamp'>) => void): Promise<void> {
    if (plan.runtimeId === 'ollama') return this.performOllama(plan, signal, emit)
    if (plan.runtimeId === 'lmStudio') return this.performLmStudio(plan, signal, emit)
    throw new Error('The selected runtime action is not implemented')
  }

  private async performOllama(plan: AiModelRuntimeActionPlan, signal: AbortSignal, emit: (event: Omit<AiModelRuntimeActionProgressEvent, 'requestId' | 'planId' | 'runtimeId' | 'action' | 'timestamp'>) => void): Promise<void> {
    const endpoint = actionEndpoint(plan.endpoint)
    if (plan.action === 'pull') {
      emit({ status: 'downloading', message: `Pulling ${plan.modelName}.` })
      const response = await this.request(`${endpoint}/api/pull`, 'POST', { model: plan.modelName, stream: true }, plan.runtimeId, signal)
      await consumeOllamaProgress(response, emit, signal)
      return
    }
    if (plan.action === 'delete') {
      emit({ status: 'deleting', message: `Deleting ${plan.modelName}.` })
      await this.consumeJson(await this.request(`${endpoint}/api/delete`, 'DELETE', { model: plan.modelName }, plan.runtimeId, signal))
      return
    }
    const loading = plan.action === 'load'
    emit({ status: loading ? 'loading' : 'unloading', message: `${loading ? 'Loading' : 'Unloading'} ${plan.modelName}.` })
    await this.consumeJson(await this.request(`${endpoint}/api/generate`, 'POST', {
      model: plan.modelName,
      stream: false,
      keep_alive: loading ? -1 : 0
    }, plan.runtimeId, signal))
  }

  private async performLmStudio(plan: AiModelRuntimeActionPlan, signal: AbortSignal, emit: (event: Omit<AiModelRuntimeActionProgressEvent, 'requestId' | 'planId' | 'runtimeId' | 'action' | 'timestamp'>) => void): Promise<void> {
    const endpoint = actionEndpoint(plan.endpoint)
    if (plan.action === 'pull') {
      emit({ status: 'downloading', message: `Starting download for ${plan.modelName}.` })
      const initial = await this.consumeJson(await this.request(`${endpoint}/api/v1/models/download`, 'POST', { model: plan.modelName }, plan.runtimeId, signal))
      await this.pollLmStudioDownload(endpoint, initial, plan, signal, emit)
      return
    }
    if (plan.action === 'load') {
      emit({ status: 'loading', message: `Loading ${plan.modelName}.` })
      await this.consumeJson(await this.request(`${endpoint}/api/v1/models/load`, 'POST', { model: plan.modelName }, plan.runtimeId, signal))
      return
    }
    if (plan.action === 'unload') {
      emit({ status: 'unloading', message: `Unloading ${plan.modelName}.` })
      await this.consumeJson(await this.request(`${endpoint}/api/v1/models/unload`, 'POST', { instance_id: plan.runtimeInstanceId }, plan.runtimeId, signal))
      return
    }
    throw new Error('LM Studio model deletion is not exposed because no verified delete API is available')
  }

  private async pollLmStudioDownload(endpoint: string, initial: unknown, plan: AiModelRuntimeActionPlan, signal: AbortSignal, emit: (event: Omit<AiModelRuntimeActionProgressEvent, 'requestId' | 'planId' | 'runtimeId' | 'action' | 'timestamp'>) => void): Promise<void> {
    let status = readString(initial, 'status')
    const jobId = readString(initial, 'job_id')
    emitDownloadProgress(initial, emit)
    if (status === 'completed' || status === 'already_downloaded') return
    if (!jobId) throw new Error('LM Studio did not return a download job id')
    while (!signal.aborted) {
      await abortableDelay(this.pollIntervalMs, signal)
      const payload = await this.consumeJson(await this.request(`${endpoint}/api/v1/models/download/status/${encodeURIComponent(jobId)}`, 'GET', undefined, plan.runtimeId, signal))
      status = readString(payload, 'status')
      emitDownloadProgress(payload, emit)
      if (status === 'completed' || status === 'already_downloaded') return
      if (status === 'failed') throw new Error(readString(payload, 'error') || 'LM Studio model download failed')
    }
    throw new DOMException('Aborted', 'AbortError')
  }

  private async request(url: string, method: string, body: Record<string, unknown> | undefined, runtimeId: AiModelRuntimeId, signal: AbortSignal): Promise<ActionResponse> {
    const token = this.credentialProvider(runtimeId)
    const response = await this.fetcher(url, {
      method,
      headers: { ...(body ? { 'content-type': 'application/json' } : {}), ...(token ? { authorization: `Bearer ${token}` } : {}) },
      ...(body ? { body: JSON.stringify(body) } : {}),
      signal
    })
    if (!response.ok) {
      const source = await boundedText(response)
      throw new Error(`Model runtime returned HTTP ${response.status}${source ? `: ${source.slice(0, 500)}` : ''}`)
    }
    return response
  }

  private async consumeJson(response: ActionResponse): Promise<unknown> {
    const source = await boundedText(response)
    if (!source.trim()) return {}
    try { return JSON.parse(source) as unknown } catch { throw new Error('Model runtime returned malformed JSON') }
  }

  private async verify(plan: AiModelRuntimeActionPlan, signal: AbortSignal): Promise<void> {
    for (let attempt = 0; attempt < 4; attempt += 1) {
      if (signal.aborted) throw new DOMException('Aborted', 'AbortError')
      const snapshot = await this.runtimes.scan()
      const model = snapshot.runtimes.find((runtime) => runtime.id === plan.runtimeId)?.models.find((candidate) => candidate.name === plan.modelName)
      if (plan.action === 'pull' && model) return
      if (plan.action === 'delete' && !model) return
      if (plan.action === 'load' && model?.running) return
      if (plan.action === 'unload' && model && !model.running) return
      if (attempt < 3) await abortableDelay(250, signal)
    }
    throw new Error('The runtime API completed, but the expected inventory state was not observed')
  }

  private removeExpiredPlans(): void {
    const now = this.clock().getTime()
    for (const [id, plan] of this.plans) if (Date.parse(plan.expiresAt) <= now) this.plans.delete(id)
  }
}

function buildWarnings(runtime: AiModelRuntimeInstallation, input: AiModelRuntimeActionPlanInput, model: AiLocalModelRuntimeModel | undefined, affectedProfiles: number): string[] {
  const warnings: string[] = []
  if (input.action === 'pull' && model) warnings.push('The model already exists; this action may update its local layers or metadata.')
  if (input.action === 'pull') warnings.push('Download size and license are controlled by the runtime/model source and may be unknown before transfer starts.')
  if (input.action === 'load') warnings.push('Loading a model consumes RAM/VRAM and can make the Intel Mac less responsive until it is unloaded.')
  if (input.action === 'delete') warnings.push(`Deletion removes ${model?.digest ?? input.modelName} from the runtime and is not reversible in MooTool.`)
  if (affectedProfiles > 0) warnings.push(`${affectedProfiles} Agent Profile(s) currently reference this model and may stop working.`)
  if (runtime.exposure !== 'loopback') warnings.push('The runtime listens beyond loopback. Review exposure and authentication before executing this action.')
  return warnings
}

function assertLifecycleEndpoint(runtime: AiModelRuntimeInstallation): void {
  const url = new URL(runtime.endpoint)
  if (url.username || url.password) throw new Error('Model lifecycle endpoints must not contain credentials')
  if (runtime.exposure === 'remote' || runtime.exposure === 'localNetwork') {
    throw new Error('Model lifecycle actions are limited to local runtime endpoints in this release')
  }
}

function actionEndpoint(value: string): string {
  const url = new URL(value)
  if (url.hostname === '0.0.0.0') url.hostname = '127.0.0.1'
  if (url.hostname === '[::]' || url.hostname === '::') url.hostname = '[::1]'
  url.username = ''
  url.password = ''
  url.search = ''
  url.hash = ''
  return url.toString().replace(/\/$/, '')
}

function redactEndpoint(value: string): string {
  const url = new URL(value)
  url.username = ''
  url.password = ''
  url.search = ''
  url.hash = ''
  return url.toString().replace(/\/$/, '')
}

async function boundedText(response: ActionResponse): Promise<string> {
  const source = await response.text()
  if (Buffer.byteLength(source, 'utf8') > maximumResponseBytes) throw new Error('Model runtime response exceeds 8 MB')
  return source
}

async function consumeOllamaProgress(response: ActionResponse, emit: (event: Omit<AiModelRuntimeActionProgressEvent, 'requestId' | 'planId' | 'runtimeId' | 'action' | 'timestamp'>) => void, signal: AbortSignal): Promise<void> {
  if (!response.body) {
    for (const line of (await boundedText(response)).split(/\r?\n/).filter(Boolean)) emitOllamaLine(line, emit)
    return
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let pending = ''
  let totalBytes = 0
  while (!signal.aborted) {
    const chunk = await reader.read()
    if (chunk.done) break
    totalBytes += chunk.value.byteLength
    if (totalBytes > maximumResponseBytes) throw new Error('Model runtime progress exceeds 8 MB')
    pending += decoder.decode(chunk.value, { stream: true })
    const lines = pending.split(/\r?\n/)
    pending = lines.pop() ?? ''
    for (const line of lines.filter(Boolean)) emitOllamaLine(line, emit)
  }
  pending += decoder.decode()
  if (pending.trim()) emitOllamaLine(pending, emit)
  if (signal.aborted) throw new DOMException('Aborted', 'AbortError')
}

function emitOllamaLine(line: string, emit: (event: Omit<AiModelRuntimeActionProgressEvent, 'requestId' | 'planId' | 'runtimeId' | 'action' | 'timestamp'>) => void): void {
  let payload: unknown
  try { payload = JSON.parse(line) as unknown } catch { throw new Error('Ollama returned malformed pull progress') }
  const totalBytes = readNumber(payload, 'total')
  const completedBytes = readNumber(payload, 'completed')
  const percent = totalBytes && completedBytes !== undefined ? Math.min(100, Math.max(0, completedBytes / totalBytes * 100)) : undefined
  emit({
    status: 'downloading',
    message: readString(payload, 'status') || 'Downloading model layers',
    ...(completedBytes === undefined ? {} : { completedBytes }),
    ...(totalBytes === undefined ? {} : { totalBytes }),
    ...(percent === undefined ? {} : { percent })
  })
}

function emitDownloadProgress(payload: unknown, emit: (event: Omit<AiModelRuntimeActionProgressEvent, 'requestId' | 'planId' | 'runtimeId' | 'action' | 'timestamp'>) => void): void {
  const totalBytes = readNumber(payload, 'total_size_bytes')
  const completedBytes = readNumber(payload, 'downloaded_bytes')
  const percent = totalBytes && completedBytes !== undefined ? Math.min(100, Math.max(0, completedBytes / totalBytes * 100)) : undefined
  emit({
    status: 'downloading',
    message: readString(payload, 'status') || 'Downloading model',
    ...(completedBytes === undefined ? {} : { completedBytes }),
    ...(totalBytes === undefined ? {} : { totalBytes }),
    ...(percent === undefined ? {} : { percent })
  })
}

function sanitizeError(error: unknown, credential: string): string {
  let message = redactSensitiveContent(error instanceof Error ? error.message : String(error))
  if (credential) message = message.replaceAll(credential, '[REDACTED]')
  return message.slice(0, 2_000)
}

function readString(value: unknown, key: string): string {
  return isRecord(value) && typeof value[key] === 'string' ? value[key].slice(0, 1_000) : ''
}

function readNumber(value: unknown, key: string): number | undefined {
  return isRecord(value) && typeof value[key] === 'number' && Number.isFinite(value[key]) && Number(value[key]) >= 0 ? Number(value[key]) : undefined
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function abortableDelay(milliseconds: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal.aborted) { reject(new DOMException('Aborted', 'AbortError')); return }
    const abort = () => { clearTimeout(timer); reject(new DOMException('Aborted', 'AbortError')) }
    const timer = setTimeout(() => { signal.removeEventListener('abort', abort); resolve() }, milliseconds)
    signal.addEventListener('abort', abort, { once: true })
  })
}

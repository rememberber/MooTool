import type {
  AiPromptLabCaseResult,
  AiPromptLabRunInput,
  AiPromptLabRunResult
} from '../../../src/shared/contracts/aiPromptLab'
import type { AiModelRuntimeInstallation } from '../../../src/shared/contracts/aiModelRuntime'
import { ModelRuntimeService } from './modelRuntimeService'

type PromptLabResponse = {
  ok: boolean
  status: number
  text(): Promise<string>
}

type PromptLabFetcher = (url: string, init: { method: string; headers: Record<string, string>; body: string; signal: AbortSignal }) => Promise<PromptLabResponse>

type PromptLabServiceOptions = {
  runtimes: ModelRuntimeService
  fetcher?: PromptLabFetcher
  requestTimeoutMs?: number
  credentialProvider?: (runtimeId: AiModelRuntimeInstallation['id']) => string
}

const maximumResponseBytes = 128 * 1024

export class PromptLabService {
  private readonly runtimes: ModelRuntimeService
  private readonly fetcher: PromptLabFetcher
  private readonly requestTimeoutMs: number
  private readonly controllers = new Map<string, AbortController>()
  private readonly credentialProvider: NonNullable<PromptLabServiceOptions['credentialProvider']>

  constructor(options: PromptLabServiceOptions) {
    this.runtimes = options.runtimes
    this.fetcher = options.fetcher ?? ((url, init) => fetch(url, init))
    this.requestTimeoutMs = options.requestTimeoutMs ?? 60_000
    this.credentialProvider = options.credentialProvider ?? (() => '')
  }

  async run(input: AiPromptLabRunInput): Promise<AiPromptLabRunResult> {
    if (this.controllers.has(input.requestId)) throw new Error('Prompt Lab request is already running')
    const snapshot = await this.runtimes.scan()
    const runtime = snapshot.runtimes.find((candidate) => candidate.id === input.runtimeId)
    if (!runtime?.detected || !['healthy', 'degraded'].includes(runtime.health)) throw new Error('The selected model runtime is not ready')
    if (!runtime.models.some((model) => model.name === input.model)) throw new Error('The selected model is no longer available')
    assertEndpointPermission(runtime, input.confirmNetworkEndpoint)

    const controller = new AbortController()
    this.controllers.set(input.requestId, controller)
    const startedAt = new Date()
    const results: AiPromptLabCaseResult[] = []
    let cancelled = false
    try {
      for (const testCase of input.testCases) {
        if (controller.signal.aborted) { cancelled = true; break }
        try {
          results.push(await this.runCase(runtime, input, testCase, controller.signal))
        } catch (error) {
          if (controller.signal.aborted) { cancelled = true; break }
          results.push({
            caseId: testCase.id,
            name: testCase.name,
            renderedPrompt: renderPrompt(input.promptTemplate, testCase.input),
            output: '',
            expectedContains: testCase.expectedContains,
            durationMs: 0,
            error: error instanceof Error ? error.message : String(error)
          })
        }
      }
    } finally {
      this.controllers.delete(input.requestId)
    }
    const completedAt = new Date()
    const scored = results.filter((result) => result.passed !== undefined)
    const passed = scored.filter((result) => result.passed).length
    return {
      requestId: input.requestId,
      runtimeId: runtime.id,
      runtimeName: runtime.name,
      model: input.model,
      endpoint: redactEndpoint(runtime.endpoint),
      startedAt: startedAt.toISOString(),
      completedAt: completedAt.toISOString(),
      cancelled,
      results,
      summary: {
        cases: input.testCases.length,
        completed: results.length,
        passed,
        scored: scored.length,
        passRate: scored.length > 0 ? passed / scored.length : undefined,
        durationMs: Math.max(0, completedAt.getTime() - startedAt.getTime()),
        promptTokens: results.reduce((sum, result) => sum + (result.promptTokens ?? 0), 0),
        completionTokens: results.reduce((sum, result) => sum + (result.completionTokens ?? 0), 0),
        totalTokens: results.reduce((sum, result) => sum + (result.totalTokens ?? 0), 0)
      }
    }
  }

  cancel(requestId: string): boolean {
    const controller = this.controllers.get(requestId)
    if (!controller) return false
    controller.abort()
    return true
  }

  private async runCase(runtime: AiModelRuntimeInstallation, input: AiPromptLabRunInput, testCase: AiPromptLabRunInput['testCases'][number], parentSignal: AbortSignal): Promise<AiPromptLabCaseResult> {
    const renderedPrompt = renderPrompt(input.promptTemplate, testCase.input)
    const controller = new AbortController()
    const abort = () => controller.abort()
    parentSignal.addEventListener('abort', abort, { once: true })
    const timer = setTimeout(() => controller.abort(), this.requestTimeoutMs)
    const started = performance.now()
    try {
      const endpoint = probeEndpoint(runtime.endpoint)
      const credential = this.credentialProvider(runtime.id)
      const response = await this.fetcher(`${endpoint}/v1/chat/completions`, {
        method: 'POST',
        headers: { 'content-type': 'application/json', ...(credential ? { authorization: `Bearer ${credential}` } : {}) },
        body: JSON.stringify({
          model: input.model,
          messages: [
            ...(input.systemPrompt ? [{ role: 'system', content: input.systemPrompt }] : []),
            { role: 'user', content: renderedPrompt }
          ],
          temperature: input.temperature,
          max_tokens: input.maxTokens,
          stream: false
        }),
        signal: controller.signal
      })
      if (!response.ok) throw new Error(`Model runtime returned HTTP ${response.status}`)
      const source = await response.text()
      if (Buffer.byteLength(source, 'utf8') > maximumResponseBytes) throw new Error('Model response exceeds the 128 KB Prompt Lab limit')
      const payload = JSON.parse(source) as unknown
      const output = readOutput(payload)
      const usage = readUsage(payload)
      const expected = testCase.expectedContains.trim()
      return {
        caseId: testCase.id,
        name: testCase.name,
        renderedPrompt,
        output,
        expectedContains: testCase.expectedContains,
        passed: expected ? output.toLocaleLowerCase().includes(expected.toLocaleLowerCase()) : undefined,
        durationMs: Math.max(0, Math.round(performance.now() - started)),
        ...usage
      }
    } catch (error) {
      if (controller.signal.aborted) throw new Error(parentSignal.aborted ? 'Prompt Lab run was cancelled' : 'Model request timed out')
      if (error instanceof SyntaxError) throw new Error('Model runtime returned malformed JSON')
      throw error
    } finally {
      clearTimeout(timer)
      parentSignal.removeEventListener('abort', abort)
    }
  }
}

function assertEndpointPermission(runtime: AiModelRuntimeInstallation, confirmed: boolean): void {
  if (runtime.exposure === 'remote' && runtime.endpoint.startsWith('http://')) throw new Error('Prompt Lab blocks unencrypted remote model endpoints')
  if ((runtime.exposure === 'remote' || runtime.exposure === 'localNetwork') && !confirmed) throw new Error('Network model endpoints require explicit confirmation')
}

function probeEndpoint(value: string): string {
  const url = new URL(value)
  if (url.hostname === '0.0.0.0') url.hostname = '127.0.0.1'
  if (url.hostname === '[::]' || url.hostname === '::') url.hostname = '[::1]'
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

function renderPrompt(template: string, input: string): string {
  return template.replaceAll('{{input}}', input)
}

function readOutput(payload: unknown): string {
  if (!isRecord(payload) || !Array.isArray(payload.choices) || !isRecord(payload.choices[0])) throw new Error('Model runtime returned an invalid completion response')
  const message = payload.choices[0].message
  if (!isRecord(message) || typeof message.content !== 'string') throw new Error('Model runtime returned a completion without text content')
  return message.content.slice(0, maximumResponseBytes)
}

function readUsage(payload: unknown): Pick<AiPromptLabCaseResult, 'promptTokens' | 'completionTokens' | 'totalTokens'> {
  if (!isRecord(payload) || !isRecord(payload.usage)) return {}
  return {
    promptTokens: readNonNegativeInteger(payload.usage.prompt_tokens),
    completionTokens: readNonNegativeInteger(payload.usage.completion_tokens),
    totalTokens: readNonNegativeInteger(payload.usage.total_tokens)
  }
}

function readNonNegativeInteger(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0 ? value : undefined
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

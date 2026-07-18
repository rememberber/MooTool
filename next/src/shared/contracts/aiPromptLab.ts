import { aiModelRuntimeIds, type AiModelRuntimeId } from './aiModelRuntime'

export type AiPromptLabCase = {
  id: string
  name: string
  input: string
  expectedContains: string
}

export type AiPromptLabSuite = {
  id: string
  name: string
  systemPrompt: string
  promptTemplate: string
  testCases: AiPromptLabCase[]
  createdAt: string
  updatedAt: string
}

export type AiPromptLabSuiteSaveInput = {
  id?: string
  name: string
  systemPrompt: string
  promptTemplate: string
  testCases: AiPromptLabCase[]
}

export type AiPromptLabRunInput = {
  requestId: string
  runtimeId: AiModelRuntimeId
  model: string
  systemPrompt: string
  promptTemplate: string
  testCases: AiPromptLabCase[]
  temperature: number
  maxTokens: number
  confirmNetworkEndpoint: boolean
}

export type AiPromptLabCaseResult = {
  caseId: string
  name: string
  renderedPrompt: string
  output: string
  expectedContains: string
  passed?: boolean
  durationMs: number
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  error?: string
}

export type AiPromptLabRunResult = {
  requestId: string
  runtimeId: AiModelRuntimeId
  runtimeName: string
  model: string
  endpoint: string
  startedAt: string
  completedAt: string
  cancelled: boolean
  results: AiPromptLabCaseResult[]
  summary: {
    cases: number
    completed: number
    passed: number
    scored: number
    passRate?: number
    durationMs: number
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
}

export function isAiPromptLabSuiteSaveInput(value: unknown): value is AiPromptLabSuiteSaveInput {
  return isRecord(value)
    && (value.id === undefined || isUuid(value.id))
    && isBoundedString(value.name, 1, 200)
    && isBoundedString(value.systemPrompt, 0, 20_000)
    && isBoundedString(value.promptTemplate, 1, 20_000)
    && isCases(value.testCases)
}

export function isAiPromptLabRunInput(value: unknown): value is AiPromptLabRunInput {
  return isRecord(value)
    && isUuid(value.requestId)
    && aiModelRuntimeIds.includes(value.runtimeId as AiModelRuntimeId)
    && isBoundedString(value.model, 1, 500)
    && isBoundedString(value.systemPrompt, 0, 20_000)
    && isBoundedString(value.promptTemplate, 1, 20_000)
    && isCases(value.testCases)
    && typeof value.temperature === 'number'
    && Number.isFinite(value.temperature)
    && value.temperature >= 0
    && value.temperature <= 2
    && typeof value.maxTokens === 'number'
    && Number.isInteger(value.maxTokens)
    && value.maxTokens >= 1
    && value.maxTokens <= 32_768
    && typeof value.confirmNetworkEndpoint === 'boolean'
}

export function isAiPromptLabSuiteId(value: unknown): value is string {
  return isUuid(value)
}

function isCases(value: unknown): value is AiPromptLabCase[] {
  return Array.isArray(value)
    && value.length > 0
    && value.length <= 20
    && value.every((item) => isRecord(item)
      && isUuid(item.id)
      && isBoundedString(item.name, 1, 200)
      && isBoundedString(item.input, 0, 20_000)
      && isBoundedString(item.expectedContains, 0, 2_000))
}

function isBoundedString(value: unknown, minimum: number, maximum: number): value is string {
  return typeof value === 'string' && value.length >= minimum && value.length <= maximum && !value.includes('\0')
}

function isUuid(value: unknown): value is string {
  return typeof value === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

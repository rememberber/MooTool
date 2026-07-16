export const codeRuntimeIds = ['java', 'groovy', 'python', 'node'] as const

export type CodeRuntimeId = (typeof codeRuntimeIds)[number]

export type RuntimeExecutionInput = {
  requestId: string
  runtime: CodeRuntimeId
  code: string
  timeoutMs?: number
  arguments?: string[]
  workingDirectory?: string
}

export type RuntimeOutputEvent = {
  requestId: string
  stream: 'stdout' | 'stderr'
  text: string
}

export type RuntimeExecutionResult = {
  requestId: string
  runtime: CodeRuntimeId
  command: string
  stdout: string
  stderr: string
  exitCode: number | null
  durationMs: number
  timedOut: boolean
  cancelled: boolean
  truncated: boolean
}

export type RuntimeCommandPaths = Partial<Record<CodeRuntimeId, string>>

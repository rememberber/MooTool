import { spawn, type ChildProcessWithoutNullStreams, type SpawnOptionsWithoutStdio } from 'node:child_process'
import { requiresAiAgentTaskWriteConfirmation, type AiAgentTaskOutputEvent, type AiAgentTaskResult, type AiAgentTaskStartInput, type AiAgentTaskStatus } from '../../../src/shared/contracts/aiAgentTasks'
import type { AiAgentProfile } from '../../../src/shared/contracts/aiAgents'
import { redactSensitiveContent } from './securityScanner'
import type { AgentManagerService } from './agentManagerService'
import type { AiAgentProfileRepository } from './agentProfileRepository'

type SpawnTaskProcess = (
  executable: string,
  args: string[],
  options: SpawnOptionsWithoutStdio & { stdio: ['pipe', 'pipe', 'pipe'] }
) => ChildProcessWithoutNullStreams

type AgentTaskServiceOptions = {
  manager: Pick<AgentManagerService, 'launchPlan'>
  repository: Pick<AiAgentProfileRepository, 'getRequired'>
  environment?: NodeJS.ProcessEnv
  platform?: NodeJS.Platform
  maximumOutputBytes?: number
  spawnProcess?: SpawnTaskProcess
  clock?: () => Date
}

type ActiveTask = {
  child: ChildProcessWithoutNullStreams
  cancelled: boolean
  timedOut: boolean
  outputLimited: boolean
  killTimer?: NodeJS.Timeout
}

const defaultMaximumOutputBytes = 1024 * 1024
const maximumEventCharacters = 8 * 1024

export class AgentTaskService {
  private readonly manager: AgentTaskServiceOptions['manager']
  private readonly repository: AgentTaskServiceOptions['repository']
  private readonly environment: NodeJS.ProcessEnv
  private readonly platform: NodeJS.Platform
  private readonly maximumOutputBytes: number
  private readonly spawnProcess: SpawnTaskProcess
  private readonly clock: () => Date
  private readonly active = new Map<string, ActiveTask>()

  constructor(options: AgentTaskServiceOptions) {
    this.manager = options.manager
    this.repository = options.repository
    this.environment = options.environment ?? process.env
    this.platform = options.platform ?? process.platform
    this.maximumOutputBytes = options.maximumOutputBytes ?? defaultMaximumOutputBytes
    this.spawnProcess = options.spawnProcess ?? (spawn as SpawnTaskProcess)
    this.clock = options.clock ?? (() => new Date())
  }

  async run(input: AiAgentTaskStartInput, onOutput: (event: AiAgentTaskOutputEvent) => void = () => undefined): Promise<AiAgentTaskResult> {
    if (!input.confirmExecution) throw new Error('Starting an Agent task requires explicit execution confirmation')
    if (this.active.has(input.requestId)) throw new Error('Agent task request is already active')
    const profile = this.repository.getRequired(input.profileId)
    if (requiresAiAgentTaskWriteConfirmation(profile) && !input.confirmWrite) {
      throw new Error('This Agent Profile may modify files and requires explicit write confirmation')
    }
    const plan = await this.manager.launchPlan(profile.id)
    const args = taskArguments(profile, plan.args, input.maxTurns)
    const startedAt = this.clock()
    const startedAtMs = startedAt.getTime()
    const secrets = collectSensitiveEnvironmentValues(this.environment, profile.environmentVariableRefs)
    let stdout = ''
    let stderr = ''
    let outputBytes = 0
    let sequence = 0
    const pending: Record<'stdout' | 'stderr', string> = { stdout: '', stderr: '' }

    const emit = (stream: AiAgentTaskOutputEvent['stream'], text: string): void => {
      if (!text) return
      const redacted = redactTaskOutput(text, input.prompt, secrets)
      if (!redacted) return
      if (stream === 'stdout') stdout += redacted
      if (stream === 'stderr') stderr += redacted
      for (let offset = 0; offset < redacted.length; offset += maximumEventCharacters) {
        onOutput({
          requestId: input.requestId,
          sequence: sequence++,
          stream,
          text: redacted.slice(offset, offset + maximumEventCharacters),
          timestamp: this.clock().toISOString()
        })
      }
    }

    const append = (stream: 'stdout' | 'stderr', chunk: Buffer): void => {
      const task = this.active.get(input.requestId)
      if (!task || task.outputLimited) return
      const remaining = this.maximumOutputBytes - outputBytes
      if (remaining <= 0) {
        task.outputLimited = true
        emit('system', 'Output exceeded the 1 MB safety limit; stopping the Agent task.\n')
        this.terminate(input.requestId)
        return
      }
      const accepted = chunk.length > remaining ? chunk.subarray(0, remaining) : chunk
      outputBytes += accepted.length
      pending[stream] += accepted.toString('utf8')
      flushCompleteLines(stream)
      if (chunk.length > remaining) {
        task.outputLimited = true
        emit('system', 'Output exceeded the 1 MB safety limit; stopping the Agent task.\n')
        this.terminate(input.requestId)
      }
    }

    const flushCompleteLines = (stream: 'stdout' | 'stderr'): void => {
      const value = pending[stream]
      const boundary = Math.max(value.lastIndexOf('\n'), value.lastIndexOf('\r'))
      if (boundary < 0) return
      pending[stream] = value.slice(boundary + 1)
      emit(stream, value.slice(0, boundary + 1))
    }

    const flushPending = (stream: 'stdout' | 'stderr'): void => {
      const value = pending[stream]
      pending[stream] = ''
      emit(stream, value)
    }

    return new Promise<AiAgentTaskResult>((resolve) => {
      let settled = false
      const child = this.spawnProcess(plan.executable, args, {
        cwd: plan.workingDirectory,
        env: taskEnvironment(this.environment, profile.clientId),
        shell: false,
        windowsHide: true,
        detached: this.platform !== 'win32',
        stdio: ['pipe', 'pipe', 'pipe']
      })
      const task: ActiveTask = { child, cancelled: false, timedOut: false, outputLimited: false }
      this.active.set(input.requestId, task)
      emit('system', `Starting ${profile.clientId === 'codex' ? 'Codex' : 'Claude Code'} with prompt over stdin.\n`)

      const timeout = setTimeout(() => {
        const current = this.active.get(input.requestId)
        if (!current) return
        current.timedOut = true
        emit('system', `Time limit of ${input.maxDurationSeconds} seconds reached; stopping the Agent task.\n`)
        this.terminate(input.requestId)
      }, input.maxDurationSeconds * 1_000)

      const finish = (exitCode: number | null, signal: NodeJS.Signals | null, startError?: Error): void => {
        if (settled) return
        settled = true
        clearTimeout(timeout)
        flushPending('stdout')
        flushPending('stderr')
        if (startError) emit('stderr', `Unable to start Agent task: ${startError.message}\n`)
        const current = this.active.get(input.requestId) ?? task
        clearTimeout(current.killTimer)
        this.active.delete(input.requestId)
        const status: AiAgentTaskStatus = current.cancelled
          ? 'cancelled'
          : current.timedOut
            ? 'timedOut'
            : current.outputLimited
              ? 'outputLimit'
              : exitCode === 0 && !startError
                ? 'completed'
                : 'failed'
        const finishedAt = this.clock()
        resolve({
          requestId: input.requestId,
          profileId: profile.id,
          clientId: profile.clientId,
          status,
          executable: plan.executable,
          args,
          workingDirectory: plan.workingDirectory,
          stdout,
          stderr,
          exitCode,
          signal,
          durationMs: Math.max(0, finishedAt.getTime() - startedAtMs),
          startedAt: startedAt.toISOString(),
          finishedAt: finishedAt.toISOString(),
          truncated: current.outputLimited,
          promptDeliveredVia: 'stdin'
        })
      }

      child.stdout.on('data', (chunk: Buffer) => append('stdout', chunk))
      child.stderr.on('data', (chunk: Buffer) => append('stderr', chunk))
      child.stdin.on('error', () => undefined)
      child.once('error', (error) => finish(null, null, error))
      child.once('close', (exitCode, signal) => finish(exitCode, signal))
      child.stdin.end(input.prompt)
    })
  }

  cancel(requestId: string): boolean {
    const task = this.active.get(requestId)
    if (!task) return false
    task.cancelled = true
    this.terminate(requestId)
    return true
  }

  cancelAll(): void {
    for (const requestId of this.active.keys()) this.cancel(requestId)
  }

  private terminate(requestId: string): void {
    const task = this.active.get(requestId)
    if (!task || task.child.killed || task.child.exitCode !== null) return
    killProcessTree(task.child, 'SIGTERM', this.platform)
    clearTimeout(task.killTimer)
    task.killTimer = setTimeout(() => killProcessTree(task.child, 'SIGKILL', this.platform), 1_200)
  }
}

export function taskArguments(profile: AiAgentProfile, launchArgs: string[], maxTurns: number): string[] {
  if (profile.clientId === 'codex') {
    const globalArgs = launchArgs.filter((argument) => argument !== '--no-alt-screen')
    if (profile.modelRuntimeId === 'ollama') globalArgs.push('--oss', '--local-provider', 'ollama')
    if (profile.modelRuntimeId === 'lmStudio') globalArgs.push('--oss', '--local-provider', 'lmstudio')
    return [...globalArgs, 'exec', '--json', '--color', 'never', '--ephemeral', '-']
  }
  return [
    ...launchArgs,
    '--print',
    '--output-format', 'stream-json',
    '--verbose',
    '--include-partial-messages',
    '--max-turns', String(maxTurns),
    '--no-session-persistence'
  ]
}

function taskEnvironment(source: NodeJS.ProcessEnv, clientId: AiAgentProfile['clientId']): NodeJS.ProcessEnv {
  return {
    ...source,
    NO_COLOR: '1',
    ...(clientId === 'claudeCode' ? { CLAUDE_CODE_SKIP_PROMPT_HISTORY: '1' } : {})
  }
}

function collectSensitiveEnvironmentValues(environment: NodeJS.ProcessEnv, references: string[]): string[] {
  const names = new Set([
    ...references,
    ...Object.keys(environment).filter((name) => /(api.?key|token|secret|password|credential|authorization|auth$)/i.test(name))
  ])
  return [...names]
    .map((name) => environment[name])
    .filter((value): value is string => typeof value === 'string' && value.length >= 6)
    .sort((left, right) => right.length - left.length)
}

function redactTaskOutput(value: string, prompt: string, secrets: string[]): string {
  let output = redactSensitiveContent(value)
  const promptRepresentations = [prompt, JSON.stringify(prompt).slice(1, -1)].filter((candidate) => candidate.length >= 4)
  for (const sensitive of [...secrets, ...promptRepresentations]) output = output.replaceAll(sensitive, '[REDACTED]')
  return output
}

function killProcessTree(child: ChildProcessWithoutNullStreams, signal: NodeJS.Signals, platform: NodeJS.Platform): void {
  if (!child.pid) return
  if (platform === 'win32') {
    const force = signal === 'SIGKILL' ? ['/F'] : []
    const killer = spawn('taskkill', ['/PID', String(child.pid), '/T', ...force], { windowsHide: true, stdio: 'ignore' })
    killer.unref()
    return
  }
  try {
    process.kill(-child.pid, signal)
  } catch {
    try { child.kill(signal) } catch { /* The process already exited. */ }
  }
}

import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process'
import { mkdtemp, mkdir, realpath, rm, stat, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import type {
  CodeRuntimeId,
  RuntimeCommandPaths,
  RuntimeExecutionInput,
  RuntimeExecutionResult,
  RuntimeOutputEvent
} from '../../src/shared/contracts/runtime'

const maxCodeBytes = 1024 * 1024
const maxOutputBytes = 2 * 1024 * 1024

type ActiveExecution = {
  child: ChildProcessWithoutNullStreams
  cancelled: boolean
  timedOut: boolean
  truncated: boolean
  killTimer?: NodeJS.Timeout
}

export class RuntimeExecutionService {
  private readonly active = new Map<string, ActiveExecution>()

  constructor(private readonly tempRoot: string) {}

  async run(
    input: RuntimeExecutionInput,
    paths: RuntimeCommandPaths,
    onOutput: (event: RuntimeOutputEvent) => void = () => undefined
  ): Promise<RuntimeExecutionResult> {
    validateExecutionInput(input)
    if (this.active.has(input.requestId)) throw new Error('Runtime request is already active')
    await mkdir(this.tempRoot, { recursive: true })
    const directory = await mkdtemp(join(this.tempRoot, 'run-'))
    const definition = runtimeDefinition(input.runtime, paths[input.runtime], directory, input.code)
    await writeFile(definition.file, input.code, 'utf8')
    const workingDirectory = await resolveWorkingDirectory(input.workingDirectory, directory)
    const argumentsList = [...definition.args, ...(input.arguments ?? [])]
    const startedAt = Date.now()
    const timeoutMs = clampTimeout(input.timeoutMs)
    let stdout = ''
    let stderr = ''
    let outputBytes = 0

    try {
      return await new Promise<RuntimeExecutionResult>((resolve, reject) => {
        const child = spawn(definition.command, argumentsList, {
          cwd: workingDirectory,
          env: runtimeEnvironment(),
          windowsHide: true,
          detached: process.platform !== 'win32',
          stdio: ['pipe', 'pipe', 'pipe']
        })
        const execution: ActiveExecution = { child, cancelled: false, timedOut: false, truncated: false }
        this.active.set(input.requestId, execution)
        const timeout = setTimeout(() => {
          execution.timedOut = true
          this.terminate(input.requestId)
        }, timeoutMs)

        const append = (stream: 'stdout' | 'stderr', chunk: Buffer): void => {
          if (execution.truncated) return
          const remaining = maxOutputBytes - outputBytes
          if (remaining <= 0) {
            execution.truncated = true
            this.terminate(input.requestId)
            return
          }
          const buffer = chunk.length > remaining ? chunk.subarray(0, remaining) : chunk
          const text = buffer.toString('utf8')
          outputBytes += buffer.length
          if (stream === 'stdout') stdout += text
          else stderr += text
          onOutput({ requestId: input.requestId, stream, text })
          if (chunk.length > remaining) {
            execution.truncated = true
            this.terminate(input.requestId)
          }
        }

        child.stdout.on('data', (chunk: Buffer) => append('stdout', chunk))
        child.stderr.on('data', (chunk: Buffer) => append('stderr', chunk))
        child.once('error', (error) => {
          clearTimeout(timeout)
          this.finish(input.requestId)
          reject(new Error(`Unable to start ${input.runtime}: ${error.message}`))
        })
        child.once('close', (exitCode) => {
          clearTimeout(timeout)
          this.finish(input.requestId)
          resolve({
            requestId: input.requestId,
            runtime: input.runtime,
            command: [definition.command, ...argumentsList].map(displayArgument).join(' '),
            stdout,
            stderr,
            exitCode,
            durationMs: Date.now() - startedAt,
            timedOut: execution.timedOut,
            cancelled: execution.cancelled,
            truncated: execution.truncated
          })
        })
        child.stdin.end()
      })
    } finally {
      await rm(directory, { recursive: true, force: true })
    }
  }

  cancel(requestId: string): boolean {
    const execution = this.active.get(requestId)
    if (!execution) return false
    execution.cancelled = true
    this.terminate(requestId)
    return true
  }

  cancelAll(): void {
    for (const requestId of this.active.keys()) this.cancel(requestId)
  }

  private terminate(requestId: string): void {
    const execution = this.active.get(requestId)
    if (!execution || execution.child.killed) return
    killProcessTree(execution.child, 'SIGTERM')
    clearTimeout(execution.killTimer)
    execution.killTimer = setTimeout(() => killProcessTree(execution.child, 'SIGKILL'), 1200)
  }

  private finish(requestId: string): void {
    const execution = this.active.get(requestId)
    clearTimeout(execution?.killTimer)
    this.active.delete(requestId)
  }
}

function runtimeDefinition(runtime: CodeRuntimeId, configuredPath: string | undefined, directory: string, code: string): { command: string; args: string[]; file: string } {
  if (runtime === 'java') {
    const publicType = /\bpublic\s+(?:(?:abstract|final|sealed|non-sealed)\s+)*(?:class|record|interface|enum)\s+([A-Za-z_$][\w$]*)/.exec(code)?.[1]
    const file = join(directory, `${publicType || 'Main'}.java`)
    return { command: configuredPath || 'java', args: [file], file }
  }
  if (runtime === 'groovy') {
    const file = join(directory, 'main.groovy')
    return { command: configuredPath || 'groovy', args: [file], file }
  }
  if (runtime === 'python') {
    const file = join(directory, 'main.py')
    return { command: configuredPath || (process.platform === 'win32' ? 'python' : 'python3'), args: ['-u', file], file }
  }
  const file = join(directory, 'main.mjs')
  return { command: configuredPath || 'node', args: [file], file }
}

function validateExecutionInput(input: RuntimeExecutionInput): void {
  if (!input || typeof input !== 'object') throw new Error('Invalid runtime request')
  if (!/^[A-Za-z0-9_-]{8,80}$/.test(input.requestId)) throw new Error('Invalid runtime request id')
  if (!['java', 'groovy', 'python', 'node'].includes(input.runtime)) throw new Error('Unsupported runtime')
  if (typeof input.code !== 'string' || Buffer.byteLength(input.code, 'utf8') > maxCodeBytes) throw new Error('Code exceeds 1 MB limit')
  if (input.arguments !== undefined && (!Array.isArray(input.arguments) || input.arguments.length > 40 || input.arguments.some((argument) => typeof argument !== 'string' || argument.length > 1000 || argument.includes('\0')))) {
    throw new Error('Invalid runtime arguments')
  }
  if (input.workingDirectory !== undefined && (typeof input.workingDirectory !== 'string' || input.workingDirectory.length > 1000 || input.workingDirectory.includes('\0'))) {
    throw new Error('Invalid runtime working directory')
  }
}

async function resolveWorkingDirectory(value: string | undefined, fallback: string): Promise<string> {
  if (!value?.trim()) return fallback
  const resolved = await realpath(value.trim())
  if (!(await stat(resolved)).isDirectory()) throw new Error('Runtime working directory is not a directory')
  return resolved
}

function displayArgument(value: string): string {
  return /[\s"']/.test(value) ? JSON.stringify(value) : value
}

function clampTimeout(value: number | undefined): number {
  if (!Number.isFinite(value)) return 30_000
  return Math.min(120_000, Math.max(1_000, Math.round(value!)))
}

function runtimeEnvironment(): NodeJS.ProcessEnv {
  const allowed = ['PATH', 'HOME', 'USERPROFILE', 'TMPDIR', 'TMP', 'TEMP', 'SystemRoot', 'WINDIR', 'LANG', 'LC_ALL', 'JAVA_HOME', 'GROOVY_HOME']
  return Object.fromEntries(allowed.flatMap((key) => process.env[key] === undefined ? [] : [[key, process.env[key]]]))
}

function killProcessTree(child: ChildProcessWithoutNullStreams, signal: NodeJS.Signals): void {
  if (!child.pid) return
  if (process.platform === 'win32') {
    const force = signal === 'SIGKILL' ? ['/F'] : []
    const killer = spawn('taskkill', ['/PID', String(child.pid), '/T', ...force], { windowsHide: true, stdio: 'ignore' })
    killer.unref()
    return
  }
  try {
    process.kill(-child.pid, signal)
  } catch {
    try {
      child.kill(signal)
    } catch {
      // The process already exited.
    }
  }
}

import { createHash } from 'node:crypto'
import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process'
import { constants } from 'node:fs'
import { access, readFile, realpath, stat } from 'node:fs/promises'
import { delimiter, isAbsolute, join, resolve } from 'node:path'
import type { AiMcpProbeResult, AiMcpServer } from '../../../src/shared/contracts/aiMcp'
import { redactSensitiveContent } from './securityScanner'
import type { ParsedMcpServer } from './mcpConfig'

type McpProbeServiceOptions = {
  homeDirectory: string
  pathValue?: string
  timeoutMs?: number
  fetcher?: typeof fetch
}

type ActiveProbe = {
  controller: AbortController
  stop?: () => void
}

type ProbeSuccess = Pick<AiMcpProbeResult, 'protocolVersion' | 'tools' | 'resources' | 'prompts' | 'toolSchemas' | 'schemaEstimatedTokens' | 'executablePath' | 'executableSha256' | 'logs'>

const maximumOutputBytes = 1024 * 1024
const maximumLogLines = 50

export class McpProbeService {
  private readonly homeDirectory: string
  private readonly pathValue: string
  private readonly timeoutMs: number
  private readonly fetcher: typeof fetch
  private readonly active = new Map<string, ActiveProbe>()

  constructor(options: McpProbeServiceOptions) {
    this.homeDirectory = resolve(options.homeDirectory)
    this.pathValue = options.pathValue ?? process.env.PATH ?? ''
    this.timeoutMs = options.timeoutMs ?? 15_000
    this.fetcher = options.fetcher ?? fetch
  }

  async probe(requestId: string, publicServer: AiMcpServer, server: ParsedMcpServer, workingDirectory: string, confirmCommand: boolean): Promise<AiMcpProbeResult> {
    if (this.active.has(requestId)) throw new Error('MCP probe request is already active')
    const startedAt = Date.now()
    const active: ActiveProbe = { controller: new AbortController() }
    this.active.set(requestId, active)
    try {
      if (server.transport === 'stdio' && !confirmCommand) throw new ProbeError('CONFIRMATION_REQUIRED', 'Starting a local MCP command requires explicit confirmation')
      const task = server.transport === 'stdio'
        ? this.probeStdio(server, workingDirectory, active)
        : server.transport === 'streamableHttp'
          ? this.probeHttp(server, active.controller.signal)
          : Promise.reject(new ProbeError('PROTOCOL_ERROR', 'Only stdio and Streamable HTTP probes are supported'))
      const success = await withTimeout(task, this.timeoutMs, active)
      return { requestId, serverId: publicServer.id, status: 'healthy', latencyMs: Date.now() - startedAt, ...success }
    } catch (error) {
      const failure = normalizeProbeError(error, active.controller.signal.aborted)
      return {
        requestId,
        serverId: publicServer.id,
        status: failure.code === 'CANCELLED' ? 'cancelled' : 'error',
        latencyMs: Date.now() - startedAt,
        tools: 0,
        resources: 0,
        prompts: 0,
        toolSchemas: [],
        schemaEstimatedTokens: 0,
        logs: failure.logs,
        errorCode: failure.code,
        message: failure.message
      }
    } finally {
      active.stop?.()
      this.active.delete(requestId)
    }
  }

  cancel(requestId: string): boolean {
    const active = this.active.get(requestId)
    if (!active) return false
    active.controller.abort()
    active.stop?.()
    return true
  }

  private async probeStdio(server: ParsedMcpServer, workingDirectory: string, active: ActiveProbe): Promise<ProbeSuccess> {
    throwIfAborted(active.controller.signal)
    if (!server.command) throw new ProbeError('COMMAND_NOT_FOUND', 'The MCP command is missing')
    const expandedCommand = expandEnvironment(server.command)
    const executablePath = await findExecutable(expandedCommand, this.pathValue, this.homeDirectory)
    if (!executablePath) throw new ProbeError('COMMAND_NOT_FOUND', 'The MCP executable was not found')
    const executableSha256 = createHash('sha256').update(await readFile(executablePath)).digest('hex')
    throwIfAborted(active.controller.signal)
    const { environment, secrets } = buildEnvironment(server)
    const args = server.args.map(expandEnvironment)
    const child = spawn(executablePath, args, {
      cwd: workingDirectory,
      env: environment,
      shell: false,
      detached: process.platform !== 'win32',
      stdio: ['pipe', 'pipe', 'pipe']
    })
    active.stop = () => stopProcessTree(child)
    throwIfAborted(active.controller.signal)
    const client = new StdioJsonRpcClient(child, active.controller.signal, secrets)
    try {
      const initialize = await client.request('initialize', {
        protocolVersion: '2025-11-25',
        capabilities: {},
        clientInfo: { name: 'MooTool', version: '1.0' }
      })
      client.notify('notifications/initialized')
      const [toolItems, resourceItems, promptItems] = await Promise.all([
        client.optionalItems('tools/list', 'tools'),
        client.optionalItems('resources/list', 'resources'),
        client.optionalItems('prompts/list', 'prompts')
      ])
      const toolSchemas = summarizeToolSchemas(toolItems)
      return {
        protocolVersion: readString(initialize, 'protocolVersion'),
        tools: toolItems.length,
        resources: resourceItems.length,
        prompts: promptItems.length,
        toolSchemas,
        schemaEstimatedTokens: toolSchemas.reduce((sum, item) => sum + item.estimatedTokens, 0),
        executablePath,
        executableSha256,
        logs: client.logs()
      }
    } finally {
      client.close()
    }
  }

  private async probeHttp(server: ParsedMcpServer, signal: AbortSignal): Promise<ProbeSuccess> {
    if (!server.url) throw new ProbeError('CONNECTION_ERROR', 'The MCP URL is missing')
    const url = expandEnvironment(server.url)
    assertSafeHttpEndpoint(url)
    const headers = buildHttpHeaders(server)
    let sessionId: string | undefined
    const request = async (id: number | undefined, method: string, params?: Record<string, unknown>): Promise<unknown> => {
      const response = await this.fetcher(url, {
        method: 'POST',
        signal,
        headers: {
          Accept: 'application/json, text/event-stream',
          'Content-Type': 'application/json',
          ...headers,
          ...(sessionId ? { 'Mcp-Session-Id': sessionId } : {})
        },
        body: JSON.stringify({ jsonrpc: '2.0', ...(id === undefined ? {} : { id }), method, ...(params ? { params } : {}) })
      })
      if (!response.ok) throw new ProbeError('CONNECTION_ERROR', `MCP HTTP endpoint returned status ${response.status}`)
      sessionId = response.headers.get('mcp-session-id') ?? sessionId
      if (id === undefined || response.status === 202 || response.status === 204) return {}
      const source = await response.text()
      if (Buffer.byteLength(source) > maximumOutputBytes) throw new ProbeError('PROTOCOL_ERROR', 'MCP HTTP output exceeded the 1 MB safety limit')
      const payload = parseHttpJsonRpc(source, response.headers.get('content-type') ?? '')
      if (!isRecord(payload) || payload.id !== id) throw new ProbeError('PROTOCOL_ERROR', 'MCP HTTP response did not match the request')
      if (isRecord(payload.error)) throw new ProbeError('PROTOCOL_ERROR', readString(payload.error, 'message') ?? 'MCP HTTP returned a protocol error', Number(payload.error.code) === -32601)
      return payload.result
    }
    const initialize = await request(1, 'initialize', { protocolVersion: '2025-11-25', capabilities: {}, clientInfo: { name: 'MooTool', version: '1.0' } })
    await request(undefined, 'notifications/initialized')
    const optionalItems = async (id: number, method: string, key: string): Promise<unknown[]> => {
      try {
        const result = await request(id, method)
        return isRecord(result) && Array.isArray(result[key]) ? result[key] : []
      } catch (error) {
        if (error instanceof ProbeError && error.methodNotFound) return []
        throw error
      }
    }
    const [toolItems, resourceItems, promptItems] = await Promise.all([
      optionalItems(2, 'tools/list', 'tools'),
      optionalItems(3, 'resources/list', 'resources'),
      optionalItems(4, 'prompts/list', 'prompts')
    ])
    const toolSchemas = summarizeToolSchemas(toolItems)
    return {
      protocolVersion: readString(initialize, 'protocolVersion'),
      tools: toolItems.length,
      resources: resourceItems.length,
      prompts: promptItems.length,
      toolSchemas,
      schemaEstimatedTokens: toolSchemas.reduce((sum, item) => sum + item.estimatedTokens, 0),
      logs: []
    }
  }
}

class StdioJsonRpcClient {
  private readonly pending = new Map<number, { resolve: (value: unknown) => void; reject: (error: Error) => void }>()
  private readonly logLines: string[] = []
  private stdoutBuffer = ''
  private stderrBuffer = ''
  private outputBytes = 0
  private requestId = 0
  private closed = false

  constructor(private readonly child: ChildProcessWithoutNullStreams, signal: AbortSignal, private readonly secrets: string[]) {
    child.stdout.on('data', (chunk: Buffer) => this.onStdout(chunk))
    child.stderr.on('data', (chunk: Buffer) => this.onStderr(chunk))
    child.on('error', (error) => this.rejectAll(new ProbeError('CONNECTION_ERROR', error.message)))
    child.on('close', () => {
      if (!this.closed) this.rejectAll(new ProbeError('CONNECTION_ERROR', 'MCP process exited before the probe completed', false, this.logs()))
    })
    signal.addEventListener('abort', () => this.rejectAll(new ProbeError('CANCELLED', 'MCP probe was cancelled', false, this.logs())), { once: true })
  }

  request(method: string, params?: Record<string, unknown>): Promise<unknown> {
    const id = ++this.requestId
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject })
      this.write({ jsonrpc: '2.0', id, method, ...(params ? { params } : {}) })
    })
  }

  notify(method: string): void {
    this.write({ jsonrpc: '2.0', method })
  }

  async optionalItems(method: string, key: string): Promise<unknown[]> {
    try {
      const result = await this.request(method)
      return isRecord(result) && Array.isArray(result[key]) ? result[key] : []
    } catch (error) {
      if (error instanceof ProbeError && error.methodNotFound) return []
      throw error
    }
  }

  logs(): string[] {
    return [...this.logLines]
  }

  close(): void {
    this.closed = true
  }

  private write(payload: Record<string, unknown>): void {
    if (!this.child.stdin.writable) throw new ProbeError('CONNECTION_ERROR', 'MCP process input is unavailable')
    this.child.stdin.write(`${JSON.stringify(payload)}\n`)
  }

  private onStdout(chunk: Buffer): void {
    this.outputBytes += chunk.byteLength
    if (this.outputBytes > maximumOutputBytes) {
      this.rejectAll(new ProbeError('PROTOCOL_ERROR', 'MCP output exceeded the 1 MB safety limit', false, this.logs()))
      return
    }
    this.stdoutBuffer += chunk.toString('utf8')
    let newline = this.stdoutBuffer.indexOf('\n')
    while (newline >= 0) {
      const line = this.stdoutBuffer.slice(0, newline).trim()
      this.stdoutBuffer = this.stdoutBuffer.slice(newline + 1)
      if (line) this.onMessage(line)
      newline = this.stdoutBuffer.indexOf('\n')
    }
  }

  private onStderr(chunk: Buffer): void {
    this.outputBytes += chunk.byteLength
    if (this.outputBytes > maximumOutputBytes) {
      this.rejectAll(new ProbeError('PROTOCOL_ERROR', 'MCP output exceeded the 1 MB safety limit', false, this.logs()))
      return
    }
    this.stderrBuffer += chunk.toString('utf8')
    const lines = this.stderrBuffer.split(/\r?\n/)
    this.stderrBuffer = lines.pop() ?? ''
    for (const line of lines) {
      if (this.logLines.length >= maximumLogLines) break
      const redacted = redactLog(line, this.secrets).slice(0, 2_000)
      if (redacted) this.logLines.push(redacted)
    }
  }

  private onMessage(line: string): void {
    let payload: unknown
    try {
      payload = JSON.parse(line)
    } catch {
      this.rejectAll(new ProbeError('PROTOCOL_ERROR', 'MCP process emitted invalid JSON-RPC', false, this.logs()))
      return
    }
    if (!isRecord(payload) || typeof payload.id !== 'number') return
    const pending = this.pending.get(payload.id)
    if (!pending) return
    this.pending.delete(payload.id)
    if (isRecord(payload.error)) {
      pending.reject(new ProbeError('PROTOCOL_ERROR', readString(payload.error, 'message') ?? 'MCP returned a protocol error', Number(payload.error.code) === -32601, this.logs()))
    } else pending.resolve(payload.result)
  }

  private rejectAll(error: Error): void {
    for (const pending of this.pending.values()) pending.reject(error)
    this.pending.clear()
  }
}

class ProbeError extends Error {
  constructor(
    readonly code: NonNullable<AiMcpProbeResult['errorCode']>,
    message: string,
    readonly methodNotFound = false,
    readonly probeLogs: string[] = []
  ) {
    super(message)
  }
}

async function withTimeout<T>(task: Promise<T>, timeoutMs: number, active: ActiveProbe): Promise<T> {
  let timer: NodeJS.Timeout | undefined
  try {
    return await Promise.race([
      task,
      new Promise<never>((_resolve, reject) => {
        timer = setTimeout(() => {
          active.stop?.()
          reject(new ProbeError('TIMEOUT', 'MCP probe timed out'))
        }, timeoutMs)
      })
    ])
  } finally {
    clearTimeout(timer)
  }
}

async function findExecutable(command: string, pathValue: string, homeDirectory: string): Promise<string | undefined> {
  const candidates = isAbsolute(command) ? [command] : [
    ...pathValue.split(delimiter).filter(Boolean).map((directory) => join(directory, command)),
    join(homeDirectory, '.local', 'bin', command),
    join('/usr/local/bin', command),
    join('/opt/homebrew/bin', command)
  ]
  for (const candidate of candidates) {
    try {
      const info = await stat(candidate)
      if (!info.isFile()) continue
      if (process.platform !== 'win32') await access(candidate, constants.X_OK)
      return await realpath(candidate)
    } catch {
      // Try the next PATH entry.
    }
  }
  return undefined
}

function buildEnvironment(server: ParsedMcpServer): { environment: NodeJS.ProcessEnv; secrets: string[] } {
  const environment: NodeJS.ProcessEnv = { ...process.env }
  const secrets: string[] = []
  for (const [name, value] of Object.entries(server.environment)) {
    const expanded = expandEnvironment(value)
    environment[name] = expanded
    if (isSensitiveName(name)) secrets.push(expanded)
  }
  for (const [name, reference] of Object.entries(server.environmentReferences)) {
    const value = process.env[reference]
    if (value === undefined) throw new ProbeError('CONNECTION_ERROR', `Required environment variable is not set: ${reference}`)
    environment[name] = value
    if (isSensitiveName(name) || isSensitiveName(reference)) secrets.push(value)
  }
  return { environment, secrets: secrets.filter(Boolean) }
}

function buildHttpHeaders(server: ParsedMcpServer): Record<string, string> {
  const headers: Record<string, string> = {}
  for (const [name, value] of Object.entries(server.headers)) headers[name] = expandEnvironment(value)
  for (const [name, reference] of Object.entries(server.headerReferences)) {
    const value = process.env[reference]
    if (value === undefined) throw new ProbeError('CONNECTION_ERROR', `Required environment variable is not set: ${reference}`)
    headers[name] = name.toLowerCase() === 'authorization' && !/^\S+\s/.test(value) ? `Bearer ${value}` : value
  }
  if (server.bearerTokenEnvironmentVariable) {
    const value = process.env[server.bearerTokenEnvironmentVariable]
    if (value === undefined) throw new ProbeError('CONNECTION_ERROR', `Required environment variable is not set: ${server.bearerTokenEnvironmentVariable}`)
    headers.Authorization = `Bearer ${value}`
  }
  return headers
}

function expandEnvironment(value: string): string {
  return value.replace(/\$\{([A-Z_][A-Z0-9_]*)(?::-([^}]*))?}/g, (_match, name: string, fallback: string | undefined) => {
    const resolved = process.env[name] ?? fallback
    if (resolved === undefined) throw new ProbeError('CONNECTION_ERROR', `Required environment variable is not set: ${name}`)
    return resolved
  })
}

function assertSafeHttpEndpoint(value: string): void {
  let url: URL
  try { url = new URL(value) } catch { throw new ProbeError('CONNECTION_ERROR', 'The MCP URL is invalid') }
  if (url.protocol === 'https:') return
  if (url.protocol === 'http:' && ['127.0.0.1', 'localhost', '::1'].includes(url.hostname)) return
  throw new ProbeError('INSECURE_ENDPOINT', 'Remote MCP probes require HTTPS')
}

function parseHttpJsonRpc(source: string, contentType: string): unknown {
  if (contentType.includes('text/event-stream')) {
    const data = source.split(/\r?\n/).filter((line) => line.startsWith('data:')).map((line) => line.slice(5).trim()).find(Boolean)
    if (!data) throw new ProbeError('PROTOCOL_ERROR', 'MCP HTTP event stream did not contain JSON-RPC data')
    try { return JSON.parse(data) as unknown } catch { throw new ProbeError('PROTOCOL_ERROR', 'MCP HTTP event stream contained invalid JSON') }
  }
  try { return JSON.parse(source) as unknown } catch { throw new ProbeError('PROTOCOL_ERROR', 'MCP HTTP endpoint returned invalid JSON') }
}

function stopProcessTree(child: ChildProcessWithoutNullStreams): void {
  if (child.killed || child.exitCode !== null) return
  try {
    if (process.platform !== 'win32' && child.pid) process.kill(-child.pid, 'SIGTERM')
    else child.kill('SIGTERM')
  } catch {
    child.kill('SIGKILL')
  }
}

function redactLog(value: string, secrets: string[]): string {
  let output = redactSensitiveContent(value)
  for (const secret of secrets) {
    if (secret) output = output.replaceAll(secret, '[REDACTED]')
  }
  return output
}

function normalizeProbeError(error: unknown, aborted: boolean): { code: NonNullable<AiMcpProbeResult['errorCode']>; message: string; logs: string[] } {
  if (aborted) return { code: 'CANCELLED', message: 'MCP probe was cancelled', logs: error instanceof ProbeError ? error.probeLogs : [] }
  if (error instanceof ProbeError) return { code: error.code, message: error.message, logs: error.probeLogs }
  return { code: 'CONNECTION_ERROR', message: 'MCP connection failed', logs: [] }
}

function throwIfAborted(signal: AbortSignal): void {
  if (signal.aborted) throw new ProbeError('CANCELLED', 'MCP probe was cancelled')
}

function readString(value: unknown, key: string): string | undefined {
  return isRecord(value) && typeof value[key] === 'string' ? value[key] : undefined
}

function summarizeToolSchemas(items: unknown[]) {
  return items.slice(0, 500).map((item, index) => {
    const source = JSON.stringify(item) ?? ''
    return {
      name: readString(item, 'name')?.slice(0, 200) || `tool-${index + 1}`,
      estimatedTokens: estimateTokens(source)
    }
  })
}

function estimateTokens(value: string): number {
  const cjk = (value.match(/[\u3400-\u9fff\uf900-\ufaff]/g) ?? []).length
  return Math.max(1, Math.ceil((value.length - cjk) / 4 + cjk))
}

function isSensitiveName(value: string): boolean {
  return /(?:api[_-]?key|token|secret|password|authorization|credential|private[_-]?key)/i.test(value)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

import { createHash } from 'node:crypto'
import { lstat, readFile, realpath } from 'node:fs/promises'
import { join, relative, resolve } from 'node:path'
import type { AiClientId, AiPrimaryClientId, AiScope } from '../../../src/shared/contracts/ai'
import type { AiChangeApplyResult, AiChangePlan, AiChangeRollbackResult } from '../../../src/shared/contracts/aiChanges'
import type {
  AiMcpCopyInput,
  AiMcpCopyPreview,
  AiMcpCopyWarning,
  AiMcpInventory,
  AiMcpInventoryInput,
  AiMcpProbeInput,
  AiMcpProbeResult,
  AiMcpSchemaSnapshot,
  AiMcpSecretMapping,
  AiMcpServer
} from '../../../src/shared/contracts/aiMcp'
import { ConfigChangeService } from './configChangeService'
import {
  addClaudeMcpServer,
  addCodexMcpServer,
  parseClaudeMcpConfig,
  parseCodexMcpConfig,
  sanitizeMcpServer,
  type ParsedMcpServer
} from './mcpConfig'
import { McpProbeService } from './mcpProbeService'

type McpServiceOptions = {
  homeDirectory: string
  changes: ConfigChangeService
  pathValue?: string
  probeTimeoutMs?: number
  fetcher?: typeof fetch
}

type ConfigCandidate = {
  clientId: AiClientId
  scope: AiScope
  path: string
}

type LoadedConfiguration = ConfigCandidate & {
  source: string
  sourceHash: string
  servers: ParsedMcpServer[]
}

type CopyPlanState = {
  sourcePath: string
  sourceHash: string
  sourceServerName: string
  targetPath: string
  targetClientId: AiPrimaryClientId
  targetScope: AiScope
  expectedTransport: ParsedMcpServer['transport']
}

const maximumConfigBytes = 5 * 1024 * 1024

export class McpService {
  private readonly homeDirectory: string
  private readonly changes: ConfigChangeService
  private readonly probes: McpProbeService
  private readonly plans = new Map<string, CopyPlanState>()
  private readonly schemaSnapshots = new Map<string, AiMcpSchemaSnapshot>()

  constructor(options: McpServiceOptions) {
    this.homeDirectory = resolve(options.homeDirectory)
    this.changes = options.changes
    this.probes = new McpProbeService({
      homeDirectory: this.homeDirectory,
      pathValue: options.pathValue,
      timeoutMs: options.probeTimeoutMs,
      fetcher: options.fetcher
    })
  }

  async inventory(input: AiMcpInventoryInput = {}): Promise<AiMcpInventory> {
    const projectRoot = await resolveOptionalProject(input.projectRoot)
    const { configurations, invalidConfigPaths } = await this.loadConfigurations(projectRoot)
    const servers = configurations.flatMap((configuration) => configuration.servers.map((server) => sanitizeMcpServer(
      server,
      configuration.clientId,
      configuration.scope,
      configuration.path
    ))).sort((left, right) => left.name.localeCompare(right.name) || left.clientId.localeCompare(right.clientId) || left.scope.localeCompare(right.scope))
    return { scannedAt: new Date().toISOString(), projectRoot, servers, invalidConfigPaths }
  }

  async previewCopy(input: AiMcpCopyInput): Promise<AiMcpCopyPreview> {
    const projectRoot = await resolveOptionalProject(input.projectRoot)
    if (input.targetScope === 'project' && !projectRoot) throw new Error('A project is required for project-scoped MCP configuration')
    const { configurations } = await this.loadConfigurations(projectRoot)
    const sourceMatch = findSourceServer(configurations, input.sourceServerId)
    if (!sourceMatch) throw new Error('The selected MCP server is no longer available')
    const sourcePublic = sanitizeMcpServer(sourceMatch.server, sourceMatch.configuration.clientId, sourceMatch.configuration.scope, sourceMatch.configuration.path)
    if (sourcePublic.clientId === input.targetClientId && sourcePublic.scope === input.targetScope) throw new Error('The selected MCP server already has the requested client and scope')
    if (sourcePublic.risks.includes('sensitiveArgument')) throw new Error('Move credentials out of MCP command arguments before copying this server')
    if (sourcePublic.risks.includes('unknownTransport')) throw new Error('Unknown MCP transports cannot be copied safely')
    if (sourcePublic.risks.includes('legacyTransport')) throw new Error('Legacy SSE MCP servers must be migrated to Streamable HTTP before copying')
    if (hasSensitiveUrl(sourceMatch.server.url)) throw new Error('Move credentials out of the MCP URL before copying this server')

    const targetRoot = input.targetScope === 'user' ? await realpath(this.homeDirectory) : projectRoot!
    const targetPath = targetConfigPath(input.targetClientId, input.targetScope, targetRoot)
    const target = await this.loadConfiguration({ clientId: input.targetClientId, scope: input.targetScope, path: targetPath })
    if (target?.servers.some((server) => server.name === sourceMatch.server.name)) throw new Error(`MCP server already exists in target: ${sourceMatch.server.name}`)

    const mapping = createSafeMapping(sourceMatch.server, input.targetClientId)
    const nextContent = input.targetClientId === 'claudeCode'
      ? addClaudeMcpServer(target?.source, sourceMatch.server.name, toClaudeDefinition(mapping.server, mapping.environmentReferences, mapping.headerReferences))
      : addCodexMcpServer(target?.source, sourceMatch.server.name, mapping.server, {
          environmentReferences: mapping.environmentReferences,
          headerReferences: mapping.headerReferences
        })
    const plan = await this.changes.createPlan(targetRoot, [{
      targetPath: relative(targetRoot, targetPath),
      nextContent,
      summary: `Copy MCP server ${sourceMatch.server.name} to ${input.targetClientId}`,
      expectedState: target ? 'existing' : 'missing'
    }])
    this.plans.set(plan.id, {
      sourcePath: sourceMatch.configuration.path,
      sourceHash: sourceMatch.configuration.sourceHash,
      sourceServerName: sourceMatch.server.name,
      targetPath,
      targetClientId: input.targetClientId,
      targetScope: input.targetScope,
      expectedTransport: mapping.server.transport
    })
    return {
      plan,
      source: sourcePublic,
      targetClientId: input.targetClientId,
      targetScope: input.targetScope,
      targetPath,
      secretMappings: mapping.secretMappings,
      warnings: mapping.warnings
    }
  }

  async applyCopy(planId: string): Promise<AiChangeApplyResult> {
    const state = this.plans.get(planId)
    if (!state) throw new Error('Unknown or expired MCP copy plan')
    const source = await readSafeFile(state.sourcePath)
    if (!source || digest(source) !== state.sourceHash) throw new Error('MCP source configuration changed after preview')
    const result = await this.changes.apply(planId)
    try {
      const target = await this.loadConfiguration({ clientId: state.targetClientId, scope: state.targetScope, path: state.targetPath })
      const server = target?.servers.find((candidate) => candidate.name === state.sourceServerName)
      if (!server || server.transport !== state.expectedTransport) throw new Error('MCP target verification failed')
      this.plans.delete(planId)
      return result
    } catch (error) {
      await this.changes.rollback(result.snapshotId)
      this.plans.delete(planId)
      throw error
    }
  }

  rollbackCopy(snapshotId: string): Promise<AiChangeRollbackResult> {
    return this.changes.rollback(snapshotId)
  }

  async probe(input: AiMcpProbeInput): Promise<AiMcpProbeResult> {
    const projectRoot = await resolveOptionalProject(input.projectRoot)
    const { configurations } = await this.loadConfigurations(projectRoot)
    const sourceMatch = findSourceServer(configurations, input.sourceServerId)
    if (!sourceMatch) throw new Error('The selected MCP server is no longer available')
    const publicServer = sanitizeMcpServer(sourceMatch.server, sourceMatch.configuration.clientId, sourceMatch.configuration.scope, sourceMatch.configuration.path)
    if (publicServer.risks.includes('sensitiveArgument')) throw new Error('MCP commands with sensitive arguments cannot be started')
    const result = await this.probes.probe(input.requestId, publicServer, sourceMatch.server, projectRoot ?? this.homeDirectory, input.confirmCommand)
    if (result.status === 'healthy') {
      this.schemaSnapshots.set(publicServer.id, {
        serverId: publicServer.id,
        observedAt: new Date().toISOString(),
        toolSchemas: result.toolSchemas,
        schemaEstimatedTokens: result.schemaEstimatedTokens
      })
    }
    return result
  }

  contextSchemaSnapshots(serverIds: string[]): AiMcpSchemaSnapshot[] {
    return serverIds.flatMap((id) => {
      const snapshot = this.schemaSnapshots.get(id)
      return snapshot ? [{ ...snapshot, toolSchemas: snapshot.toolSchemas.map((item) => ({ ...item })) }] : []
    })
  }

  cancelProbe(requestId: string): boolean {
    return this.probes.cancel(requestId)
  }

  private async loadConfigurations(projectRoot?: string): Promise<{ configurations: LoadedConfiguration[]; invalidConfigPaths: string[] }> {
    const configurations: LoadedConfiguration[] = []
    const invalidConfigPaths: string[] = []
    for (const candidate of configCandidates(this.homeDirectory, projectRoot)) {
      try {
        const loaded = await this.loadConfiguration(candidate)
        if (loaded) configurations.push(loaded)
      } catch {
        invalidConfigPaths.push(candidate.path)
      }
    }
    return { configurations, invalidConfigPaths }
  }

  private async loadConfiguration(candidate: ConfigCandidate): Promise<LoadedConfiguration | undefined> {
    const source = await readSafeFile(candidate.path)
    if (source === undefined) return undefined
    const servers = candidate.clientId === 'codex' ? parseCodexMcpConfig(source) : parseClaudeMcpConfig(source)
    return { ...candidate, source, sourceHash: digest(source), servers }
  }
}

function configCandidates(homeDirectory: string, projectRoot?: string): ConfigCandidate[] {
  return [
    { clientId: 'codex', scope: 'user', path: join(homeDirectory, '.codex', 'config.toml') },
    { clientId: 'claudeCode', scope: 'user', path: join(homeDirectory, '.claude.json') },
    { clientId: 'cursor', scope: 'user', path: join(homeDirectory, '.cursor', 'mcp.json') },
    { clientId: 'geminiCli', scope: 'user', path: join(homeDirectory, '.gemini', 'settings.json') },
    { clientId: 'githubCopilot', scope: 'user', path: join(homeDirectory, '.copilot', 'mcp-config.json') },
    ...(projectRoot ? [
      { clientId: 'codex' as const, scope: 'project' as const, path: join(projectRoot, '.codex', 'config.toml') },
      { clientId: 'claudeCode' as const, scope: 'project' as const, path: join(projectRoot, '.mcp.json') },
      { clientId: 'cursor' as const, scope: 'project' as const, path: join(projectRoot, '.cursor', 'mcp.json') },
      { clientId: 'geminiCli' as const, scope: 'project' as const, path: join(projectRoot, '.gemini', 'settings.json') },
      { clientId: 'githubCopilot' as const, scope: 'project' as const, path: join(projectRoot, '.mcp.json') },
      { clientId: 'githubCopilot' as const, scope: 'project' as const, path: join(projectRoot, '.github', 'mcp.json') },
      { clientId: 'githubCopilot' as const, scope: 'project' as const, path: join(projectRoot, '.vscode', 'mcp.json') }
    ] : [])
  ]
}

function targetConfigPath(clientId: AiPrimaryClientId, scope: AiScope, root: string): string {
  if (clientId === 'codex') return join(root, '.codex', 'config.toml')
  return scope === 'user' ? join(root, '.claude.json') : join(root, '.mcp.json')
}

function findSourceServer(configurations: LoadedConfiguration[], id: string): { configuration: LoadedConfiguration; server: ParsedMcpServer } | undefined {
  for (const configuration of configurations) {
    for (const server of configuration.servers) {
      const publicServer = sanitizeMcpServer(server, configuration.clientId, configuration.scope, configuration.path)
      if (publicServer.id === id) return { configuration, server }
    }
  }
  return undefined
}

function createSafeMapping(source: ParsedMcpServer, targetClientId: AiPrimaryClientId): {
  server: ParsedMcpServer
  environmentReferences: Record<string, string>
  headerReferences: Record<string, string>
  secretMappings: AiMcpSecretMapping[]
  warnings: AiMcpCopyWarning[]
} {
  const server: ParsedMcpServer = {
    ...source,
    environment: { ...source.environment },
    environmentReferences: {},
    headers: { ...source.headers },
    headerReferences: {},
    bearerTokenEnvironmentVariable: undefined
  }
  const environmentReferences: Record<string, string> = {}
  const headerReferences: Record<string, string> = {}
  const secretMappings: AiMcpSecretMapping[] = []
  const warnings = new Set<AiMcpCopyWarning>()

  for (const [name, value] of Object.entries(source.environment)) {
    if (!isSensitive(name, value)) continue
    delete server.environment[name]
    const reference = environmentVariable(source.name, name)
    environmentReferences[name] = targetClientId === 'codex' ? name : reference
    secretMappings.push({ field: `env.${name}`, environmentVariable: environmentReferences[name] })
  }
  for (const [name, reference] of Object.entries(source.environmentReferences)) {
    environmentReferences[name] = targetClientId === 'codex' ? name : reference
    secretMappings.push({ field: `env.${name}`, environmentVariable: environmentReferences[name] })
  }
  for (const [name, value] of Object.entries(source.headers)) {
    if (!isSensitive(name, value)) continue
    delete server.headers[name]
    const reference = environmentVariable(source.name, name)
    headerReferences[name] = reference
    secretMappings.push({ field: `headers.${name}`, environmentVariable: reference })
  }
  for (const [name, reference] of Object.entries(source.headerReferences)) {
    headerReferences[name] = reference
    secretMappings.push({ field: `headers.${name}`, environmentVariable: reference })
  }
  if (source.bearerTokenEnvironmentVariable) {
    headerReferences.Authorization = source.bearerTokenEnvironmentVariable
    secretMappings.push({ field: 'headers.Authorization', environmentVariable: source.bearerTokenEnvironmentVariable })
  }
  if (secretMappings.length > 0) warnings.add('environmentVariablesRequired')
  if ((source.startupTimeoutMs !== undefined || source.toolTimeoutMs !== undefined) && targetClientId === 'claudeCode') warnings.add('timeoutNotPortable')
  if (!source.enabled && targetClientId === 'claudeCode') warnings.add('disabledNotPortable')
  if (source.oauth) warnings.add('oauthReauthorizationRequired')
  return { server, environmentReferences, headerReferences, secretMappings: uniqueMappings(secretMappings), warnings: [...warnings] }
}

function toClaudeDefinition(server: ParsedMcpServer, environmentReferences: Record<string, string>, headerReferences: Record<string, string>): Record<string, unknown> {
  if (server.transport === 'stdio') {
    if (!server.command) throw new Error('A stdio MCP server requires a command')
    const env = {
      ...server.environment,
      ...Object.fromEntries(Object.entries(environmentReferences).map(([name, reference]) => [name, `\${${reference}}`]))
    }
    return { type: 'stdio', command: server.command, args: server.args, ...(Object.keys(env).length > 0 ? { env } : {}) }
  }
  if (server.transport === 'streamableHttp') {
    if (!server.url) throw new Error('An HTTP MCP server requires a URL')
    const headers = {
      ...server.headers,
      ...Object.fromEntries(Object.entries(headerReferences).map(([name, reference]) => [name, name.toLowerCase() === 'authorization' ? `Bearer \${${reference}}` : `\${${reference}}`]))
    }
    return { type: 'http', url: server.url, ...(Object.keys(headers).length > 0 ? { headers } : {}), ...(server.oauth ? { oauth: {} } : {}) }
  }
  throw new Error('Only stdio and Streamable HTTP MCP servers can be copied')
}

async function readSafeFile(path: string): Promise<string | undefined> {
  try {
    const info = await lstat(path)
    if (info.isSymbolicLink() || !info.isFile()) throw new Error('MCP configuration must be a regular file')
    if (info.size > maximumConfigBytes) throw new Error('MCP configuration exceeds 5 MB')
    return await readFile(path, 'utf8')
  } catch (error) {
    if (isMissing(error)) return undefined
    throw error
  }
}

async function resolveOptionalProject(value?: string): Promise<string | undefined> {
  if (!value?.trim()) return undefined
  const root = await realpath(resolve(value))
  const info = await lstat(root)
  if (!info.isDirectory()) throw new Error('MCP project root must be a directory')
  return root
}

function hasSensitiveUrl(value?: string): boolean {
  if (!value) return false
  try {
    const url = new URL(value)
    if (url.username || url.password) return true
    return [...url.searchParams].some(([name, item]) => isSensitive(name, item))
  } catch {
    return true
  }
}

function isSensitive(name: string, value: string): boolean {
  return /(?:api[_-]?key|token|secret|password|authorization|credential|private[_-]?key)/i.test(name)
    || /\bsk-[A-Za-z0-9_-]{20,}\b|\bgh[opusr]_[A-Za-z0-9]{20,}\b|\bAKIA[0-9A-Z]{16}\b/.test(value)
}

function environmentVariable(serverName: string, field: string): string {
  return `MCP_${serverName}_${field}`.replace(/[^A-Za-z0-9]+/g, '_').replace(/^_|_$/g, '').toUpperCase().slice(0, 100)
}

function uniqueMappings(mappings: AiMcpSecretMapping[]): AiMcpSecretMapping[] {
  return [...new Map(mappings.map((mapping) => [`${mapping.field}:${mapping.environmentVariable}`, mapping])).values()]
}

function digest(value: string): string {
  return createHash('sha256').update(value).digest('hex')
}

function isMissing(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'code' in error && (error as { code?: string }).code === 'ENOENT'
}

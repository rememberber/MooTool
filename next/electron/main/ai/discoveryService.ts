import { createHash } from 'node:crypto'
import { constants } from 'node:fs'
import { access, lstat, readdir, readFile, realpath, stat } from 'node:fs/promises'
import { basename, delimiter, dirname, extname, join, relative, resolve, sep } from 'node:path'
import type {
  AiArtifact,
  AiArtifactKind,
  AiClientId,
  AiClientInstallation,
  AiDiagnostic,
  AiDiagnosticCode,
  AiDiagnosticSeverity,
  AiDiscoveryInput,
  AiDoctorSnapshot,
  AiLocalModel,
  AiRuntimeInstallation,
  AiRuntimeId,
  AiScope
} from '../../../src/shared/contracts/ai'
import { AdapterRegistry } from './adapterRegistry'
import { scanSensitiveContent } from './securityScanner'
import { inspectSkillPackage } from './skillInspector'
import { analyzeInstructions, inspectInstruction } from './instructionInspector'

type HttpResponse = {
  ok: boolean
  status: number
  json(): Promise<unknown>
}

type HttpFetcher = (url: string, init: { signal: AbortSignal }) => Promise<HttpResponse>

type DiscoveryContext = {
  homeDirectory: string
  projectRoot?: string
}

type FileCandidate = {
  path: string
  kind: Exclude<AiArtifactKind, 'skill' | 'mcpServer'>
  scope: AiScope
  source?: AiArtifact['source']
  mcpConfig?: boolean
  appliesTo?: string
}

type SkillRoot = {
  path: string
  scope: AiScope
  source: AiArtifact['source']
}

type ClientDiscoveryAdapter = {
  id: AiClientId
  name: string
  binaryNames: string[]
  configRoot(context: DiscoveryContext): string
  files(context: DiscoveryContext): FileCandidate[]
  skillRoots(context: DiscoveryContext): SkillRoot[]
}

export type ModelRuntimeDiscoveryAdapter = {
  id: AiRuntimeId
  name: string
  binaryNames: string[]
  endpoint: string
  protocols: AiRuntimeInstallation['protocols']
  dataDirectory(context: DiscoveryContext): string
}

type DiscoveryOptions = {
  homeDirectory: string
  pathValue?: string
  platform?: NodeJS.Platform
  includeDefaultExecutablePaths?: boolean
  fetcher?: HttpFetcher
  requestTimeoutMs?: number
  maxArtifacts?: number
}

const ignoredProjectDirectories = new Set(['.git', 'node_modules', 'dist', 'out', 'build', 'coverage', '.next', 'target', 'vendor'])
const maxConfigBytes = 1024 * 1024
const maxProjectDepth = 8
const maxSkillDepth = 3
const maxDiagnostics = 200
const ollamaEndpoint = 'http://127.0.0.1:11434'

const clientAdapters = new AdapterRegistry<ClientDiscoveryAdapter>([
  {
    id: 'codex',
    name: 'Codex',
    binaryNames: ['codex'],
    configRoot: ({ homeDirectory }) => join(homeDirectory, '.codex'),
    files: ({ homeDirectory, projectRoot }) => [
      { path: join(homeDirectory, '.codex', 'config.toml'), kind: 'clientConfig', scope: 'user', mcpConfig: true },
      { path: join(homeDirectory, '.codex', 'AGENTS.md'), kind: 'instruction', scope: 'user', appliesTo: homeDirectory },
      ...(projectRoot ? [{ path: join(projectRoot, '.codex', 'config.toml'), kind: 'clientConfig' as const, scope: 'project' as const, mcpConfig: true }] : [])
    ],
    skillRoots: ({ homeDirectory, projectRoot }) => [
      { path: join(homeDirectory, '.agents', 'skills'), scope: 'user', source: 'standard' },
      { path: join(homeDirectory, '.codex', 'skills'), scope: 'user', source: 'legacy' },
      ...(projectRoot ? [
        { path: join(projectRoot, '.agents', 'skills'), scope: 'project' as const, source: 'standard' as const },
        { path: join(projectRoot, '.codex', 'skills'), scope: 'project' as const, source: 'legacy' as const }
      ] : [])
    ]
  },
  {
    id: 'claudeCode',
    name: 'Claude Code',
    binaryNames: ['claude'],
    configRoot: ({ homeDirectory }) => join(homeDirectory, '.claude'),
    files: ({ homeDirectory, projectRoot }) => [
      { path: join(homeDirectory, '.claude', 'settings.json'), kind: 'clientConfig', scope: 'user' },
      { path: join(homeDirectory, '.claude.json'), kind: 'clientConfig', scope: 'user', mcpConfig: true },
      { path: join(homeDirectory, '.claude', 'CLAUDE.md'), kind: 'instruction', scope: 'user', appliesTo: homeDirectory },
      ...(projectRoot ? [
        { path: join(projectRoot, '.claude', 'settings.json'), kind: 'clientConfig' as const, scope: 'project' as const },
        { path: join(projectRoot, '.mcp.json'), kind: 'clientConfig' as const, scope: 'project' as const, mcpConfig: true }
      ] : [])
    ],
    skillRoots: ({ homeDirectory, projectRoot }) => [
      { path: join(homeDirectory, '.claude', 'skills'), scope: 'user', source: 'standard' },
      ...(projectRoot ? [{ path: join(projectRoot, '.claude', 'skills'), scope: 'project' as const, source: 'standard' as const }] : [])
    ]
  },
  {
    id: 'cursor',
    name: 'Cursor',
    binaryNames: ['cursor-agent'],
    configRoot: ({ homeDirectory }) => join(homeDirectory, '.cursor'),
    files: ({ homeDirectory, projectRoot }) => [
      { path: join(homeDirectory, '.cursor', 'cli-config.json'), kind: 'clientConfig', scope: 'user' },
      { path: join(homeDirectory, '.cursor', 'mcp.json'), kind: 'clientConfig', scope: 'user', mcpConfig: true },
      ...(projectRoot ? [
        { path: join(projectRoot, '.cursor', 'cli.json'), kind: 'clientConfig' as const, scope: 'project' as const },
        { path: join(projectRoot, '.cursor', 'mcp.json'), kind: 'clientConfig' as const, scope: 'project' as const, mcpConfig: true }
      ] : [])
    ],
    skillRoots: ({ homeDirectory, projectRoot }) => [
      { path: join(homeDirectory, '.cursor', 'skills'), scope: 'user', source: 'standard' },
      { path: join(homeDirectory, '.agents', 'skills'), scope: 'user', source: 'standard' },
      ...(projectRoot ? [
        { path: join(projectRoot, '.cursor', 'skills'), scope: 'project' as const, source: 'standard' as const },
        { path: join(projectRoot, '.agents', 'skills'), scope: 'project' as const, source: 'standard' as const }
      ] : [])
    ]
  },
  {
    id: 'geminiCli',
    name: 'Gemini CLI',
    binaryNames: ['gemini'],
    configRoot: ({ homeDirectory }) => join(homeDirectory, '.gemini'),
    files: ({ homeDirectory, projectRoot }) => [
      { path: join(homeDirectory, '.gemini', 'settings.json'), kind: 'clientConfig', scope: 'user', mcpConfig: true },
      { path: join(homeDirectory, '.gemini', 'GEMINI.md'), kind: 'instruction', scope: 'user', appliesTo: homeDirectory },
      ...(projectRoot ? [
        { path: join(projectRoot, '.gemini', 'settings.json'), kind: 'clientConfig' as const, scope: 'project' as const, mcpConfig: true }
      ] : [])
    ],
    skillRoots: ({ homeDirectory, projectRoot }) => [
      { path: join(homeDirectory, '.gemini', 'skills'), scope: 'user', source: 'standard' },
      { path: join(homeDirectory, '.agents', 'skills'), scope: 'user', source: 'standard' },
      ...(projectRoot ? [
        { path: join(projectRoot, '.gemini', 'skills'), scope: 'project' as const, source: 'standard' as const },
        { path: join(projectRoot, '.agents', 'skills'), scope: 'project' as const, source: 'standard' as const }
      ] : [])
    ]
  },
  {
    id: 'githubCopilot',
    name: 'GitHub Copilot',
    binaryNames: ['copilot'],
    configRoot: ({ homeDirectory }) => join(homeDirectory, '.copilot'),
    files: ({ homeDirectory, projectRoot }) => [
      { path: join(homeDirectory, '.copilot', 'settings.json'), kind: 'clientConfig', scope: 'user' },
      { path: join(homeDirectory, '.copilot', 'mcp-config.json'), kind: 'clientConfig', scope: 'user', mcpConfig: true },
      { path: join(homeDirectory, '.copilot', 'copilot-instructions.md'), kind: 'instruction', scope: 'user', appliesTo: homeDirectory },
      ...(projectRoot ? [
        { path: join(projectRoot, '.github', 'copilot', 'settings.json'), kind: 'clientConfig' as const, scope: 'project' as const },
        { path: join(projectRoot, '.mcp.json'), kind: 'clientConfig' as const, scope: 'project' as const, mcpConfig: true },
        { path: join(projectRoot, '.github', 'mcp.json'), kind: 'clientConfig' as const, scope: 'project' as const, mcpConfig: true },
        { path: join(projectRoot, '.vscode', 'mcp.json'), kind: 'clientConfig' as const, scope: 'project' as const, mcpConfig: true }
      ] : [])
    ],
    skillRoots: ({ homeDirectory, projectRoot }) => [
      { path: join(homeDirectory, '.copilot', 'skills'), scope: 'user', source: 'standard' },
      { path: join(homeDirectory, '.agents', 'skills'), scope: 'user', source: 'standard' },
      ...(projectRoot ? [
        { path: join(projectRoot, '.github', 'skills'), scope: 'project' as const, source: 'standard' as const },
        { path: join(projectRoot, '.agents', 'skills'), scope: 'project' as const, source: 'standard' as const },
        { path: join(projectRoot, '.claude', 'skills'), scope: 'project' as const, source: 'standard' as const }
      ] : [])
    ]
  }
])

const runtimeAdapters = new AdapterRegistry<ModelRuntimeDiscoveryAdapter>([
  {
    id: 'ollama',
    name: 'Ollama',
    binaryNames: ['ollama'],
    endpoint: ollamaEndpoint,
    protocols: ['ollama', 'openaiCompatible'],
    dataDirectory: ({ homeDirectory }) => join(homeDirectory, '.ollama')
  }
])

export class AiDiscoveryService {
  private readonly homeDirectory: string
  private readonly pathValue: string
  private readonly platform: NodeJS.Platform
  private readonly includeDefaultExecutablePaths: boolean
  private readonly fetcher: HttpFetcher
  private readonly requestTimeoutMs: number
  private readonly maxArtifacts: number

  constructor(options: DiscoveryOptions) {
    this.homeDirectory = resolve(options.homeDirectory)
    this.pathValue = options.pathValue ?? process.env.PATH ?? ''
    this.platform = options.platform ?? process.platform
    this.includeDefaultExecutablePaths = options.includeDefaultExecutablePaths ?? true
    this.fetcher = options.fetcher ?? ((url, init) => fetch(url, init))
    this.requestTimeoutMs = options.requestTimeoutMs ?? 800
    this.maxArtifacts = options.maxArtifacts ?? 2_000
  }

  async scan(input: AiDiscoveryInput = {}): Promise<AiDoctorSnapshot> {
    const diagnostics: AiDiagnostic[] = []
    const projectRoot = await this.resolveProjectRoot(input.projectRoot)
    const context: DiscoveryContext = { homeDirectory: this.homeDirectory, projectRoot }
    const artifacts: AiArtifact[] = []
    const clients: AiClientInstallation[] = []

    if (!projectRoot) {
      this.addDiagnostic(diagnostics, 'PROJECT_NOT_SELECTED', 'info', 'No project selected; only user-level configuration was scanned.')
    }

    for (const adapter of clientAdapters.list()) {
      const binaryPath = await this.findExecutable(adapter.binaryNames)
      const configRoot = adapter.configRoot(context)

      for (const candidate of adapter.files(context)) {
        await this.addFileArtifact(artifacts, diagnostics, adapter.id, candidate)
        if (candidate.kind === 'clientConfig') {
          await this.addSecurityDiagnostics(diagnostics, adapter.id, candidate.path)
          if (candidate.mcpConfig) await this.addMcpArtifacts(artifacts, diagnostics, adapter.id, candidate.path, candidate.scope)
        }
      }
      for (const skillRoot of adapter.skillRoots(context)) {
        await this.addSkillArtifacts(artifacts, diagnostics, adapter.id, skillRoot)
      }

      if (projectRoot) await this.addProjectInstructions(artifacts, diagnostics, adapter.id, projectRoot)

      const configExists = await isDirectory(configRoot)
      const artifactCount = artifacts.filter((artifact) => artifact.clientId === adapter.id).length
      const detected = Boolean(binaryPath) || configExists || artifactCount > 0
      const status = !detected ? 'missing' : binaryPath ? 'healthy' : 'warning'
      if (detected && !binaryPath) {
        this.addDiagnostic(
          diagnostics,
          'CLIENT_CONFIG_WITHOUT_BINARY',
          'warning',
          `${adapter.name} configuration exists, but its CLI executable was not found in PATH.`,
          configRoot,
          adapter.id
        )
      }
      clients.push({ id: adapter.id, name: adapter.name, detected, status, binaryPath, configRoot, artifactCount })
    }

    for (const finding of analyzeInstructions(artifacts)) {
      this.addDiagnostic(
        diagnostics,
        finding.code,
        'warning',
        finding.code === 'INSTRUCTION_DUPLICATE' ? 'An instruction file duplicates another discovered instruction.' : 'Instruction files contain potentially contradictory requirements.',
        finding.path,
        finding.clientId
      )
    }

    const runtimes = await Promise.all(runtimeAdapters.list().map((adapter) => this.scanRuntime(adapter, diagnostics)))
    const sortedArtifacts = artifacts.sort((left, right) => left.path.localeCompare(right.path) || left.name.localeCompare(right.name))
    const sortedDiagnostics = diagnostics.sort((left, right) => severityRank(right.severity) - severityRank(left.severity) || left.code.localeCompare(right.code))

    return {
      scannedAt: new Date().toISOString(),
      projectRoot,
      readOnly: true,
      clients,
      runtimes,
      artifacts: sortedArtifacts,
      diagnostics: sortedDiagnostics,
      summary: {
        detectedClients: clients.filter((client) => client.detected).length,
        detectedRuntimes: runtimes.filter((runtime) => runtime.detected).length,
        artifacts: sortedArtifacts.length,
        instructions: sortedArtifacts.filter((artifact) => artifact.kind === 'instruction').length,
        skills: sortedArtifacts.filter((artifact) => artifact.kind === 'skill').length,
        mcpServers: sortedArtifacts.filter((artifact) => artifact.kind === 'mcpServer').length,
        diagnostics: sortedDiagnostics.filter((diagnostic) => diagnostic.severity !== 'info').length
      }
    }
  }

  private async resolveProjectRoot(value?: string): Promise<string | undefined> {
    const requested = value?.trim()
    if (!requested) return undefined
    const resolved = await realpath(resolve(requested))
    const info = await stat(resolved)
    if (!info.isDirectory()) throw new Error('AI project root must be a directory')
    return resolved
  }

  private async findExecutable(names: string[]): Promise<string | undefined> {
    const searchDirectories = new Set(this.pathValue.split(delimiter).filter(Boolean).map((entry) => resolve(entry)))
    if (this.includeDefaultExecutablePaths) {
      searchDirectories.add(join(this.homeDirectory, '.local', 'bin'))
      searchDirectories.add('/usr/local/bin')
      searchDirectories.add('/opt/homebrew/bin')
    }

    const candidates: string[] = []
    for (const directory of searchDirectories) {
      for (const name of names) {
        candidates.push(join(directory, name))
        if (this.platform === 'win32') candidates.push(join(directory, `${name}.exe`), join(directory, `${name}.cmd`))
      }
    }
    if (this.includeDefaultExecutablePaths && names.includes('ollama') && this.platform === 'darwin') {
      candidates.push('/Applications/Ollama.app/Contents/Resources/ollama')
    }

    for (const candidate of candidates) {
      try {
        const info = await stat(candidate)
        if (!info.isFile()) continue
        if (this.platform !== 'win32') await access(candidate, constants.X_OK)
        return candidate
      } catch {
        // Missing or non-executable candidates are expected during discovery.
      }
    }
    return undefined
  }

  private async addFileArtifact(
    artifacts: AiArtifact[],
    diagnostics: AiDiagnostic[],
    clientId: AiClientId,
    candidate: FileCandidate
  ): Promise<void> {
    if (artifacts.length >= this.maxArtifacts) {
      this.addDiagnostic(diagnostics, 'SCAN_LIMIT_REACHED', 'warning', 'The AI artifact scan limit was reached.')
      return
    }
    try {
      const linkInfo = await lstat(candidate.path)
      if (linkInfo.isSymbolicLink()) {
        this.addDiagnostic(diagnostics, 'SYMLINK_SKIPPED', 'warning', 'A symbolic link was skipped during read-only discovery.', candidate.path, clientId)
        return
      }
      if (!linkInfo.isFile()) return
      let metadata: AiArtifact['metadata']
      if (candidate.kind === 'instruction') {
        const inspection = await inspectInstruction(candidate.path, candidate.appliesTo ?? dirname(candidate.path))
        metadata = inspection.metadata
        if (inspection.tooLarge) this.addDiagnostic(diagnostics, 'INSTRUCTION_TOO_LARGE', 'warning', 'An instruction file exceeds the recommended context budget.', candidate.path, clientId)
      }
      artifacts.push(createArtifact(clientId, candidate.kind, candidate.scope, basename(candidate.path), candidate.path, candidate.source ?? 'standard', linkInfo, metadata))
    } catch (error) {
      if (isMissing(error)) return
      this.addDiagnostic(diagnostics, 'UNREADABLE_PATH', 'warning', 'A configuration path could not be read.', candidate.path, clientId)
    }
  }

  private async addSkillArtifacts(
    artifacts: AiArtifact[],
    diagnostics: AiDiagnostic[],
    clientId: AiClientId,
    root: SkillRoot
  ): Promise<void> {
    let rootInfo
    try {
      rootInfo = await lstat(root.path)
    } catch (error) {
      if (isMissing(error)) return
      this.addDiagnostic(diagnostics, 'UNREADABLE_PATH', 'warning', 'A skill root could not be read.', root.path, clientId)
      return
    }
    if (rootInfo.isSymbolicLink()) {
      this.addDiagnostic(diagnostics, 'SYMLINK_SKIPPED', 'warning', 'A symbolic-link skill root was skipped.', root.path, clientId)
      return
    }
    if (!rootInfo.isDirectory()) return

    const queue: Array<{ directory: string; depth: number }> = [{ directory: root.path, depth: 0 }]
    while (queue.length > 0 && artifacts.length < this.maxArtifacts) {
      const current = queue.shift()!
      let entries
      try {
        entries = await readdir(current.directory, { withFileTypes: true })
      } catch {
        this.addDiagnostic(diagnostics, 'UNREADABLE_PATH', 'warning', 'A skill directory could not be read.', current.directory, clientId)
        continue
      }
      const entry = entries.find((candidate) => candidate.name === 'SKILL.md' && candidate.isFile())
      if (entry && current.directory !== root.path) {
        const info = await stat(join(current.directory, entry.name))
        const name = relative(root.path, current.directory).split(sep).join('/')
        try {
          const inspection = await inspectSkillPackage(current.directory)
          artifacts.push(createArtifact(clientId, 'skill', root.scope, name, current.directory, root.source, info, inspection.metadata))
          for (const finding of inspection.findings) {
            this.addDiagnostic(diagnostics, finding.code, finding.severity, skillFindingMessage(finding.code), finding.path, clientId)
          }
        } catch {
          this.addDiagnostic(diagnostics, 'SKILL_ENTRY_TOO_LARGE', 'warning', 'A Skill entry could not be inspected within the safe size limit.', join(current.directory, entry.name), clientId)
        }
        continue
      }
      if (current.depth >= maxSkillDepth) {
        if (current.directory !== root.path && entries.some((candidate) => candidate.isFile())) {
          this.addDiagnostic(diagnostics, 'SKILL_MISSING_ENTRY', 'warning', 'A skill directory has no SKILL.md entry.', current.directory, clientId)
        }
        continue
      }
      for (const child of entries) {
        if (child.name.startsWith('.') && child.name !== '.system') continue
        const childPath = join(current.directory, child.name)
        if (child.isSymbolicLink()) {
          this.addDiagnostic(diagnostics, 'SYMLINK_SKIPPED', 'warning', 'A symbolic-link skill directory was skipped.', childPath, clientId)
        } else if (child.isDirectory()) {
          queue.push({ directory: childPath, depth: current.depth + 1 })
        }
      }
    }
    if (artifacts.length >= this.maxArtifacts) {
      this.addDiagnostic(diagnostics, 'SCAN_LIMIT_REACHED', 'warning', 'The AI artifact scan limit was reached.', root.path, clientId)
    }
  }

  private async addProjectInstructions(
    artifacts: AiArtifact[],
    diagnostics: AiDiagnostic[],
    clientId: AiClientId,
    projectRoot: string
  ): Promise<void> {
    const queue: Array<{ directory: string; depth: number }> = [{ directory: projectRoot, depth: 0 }]
    while (queue.length > 0 && artifacts.length < this.maxArtifacts) {
      const current = queue.shift()!
      let entries
      try {
        entries = await readdir(current.directory, { withFileTypes: true })
      } catch {
        this.addDiagnostic(diagnostics, 'UNREADABLE_PATH', 'warning', 'A project directory could not be read.', current.directory, clientId)
        continue
      }
      for (const entry of entries) {
        if (artifacts.length >= this.maxArtifacts) break
        const path = join(current.directory, entry.name)
        if (entry.isSymbolicLink()) continue
        if (entry.isDirectory()) {
          if (current.depth < maxProjectDepth && !ignoredProjectDirectories.has(entry.name)) queue.push({ directory: path, depth: current.depth + 1 })
          continue
        }
        if (!entry.isFile()) continue
        const instruction = matchProjectInstruction(clientId, projectRoot, current.directory, path, entry.name)
        if (!instruction) continue
        const info = await stat(path)
        try {
          const inspection = await inspectInstruction(path, instruction.appliesTo)
          artifacts.push(createArtifact(clientId, 'instruction', 'project', relative(projectRoot, path), path, 'standard', info, inspection.metadata))
          if (inspection.tooLarge) this.addDiagnostic(diagnostics, 'INSTRUCTION_TOO_LARGE', 'warning', 'An instruction file exceeds the recommended context budget.', path, clientId)
        } catch {
          this.addDiagnostic(diagnostics, 'INSTRUCTION_TOO_LARGE', 'warning', 'An instruction file could not be inspected within the safe size limit.', path, clientId)
        }
      }
    }
    if (artifacts.length >= this.maxArtifacts) {
      this.addDiagnostic(diagnostics, 'SCAN_LIMIT_REACHED', 'warning', 'The AI artifact scan limit was reached.', projectRoot, clientId)
    }
  }

  private async addMcpArtifacts(
    artifacts: AiArtifact[],
    diagnostics: AiDiagnostic[],
    clientId: AiClientId,
    path: string,
    scope: AiScope
  ): Promise<void> {
    let content: string
    try {
      const info = await stat(path)
      if (!info.isFile() || info.size > maxConfigBytes) return
      content = await readFile(path, 'utf8')
    } catch (error) {
      if (!isMissing(error)) this.addDiagnostic(diagnostics, 'UNREADABLE_PATH', 'warning', 'An MCP configuration could not be read.', path, clientId)
      return
    }

    let serverNames: string[] = []
    try {
      if (extname(path).toLowerCase() === '.toml') {
        serverNames = [...content.matchAll(/^\s*\[mcp_servers\.([^\]]+)]\s*$/gm)].map((match) => unquoteTomlKey(match[1]))
      } else {
        const parsed = JSON.parse(content) as unknown
        serverNames = extractMcpServerNames(parsed)
      }
    } catch {
      this.addDiagnostic(diagnostics, 'MCP_CONFIG_INVALID', 'warning', 'An MCP configuration could not be parsed.', path, clientId)
      return
    }

    for (const serverName of [...new Set(serverNames)].slice(0, 500)) {
      artifacts.push({
        id: stableId(clientId, 'mcpServer', path, serverName),
        clientId,
        kind: 'mcpServer',
        scope,
        name: serverName.slice(0, 200),
        path,
        source: 'standard'
      })
    }
  }

  private async addSecurityDiagnostics(diagnostics: AiDiagnostic[], clientId: AiClientId, path: string): Promise<void> {
    try {
      const info = await stat(path)
      if (!info.isFile() || info.size > maxConfigBytes) return
      if (scanSensitiveContent(await readFile(path, 'utf8')).length > 0) {
        this.addDiagnostic(diagnostics, 'PLAINTEXT_SECRET_RISK', 'warning', 'A configuration file may contain a plaintext credential.', path, clientId)
      }
    } catch (error) {
      if (!isMissing(error)) this.addDiagnostic(diagnostics, 'UNREADABLE_PATH', 'warning', 'A configuration file could not be inspected safely.', path, clientId)
    }
  }

  private async scanRuntime(adapter: ModelRuntimeDiscoveryAdapter, diagnostics: AiDiagnostic[]): Promise<AiRuntimeInstallation> {
    if (adapter.id !== 'ollama') throw new Error(`Unsupported model runtime adapter: ${adapter.id}`)
    const binaryPath = await this.findExecutable(adapter.binaryNames)
    const dataDirectoryExists = await isDirectory(adapter.dataDirectory({ homeDirectory: this.homeDirectory }))
    let version: string | undefined
    let models: AiLocalModel[] = []
    let apiHealthy = false

    try {
      const versionPayload = await this.fetchJson(`${adapter.endpoint}/api/version`)
      version = readStringProperty(versionPayload, 'version')
      apiHealthy = true
      const [tagsPayload, runningPayload] = await Promise.all([
        this.fetchJson(`${adapter.endpoint}/api/tags`).catch(() => undefined),
        this.fetchJson(`${adapter.endpoint}/api/ps`).catch(() => undefined)
      ])
      models = normalizeOllamaModels(tagsPayload, runningPayload)
    } catch {
      // A stopped or absent Ollama server is a normal discovery result.
    }

    const detected = Boolean(binaryPath) || dataDirectoryExists || apiHealthy
    const status = apiHealthy ? 'healthy' : detected ? 'warning' : 'missing'
    if (detected && !apiHealthy) {
      this.addDiagnostic(diagnostics, 'OLLAMA_NOT_RUNNING', 'warning', 'Ollama was detected, but its loopback API is not responding.', undefined, undefined, 'ollama')
    }
    return { id: adapter.id, name: adapter.name, detected, status, binaryPath, endpoint: adapter.endpoint, protocols: adapter.protocols, version, models }
  }

  private async fetchJson(url: string): Promise<unknown> {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), this.requestTimeoutMs)
    try {
      const response = await this.fetcher(url, { signal: controller.signal })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      return await response.json()
    } finally {
      clearTimeout(timer)
    }
  }

  private addDiagnostic(
    diagnostics: AiDiagnostic[],
    code: AiDiagnosticCode,
    severity: AiDiagnosticSeverity,
    message: string,
    path?: string,
    clientId?: AiClientId,
    runtimeId?: 'ollama'
  ): void {
    if (diagnostics.length >= maxDiagnostics) return
    diagnostics.push({ id: stableId(code, path ?? '', clientId ?? '', runtimeId ?? ''), code, severity, message, path, clientId, runtimeId })
  }
}

function createArtifact(
  clientId: AiClientId,
  kind: AiArtifactKind,
  scope: AiScope,
  name: string,
  path: string,
  source: AiArtifact['source'],
  info: { size: number; mtime: Date },
  metadata?: AiArtifact['metadata']
): AiArtifact {
  return {
    id: stableId(clientId, kind, path, name),
    clientId,
    kind,
    scope,
    name,
    path,
    source,
    sizeBytes: info.size,
    modifiedAt: info.mtime.toISOString(),
    metadata
  }
}

function extractMcpServerNames(value: unknown): string[] {
  if (!isRecord(value)) return []
  const direct = isRecord(value.mcpServers) ? Object.keys(value.mcpServers) : []
  const vscode = isRecord(value.servers) ? Object.keys(value.servers) : []
  const projects = isRecord(value.projects)
    ? Object.values(value.projects).flatMap((project) => isRecord(project) && isRecord(project.mcpServers) ? Object.keys(project.mcpServers) : [])
    : []
  return [...direct, ...vscode, ...projects].filter((name) => name.trim()).map((name) => name.trim())
}

function matchProjectInstruction(
  clientId: AiClientId,
  projectRoot: string,
  directory: string,
  path: string,
  name: string
): { appliesTo: string } | undefined {
  if (clientId === 'codex' && name === 'AGENTS.md') return { appliesTo: directory }
  if (clientId === 'claudeCode' && (
    name === 'CLAUDE.md'
    || name === 'CLAUDE.local.md'
    || (path.includes(`${sep}.claude${sep}rules${sep}`) && extname(name).toLowerCase() === '.md')
  )) return { appliesTo: projectRoot }
  if (clientId === 'cursor') {
    const isRule = path.includes(`${sep}.cursor${sep}rules${sep}`) && ['.md', '.mdc'].includes(extname(name).toLowerCase())
    const isRootCompatibilityFile = directory === projectRoot && (name === 'AGENTS.md' || name === 'CLAUDE.md')
    if (isRule || isRootCompatibilityFile) return { appliesTo: projectRoot }
  }
  if (clientId === 'geminiCli' && name === 'GEMINI.md') return { appliesTo: directory }
  if (clientId === 'githubCopilot') {
    if (name === 'AGENTS.md') return { appliesTo: directory }
    if (path === join(projectRoot, '.github', 'copilot-instructions.md')) return { appliesTo: projectRoot }
    if (path.includes(`${sep}.github${sep}instructions${sep}`) && name.endsWith('.instructions.md')) return { appliesTo: projectRoot }
  }
  return undefined
}

function normalizeOllamaModels(tagsPayload: unknown, runningPayload: unknown): AiLocalModel[] {
  const runningModels = readModelArray(runningPayload)
  const runningByDigest = new Map(runningModels.map((model) => [readStringProperty(model, 'digest'), model]))
  const runningByName = new Map(runningModels.map((model) => [readStringProperty(model, 'name') || readStringProperty(model, 'model'), model]))
  return readModelArray(tagsPayload).slice(0, 1_000).map((model) => {
    const name = readStringProperty(model, 'name') || readStringProperty(model, 'model') || 'unknown'
    const digest = readStringProperty(model, 'digest')
    const running = runningByDigest.get(digest) ?? runningByName.get(name)
    const details = isRecord(model.details) ? model.details : {}
    return {
      name,
      digest,
      sizeBytes: readNumberProperty(model, 'size') ?? 0,
      parameterSize: readStringProperty(details, 'parameter_size') || undefined,
      quantization: readStringProperty(details, 'quantization_level') || undefined,
      family: readStringProperty(details, 'family') || undefined,
      running: Boolean(running),
      contextLength: running ? readNumberProperty(running, 'context_length') : undefined
    }
  })
}

function readModelArray(value: unknown): Record<string, unknown>[] {
  if (!isRecord(value) || !Array.isArray(value.models)) return []
  return value.models.filter(isRecord)
}

function readStringProperty(value: unknown, key: string): string {
  return isRecord(value) && typeof value[key] === 'string' ? value[key].slice(0, 500) : ''
}

function readNumberProperty(value: unknown, key: string): number | undefined {
  if (!isRecord(value)) return undefined
  const candidate = value[key]
  return typeof candidate === 'number' && Number.isFinite(candidate) && candidate >= 0 ? candidate : undefined
}

function unquoteTomlKey(value: string): string {
  const trimmed = value.trim()
  if ((trimmed.startsWith('"') && trimmed.endsWith('"')) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
    return trimmed.slice(1, -1)
  }
  return trimmed
}

function stableId(...parts: string[]): string {
  return createHash('sha256').update(parts.join('\0')).digest('hex').slice(0, 24)
}

function severityRank(value: AiDiagnosticSeverity): number {
  return value === 'error' ? 2 : value === 'warning' ? 1 : 0
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isMissing(error: unknown): boolean {
  return isRecord(error) && error.code === 'ENOENT'
}

function skillFindingMessage(code: 'SKILL_ENTRY_INVALID' | 'SKILL_REFERENCE_MISSING' | 'SKILL_DANGEROUS_PATTERN' | 'SKILL_ENTRY_TOO_LARGE' | 'PLAINTEXT_SECRET_RISK'): string {
  if (code === 'SKILL_ENTRY_INVALID') return 'A Skill entry has invalid or incomplete frontmatter.'
  if (code === 'SKILL_REFERENCE_MISSING') return 'A Skill references a missing or unsafe local resource.'
  if (code === 'SKILL_DANGEROUS_PATTERN') return 'A Skill script contains a command pattern that requires manual review.'
  if (code === 'SKILL_ENTRY_TOO_LARGE') return 'A Skill entry exceeds the recommended context size.'
  return 'A Skill may contain a plaintext credential.'
}

async function isDirectory(path: string): Promise<boolean> {
  try {
    return (await stat(path)).isDirectory()
  } catch {
    return false
  }
}

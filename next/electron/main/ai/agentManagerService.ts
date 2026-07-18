import { createHash } from 'node:crypto'
import { realpath, stat } from 'node:fs/promises'
import type {
  AiAgentCapabilityId,
  AiAgentClientCapability,
  AiAgentLaunchPlan,
  AiAgentManagerInput,
  AiAgentManagerSnapshot
} from '../../../src/shared/contracts/aiAgents'
import { aiPrimaryClientIds, type AiArtifact, type AiClientId } from '../../../src/shared/contracts/ai'
import { AiDiscoveryService } from './discoveryService'
import { AiAgentProfileRepository } from './agentProfileRepository'

type AgentManagerServiceOptions = {
  discovery: AiDiscoveryService
  repository: AiAgentProfileRepository
  platform?: NodeJS.Platform
}

const capabilityIds: AiAgentCapabilityId[] = ['instructions', 'skills', 'mcp', 'subagents', 'hooks', 'structuredOutput', 'usage', 'permissionModes']

const capabilityMatrix: Record<AiClientId, Partial<Record<AiAgentCapabilityId, AiAgentClientCapability['support']>>> = {
  codex: {
    instructions: 'full', skills: 'full', mcp: 'full', subagents: 'full', hooks: 'full',
    structuredOutput: 'partial', usage: 'partial', permissionModes: 'full'
  },
  claudeCode: {
    instructions: 'full', skills: 'full', mcp: 'full', subagents: 'full', hooks: 'full',
    structuredOutput: 'partial', usage: 'partial', permissionModes: 'full'
  },
  cursor: {
    instructions: 'full', skills: 'full', mcp: 'full', subagents: 'full', hooks: 'full',
    structuredOutput: 'full', usage: 'partial', permissionModes: 'full'
  },
  geminiCli: {
    instructions: 'full', skills: 'full', mcp: 'full', subagents: 'full', hooks: 'full',
    structuredOutput: 'full', usage: 'partial', permissionModes: 'full'
  },
  githubCopilot: {
    instructions: 'full', skills: 'full', mcp: 'full', subagents: 'full', hooks: 'full',
    structuredOutput: 'partial', usage: 'partial', permissionModes: 'full'
  }
}

export class AgentManagerService {
  private readonly discovery: AiDiscoveryService
  private readonly repository: AiAgentProfileRepository
  private readonly platform: NodeJS.Platform

  constructor(options: AgentManagerServiceOptions) {
    this.discovery = options.discovery
    this.repository = options.repository
    this.platform = options.platform ?? process.platform
  }

  async snapshot(input: AiAgentManagerInput = {}): Promise<AiAgentManagerSnapshot> {
    const discovered = await this.discovery.scan(input)
    return {
      scannedAt: discovered.scannedAt,
      ...(discovered.projectRoot ? { projectRoot: discovered.projectRoot } : {}),
      clients: discovered.clients.map((client) => {
        const artifacts = discovered.artifacts.filter((artifact) => artifact.clientId === client.id)
        const configurationFingerprint = fingerprintArtifacts(artifacts)
        const observation = aiPrimaryClientIds.includes(client.id as (typeof aiPrimaryClientIds)[number])
          ? this.repository.observeConfiguration(discovered.projectRoot ?? '<user>', client.id as (typeof aiPrimaryClientIds)[number], configurationFingerprint)
          : { changed: false }
        const diagnostics = discovered.diagnostics
          .filter((diagnostic) => diagnostic.clientId === client.id)
          .map((diagnostic) => `${diagnostic.code}: ${diagnostic.message}`)
        return {
          id: client.id,
          name: client.name,
          detected: client.detected,
          health: client.status === 'healthy' ? 'healthy' : client.status === 'missing' ? 'missing' : 'warning',
          ...(client.binaryPath ? { binaryPath: client.binaryPath } : {}),
          configRoot: client.configRoot,
          artifactCount: client.artifactCount,
          configurationFingerprint,
          ...(observation.previousFingerprint ? { previousConfigurationFingerprint: observation.previousFingerprint } : {}),
          configurationChanged: observation.changed,
          capabilities: capabilityIds.map((id) => ({ id, support: capabilityMatrix[client.id][id] ?? 'none' })),
          diagnostics
        }
      }),
      profiles: this.repository.list()
    }
  }

  async launchPlan(profileId: string): Promise<AiAgentLaunchPlan> {
    const profile = this.repository.getRequired(profileId)
    const workingDirectory = await realpath(profile.workingDirectory)
    const info = await stat(workingDirectory)
    if (!info.isDirectory()) throw new Error('Agent Profile working directory is not a directory')

    const discovered = await this.discovery.scan({ projectRoot: workingDirectory })
    const client = discovered.clients.find((candidate) => candidate.id === profile.clientId)
    if (!client?.binaryPath) throw new Error(`${profile.clientId === 'codex' ? 'Codex' : 'Claude Code'} CLI executable was not found`)

    const args = buildArgs(profile)
    const warnings: string[] = []
    if (['workspaceWrite', 'acceptEdits', 'dontAsk'].includes(profile.permissionMode)) {
      warnings.push('This profile permits the client to make changes; review the generated command and client prompts before use.')
    }
    if (profile.environmentVariableRefs.length > 0) {
      warnings.push('Environment variable values are intentionally omitted. Set the named variables in your shell or secret manager.')
    }
    if (profile.mcpServerNames.length > 0 || profile.skillNames.length > 0) {
      warnings.push('Selected MCP servers and skills are configuration dependencies; the launch command does not inject or modify them.')
    }
    if (profile.modelRuntimeId) {
      warnings.push('The model runtime binding is declarative. Confirm the client Provider endpoint is configured for the selected runtime before using this command.')
    }

    return {
      profileId: profile.id,
      clientId: profile.clientId,
      executable: client.binaryPath,
      args,
      workingDirectory,
      requiredEnvironmentVariables: profile.environmentVariableRefs,
      displayCommand: `cd ${shellQuote(workingDirectory, this.platform)} && ${[client.binaryPath, ...args].map((value) => shellQuote(value, this.platform)).join(' ')}`,
      executes: false,
      warnings
    }
  }
}

function buildArgs(profile: ReturnType<AiAgentProfileRepository['getRequired']>): string[] {
  const args: string[] = []
  if (profile.model) args.push('--model', profile.model)
  if (profile.clientId === 'codex') {
    if (profile.configProfile) args.push('--profile', profile.configProfile)
    if (profile.permissionMode === 'readOnly') args.push('--sandbox', 'read-only')
    if (profile.permissionMode === 'workspaceWrite') args.push('--sandbox', 'workspace-write')
  } else if (profile.permissionMode !== 'default') {
    args.push('--permission-mode', profile.permissionMode)
  }
  args.push(...profile.optionalFlags)
  return args
}

function fingerprintArtifacts(artifacts: AiArtifact[]): string {
  const metadata = artifacts
    .map((artifact) => ({
      path: artifact.path,
      kind: artifact.kind,
      scope: artifact.scope,
      sizeBytes: artifact.sizeBytes ?? null,
      modifiedAt: artifact.modifiedAt ?? null
    }))
    .sort((left, right) => left.path.localeCompare(right.path) || left.kind.localeCompare(right.kind))
  return `sha256:${createHash('sha256').update(JSON.stringify(metadata)).digest('hex')}`
}

function shellQuote(value: string, platform: NodeJS.Platform): string {
  if (platform === 'win32') return `"${value.replace(/(\\*)"/g, '$1$1\\"').replace(/(\\+)$/g, '$1$1')}"`
  return `'${value.replace(/'/g, `'"'"'`)}'`
}

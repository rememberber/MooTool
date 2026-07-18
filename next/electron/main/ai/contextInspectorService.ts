import { realpath } from 'node:fs/promises'
import { resolve } from 'node:path'
import type { AiArtifact, AiClientId } from '../../../src/shared/contracts/ai'
import {
  aiContextCategories,
  aiContextLayers,
  type AiContextDuplicateGroup,
  type AiContextInspectorInput,
  type AiContextInspectorSnapshot,
  type AiContextItem,
  type AiContextRecommendation
} from '../../../src/shared/contracts/aiContext'
import { AiAgentProfileRepository } from './agentProfileRepository'
import { AiDiscoveryService } from './discoveryService'
import { InstructionScopeService } from './instructionScopeService'
import { AiMemoryRepository } from './memoryRepository'
import { McpService } from './mcpService'

type ContextInspectorServiceOptions = {
  discovery: AiDiscoveryService
  instructions: InstructionScopeService
  memories: AiMemoryRepository
  mcp: McpService
  profiles: AiAgentProfileRepository
  clock?: () => Date
}

type FingerprintedItem = AiContextItem & { fingerprint?: string }

export class ContextInspectorService {
  private readonly clock: () => Date

  constructor(private readonly options: ContextInspectorServiceOptions) {
    this.clock = options.clock ?? (() => new Date())
  }

  async inspect(input: AiContextInspectorInput): Promise<AiContextInspectorSnapshot> {
    const projectRoot = await realpath(resolve(input.projectRoot))
    const targetPath = await realpath(resolve(input.targetPath))
    const profile = input.agentProfileId ? this.options.profiles.getRequired(input.agentProfileId) : undefined
    const profileWorkingDirectory = profile ? await realpath(resolve(profile.workingDirectory)).catch(() => resolve(profile.workingDirectory)) : undefined
    const clientId = profile?.clientId ?? input.clientId
    const [instructionPreview, discovery, mcpInventory] = await Promise.all([
      this.options.instructions.preview({ projectRoot, targetPath, clientId }),
      this.options.discovery.scan({ projectRoot }),
      this.options.mcp.inventory({ projectRoot })
    ])

    const items: FingerprintedItem[] = []
    const artifactsById = new Map(discovery.artifacts.map((artifact) => [artifact.id, artifact]))
    for (const instruction of instructionPreview.instructions) {
      const artifact = artifactsById.get(instruction.artifactId)
      items.push({
        id: `instruction:${instruction.artifactId}`,
        category: 'instruction',
        layer: instruction.reason === 'userScope' ? 'resident' : 'pathTriggered',
        name: instruction.name,
        clientId: instruction.clientId,
        sourcePath: instruction.path,
        sourceToolId: 'instructionManager',
        estimatedTokens: instruction.estimatedTokens,
        reason: instruction.reason,
        fingerprint: metadataString(artifact, 'contentDigest')
      })
    }

    const allowedSkills = new Set(profile?.skillNames ?? [])
    const selectedSkills = new Set(input.selectedSkillNames)
    const skillArtifacts = uniqueArtifacts(discovery.artifacts.filter((artifact) =>
      artifact.kind === 'skill' && artifact.clientId === clientId && (allowedSkills.size === 0 || allowedSkills.has(skillName(artifact)))
    ))
    for (const skill of skillArtifacts) {
      const name = skillName(skill)
      const metadataTokens = estimateTokens(`${name}\n${metadataString(skill, 'description')}`)
      const fullTokens = metadataNumber(skill, 'estimatedTokens')
      items.push({
        id: `skill-metadata:${skill.id}`,
        category: 'skillMetadata',
        layer: 'resident',
        name,
        clientId,
        sourcePath: metadataString(skill, 'entryPath') || skill.path,
        sourceToolId: 'skillManager',
        estimatedTokens: metadataTokens,
        reason: 'Skill name and description available for discovery'
      })
      if (selectedSkills.has(name) || selectedSkills.has(skill.name)) {
        items.push({
          id: `skill-body:${skill.id}`,
          category: 'skillBody',
          layer: 'onDemand',
          name,
          clientId,
          sourcePath: metadataString(skill, 'entryPath') || skill.path,
          sourceToolId: 'skillManager',
          estimatedTokens: Math.max(0, fullTokens - metadataTokens),
          reason: 'Selected on-demand Skill body',
          fingerprint: metadataString(skill, 'contentDigest')
        })
      }
    }

    const memoryPreview = this.options.memories.preview({
      projectRoot: resolve(input.projectRoot),
      targetPath: resolve(input.targetPath),
      ...(input.branch ? { branch: input.branch } : {}),
      ...(profile ? { agentProfileId: profile.id } : {}),
      ...(input.taskRef ? { taskRef: input.taskRef } : {}),
      tokenBudget: input.memoryTokenBudget,
      maxItems: input.maxMemoryItems
    })
    for (const memory of memoryPreview.memories) {
      items.push({
        id: `memory:${memory.memory.id}`,
        category: 'memory',
        layer: 'onDemand',
        name: memory.memory.content.slice(0, 80),
        sourceToolId: 'agentMemoryManager',
        estimatedTokens: memory.estimatedTokens,
        reason: memory.reason,
        fingerprint: memory.memory.fingerprint
      })
    }

    const allowedMcp = new Set(profile?.mcpServerNames ?? [])
    const servers = mcpInventory.servers.filter((server) => server.clientId === clientId && server.enabled && (allowedMcp.size === 0 || allowedMcp.has(server.name)))
    const schemaSnapshots = this.options.mcp.contextSchemaSnapshots(servers.map((server) => server.id))
    const schemasByServer = new Map(schemaSnapshots.map((snapshot) => [snapshot.serverId, snapshot]))
    for (const server of servers) {
      const schema = schemasByServer.get(server.id)
      for (const tool of schema?.toolSchemas ?? []) {
        items.push({
          id: `mcp-schema:${server.id}:${tool.name}`,
          category: 'mcpSchema',
          layer: 'resident',
          name: `${server.name} / ${tool.name}`,
          clientId,
          sourcePath: server.configPath,
          sourceToolId: 'mcpManager',
          estimatedTokens: tool.estimatedTokens,
          reason: `Observed by explicit MCP capability probe at ${schema!.observedAt}`
        })
      }
    }

    const duplicates = duplicateGroups(items)
    const publicItems = items.map(({ fingerprint: _fingerprint, ...item }) => item)
    const totals = {
      estimatedTokens: publicItems.reduce((sum, item) => sum + item.estimatedTokens, 0),
      residentTokens: layerTokens(publicItems, 'resident'),
      pathTriggeredTokens: layerTokens(publicItems, 'pathTriggered'),
      onDemandTokens: layerTokens(publicItems, 'onDemand'),
      runtimeTokens: layerTokens(publicItems, 'runtime')
    }
    const unknownServers = servers.filter((server) => !schemasByServer.has(server.id)).map((server) => server.name)
    const recommendations = buildRecommendations(items, duplicates, totals.residentTokens, unknownServers, memoryPreview.omittedByBudget, profileWorkingDirectory, projectRoot)

    return {
      inspectedAt: this.clock().toISOString(),
      projectRoot,
      targetPath,
      clientId,
      ...(profile ? { agentProfileId: profile.id } : {}),
      items: publicItems.sort((left, right) => layerOrder(left.layer) - layerOrder(right.layer) || right.estimatedTokens - left.estimatedTokens || left.name.localeCompare(right.name)),
      topItems: [...publicItems].sort((left, right) => right.estimatedTokens - left.estimatedTokens || left.name.localeCompare(right.name)).slice(0, input.topN),
      breakdown: aiContextCategories.map((category) => ({
        category,
        items: publicItems.filter((item) => item.category === category).length,
        estimatedTokens: publicItems.filter((item) => item.category === category).reduce((sum, item) => sum + item.estimatedTokens, 0)
      })),
      totals,
      duplicates,
      recommendations,
      mcpSchemas: { servers: servers.length, observedServers: schemaSnapshots.length, unknownServers },
      memory: { selected: memoryPreview.memories.length, omittedByBudget: memoryPreview.omittedByBudget, budgetTokens: input.memoryTokenBudget },
      tokenizer: {
        id: 'heuristic-v1',
        label: 'Unicode-aware character heuristic',
        relativeErrorNotice: 'Token counts are estimates for relative comparison. Actual totals vary by model tokenizer and client serialization.'
      }
    }
  }
}

function buildRecommendations(
  items: FingerprintedItem[],
  duplicates: AiContextDuplicateGroup[],
  residentTokens: number,
  unknownServers: string[],
  omittedMemories: number,
  profileDirectory: string | undefined,
  projectRoot: string
): AiContextRecommendation[] {
  const recommendations: AiContextRecommendation[] = []
  if (residentTokens > 8_000) recommendations.push({ code: 'largeResidentContext', severity: 'warning', sourceToolId: 'instructionManager', message: `Resident context is approximately ${residentTokens} tokens.` })
  for (const item of items.filter((candidate) => candidate.category === 'instruction' && candidate.estimatedTokens > 4_000)) {
    recommendations.push({ code: 'largeInstruction', severity: 'suggestion', sourceToolId: 'instructionManager', itemId: item.id, message: `${item.name} contributes approximately ${item.estimatedTokens} tokens.` })
  }
  for (const item of items.filter((candidate) => candidate.category === 'skillBody' && candidate.estimatedTokens > 8_000)) {
    recommendations.push({ code: 'largeSkillEntry', severity: 'suggestion', sourceToolId: 'skillManager', itemId: item.id, message: `${item.name} should move optional detail into references.` })
  }
  if (duplicates.length > 0) recommendations.push({ code: 'duplicateContent', severity: 'suggestion', sourceToolId: duplicates[0].category === 'instruction' ? 'instructionManager' : duplicates[0].category === 'memory' ? 'agentMemoryManager' : 'skillManager', message: `${duplicates.length} exact duplicate context group(s) were found.` })
  if (unknownServers.length > 0) recommendations.push({ code: 'unprobedMcp', severity: 'info', sourceToolId: 'mcpManager', message: `Tool Schema size is unknown for: ${unknownServers.join(', ')}. Run an explicit MCP capability check first.` })
  if (omittedMemories > 0) recommendations.push({ code: 'memoryBudgetExceeded', severity: 'info', sourceToolId: 'agentMemoryManager', message: `${omittedMemories} matching memories were omitted by the configured budget.` })
  if (profileDirectory && resolve(profileDirectory) !== resolve(projectRoot)) recommendations.push({ code: 'profileProjectMismatch', severity: 'warning', sourceToolId: 'agentManager', message: 'The selected Agent Profile working directory differs from this project.' })
  return recommendations
}

function duplicateGroups(items: FingerprintedItem[]): AiContextDuplicateGroup[] {
  const groups = new Map<string, FingerprintedItem[]>()
  for (const item of items) {
    if (!item.fingerprint || !['instruction', 'skillBody', 'memory'].includes(item.category)) continue
    const key = `${item.category}:${item.fingerprint}`
    groups.set(key, [...(groups.get(key) ?? []), item])
  }
  return [...groups.values()].filter((group) => new Set(group.map((item) => item.sourcePath ?? item.id)).size > 1).map((group) => ({
    category: group[0].category as AiContextDuplicateGroup['category'],
    itemIds: group.map((item) => item.id),
    names: group.map((item) => item.name),
    estimatedWasteTokens: group.slice(1).reduce((sum, item) => sum + item.estimatedTokens, 0)
  }))
}

function uniqueArtifacts(artifacts: AiArtifact[]): AiArtifact[] {
  return [...new Map(artifacts.map((artifact) => [artifact.path, artifact])).values()]
}

function skillName(artifact: AiArtifact): string {
  return metadataString(artifact, 'declaredName') || artifact.name
}

function metadataString(artifact: AiArtifact | undefined, key: string): string {
  const value = artifact?.metadata?.[key]
  return typeof value === 'string' ? value : ''
}

function metadataNumber(artifact: AiArtifact, key: string): number {
  const value = artifact.metadata?.[key]
  return typeof value === 'number' ? value : 0
}

function estimateTokens(value: string): number {
  const cjk = (value.match(/[\u3400-\u9fff\uf900-\ufaff]/g) ?? []).length
  return Math.max(1, Math.ceil((value.length - cjk) / 4 + cjk))
}

function layerTokens(items: AiContextItem[], layer: AiContextItem['layer']): number {
  return items.filter((item) => item.layer === layer).reduce((sum, item) => sum + item.estimatedTokens, 0)
}

function layerOrder(layer: AiContextItem['layer']): number {
  return aiContextLayers.indexOf(layer)
}

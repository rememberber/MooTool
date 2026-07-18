import { randomUUID } from 'node:crypto'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { createServer, type Server } from 'node:http'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { AiAgentProfileRepository } from '../../electron/main/ai/agentProfileRepository'
import { ConfigChangeService, type SnapshotProtector } from '../../electron/main/ai/configChangeService'
import { ContextInspectorService } from '../../electron/main/ai/contextInspectorService'
import { AiDiscoveryService } from '../../electron/main/ai/discoveryService'
import { InstructionScopeService } from '../../electron/main/ai/instructionScopeService'
import { AiMemoryRepository } from '../../electron/main/ai/memoryRepository'
import { McpService } from '../../electron/main/ai/mcpService'

const temporaryDirectories: string[] = []
const servers: Server[] = []

afterEach(async () => {
  await Promise.all(servers.splice(0).map((server) => new Promise<void>((resolve, reject) => server.close((error) => error ? reject(error) : resolve()))))
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('ContextInspectorService', () => {
  it('connects effective instructions, Skill metadata/body, scoped memories, and explicitly observed MCP Tool Schemas', async () => {
    const root = await mkdtemp(join(tmpdir(), 'mootool-context-inspector-'))
    temporaryDirectories.push(root)
    const home = join(root, 'home')
    const project = join(root, 'project')
    const skill = join(project, '.agents', 'skills', 'review')
    await Promise.all([mkdir(join(home, '.codex'), { recursive: true }), mkdir(skill, { recursive: true }), mkdir(join(project, '.codex'), { recursive: true })])
    const duplicateInstruction = 'Always run npm test before finishing.\n'
    await Promise.all([
      writeFile(join(home, '.codex', 'AGENTS.md'), duplicateInstruction),
      writeFile(join(project, 'AGENTS.md'), duplicateInstruction),
      writeFile(join(skill, 'SKILL.md'), '---\nname: review\ndescription: Review changed code safely.\n---\n\nUse the project test suite and inspect every diff.\n')
    ])

    const mcpServer = createServer((request, response) => {
      let source = ''
      request.setEncoding('utf8')
      request.on('data', (chunk) => { source += chunk })
      request.on('end', () => {
        const payload = JSON.parse(source) as { id?: number; method: string }
        if (payload.id === undefined) { response.statusCode = 202; response.end(); return }
        const result = payload.method === 'initialize'
          ? { protocolVersion: '2025-11-25', capabilities: {} }
          : payload.method === 'tools/list'
            ? { tools: [{ name: 'search_docs', description: 'Search documentation', inputSchema: { type: 'object', properties: { query: { type: 'string' } } } }] }
            : payload.method === 'resources/list' ? { resources: [] } : { prompts: [] }
        response.setHeader('content-type', 'application/json')
        response.end(JSON.stringify({ jsonrpc: '2.0', id: payload.id, result }))
      })
    })
    servers.push(mcpServer)
    await new Promise<void>((resolve) => mcpServer.listen(0, '127.0.0.1', resolve))
    const address = mcpServer.address()
    if (!address || typeof address === 'string') throw new Error('MCP fixture has no port')
    await writeFile(join(project, '.codex', 'config.toml'), `[mcp_servers.fixture]\nurl = "http://127.0.0.1:${address.port}/mcp"\n`)

    const databasePath = join(root, 'context.db')
    const profiles = new AiAgentProfileRepository(databasePath)
    const memories = new AiMemoryRepository(databasePath)
    memories.save({ kind: 'projectFact', scope: 'project', scopeValue: project, content: 'The project uses SQLite for local state.', sourceKind: 'user', confidence: 1, sensitivity: 'internal' })
    memories.save({ kind: 'userPreference', scope: 'user', content: 'Prefer concise reports.', sourceKind: 'user', confidence: 1, sensitivity: 'internal' })
    const profile = profiles.save({
      name: 'Context fixture', clientId: 'codex', workingDirectory: project, permissionMode: 'readOnly',
      mcpServerNames: ['fixture'], skillNames: ['review'], environmentVariableRefs: [], optionalFlags: []
    })
    const discovery = new AiDiscoveryService({ homeDirectory: home, pathValue: '', includeDefaultExecutablePaths: false, fetcher: async () => { throw new Error('offline') } })
    const changes = new ConfigChangeService({ snapshotDirectory: join(root, 'snapshots'), protector })
    const mcp = new McpService({ homeDirectory: home, changes, probeTimeoutMs: 2_000 })
    const service = new ContextInspectorService({ discovery, instructions: new InstructionScopeService(discovery), memories, mcp, profiles })
    const input = {
      projectRoot: project, targetPath: project, clientId: 'codex' as const, agentProfileId: profile.id,
      selectedSkillNames: ['review'], memoryTokenBudget: 5, maxMemoryItems: 10, topN: 5
    }

    const beforeProbe = await service.inspect(input)
    expect(beforeProbe.mcpSchemas).toMatchObject({ servers: 1, observedServers: 0, unknownServers: ['fixture'] })
    expect(beforeProbe.recommendations).toContainEqual(expect.objectContaining({ code: 'unprobedMcp', sourceToolId: 'mcpManager' }))

    const [server] = (await mcp.inventory({ projectRoot: project })).servers
    const probe = await mcp.probe({ requestId: randomUUID(), sourceServerId: server.id, projectRoot: project, confirmCommand: false })
    expect(probe).toMatchObject({ status: 'healthy', schemaEstimatedTokens: expect.any(Number) })
    const snapshot = await service.inspect(input)

    expect(snapshot.items).toEqual(expect.arrayContaining([
      expect.objectContaining({ category: 'instruction', sourceToolId: 'instructionManager' }),
      expect.objectContaining({ category: 'skillMetadata', name: 'review', layer: 'resident' }),
      expect.objectContaining({ category: 'skillBody', name: 'review', layer: 'onDemand' }),
      expect.objectContaining({ category: 'mcpSchema', name: 'fixture / search_docs', estimatedTokens: probe.schemaEstimatedTokens })
    ]))
    expect(snapshot.mcpSchemas).toMatchObject({ servers: 1, observedServers: 1, unknownServers: [] })
    expect(snapshot.memory.omittedByBudget).toBeGreaterThan(0)
    expect(snapshot.duplicates).toContainEqual(expect.objectContaining({ category: 'instruction', estimatedWasteTokens: expect.any(Number) }))
    expect(snapshot.totals.estimatedTokens).toBe(snapshot.items.reduce((sum, item) => sum + item.estimatedTokens, 0))
    expect(snapshot.topItems).toHaveLength(5)

    memories.close()
    profiles.close()
  })
})

const protector: SnapshotProtector = {
  isAvailable: () => true,
  encrypt: (value) => Buffer.from(value).reverse(),
  decrypt: (value) => Buffer.from(value).reverse()
}

import { randomUUID } from 'node:crypto'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { createServer, type Server } from 'node:http'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { ConfigChangeService, type SnapshotProtector } from '../../electron/main/ai/configChangeService'
import { McpService } from '../../electron/main/ai/mcpService'

const temporaryDirectories: string[] = []
const servers: Server[] = []

afterEach(async () => {
  await Promise.all(servers.splice(0).map((server) => new Promise<void>((resolve, reject) => server.close((error) => error ? reject(error) : resolve()))))
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('MCP connection probes', () => {
  it('requires confirmation, performs only capability discovery, hashes the executable, and redacts logs', async () => {
    const fixture = await createFixture(2_000)
    const script = join(fixture.root, 'stdio-server.mjs')
    const secret = 'sk-this_is_a_test_token_value_123456'
    await writeFile(script, `
import readline from 'node:readline'
console.error('api_key = "' + process.env.API_TOKEN + '"')
const lines = readline.createInterface({ input: process.stdin })
lines.on('line', (line) => {
  const message = JSON.parse(line)
  if (message.id === undefined) return
  const results = {
    initialize: { protocolVersion: '2025-11-25', capabilities: {}, serverInfo: { name: 'fixture', version: '1' } },
    'tools/list': { tools: [{ name: 'read' }, { name: 'write' }] },
    'resources/list': { resources: [{ uri: 'fixture://one' }] },
    'prompts/list': { prompts: [{ name: 'review' }] }
  }
  process.stdout.write(JSON.stringify({ jsonrpc: '2.0', id: message.id, result: results[message.method] }) + '\\n')
})
`)
    await mkdir(join(fixture.home, '.codex'))
    await writeFile(join(fixture.home, '.codex', 'config.toml'), `[mcp_servers.fixture]\ncommand = ${JSON.stringify(process.execPath)}\nargs = [${JSON.stringify(script)}]\nenv = { API_TOKEN = ${JSON.stringify(secret)} }\n`)
    const [server] = (await fixture.service.inventory()).servers

    const notConfirmed = await fixture.service.probe({ requestId: randomUUID(), sourceServerId: server.id, confirmCommand: false })
    expect(notConfirmed).toMatchObject({ status: 'error', errorCode: 'CONFIRMATION_REQUIRED' })

    const result = await fixture.service.probe({ requestId: randomUUID(), sourceServerId: server.id, confirmCommand: true })
    expect(result).toMatchObject({
      status: 'healthy',
      protocolVersion: '2025-11-25',
      tools: 2,
      resources: 1,
      prompts: 1,
      executablePath: process.execPath,
      executableSha256: expect.stringMatching(/^[0-9a-f]{64}$/)
    })
    expect(result.toolSchemas).toEqual([
      { name: 'read', estimatedTokens: expect.any(Number) },
      { name: 'write', estimatedTokens: expect.any(Number) }
    ])
    expect(result.schemaEstimatedTokens).toBeGreaterThan(0)
    expect(result.logs.join('\n')).toContain('[REDACTED]')
    expect(JSON.stringify(result)).not.toContain(secret)
  })

  it('times out and cancels silent stdio processes without leaving the request active', async () => {
    const fixture = await createFixture(120)
    const script = join(fixture.root, 'silent-server.mjs')
    await writeFile(script, 'setInterval(() => {}, 1000)\n')
    await mkdir(join(fixture.home, '.codex'))
    await writeFile(join(fixture.home, '.codex', 'config.toml'), `[mcp_servers.silent]\ncommand = ${JSON.stringify(process.execPath)}\nargs = [${JSON.stringify(script)}]\n`)
    const [server] = (await fixture.service.inventory()).servers
    const timeout = await fixture.service.probe({ requestId: randomUUID(), sourceServerId: server.id, confirmCommand: true })
    expect(timeout).toMatchObject({ status: 'error', errorCode: 'TIMEOUT' })

    const cancelFixture = await createFixture(5_000)
    const cancelScript = join(cancelFixture.root, 'silent-server.mjs')
    await writeFile(cancelScript, 'setInterval(() => {}, 1000)\n')
    await mkdir(join(cancelFixture.home, '.codex'))
    await writeFile(join(cancelFixture.home, '.codex', 'config.toml'), `[mcp_servers.silent]\ncommand = ${JSON.stringify(process.execPath)}\nargs = [${JSON.stringify(cancelScript)}]\n`)
    const [cancelServer] = (await cancelFixture.service.inventory()).servers
    const requestId = randomUUID()
    const probing = cancelFixture.service.probe({ requestId, sourceServerId: cancelServer.id, confirmCommand: true })
    await new Promise((resolve) => setTimeout(resolve, 40))
    expect(cancelFixture.service.cancelProbe(requestId)).toBe(true)
    expect(await probing).toMatchObject({ status: 'cancelled', errorCode: 'CANCELLED' })
    expect(cancelFixture.service.cancelProbe(requestId)).toBe(false)
  })

  it('initializes a loopback Streamable HTTP server and lists capabilities without invoking tools', async () => {
    let toolCalls = 0
    const server = createServer((request, response) => {
      let source = ''
      request.setEncoding('utf8')
      request.on('data', (chunk) => { source += chunk })
      request.on('end', () => {
        const payload = JSON.parse(source) as { id?: number; method: string }
        if (payload.method === 'tools/call') toolCalls += 1
        if (payload.id === undefined) { response.statusCode = 202; response.end(); return }
        const result = payload.method === 'initialize'
          ? { protocolVersion: '2025-11-25', capabilities: {} }
          : payload.method === 'tools/list'
            ? { tools: [{ name: 'safe-tool' }] }
            : payload.method === 'resources/list'
              ? { resources: [] }
              : { prompts: [{ name: 'safe-prompt' }] }
        response.setHeader('content-type', 'application/json')
        response.setHeader('mcp-session-id', 'fixture-session')
        response.end(JSON.stringify({ jsonrpc: '2.0', id: payload.id, result }))
      })
    })
    servers.push(server)
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve))
    const address = server.address()
    if (!address || typeof address === 'string') throw new Error('Fixture server has no port')
    const fixture = await createFixture(2_000)
    await writeFile(join(fixture.project, '.mcp.json'), JSON.stringify({ mcpServers: { remote: { type: 'http', url: `http://127.0.0.1:${address.port}/mcp` } } }))
    const [definition] = (await fixture.service.inventory({ projectRoot: fixture.project })).servers

    const result = await fixture.service.probe({ requestId: randomUUID(), sourceServerId: definition.id, projectRoot: fixture.project, confirmCommand: false })
    expect(result).toMatchObject({ status: 'healthy', tools: 1, resources: 0, prompts: 1 })
    expect(result.toolSchemas).toEqual([{ name: 'safe-tool', estimatedTokens: expect.any(Number) }])
    expect(toolCalls).toBe(0)
  })

  it('rejects non-loopback HTTP endpoints before making a request', async () => {
    const fixture = await createFixture(500)
    await writeFile(join(fixture.project, '.mcp.json'), JSON.stringify({ mcpServers: { remote: { type: 'http', url: 'http://example.test/mcp' } } }))
    const [server] = (await fixture.service.inventory({ projectRoot: fixture.project })).servers
    const result = await fixture.service.probe({ requestId: randomUUID(), sourceServerId: server.id, projectRoot: fixture.project, confirmCommand: false })
    expect(result).toMatchObject({ status: 'error', errorCode: 'INSECURE_ENDPOINT' })
  })
})

async function createFixture(probeTimeoutMs: number): Promise<{ root: string; home: string; project: string; service: McpService }> {
  const root = await mkdtemp(join(tmpdir(), 'mootool-ai-mcp-probe-'))
  temporaryDirectories.push(root)
  const home = join(root, 'home')
  const project = join(root, 'project')
  await Promise.all([mkdir(home), mkdir(project)])
  const changes = new ConfigChangeService({ snapshotDirectory: join(root, 'snapshots'), protector })
  return { root, home, project, service: new McpService({ homeDirectory: home, changes, probeTimeoutMs }) }
}

const protector: SnapshotProtector = {
  isAvailable: () => true,
  encrypt: (value) => Buffer.from(value).reverse(),
  decrypt: (value) => Buffer.from(value).reverse()
}

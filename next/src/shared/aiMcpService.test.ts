import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { ConfigChangeService, type SnapshotProtector } from '../../electron/main/ai/configChangeService'
import { McpService } from '../../electron/main/ai/mcpService'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('McpService', () => {
  it('discovers redacted definitions and copies Codex stdio config to Claude with environment references', async () => {
    const fixture = await createFixture()
    const secret = 'sk-this_is_a_test_token_value_123456'
    await mkdir(join(fixture.home, '.codex'))
    await writeFile(join(fixture.home, '.codex', 'config.toml'), `[mcp_servers.files]\ncommand = "npx"\nargs = ["-y", "server-files"]\nenv = { API_KEY = "${secret}", CACHE_DIR = "/tmp/cache" }\n`)

    const inventory = await fixture.service.inventory({ projectRoot: fixture.project })
    expect(inventory.servers).toHaveLength(1)
    expect(inventory.servers[0]).toMatchObject({ clientId: 'codex', transport: 'stdio', risks: ['plaintextSecret'] })
    expect(JSON.stringify(inventory)).not.toContain(secret)

    const preview = await fixture.service.previewCopy({
      projectRoot: fixture.project,
      sourceServerId: inventory.servers[0].id,
      targetClientId: 'claudeCode',
      targetScope: 'project'
    })
    expect(preview.secretMappings).toContainEqual({ field: 'env.API_KEY', environmentVariable: 'MCP_FILES_API_KEY' })
    expect(preview.warnings).toContain('environmentVariablesRequired')
    expect(JSON.stringify(preview)).not.toContain(secret)
    expect(preview.plan.operations[0].redactedDiff).toContain('${MCP_FILES_API_KEY}')

    const applied = await fixture.service.applyCopy(preview.plan.id)
    const target = join(fixture.project, '.mcp.json')
    const targetSource = await readFile(target, 'utf8')
    expect(targetSource).toContain('"API_KEY": "${MCP_FILES_API_KEY}"')
    expect(targetSource).toContain('"CACHE_DIR": "/tmp/cache"')
    expect(targetSource).not.toContain(secret)

    await fixture.service.rollbackCopy(applied.snapshotId)
    await expect(readFile(target)).rejects.toMatchObject({ code: 'ENOENT' })
  })

  it('copies Claude HTTP references to Codex without materializing credentials', async () => {
    const fixture = await createFixture()
    await writeFile(join(fixture.project, '.mcp.json'), JSON.stringify({
      unknownTopLevel: { keep: true },
      mcpServers: {
        remote: {
          type: 'http',
          url: 'https://mcp.example.test/mcp',
          headers: { Authorization: 'Bearer ${TEAM_MCP_TOKEN}', 'X-Workspace': '${TEAM_WORKSPACE}' }
        }
      }
    }, null, 2))
    const inventory = await fixture.service.inventory({ projectRoot: fixture.project })
    const source = inventory.servers.find((server) => server.name === 'remote')!

    const preview = await fixture.service.previewCopy({
      projectRoot: fixture.project,
      sourceServerId: source.id,
      targetClientId: 'codex',
      targetScope: 'user'
    })
    expect(preview.secretMappings).toEqual(expect.arrayContaining([
      { field: 'headers.Authorization', environmentVariable: 'TEAM_MCP_TOKEN' },
      { field: 'headers.X-Workspace', environmentVariable: 'TEAM_WORKSPACE' }
    ]))
    const applied = await fixture.service.applyCopy(preview.plan.id)
    const targetSource = await readFile(join(fixture.home, '.codex', 'config.toml'), 'utf8')
    expect(targetSource).toContain('bearer_token_env_var = "TEAM_MCP_TOKEN"')
    expect(targetSource).toContain('env_http_headers = { X-Workspace = "TEAM_WORKSPACE" }')
    expect(targetSource).not.toContain('Bearer')
    await fixture.service.rollbackCopy(applied.snapshotId)
  })

  it('invalidates copy plans when the source changes and reports invalid configs without values', async () => {
    const fixture = await createFixture()
    await mkdir(join(fixture.home, '.codex'))
    const sourcePath = join(fixture.home, '.codex', 'config.toml')
    await writeFile(sourcePath, '[mcp_servers.safe]\ncommand = "node"\nargs = ["server.js"]\n')
    await writeFile(join(fixture.home, '.claude.json'), '{"mcpServers": ')
    const inventory = await fixture.service.inventory({ projectRoot: fixture.project })
    expect(inventory.invalidConfigPaths).toEqual([join(fixture.home, '.claude.json')])
    const preview = await fixture.service.previewCopy({ projectRoot: fixture.project, sourceServerId: inventory.servers[0].id, targetClientId: 'claudeCode', targetScope: 'project' })
    await writeFile(sourcePath, '[mcp_servers.changed]\ncommand = "node"\n')

    await expect(fixture.service.applyCopy(preview.plan.id)).rejects.toThrow('source configuration changed')
    await expect(readFile(join(fixture.project, '.mcp.json'))).rejects.toMatchObject({ code: 'ENOENT' })
  })

  it('blocks sensitive command arguments and legacy transports', async () => {
    const fixture = await createFixture()
    await writeFile(join(fixture.project, '.mcp.json'), JSON.stringify({ mcpServers: {
      unsafe: { command: 'node', args: ['server.js', '--api-key', 'plaintext-value'] },
      legacy: { type: 'sse', url: 'https://mcp.example.test/sse' }
    } }))
    const inventory = await fixture.service.inventory({ projectRoot: fixture.project })
    const unsafe = inventory.servers.find((server) => server.name === 'unsafe')!
    const legacy = inventory.servers.find((server) => server.name === 'legacy')!
    await expect(fixture.service.previewCopy({ projectRoot: fixture.project, sourceServerId: unsafe.id, targetClientId: 'codex', targetScope: 'project' })).rejects.toThrow('command arguments')
    await expect(fixture.service.previewCopy({ projectRoot: fixture.project, sourceServerId: legacy.id, targetClientId: 'codex', targetScope: 'project' })).rejects.toThrow('Legacy SSE')
  })

  it('discovers Cursor, Gemini CLI, and GitHub Copilot MCP configurations as redacted read-only sources', async () => {
    const fixture = await createFixture()
    await Promise.all([
      mkdir(join(fixture.home, '.cursor'), { recursive: true }),
      mkdir(join(fixture.project, '.gemini'), { recursive: true }),
      mkdir(join(fixture.project, '.vscode'), { recursive: true })
    ])
    await Promise.all([
      writeFile(join(fixture.home, '.cursor', 'mcp.json'), JSON.stringify({ mcpServers: {
        cursorDocs: { command: 'node', args: ['cursor-server.js'], env: { TOKEN: 'SECRET_CURSOR_MCP' } }
      } })),
      writeFile(join(fixture.project, '.gemini', 'settings.json'), JSON.stringify({ mcpServers: {
        geminiDocs: { type: 'http', url: 'https://gemini.example.test/mcp', headers: { Authorization: '${GEMINI_MCP_TOKEN}' } }
      } })),
      writeFile(join(fixture.project, '.vscode', 'mcp.json'), JSON.stringify({ servers: {
        copilotDocs: { type: 'stdio', command: 'node', args: ['copilot-server.js'] }
      } }))
    ])

    const inventory = await fixture.service.inventory({ projectRoot: fixture.project })
    expect(inventory.servers).toEqual(expect.arrayContaining([
      expect.objectContaining({ name: 'cursorDocs', clientId: 'cursor', transport: 'stdio' }),
      expect.objectContaining({ name: 'geminiDocs', clientId: 'geminiCli', transport: 'streamableHttp' }),
      expect.objectContaining({ name: 'copilotDocs', clientId: 'githubCopilot', transport: 'stdio' })
    ]))
    expect(JSON.stringify(inventory)).not.toContain('SECRET_CURSOR_MCP')

    const cursorSource = inventory.servers.find((server) => server.name === 'cursorDocs')!
    const preview = await fixture.service.previewCopy({
      projectRoot: fixture.project,
      sourceServerId: cursorSource.id,
      targetClientId: 'codex',
      targetScope: 'project'
    })
    expect(preview.source.clientId).toBe('cursor')
    expect(preview.secretMappings).toContainEqual({ field: 'env.TOKEN', environmentVariable: 'TOKEN' })
  })
})

async function createFixture(): Promise<{ root: string; home: string; project: string; service: McpService }> {
  const root = await mkdtemp(join(tmpdir(), 'mootool-ai-mcp-'))
  temporaryDirectories.push(root)
  const home = join(root, 'home')
  const project = join(root, 'project')
  await Promise.all([mkdir(home), mkdir(project)])
  const changes = new ConfigChangeService({ snapshotDirectory: join(root, 'snapshots'), protector })
  return { root, home, project, service: new McpService({ homeDirectory: home, changes }) }
}

const protector: SnapshotProtector = {
  isAvailable: () => true,
  encrypt: (value) => Buffer.from(value).reverse(),
  decrypt: (value) => Buffer.from(value).reverse()
}

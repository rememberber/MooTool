import { describe, expect, it } from 'vitest'
import { addClaudeMcpServer, addCodexMcpServer, parseClaudeMcpConfig, parseCodexMcpConfig, sanitizeMcpServer } from '../../electron/main/ai/mcpConfig'

describe('AI MCP configuration adapters', () => {
  it('parses Codex stdio and HTTP servers with current environment-reference fields', () => {
    const source = `# unknown settings stay opaque to the parser
model = "gpt-5"

[mcp_servers.files]
command = "npx"
args = [
  "-y",
  "@modelcontextprotocol/server-filesystem"
]
env_vars = ["LOCAL_TOKEN", { name = "REMOTE_TOKEN", source = "remote" }]
env = { CACHE_DIR = "/tmp/mcp" }
startup_timeout_sec = 20

[mcp_servers."remote.api"]
url = "https://mcp.example.test/mcp?tenant=internal"
bearer_token_env_var = "MCP_API_TOKEN"
env_http_headers = { "X-Workspace" = "MCP_WORKSPACE" }
enabled = false
`

    const servers = parseCodexMcpConfig(source)

    expect(servers).toHaveLength(2)
    expect(servers[0]).toMatchObject({
      name: 'files',
      transport: 'stdio',
      command: 'npx',
      args: ['-y', '@modelcontextprotocol/server-filesystem'],
      environment: { CACHE_DIR: '/tmp/mcp' },
      environmentReferences: { LOCAL_TOKEN: 'LOCAL_TOKEN', REMOTE_TOKEN: 'REMOTE_TOKEN' },
      startupTimeoutMs: 20_000
    })
    expect(servers[1]).toMatchObject({
      name: 'remote.api',
      transport: 'streamableHttp',
      enabled: false,
      bearerTokenEnvironmentVariable: 'MCP_API_TOKEN',
      headerReferences: { 'X-Workspace': 'MCP_WORKSPACE' }
    })
  })

  it('parses Claude transports and never exposes plaintext credentials in the renderer model', () => {
    const secret = 'sk-this_is_a_test_token_value_123456'
    const [server] = parseClaudeMcpConfig(JSON.stringify({
      mcpServers: {
        github: {
          type: 'http',
          url: 'https://mcp.example.test/mcp?token=hidden',
          headers: { Authorization: `Bearer ${secret}`, 'X-Workspace': '${MCP_WORKSPACE}' },
          env: { API_KEY: secret, CACHE_DIR: '/tmp/cache' }
        }
      },
      futureField: { preserve: true }
    }))

    const sanitized = sanitizeMcpServer(server, 'claudeCode', 'project', '/project/.mcp.json')

    expect(sanitized).toMatchObject({
      name: 'github',
      transport: 'streamableHttp',
      risks: expect.arrayContaining(['plaintextSecret']),
      environment: expect.arrayContaining([
        { name: 'API_KEY', source: 'literal', sensitive: true },
        { name: 'CACHE_DIR', source: 'literal', sensitive: false }
      ]),
      headers: expect.arrayContaining([
        { name: 'Authorization', source: 'literal', sensitive: true },
        { name: 'X-Workspace', source: 'environment', reference: 'MCP_WORKSPACE', sensitive: false }
      ])
    })
    expect(sanitized.url).not.toContain('hidden')
    expect(JSON.stringify(sanitized)).not.toContain(secret)
  })

  it('flags legacy, insecure, unknown, and sensitive-argument definitions', () => {
    const [legacy, local, unknown] = parseClaudeMcpConfig(JSON.stringify({ mcpServers: {
      legacy: { type: 'sse', url: 'http://example.test/sse' },
      local: { command: 'node', args: ['server.js', '--api-key', 'plain-secret-value'] },
      unknown: { futureTransport: true }
    } }))

    expect(sanitizeMcpServer(legacy, 'claudeCode', 'user', '/home/.claude.json').risks).toEqual(expect.arrayContaining(['legacyTransport', 'insecureRemoteHttp']))
    const sanitizedLocal = sanitizeMcpServer(local, 'claudeCode', 'user', '/home/.claude.json')
    expect(sanitizedLocal.args).toContain('[REDACTED]')
    expect(sanitizedLocal.risks).toContain('sensitiveArgument')
    expect(sanitizeMcpServer(unknown, 'claudeCode', 'user', '/home/.claude.json').risks).toContain('unknownTransport')
  })

  it('inserts target definitions while retaining existing unknown configuration text', () => {
    const claudeSource = '{\n  "futureField": { "keep": [1, 2] },\n  "mcpServers": {\n    "existing": { "command": "node" }\n  }\n}\n'
    const claudeResult = addClaudeMcpServer(claudeSource, 'remote', { type: 'http', url: 'https://mcp.example.test/mcp' })
    expect(claudeResult).toContain('"futureField": { "keep": [1, 2] }')
    expect(parseClaudeMcpConfig(claudeResult).map((server) => server.name)).toEqual(['existing', 'remote'])

    const codexSource = '# keep comment\nmodel = "gpt-5"\n'
    const codexResult = addCodexMcpServer(codexSource, 'remote.api', {
      name: 'remote.api', transport: 'streamableHttp', enabled: true, args: [], url: 'https://mcp.example.test/mcp', environment: {}, environmentReferences: {}, headers: {}, headerReferences: {}, oauth: false
    }, { environmentReferences: {}, headerReferences: { Authorization: 'MCP_REMOTE_API_AUTHORIZATION' } })
    expect(codexResult).toContain('# keep comment\nmodel = "gpt-5"')
    expect(codexResult).toContain('[mcp_servers."remote.api"]')
    expect(codexResult).toContain('bearer_token_env_var = "MCP_REMOTE_API_AUTHORIZATION"')
    expect(parseCodexMcpConfig(codexResult)).toHaveLength(1)
  })
})

import { chmod, mkdir, mkdtemp, realpath, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { AiDiscoveryService } from '../../electron/main/ai/discoveryService'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('AiDiscoveryService', () => {
  it('discovers clients, instructions, skills, MCP servers, and Ollama without exposing configuration values', async () => {
    const fixtureRoot = await createFixtureRoot()
    const homeDirectory = join(fixtureRoot, 'home')
    const projectRoot = join(fixtureRoot, 'project')
    const binaryDirectory = join(fixtureRoot, 'bin')

    await Promise.all([
      mkdir(join(homeDirectory, '.codex'), { recursive: true }),
      mkdir(join(homeDirectory, '.agents', 'skills', 'review'), { recursive: true }),
      mkdir(join(homeDirectory, '.claude'), { recursive: true }),
      mkdir(join(homeDirectory, '.cursor'), { recursive: true }),
      mkdir(join(homeDirectory, '.gemini'), { recursive: true }),
      mkdir(join(homeDirectory, '.copilot'), { recursive: true }),
      mkdir(join(projectRoot, '.claude', 'skills', 'release'), { recursive: true }),
      mkdir(join(projectRoot, '.cursor', 'rules'), { recursive: true }),
      mkdir(join(projectRoot, '.cursor', 'skills', 'cursor-review'), { recursive: true }),
      mkdir(join(projectRoot, '.gemini', 'skills', 'gemini-test'), { recursive: true }),
      mkdir(join(projectRoot, '.github', 'instructions'), { recursive: true }),
      mkdir(join(projectRoot, '.github', 'skills', 'copilot-ci'), { recursive: true }),
      mkdir(join(projectRoot, '.vscode'), { recursive: true }),
      mkdir(join(projectRoot, 'src'), { recursive: true }),
      mkdir(binaryDirectory, { recursive: true })
    ])
    await Promise.all([
      writeFile(join(homeDirectory, '.codex', 'config.toml'), '[mcp_servers.github]\napi_key = "SECRET_CODEX_TOKEN"\n'),
      writeFile(join(homeDirectory, '.agents', 'skills', 'review', 'SKILL.md'), '---\nname: review\ndescription: Review changes safely.\n---\n# Review\n'),
      writeFile(join(homeDirectory, '.claude', 'settings.json'), '{"apiKey":"SECRET_CLAUDE_TOKEN"}\n'),
      writeFile(join(homeDirectory, '.cursor', 'mcp.json'), '{"mcpServers":{"cursor-docs":{"command":"node","env":{"TOKEN":"SECRET_CURSOR_TOKEN"}}}}\n'),
      writeFile(join(homeDirectory, '.gemini', 'settings.json'), '{"mcpServers":{"gemini-docs":{"url":"https://example.test/mcp","headers":{"Authorization":"SECRET_GEMINI_TOKEN"}}}}\n'),
      writeFile(join(homeDirectory, '.gemini', 'GEMINI.md'), '# User Gemini instructions\n'),
      writeFile(join(homeDirectory, '.copilot', 'mcp-config.json'), '{"mcpServers":{"copilot-docs":{"command":"node","args":["server.js"]}}}\n'),
      writeFile(join(projectRoot, 'AGENTS.md'), '# Project instructions\n'),
      writeFile(join(projectRoot, 'src', 'AGENTS.md'), '# Nested instructions\n'),
      writeFile(join(projectRoot, 'CLAUDE.md'), '# Claude instructions\n'),
      writeFile(join(projectRoot, 'GEMINI.md'), '# Gemini instructions\n'),
      writeFile(join(projectRoot, '.mcp.json'), '{"mcpServers":{"database":{"env":{"TOKEN":"SECRET_MCP_TOKEN"}}}}\n'),
      writeFile(join(projectRoot, '.claude', 'skills', 'release', 'SKILL.md'), '---\nname: release\ndescription: Prepare a release.\n---\n# Release\n'),
      writeFile(join(projectRoot, '.cursor', 'rules', 'typescript.mdc'), '---\nglobs: src/**/*.ts\n---\n# Cursor TypeScript rule\n'),
      writeFile(join(projectRoot, '.cursor', 'skills', 'cursor-review', 'SKILL.md'), '---\nname: cursor-review\ndescription: Review with Cursor.\n---\n# Cursor review\n'),
      writeFile(join(projectRoot, '.gemini', 'skills', 'gemini-test', 'SKILL.md'), '---\nname: gemini-test\ndescription: Test with Gemini.\n---\n# Gemini test\n'),
      writeFile(join(projectRoot, '.github', 'copilot-instructions.md'), '# Copilot instructions\n'),
      writeFile(join(projectRoot, '.github', 'instructions', 'tests.instructions.md'), '---\napplyTo: "tests/**"\n---\n# Copilot test instructions\n'),
      writeFile(join(projectRoot, '.github', 'skills', 'copilot-ci', 'SKILL.md'), '---\nname: copilot-ci\ndescription: Diagnose CI with Copilot.\n---\n# Copilot CI\n'),
      writeFile(join(projectRoot, '.vscode', 'mcp.json'), '{"servers":{"vscode-docs":{"type":"http","url":"https://example.test/vscode-mcp"}}}\n'),
      createExecutable(join(binaryDirectory, 'codex')),
      createExecutable(join(binaryDirectory, 'claude')),
      createExecutable(join(binaryDirectory, 'cursor-agent')),
      createExecutable(join(binaryDirectory, 'gemini')),
      createExecutable(join(binaryDirectory, 'copilot')),
      createExecutable(join(binaryDirectory, 'ollama'))
    ])

    const service = new AiDiscoveryService({
      homeDirectory,
      pathValue: binaryDirectory,
      includeDefaultExecutablePaths: false,
      requestTimeoutMs: 100,
      fetcher: async (url) => ({
        ok: true,
        status: 200,
        json: async () => {
          if (url.endsWith('/api/version')) return { version: '0.9.0' }
          if (url.endsWith('/api/ps')) return { models: [{ name: 'qwen3:8b', digest: 'sha256:test', context_length: 32_768 }] }
          return {
            models: [{
              name: 'qwen3:8b',
              digest: 'sha256:test',
              size: 5_000_000_000,
              details: { parameter_size: '8B', quantization_level: 'Q4_K_M', family: 'qwen3' }
            }]
          }
        }
      })
    })

    const snapshot = await service.scan({ projectRoot })

    expect(snapshot.readOnly).toBe(true)
    expect(snapshot.projectRoot).toBe(await realpath(projectRoot))
    expect(snapshot.clients).toEqual(expect.arrayContaining([
      expect.objectContaining({ id: 'codex', detected: true, status: 'healthy' }),
      expect.objectContaining({ id: 'claudeCode', detected: true, status: 'healthy' }),
      expect.objectContaining({ id: 'cursor', detected: true, status: 'healthy' }),
      expect.objectContaining({ id: 'geminiCli', detected: true, status: 'healthy' }),
      expect.objectContaining({ id: 'githubCopilot', detected: true, status: 'healthy' })
    ]))
    expect(snapshot.runtimes[0]).toEqual(expect.objectContaining({
      id: 'ollama',
      detected: true,
      status: 'healthy',
      version: '0.9.0',
      protocols: ['ollama', 'openaiCompatible'],
      models: [expect.objectContaining({ name: 'qwen3:8b', running: true, contextLength: 32_768 })]
    }))
    expect(snapshot.artifacts).toEqual(expect.arrayContaining([
      expect.objectContaining({ kind: 'skill', name: 'review', clientId: 'codex', scope: 'user' }),
      expect.objectContaining({ kind: 'skill', name: 'release', clientId: 'claudeCode', scope: 'project' }),
      expect.objectContaining({ kind: 'mcpServer', name: 'github', clientId: 'codex' }),
      expect.objectContaining({ kind: 'mcpServer', name: 'database', clientId: 'claudeCode' }),
      expect.objectContaining({ kind: 'instruction', name: 'src/AGENTS.md', clientId: 'codex' }),
      expect.objectContaining({ kind: 'instruction', name: 'CLAUDE.md', clientId: 'claudeCode' }),
      expect.objectContaining({ kind: 'instruction', name: '.cursor/rules/typescript.mdc', clientId: 'cursor' }),
      expect.objectContaining({ kind: 'instruction', name: 'GEMINI.md', clientId: 'geminiCli' }),
      expect.objectContaining({ kind: 'instruction', name: '.github/copilot-instructions.md', clientId: 'githubCopilot' }),
      expect.objectContaining({ kind: 'skill', name: 'cursor-review', clientId: 'cursor' }),
      expect.objectContaining({ kind: 'skill', name: 'gemini-test', clientId: 'geminiCli' }),
      expect.objectContaining({ kind: 'skill', name: 'copilot-ci', clientId: 'githubCopilot' }),
      expect.objectContaining({ kind: 'mcpServer', name: 'cursor-docs', clientId: 'cursor' }),
      expect.objectContaining({ kind: 'mcpServer', name: 'gemini-docs', clientId: 'geminiCli' }),
      expect.objectContaining({ kind: 'mcpServer', name: 'vscode-docs', clientId: 'githubCopilot' })
    ]))
    expect(snapshot.artifacts.find((artifact) => artifact.kind === 'skill' && artifact.name === 'review')?.metadata).toMatchObject({ description: 'Review changes safely.', fileCount: 1 })
    expect(snapshot.summary.detectedClients).toBe(5)
    expect(snapshot.summary.skills).toBeGreaterThanOrEqual(5)
    expect(snapshot.summary.mcpServers).toBeGreaterThanOrEqual(6)
    expect(snapshot.diagnostics).toContainEqual(expect.objectContaining({ code: 'PLAINTEXT_SECRET_RISK', severity: 'warning' }))

    const serialized = JSON.stringify(snapshot)
    expect(serialized).not.toContain('SECRET_CODEX_TOKEN')
    expect(serialized).not.toContain('SECRET_CLAUDE_TOKEN')
    expect(serialized).not.toContain('SECRET_MCP_TOKEN')
    expect(serialized).not.toContain('SECRET_CURSOR_TOKEN')
    expect(serialized).not.toContain('SECRET_GEMINI_TOKEN')
  })

  it('reports a user-only scan and a missing runtime as normal discovery results', async () => {
    const fixtureRoot = await createFixtureRoot()
    const homeDirectory = join(fixtureRoot, 'home')
    await mkdir(homeDirectory, { recursive: true })
    const service = new AiDiscoveryService({
      homeDirectory,
      pathValue: join(fixtureRoot, 'empty-bin'),
      includeDefaultExecutablePaths: false,
      requestTimeoutMs: 10,
      fetcher: async () => {
        throw new Error('connection refused')
      }
    })

    const snapshot = await service.scan()

    expect(snapshot.projectRoot).toBeUndefined()
    expect(snapshot.runtimes[0]).toMatchObject({ id: 'ollama', detected: false, status: 'missing' })
    expect(snapshot.diagnostics).toContainEqual(expect.objectContaining({ code: 'PROJECT_NOT_SELECTED', severity: 'info' }))
  })
})

async function createFixtureRoot(): Promise<string> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-ai-discovery-'))
  temporaryDirectories.push(directory)
  return directory
}

async function createExecutable(path: string): Promise<void> {
  await writeFile(path, '#!/bin/sh\nexit 0\n')
  await chmod(path, 0o755)
}

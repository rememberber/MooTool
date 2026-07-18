import { chmod, mkdir, mkdtemp, realpath, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { AiAgentProfileRepository } from '../../electron/main/ai/agentProfileRepository'
import { AgentManagerService } from '../../electron/main/ai/agentManagerService'
import { AiDiscoveryService } from '../../electron/main/ai/discoveryService'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('AgentManagerService', () => {
  it('reports client capabilities and generates a credential-free Codex launch plan without executing it', async () => {
    const fixture = await createFixture()
    const profile = fixture.repository.save({
      name: 'Implementation profile', clientId: 'codex', model: 'qwen3:8b', modelRuntimeId: 'ollama',
      localModelDigest: 'sha256:qwen3-8b', workingDirectory: fixture.project,
      configProfile: 'work', permissionMode: 'workspaceWrite', mcpServerNames: ['docs'], skillNames: ['testing'],
      environmentVariableRefs: ['OPENAI_API_KEY'], optionalFlags: ['--search']
    })

    const snapshot = await fixture.service.snapshot({ projectRoot: fixture.project })
    expect(snapshot.profiles).toContainEqual(expect.objectContaining({ id: profile.id }))
    expect(snapshot.clients.map((client) => client.id)).toEqual(['codex', 'claudeCode', 'cursor', 'geminiCli', 'githubCopilot'])
    expect(snapshot.clients.find((client) => client.id === 'codex')).toMatchObject({
      detected: true,
      health: 'healthy',
      binaryPath: fixture.codex,
      configurationChanged: false,
      configurationFingerprint: expect.stringMatching(/^sha256:[0-9a-f]{64}$/),
      capabilities: expect.arrayContaining([
        { id: 'skills', support: 'full' },
        { id: 'usage', support: 'partial' }
      ])
    })
    expect(snapshot.clients.find((client) => client.id === 'cursor')).toMatchObject({
      detected: false,
      health: 'missing',
      configurationChanged: false,
      capabilities: expect.arrayContaining([
        { id: 'instructions', support: 'full' },
        { id: 'structuredOutput', support: 'full' }
      ])
    })
    expect(snapshot.clients.find((client) => client.id === 'geminiCli')?.configurationChanged).toBe(false)
    expect(snapshot.clients.find((client) => client.id === 'githubCopilot')?.configurationChanged).toBe(false)
    const originalFingerprint = snapshot.clients.find((client) => client.id === 'codex')!.configurationFingerprint
    await writeFile(join(fixture.root, 'home', '.codex', 'config.toml'), '[profiles.work]\nmodel = "gpt-5.4-updated"\n')
    const changed = await fixture.service.snapshot({ projectRoot: fixture.project })
    expect(changed.clients.find((client) => client.id === 'codex')).toMatchObject({ configurationChanged: true, previousConfigurationFingerprint: originalFingerprint })
    expect((await fixture.service.snapshot({ projectRoot: fixture.project })).clients.find((client) => client.id === 'codex')?.configurationChanged).toBe(false)

    process.env.OPENAI_API_KEY = 'secret-value-that-must-not-appear'
    const plan = await fixture.service.launchPlan(profile.id)
    expect(plan).toMatchObject({
      executable: fixture.codex,
      workingDirectory: fixture.project,
      executes: false,
      requiredEnvironmentVariables: ['OPENAI_API_KEY'],
      args: ['--model', 'qwen3:8b', '--profile', 'work', '--sandbox', 'workspace-write', '--search']
    })
    expect(plan.displayCommand).toContain("cd '")
    expect(JSON.stringify(plan)).not.toContain(process.env.OPENAI_API_KEY)
    expect(plan.warnings).toHaveLength(4)
    expect(plan.warnings).toContainEqual(expect.stringContaining('declarative'))
    delete process.env.OPENAI_API_KEY
    fixture.repository.close()
  })

  it('maps Claude permission modes to the documented allowlisted launch flag', async () => {
    const fixture = await createFixture()
    const profile = fixture.repository.save({
      name: 'Plan-only analyst', clientId: 'claudeCode', workingDirectory: fixture.project,
      permissionMode: 'plan', mcpServerNames: [], skillNames: [], environmentVariableRefs: [], optionalFlags: ['--ide']
    })

    const plan = await fixture.service.launchPlan(profile.id)
    expect(plan.executable).toBe(fixture.claude)
    expect(plan.args).toEqual(['--permission-mode', 'plan', '--ide'])
    expect(plan.executes).toBe(false)
    fixture.repository.close()
  })
})

async function createFixture() {
  const root = await mkdtemp(join(tmpdir(), 'mootool-agent-manager-'))
  temporaryDirectories.push(root)
  const home = join(root, 'home')
  const bin = join(root, 'bin')
  const projectPath = join(root, 'project with space')
  await Promise.all([mkdir(join(home, '.codex'), { recursive: true }), mkdir(bin, { recursive: true }), mkdir(projectPath, { recursive: true })])
  const project = await realpath(projectPath)
  await writeFile(join(home, '.codex', 'config.toml'), '[profiles.work]\nmodel = "gpt-5.4"\n')
  const codex = join(bin, 'codex')
  const claude = join(bin, 'claude')
  await Promise.all([writeFile(codex, '#!/bin/sh\nexit 0\n'), writeFile(claude, '#!/bin/sh\nexit 0\n')])
  await Promise.all([chmod(codex, 0o755), chmod(claude, 0o755)])
  const repository = new AiAgentProfileRepository(join(root, 'agents.db'))
  const discovery = new AiDiscoveryService({
    homeDirectory: home,
    pathValue: bin,
    includeDefaultExecutablePaths: false,
    requestTimeoutMs: 5,
    fetcher: async () => { throw new Error('offline') }
  })
  return { root, project, codex, claude, repository, service: new AgentManagerService({ discovery, repository, platform: 'darwin' }) }
}

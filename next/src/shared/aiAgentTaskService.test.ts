import { randomUUID } from 'node:crypto'
import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process'
import { chmod, mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AgentTaskService, taskArguments } from '../../electron/main/ai/agentTaskService'
import type { AiAgentProfile } from './contracts/aiAgents'
import { isAiAgentTaskStartInput, requiresAiAgentTaskWriteConfirmation } from './contracts/aiAgentTasks'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('controlled Agent CLI tasks', () => {
  it('uses a no-shell Codex process, sends the prompt over stdin, and redacts prompt and secret output', async () => {
    const fixture = await createFixture('codex', `
const fs = require('node:fs')
let input = ''
process.stdin.setEncoding('utf8')
process.stdin.on('data', (chunk) => { input += chunk })
process.stdin.on('end', () => {
  fs.writeFileSync(process.env.CAPTURE_FILE, JSON.stringify({ argv: process.argv.slice(2), input }))
  process.stdout.write('api_key = "' + process.env.TEST_API_KEY + '"\\n')
  process.stdout.write(JSON.stringify({ type: 'message', content: input }) + '\\n')
})
`)
    const prompt = 'Inspect the project and report only the root cause.'
    const secret = 'sk-controlled-agent-secret-123456789'
    const events: string[] = []
    const spawnCalls: Array<{ executable: string; args: string[]; shell: unknown }> = []
    const service = new AgentTaskService({
      manager: launchManager(fixture.executable, fixture.project, ['--model', 'gpt-test', '--sandbox', 'read-only', '--no-alt-screen']),
      repository: repository(profile({ id: fixture.profileId, clientId: 'codex', permissionMode: 'readOnly', workingDirectory: fixture.project, environmentVariableRefs: ['TEST_API_KEY'] })),
      environment: { ...process.env, CAPTURE_FILE: fixture.capture, TEST_API_KEY: secret },
      platform: process.platform,
      spawnProcess: (executable, args, options) => {
        spawnCalls.push({ executable, args, shell: options.shell })
        return spawn(executable, args, options) as ChildProcessWithoutNullStreams
      }
    })

    const result = await service.run(input({ profileId: fixture.profileId, prompt }), (event) => events.push(event.text))
    const captured = JSON.parse(await readFile(fixture.capture, 'utf8')) as { argv: string[]; input: string }

    expect(result).toMatchObject({ status: 'completed', exitCode: 0, promptDeliveredVia: 'stdin', truncated: false })
    expect(result.args).toEqual(['--model', 'gpt-test', '--sandbox', 'read-only', 'exec', '--json', '--color', 'never', '--ephemeral', '-'])
    expect(spawnCalls).toEqual([{ executable: fixture.executable, args: result.args, shell: false }])
    expect(captured.input).toBe(prompt)
    expect(captured.argv).toEqual(result.args)
    expect(JSON.stringify(result)).not.toContain(prompt)
    expect(JSON.stringify(result)).not.toContain(secret)
    expect(events.join('')).toContain('[REDACTED]')
    expect(events.join('')).not.toContain(prompt)
    expect(events.join('')).not.toContain(secret)
  })

  it('builds the documented ephemeral Claude print invocation while retaining profile context', () => {
    const claude = profile({ clientId: 'claudeCode', permissionMode: 'plan', optionalFlags: ['--no-chrome'] })
    expect(taskArguments(claude, ['--model', 'sonnet', '--permission-mode', 'plan', '--no-chrome'], 7)).toEqual([
      '--model', 'sonnet', '--permission-mode', 'plan', '--no-chrome',
      '--print', '--output-format', 'stream-json', '--verbose', '--include-partial-messages',
      '--max-turns', '7', '--no-session-persistence'
    ])
    expect(requiresAiAgentTaskWriteConfirmation(claude)).toBe(false)
  })

  it('requires separate execution and write confirmations before spawning', async () => {
    const writable = profile({ clientId: 'codex', permissionMode: 'workspaceWrite' })
    const launchPlan = vi.fn(async () => { throw new Error('must not launch') })
    const service = new AgentTaskService({ manager: { launchPlan }, repository: repository(writable) })
    await expect(service.run(input({ profileId: writable.id, confirmExecution: false, confirmWrite: false }))).rejects.toThrow('execution confirmation')
    await expect(service.run(input({ profileId: writable.id, confirmExecution: true, confirmWrite: false }))).rejects.toThrow('write confirmation')
    expect(launchPlan).not.toHaveBeenCalled()
    expect(requiresAiAgentTaskWriteConfirmation(writable)).toBe(true)
  })

  it('cancels and times out process trees with typed terminal states', async () => {
    const fixture = await createFixture('codex', `
process.stdin.resume()
process.stdin.on('end', () => setInterval(() => {}, 1000))
`)
    const taskProfile = profile({ id: fixture.profileId, clientId: 'codex', permissionMode: 'readOnly', workingDirectory: fixture.project })
    const service = new AgentTaskService({
      manager: launchManager(fixture.executable, fixture.project, ['--sandbox', 'read-only']),
      repository: repository(taskProfile),
      platform: process.platform
    })
    const requestId = randomUUID()
    const running = service.run(input({ requestId, profileId: taskProfile.id }))
    await new Promise((resolve) => setTimeout(resolve, 60))
    expect(service.cancel(requestId)).toBe(true)
    expect(await running).toMatchObject({ status: 'cancelled', exitCode: null })
    expect(service.cancel(requestId)).toBe(false)

    const timedOut = await service.run(input({ requestId: randomUUID(), profileId: taskProfile.id, maxDurationSeconds: 1 }))
    expect(timedOut.status).toBe('timedOut')
  }, 5_000)

  it('terminates output floods at the configured byte limit', async () => {
    const fixture = await createFixture('codex', `
process.stdin.resume()
process.stdin.on('end', () => {
  process.stdout.write('x'.repeat(4096) + '\\n')
  setInterval(() => {}, 1000)
})
`)
    const taskProfile = profile({ id: fixture.profileId, clientId: 'codex', permissionMode: 'readOnly', workingDirectory: fixture.project })
    const service = new AgentTaskService({
      manager: launchManager(fixture.executable, fixture.project, ['--sandbox', 'read-only']),
      repository: repository(taskProfile),
      maximumOutputBytes: 128,
      platform: process.platform
    })
    const result = await service.run(input({ profileId: taskProfile.id }))
    expect(result).toMatchObject({ status: 'outputLimit', truncated: true })
    expect(Buffer.byteLength(result.stdout, 'utf8')).toBeLessThanOrEqual(128)
  })

  it('validates bounded prompts, durations, turns, confirmations, and UUIDs at the IPC boundary', () => {
    expect(isAiAgentTaskStartInput(input())).toBe(true)
    expect(isAiAgentTaskStartInput({ ...input(), prompt: 'x'.repeat(70_000) })).toBe(false)
    expect(isAiAgentTaskStartInput({ ...input(), maxDurationSeconds: 0 })).toBe(false)
    expect(isAiAgentTaskStartInput({ ...input(), maxTurns: 101 })).toBe(false)
    expect(isAiAgentTaskStartInput({ ...input(), confirmExecution: 'yes' })).toBe(false)
  })
})

async function createFixture(name: 'codex' | 'claude', script: string): Promise<{ root: string; project: string; executable: string; capture: string; profileId: string }> {
  const root = await mkdtemp(join(tmpdir(), 'mootool-agent-task-'))
  temporaryDirectories.push(root)
  const project = join(root, 'project')
  const executable = join(root, name)
  await mkdir(project)
  await writeFile(executable, `#!/usr/bin/env node\n${script}`)
  await chmod(executable, 0o755)
  return { root, project, executable, capture: join(root, 'capture.json'), profileId: randomUUID() }
}

function profile(overrides: Partial<AiAgentProfile> = {}): AiAgentProfile {
  const now = new Date(0).toISOString()
  return {
    id: randomUUID(),
    name: 'Controlled task',
    clientId: 'codex',
    workingDirectory: process.cwd(),
    permissionMode: 'readOnly',
    mcpServerNames: [],
    skillNames: [],
    environmentVariableRefs: [],
    optionalFlags: [],
    createdAt: now,
    updatedAt: now,
    ...overrides
  }
}

function repository(taskProfile: AiAgentProfile) {
  return { getRequired: (id: string) => {
    if (id !== taskProfile.id) throw new Error('missing fixture profile')
    return taskProfile
  } }
}

function launchManager(executable: string, workingDirectory: string, args: string[]) {
  return { launchPlan: async (profileId: string) => ({
    profileId,
    clientId: 'codex' as const,
    executable,
    args,
    workingDirectory,
    requiredEnvironmentVariables: [],
    displayCommand: executable,
    executes: false as const,
    warnings: []
  }) }
}

function input(overrides: Partial<Parameters<AgentTaskService['run']>[0]> = {}): Parameters<AgentTaskService['run']>[0] {
  return {
    requestId: randomUUID(),
    profileId: randomUUID(),
    prompt: 'Inspect the project.',
    maxDurationSeconds: 30,
    maxTurns: 12,
    confirmExecution: true,
    confirmWrite: false,
    ...overrides
  }
}

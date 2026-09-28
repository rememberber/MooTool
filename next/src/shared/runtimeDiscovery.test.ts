// @vitest-environment node
import { chmod, mkdtemp, mkdir, rm, symlink, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { delimiter, join } from 'node:path'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { buildRuntimeEnvironment, detectRuntime, getRuntimeEnvironment, resolveRuntimeCommand } from '../../electron/main/runtimeDiscovery'
import { RuntimeExecutionService } from '../../electron/main/runtimeExecutionService'

const directories: string[] = []
afterEach(async () => {
  vi.unstubAllEnvs()
  await getRuntimeEnvironment(true)
  await Promise.all(directories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('runtime discovery', () => {
  it('merges shell PATH ahead of GUI PATH and filters unrelated environment variables', () => {
    const env = buildRuntimeEnvironment({ PATH: '/usr/bin', SECRET: 'private' }, { PATH: '/custom/bin:/usr/bin', JAVA_HOME: '/java' })
    if (process.platform !== 'win32') {
      expect(env.PATH?.split(delimiter).slice(0, 2)).toEqual(['/custom/bin', '/usr/bin'])
      expect(env.PATH?.split(delimiter)).toContain('/usr/local/bin')
      expect(env.PATH?.split(delimiter)).toContain('/opt/homebrew/bin')
      expect(env.PATH?.split(delimiter)).toContain('/java/bin')
    }
    expect(env.SECRET).toBeUndefined()
  })

  it('honors explicit paths even when they are invalid', async () => {
    const missing = join(tmpdir(), 'missing-mootool-node')
    const env = buildRuntimeEnvironment(process.env)
    expect(await resolveRuntimeCommand('node', missing, env)).toBe(missing)
    expect((await detectRuntime('node', missing, env)).available).toBe(false)
  })

  it.skipIf(process.platform === 'win32')('detects and runs the same shell-selected Node with a minimal GUI PATH', async () => {
    const root = await mkdtemp(join(tmpdir(), 'mootool discovery '))
    directories.push(root)
    const bin = join(root, 'bin')
    await mkdir(bin)
    await symlink(process.execPath, join(bin, 'node'))
    const shell = join(root, 'shell')
    await writeFile(shell, `#!/bin/sh\nprintf 'startup noise\\n\\0MOOTOOL_ENV\\0'\nprintf '%s\\0' 'PATH=${bin}:/usr/bin:/bin'\n`)
    await chmod(shell, 0o755)
    vi.stubEnv('SHELL', shell)
    vi.stubEnv('PATH', '/usr/bin:/bin')
    const env = await getRuntimeEnvironment(true)
    const status = await detectRuntime('node', '', env)
    expect(status.available).toBe(true)
    expect(status.command).toBe(join(bin, 'node'))
    const result = await new RuntimeExecutionService(join(root, 'runs')).run({
      requestId: 'discovery-test-001', runtime: 'node',
      code: 'import { execFileSync } from "node:child_process"; console.log(execFileSync("node", ["-p", "6 * 7"], { encoding: "utf8" }).trim())'
    }, {})
    expect(result.exitCode).toBe(0)
    expect(result.stdout.trim()).toBe('42')
    expect(result.command).toContain(join(bin, 'node'))

    vi.stubEnv('SHELL', join(root, 'missing-shell'))
    const fallback = await getRuntimeEnvironment(true)
    expect(fallback.PATH?.split(delimiter)).toContain('/usr/local/bin')
    expect(fallback.PATH).not.toContain(bin)
  })
})

import { execFileSync } from 'node:child_process'
import { existsSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { VaultGitService } from '../../electron/main/vaultGitService'

const tempDirectories: string[] = []
const gitAvailable = (() => {
  try {
    execFileSync('git', ['--version'], { stdio: 'ignore' })
    return true
  } catch {
    return false
  }
})()

afterEach(() => {
  for (const directory of tempDirectories.splice(0)) rmSync(directory, { recursive: true, force: true })
})

function createService(): { directory: string; service: VaultGitService } {
  const directory = mkdtempSync(join(tmpdir(), 'mootool-git-'))
  tempDirectories.push(directory)
  return { directory, service: new VaultGitService(directory, { username: 'MooTool Test' }) }
}

describe.skipIf(!gitAvailable)('VaultGitService', () => {
  it('initializes, reports changes, commits, shows history, and builds a diff', async () => {
    const { directory, service } = createService()
    expect((await service.status()).repository).toBe(false)
    expect((await service.action({ action: 'init' })).success).toBe(true)
    expect((await service.status()).branch).not.toBe('No')

    writeFileSync(join(directory, 'sample.json'), '{"value":1}\n')
    expect((await service.status()).changes[0]).toMatchObject({ path: 'sample.json', status: '??' })
    expect((await service.action({ action: 'commit', message: 'Add sample' })).success).toBe(true)
    expect((await service.history())[0]).toMatchObject({ message: 'Add sample' })
    expect((await service.history())[0]?.author).not.toBe('')

    writeFileSync(join(directory, 'sample.json'), '{"value":2}\n')
    expect(await service.diff({ path: 'sample.json' })).toContain('+{"value":2}')
  })

  it('rejects unsafe paths and unsupported remotes', async () => {
    const { service } = createService()
    await service.action({ action: 'init' })
    await expect(service.diff({ path: '../outside' })).rejects.toThrow('Invalid Git path')
    await expect(service.diff({ path: 'valid..name.json' })).resolves.toBe('')
    await expect(service.action({ action: 'configure-remote', remote: 'javascript:alert(1)' })).rejects.toThrow('Invalid Git remote')
  })

  it('discards tracked and untracked changes', async () => {
    const { directory, service } = createService()
    await service.action({ action: 'init' })
    writeFileSync(join(directory, 'tracked.json'), '{"value":1}\n')
    await service.action({ action: 'commit', message: 'Initial' })
    writeFileSync(join(directory, 'tracked.json'), '{"value":2}\n')
    writeFileSync(join(directory, 'untracked.json'), '{}\n')

    expect((await service.action({ action: 'discard', path: 'tracked.json' })).success).toBe(true)
    expect((await service.action({ action: 'discard', path: 'untracked.json' })).success).toBe(true)
    expect((await service.status()).changes).toEqual([])
    expect(existsSync(join(directory, 'untracked.json'))).toBe(false)
  })

  it('resolves and aborts real merge conflicts', async () => {
    const { directory, service } = createService()
    await service.action({ action: 'init' })
    writeFileSync(join(directory, 'conflict.json'), '{"side":"base"}\n')
    await service.action({ action: 'commit', message: 'Base' })
    const baseBranch = (await service.status()).branch
    git(directory, ['checkout', '-b', 'other'])
    writeFileSync(join(directory, 'conflict.json'), '{"side":"other"}\n')
    git(directory, ['add', '--all'])
    git(directory, ['commit', '-m', 'Other'])
    git(directory, ['checkout', baseBranch])
    writeFileSync(join(directory, 'conflict.json'), '{"side":"base-branch"}\n')
    git(directory, ['add', '--all'])
    git(directory, ['commit', '-m', 'Base branch'])
    expect(() => git(directory, ['merge', 'other'])).toThrow()
    expect(await service.status()).toMatchObject({ conflicts: 1, merging: true })

    expect((await service.action({ action: 'resolve-conflict', path: 'conflict.json', strategy: 'ours' })).success).toBe(true)
    expect((await service.status()).conflicts).toBe(0)
    expect((await service.action({ action: 'abort-merge' })).success).toBe(true)
    expect(await service.status()).toMatchObject({ conflicts: 0, merging: false })
  })
})

function git(directory: string, args: string[]): string {
  return execFileSync('git', args, { cwd: directory, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] })
}

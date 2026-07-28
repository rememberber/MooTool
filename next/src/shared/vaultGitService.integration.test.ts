import { execFileSync } from 'node:child_process'
import { closeSync, existsSync, mkdirSync, mkdtempSync, openSync, readdirSync, readFileSync, rmSync, utimesSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import { afterEach, describe, expect, it } from 'vitest'
import { VaultGitService } from '../../electron/main/vaultGitService'

const tempDirectories: string[] = []
const lockInspectorExecutable = process.platform === 'darwin' ? '/usr/sbin/lsof' : process.platform === 'linux' ? 'lsof' : undefined
const gitAvailable = (() => {
  try {
    execFileSync('git', ['--version'], { stdio: 'ignore' })
    return true
  } catch {
    return false
  }
})()
const lockInspectorAvailable = (() => {
  if (!lockInspectorExecutable) return false
  try {
    execFileSync(lockInspectorExecutable, ['-v'], { stdio: 'ignore' })
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

describe.skipIf(!gitAvailable)('VaultGitService', { timeout: 20_000 }, () => {
  it('initializes, reports changes, commits, shows history, and builds a diff', async () => {
    const { directory, service } = createService()
    expect((await service.status()).repository).toBe(false)
    expect((await service.action({ action: 'init' })).success).toBe(true)
    expect((await service.status()).branch).not.toBe('No')
    expect(readFileSync(join(directory, '.gitignore'), 'utf8')).toContain('.DS_Store')
    writeFileSync(join(directory, '.DS_Store'), 'ignored')
    expect((await service.status()).changes).toEqual([])

    writeFileSync(join(directory, 'sample.json'), '{"value":1}\n')
    expect((await service.status()).changes[0]).toMatchObject({ path: 'sample.json', status: '??' })
    expect((await service.action({ action: 'commit', message: 'Add sample' })).success).toBe(true)
    const commit = (await service.history())[0]
    expect(commit).toMatchObject({ message: 'Add sample' })
    expect(commit?.author).not.toBe('')
    expect((await service.diff({ commit: commit?.hash })).files[0]).toMatchObject({
      path: 'sample.json',
      before: '',
      after: '{"value":1}\n',
      preview: 'text'
    })

    writeFileSync(join(directory, 'sample.json'), '{"value":2}\n')
    expect((await service.diff({ path: 'sample.json' })).files[0]).toMatchObject({
      path: 'sample.json',
      before: '{"value":1}\n',
      after: '{"value":2}\n',
      preview: 'text'
    })
    writeFileSync(join(directory, 'notes.txt'), 'new note\n')
    expect((await service.action({ action: 'commit', message: 'Update sample' })).success).toBe(true)
    const updatedCommit = (await service.history())[0]
    const committedDiff = await service.diff({ commit: updatedCommit?.hash })
    expect(committedDiff.files.find((file) => file.path === 'sample.json')).toMatchObject({
      before: '{"value":1}\n',
      after: '{"value":2}\n'
    })
    expect(committedDiff.files.find((file) => file.path === 'notes.txt')).toMatchObject({
      before: '',
      after: 'new note\n'
    })
  })

  it('rejects unsafe paths and unsupported remotes', async () => {
    const { service } = createService()
    await service.action({ action: 'init' })
    await expect(service.diff({ path: '../outside' })).rejects.toThrow('Invalid Git path')
    await expect(service.diff({ path: 'valid..name.json' })).resolves.toEqual({ files: [] })
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
    const conflictedHead = (await service.history())[0]?.hash
    expect((await service.automaticCheckpoint('Must not commit conflicts')).success).toBe(true)
    expect((await service.history())[0]?.hash).toBe(conflictedHead)

    expect((await service.action({ action: 'resolve-conflict', path: 'conflict.json', strategy: 'ours' })).success).toBe(true)
    expect((await service.status()).conflicts).toBe(0)
    expect((await service.action({ action: 'abort-merge' })).success).toBe(true)
    expect(await service.status()).toMatchObject({ conflicts: 0, merging: false })
  })

  it('keeps unicode paths usable for status, diff, rename, and discard', async () => {
    const { directory, service } = createService()
    await service.action({ action: 'init' })
    const original = '中文随手记.txt'
    const renamed = '重命名后的随手记.txt'
    writeFileSync(join(directory, original), '第一版\n')
    await service.action({ action: 'commit', message: 'Add unicode note' })
    writeFileSync(join(directory, original), '第二版\n')

    expect((await service.status()).changes[0]?.path).toBe(original)
    expect((await service.diff({ path: original })).files[0]).toMatchObject({
      before: '第一版\n',
      after: '第二版\n'
    })
    expect((await service.action({ action: 'discard', path: original })).success).toBe(true)

    git(directory, ['mv', original, renamed])
    expect((await service.status()).changes[0]).toMatchObject({ path: renamed, originalPath: original })
  })

  it('does not adopt or stage files from a parent repository', async () => {
    const parent = mkdtempSync(join(tmpdir(), 'mootool-parent-git-'))
    tempDirectories.push(parent)
    git(parent, ['init'])
    git(parent, ['config', 'user.name', 'MooTool Test'])
    git(parent, ['config', 'user.email', 'mootool@example.com'])
    writeFileSync(join(parent, 'tracked.txt'), 'tracked\n')
    git(parent, ['add', 'tracked.txt'])
    git(parent, ['commit', '-m', 'Parent base'])
    const vault = join(parent, 'vault')
    mkdirSync(vault)
    writeFileSync(join(parent, 'outside-unrelated.txt'), 'outside\n')
    writeFileSync(join(vault, 'inside.json'), '{}\n')
    const service = new VaultGitService(vault, { username: 'MooTool Test' })

    expect((await service.status()).repository).toBe(false)
    expect((await service.action({ action: 'init' })).success).toBe(true)
    expect((await service.status()).repository).toBe(true)
    expect(git(parent, ['status', '--porcelain=v1'])).toContain('?? outside-unrelated.txt')
  })

  it('removes a configured remote when an empty URL is saved', async () => {
    const { service } = createService()
    const remote = mkdtempSync(join(tmpdir(), 'mootool-remote-git-'))
    tempDirectories.push(remote)
    git(remote, ['init', '--bare'])
    await service.action({ action: 'init' })

    expect((await service.action({ action: 'configure-remote', remote: pathToFileURL(remote).href })).success).toBe(true)
    expect((await service.status()).remote).not.toBe('')
    expect((await service.action({ action: 'configure-remote', remote: '' })).success).toBe(true)
    expect((await service.status()).remote).toBe('')
  })

  it('pushes automatic checkpoints when a remote is configured', async () => {
    const { directory, service } = createService()
    const remote = mkdtempSync(join(tmpdir(), 'mootool-checkpoint-remote-'))
    tempDirectories.push(remote)
    git(remote, ['init', '--bare'])
    await service.action({ action: 'init' })
    await service.action({ action: 'configure-remote', remote: pathToFileURL(remote).href })
    writeFileSync(join(directory, 'checkpoint.json'), '{"saved":true}\n')

    expect((await service.automaticCheckpoint('Automatic checkpoint')).success).toBe(true)
    expect(git(remote, ['log', '--all', '--pretty=%s'])).toContain('Automatic checkpoint')
    expect((await service.status()).ahead).toBe(0)
  })

  it('initializes a new Vault when creating its first automatic checkpoint', async () => {
    const { directory, service } = createService()
    writeFileSync(join(directory, 'first-note.txt'), 'Saved automatically\n')

    expect((await service.status()).repository).toBe(false)
    expect((await service.automaticCheckpoint('Automatic checkpoint')).success).toBe(true)
    expect((await service.status()).repository).toBe(true)
    expect((await service.status()).changes).toEqual([])
    expect(git(directory, ['log', '--pretty=%s'])).toContain('Initial MooTool Vault setup')
  })

  it('repairs a stale index lock and retries the blocked Git command', async () => {
    const { directory, service } = createService()
    await service.action({ action: 'init' })
    writeFileSync(join(directory, 'stale-lock.txt'), 'Recovered\n')
    const lockPath = join(directory, '.git', 'index.lock')
    writeFileSync(lockPath, '')
    const staleTime = new Date(Date.now() - 10 * 60_000)
    utimesSync(lockPath, staleTime, staleTime)

    expect((await service.action({ action: 'commit', message: 'Recover stale lock' })).success).toBe(true)
    expect(existsSync(lockPath)).toBe(false)
    expect(readdirSync(join(directory, '.git')).some((name) => name.startsWith('index.lock.mootool-stale-'))).toBe(false)
    expect(git(directory, ['log', '-1', '--pretty=%s']).trim()).toBe('Recover stale lock')
  })

  it('does not remove a recent index lock that may belong to another Git process', async () => {
    const { directory, service } = createService()
    await service.action({ action: 'init' })
    writeFileSync(join(directory, 'active-lock.txt'), 'Keep lock\n')
    const lockPath = join(directory, '.git', 'index.lock')
    writeFileSync(lockPath, '')

    const result = await service.action({ action: 'commit', message: 'Must stay blocked' })
    expect(result.success).toBe(false)
    expect(result.message).toContain('index.lock')
    expect(existsSync(lockPath)).toBe(true)
  })

  it.skipIf(!lockInspectorAvailable)('does not remove an old index lock while another process holds it open', async () => {
    const { directory, service } = createService()
    await service.action({ action: 'init' })
    writeFileSync(join(directory, 'held-lock.txt'), 'Keep held lock\n')
    const lockPath = join(directory, '.git', 'index.lock')
    writeFileSync(lockPath, '')
    const staleTime = new Date(Date.now() - 10 * 60_000)
    utimesSync(lockPath, staleTime, staleTime)
    const descriptor = openSync(lockPath, 'r')

    try {
      const result = await service.action({ action: 'commit', message: 'Must stay blocked' })
      expect(result.success).toBe(false)
      expect(existsSync(lockPath)).toBe(true)
    } finally {
      closeSync(descriptor)
    }
  })

  it('serializes Git actions created by separate service instances for the same Vault', async () => {
    const { directory, service } = createService()
    await service.action({ action: 'init' })
    const secondService = new VaultGitService(directory, { username: 'MooTool Test' })
    writeFileSync(join(directory, 'concurrent.txt'), 'One change\n')

    const results = await Promise.all([
      service.action({ action: 'commit', message: 'First checkpoint' }),
      secondService.action({ action: 'commit', message: 'Second checkpoint' })
    ])

    expect(results.every((result) => result.success)).toBe(true)
    expect(git(directory, ['log', '--pretty=%s', '--', 'concurrent.txt']).trim().split(/\r?\n/)).toEqual(['First checkpoint'])
  })

  it('detects and continues a rebase after conflicts are resolved', async () => {
    const { directory, service } = createService()
    await service.action({ action: 'init' })
    writeFileSync(join(directory, 'conflict.json'), '{"side":"base"}\n')
    await service.action({ action: 'commit', message: 'Base' })
    const baseBranch = (await service.status()).branch
    git(directory, ['checkout', '-b', 'feature'])
    writeFileSync(join(directory, 'conflict.json'), '{"side":"feature"}\n')
    git(directory, ['add', '--all'])
    git(directory, ['commit', '-m', 'Feature'])
    git(directory, ['checkout', baseBranch])
    writeFileSync(join(directory, 'conflict.json'), '{"side":"main"}\n')
    git(directory, ['add', '--all'])
    git(directory, ['commit', '-m', 'Main'])
    git(directory, ['checkout', 'feature'])
    expect(() => git(directory, ['rebase', baseBranch])).toThrow()

    expect(await service.status()).toMatchObject({ operation: 'rebase', conflicts: 1, merging: true })
    expect((await service.action({ action: 'resolve-conflict', path: 'conflict.json', strategy: 'theirs' })).success).toBe(true)
    expect(await service.status()).toMatchObject({ operation: 'rebase', conflicts: 0, merging: true })
    expect((await service.action({ action: 'continue-operation' })).success).toBe(true)
    expect(await service.status()).toMatchObject({ operation: 'none', conflicts: 0, merging: false })
  })
})

function git(directory: string, args: string[]): string {
  return execFileSync('git', args, { cwd: directory, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] })
}

import { mkdir, mkdtemp, readFile, realpath, rm, symlink, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { ConfigChangeService, writeFileAtomically, type SnapshotProtector } from '../../electron/main/ai/configChangeService'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('ConfigChangeService', () => {
  it('applies an approved plan with an encrypted snapshot and rolls it back', async () => {
    const { root, snapshots } = await createFixture()
    const existing = join(root, 'AGENTS.md')
    const created = join(root, 'CLAUDE.md')
    await writeFile(existing, 'api_key = "SECRET_SNAPSHOT_VALUE"\n', { mode: 0o640 })
    const service = new ConfigChangeService({ snapshotDirectory: snapshots, protector: testProtector })

    const plan = await service.createPlan(root, [
      { targetPath: 'AGENTS.md', nextContent: 'updated instructions\n', summary: 'Update shared instructions' },
      { targetPath: 'CLAUDE.md', nextContent: 'Read @AGENTS.md\n', summary: 'Create Claude compatibility entry' }
    ])

    expect(plan.operations).toEqual([
      expect.objectContaining({ kind: 'update', targetPath: await realpath(existing), beforeSizeBytes: 34 }),
      expect.objectContaining({ kind: 'create', targetPath: join(await realpath(root), 'CLAUDE.md'), beforeSizeBytes: 0 })
    ])
    expect(JSON.stringify(plan)).not.toContain('SECRET_SNAPSHOT_VALUE')
    expect(plan.operations[0].redactedDiff).toContain('[REDACTED]')

    const applied = await service.apply(plan.id)
    expect(await readFile(existing, 'utf8')).toBe('updated instructions\n')
    expect(await readFile(created, 'utf8')).toBe('Read @AGENTS.md\n')
    const encryptedSnapshot = await readFile(join(snapshots, `${applied.snapshotId}.snapshot`))
    expect(encryptedSnapshot.toString('utf8')).not.toContain('SECRET_SNAPSHOT_VALUE')

    const rolledBack = await service.rollback(applied.snapshotId)
    expect(rolledBack.operationCount).toBe(2)
    expect(await readFile(existing, 'utf8')).toBe('api_key = "SECRET_SNAPSHOT_VALUE"\n')
    await expect(readFile(created)).rejects.toMatchObject({ code: 'ENOENT' })
  })

  it('invalidates a plan when the source hash changes', async () => {
    const { root, snapshots } = await createFixture()
    const target = join(root, 'AGENTS.md')
    await writeFile(target, 'one\n')
    const service = new ConfigChangeService({ snapshotDirectory: snapshots, protector: testProtector })
    const plan = await service.createPlan(root, [{ targetPath: target, nextContent: 'two\n', summary: 'Update instructions' }])

    await writeFile(target, 'external edit\n')

    await expect(service.apply(plan.id)).rejects.toThrow('changed after preview')
    expect(await readFile(target, 'utf8')).toBe('external edit\n')
  })

  it('restores already-applied operations when a later atomic write fails', async () => {
    const { root, snapshots } = await createFixture()
    const first = join(root, 'first.md')
    const second = join(root, 'second.md')
    await Promise.all([writeFile(first, 'first-before'), writeFile(second, 'second-before')])
    let writes = 0
    const service = new ConfigChangeService({
      snapshotDirectory: snapshots,
      protector: testProtector,
      atomicWriter: async (path, content, mode) => {
        writes += 1
        if (writes === 2) throw new Error('simulated disk failure')
        await writeFileAtomically(path, content, mode)
      }
    })
    const plan = await service.createPlan(root, [
      { targetPath: first, nextContent: 'first-after', summary: 'First update' },
      { targetPath: second, nextContent: 'second-after', summary: 'Second update' }
    ])

    await expect(service.apply(plan.id)).rejects.toThrow('simulated disk failure')
    expect(await readFile(first, 'utf8')).toBe('first-before')
    expect(await readFile(second, 'utf8')).toBe('second-before')
  })

  it('rejects target escape, symbolic links, unavailable encryption, and stale rollback', async () => {
    const { fixtureRoot, root, snapshots } = await createFixture()
    const outside = join(fixtureRoot, 'outside.md')
    const target = join(root, 'AGENTS.md')
    const link = join(root, 'linked.md')
    await Promise.all([writeFile(outside, 'outside'), writeFile(target, 'before')])
    const service = new ConfigChangeService({ snapshotDirectory: snapshots, protector: testProtector })

    await expect(service.createPlan(root, [{ targetPath: '../outside.md', nextContent: 'bad', summary: 'Escape' }])).rejects.toThrow('escapes')
    try {
      await symlink(outside, link)
      await expect(service.createPlan(root, [{ targetPath: link, nextContent: 'bad', summary: 'Follow link' }])).rejects.toThrow('Symbolic-link')
    } catch (error) {
      if (!isPermissionError(error)) throw error
    }

    const unavailable = new ConfigChangeService({ snapshotDirectory: snapshots, protector: { ...testProtector, isAvailable: () => false } })
    const blocked = await unavailable.createPlan(root, [{ targetPath: target, nextContent: 'after', summary: 'Blocked update' }])
    await expect(unavailable.apply(blocked.id)).rejects.toThrow('Secure snapshot storage')

    const plan = await service.createPlan(root, [{ targetPath: target, nextContent: 'after', summary: 'Safe update' }])
    const result = await service.apply(plan.id)
    await writeFile(target, 'changed again')
    await expect(service.rollback(result.snapshotId)).rejects.toThrow('changed after apply')
    expect(await readFile(outside, 'utf8')).toBe('outside')
  })

  it('locks a target while an atomic change is in progress', async () => {
    const { root, snapshots } = await createFixture()
    const target = join(root, 'AGENTS.md')
    await writeFile(target, 'before')
    let releaseWrite!: () => void
    let signalWriterStarted!: () => void
    const writerStarted = new Promise<void>((resolve) => { signalWriterStarted = resolve })
    const writeReleased = new Promise<void>((resolve) => { releaseWrite = resolve })
    const service = new ConfigChangeService({
      snapshotDirectory: snapshots,
      protector: testProtector,
      atomicWriter: async (path, content, mode) => {
        signalWriterStarted()
        await writeReleased
        await writeFileAtomically(path, content, mode)
      }
    })
    const first = await service.createPlan(root, [{ targetPath: target, nextContent: 'first', summary: 'First' }])
    const second = await service.createPlan(root, [{ targetPath: target, nextContent: 'second', summary: 'Second' }])

    const applying = service.apply(first.id)
    await writerStarted
    await expect(service.apply(second.id)).rejects.toThrow('already in progress')
    releaseWrite()
    await applying
  })

  it('creates and rolls back missing directory trees with text and binary files', async () => {
    const { root, snapshots } = await createFixture()
    const service = new ConfigChangeService({ snapshotDirectory: snapshots, protector: testProtector })
    const binary = Buffer.from([0, 255, 1, 2, 3])
    const plan = await service.createPlan(root, [
      { targetPath: '.agents/skills/demo/SKILL.md', nextContent: '# Demo\n', summary: 'Install Skill entry', expectedState: 'missing' },
      { targetPath: '.agents/skills/demo/assets/icon.bin', nextContentBase64: binary.toString('base64'), summary: 'Install Skill asset', expectedState: 'missing' }
    ])

    expect(plan.operations[1]).toMatchObject({ binary: true, redactedDiff: expect.stringContaining('Binary file') })
    const applied = await service.apply(plan.id)
    expect(await readFile(join(root, '.agents', 'skills', 'demo', 'SKILL.md'), 'utf8')).toBe('# Demo\n')
    expect(await readFile(join(root, '.agents', 'skills', 'demo', 'assets', 'icon.bin'))).toEqual(binary)

    await service.rollback(applied.snapshotId)
    await expect(readFile(join(root, '.agents', 'skills', 'demo', 'SKILL.md'))).rejects.toMatchObject({ code: 'ENOENT' })
    await expect(readFile(join(root, '.agents'))).rejects.toMatchObject({ code: 'ENOENT' })
  })
})

const testProtector: SnapshotProtector = {
  isAvailable: () => true,
  encrypt: (value) => Buffer.from(value.map((byte) => byte ^ 0xa5)),
  decrypt: (value) => Buffer.from(value.map((byte) => byte ^ 0xa5))
}

async function createFixture(): Promise<{ fixtureRoot: string; root: string; snapshots: string }> {
  const fixtureRoot = await mkdtemp(join(tmpdir(), 'mootool-ai-changes-'))
  temporaryDirectories.push(fixtureRoot)
  const root = join(fixtureRoot, 'project')
  const snapshots = join(fixtureRoot, 'snapshots')
  await mkdir(root, { recursive: true })
  return { fixtureRoot, root, snapshots }
}

function isPermissionError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'code' in error && (error.code === 'EPERM' || error.code === 'EACCES')
}

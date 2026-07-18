import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { ConfigChangeService, type SnapshotProtector } from '../../electron/main/ai/configChangeService'
import { claudeCompatibilityEntry, InstructionChangeService } from '../../electron/main/ai/instructionChangeService'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('InstructionChangeService', () => {
  it('previews, verifies, and rolls back a thin Claude compatibility entry', async () => {
    const { root, service } = await fixture()
    await writeFile(join(root, 'AGENTS.md'), '# Shared rules\n')

    const plan = await service.previewClaudeCompatibilityEntry(root)

    expect(plan.operations).toEqual([expect.objectContaining({ kind: 'create', summary: expect.stringContaining('AGENTS.md') })])
    expect(plan.operations[0].redactedDiff).toContain('+Read and follow the shared project instructions in @AGENTS.md.')
    const applied = await service.applyClaudeCompatibilityEntry(plan.id)
    expect(await readFile(join(root, 'CLAUDE.md'), 'utf8')).toBe(claudeCompatibilityEntry)

    await service.rollbackClaudeCompatibilityEntry(applied.snapshotId)
    await expect(readFile(join(root, 'CLAUDE.md'))).rejects.toMatchObject({ code: 'ENOENT' })
  })

  it('never overwrites an existing CLAUDE.md', async () => {
    const { root, service } = await fixture()
    await Promise.all([
      writeFile(join(root, 'AGENTS.md'), '# Shared rules\n'),
      writeFile(join(root, 'CLAUDE.md'), '# Existing Claude rules\n')
    ])

    await expect(service.previewClaudeCompatibilityEntry(root)).rejects.toThrow('already exists')
    expect(await readFile(join(root, 'CLAUDE.md'), 'utf8')).toBe('# Existing Claude rules\n')
  })

  it('automatically restores the snapshot when post-write verification fails', async () => {
    const { root, service } = await fixture()
    const agents = join(root, 'AGENTS.md')
    await writeFile(agents, '# Shared rules\n')
    const plan = await service.previewClaudeCompatibilityEntry(root)
    await rm(agents)

    await expect(service.applyClaudeCompatibilityEntry(plan.id)).rejects.toThrow('AGENTS.md disappeared')
    await expect(readFile(join(root, 'CLAUDE.md'))).rejects.toMatchObject({ code: 'ENOENT' })
  })
})

async function fixture(): Promise<{ root: string; service: InstructionChangeService }> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-ai-instructions-'))
  temporaryDirectories.push(directory)
  const root = join(directory, 'project')
  await mkdir(root)
  const changes = new ConfigChangeService({ snapshotDirectory: join(directory, 'snapshots'), protector })
  return { root, service: new InstructionChangeService(changes) }
}

const protector: SnapshotProtector = {
  isAvailable: () => true,
  encrypt: (value) => Buffer.from(value).reverse(),
  decrypt: (value) => Buffer.from(value).reverse()
}

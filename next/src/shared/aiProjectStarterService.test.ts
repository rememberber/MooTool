import { mkdir, mkdtemp, readFile, realpath, rm, symlink, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { ConfigChangeService, type SnapshotProtector } from '../../electron/main/ai/configChangeService'
import { ProjectStarterService } from '../../electron/main/ai/projectStarterService'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('ProjectStarterService', () => {
  it('previews selected project artifacts, preserves existing files, applies atomically, and rolls back', async () => {
    const fixture = await createFixture()
    await writeFile(join(fixture.project, 'AGENTS.md'), '# Existing instructions\n')
    await writeFile(join(fixture.project, '.gitignore'), 'dist/\n')

    const preview = await fixture.service.preview({ projectRoot: fixture.project, items: ['instructions', 'projectSkill', 'mcpManifest', 'gitignore'] })

    expect(preview.skipped).toContainEqual(expect.objectContaining({ item: 'instructions', reason: 'alreadyExists' }))
    expect(preview.plan.operations).toHaveLength(3)
    expect(preview.plan.operations.map((operation) => operation.targetPath)).toEqual(expect.arrayContaining([
      join(fixture.project, '.agents', 'skills', 'project-workflow', 'SKILL.md'),
      join(fixture.project, '.mcp.json'),
      join(fixture.project, '.gitignore')
    ]))
    expect(preview.plan.operations.find((operation) => operation.targetPath.endsWith('.gitignore'))?.redactedDiff).toContain('dist/')

    const applied = await fixture.service.apply(preview.plan.id)
    expect(await readFile(join(fixture.project, 'AGENTS.md'), 'utf8')).toBe('# Existing instructions\n')
    expect(await readFile(join(fixture.project, '.mcp.json'), 'utf8')).toContain('"mcpServers"')
    expect(await readFile(join(fixture.project, '.agents', 'skills', 'project-workflow', 'SKILL.md'), 'utf8')).toContain('name: project-workflow')
    expect(await readFile(join(fixture.project, '.gitignore'), 'utf8')).toContain('.mootool/')

    await fixture.service.rollback(applied.snapshotId)
    await expect(readFile(join(fixture.project, '.mcp.json'))).rejects.toMatchObject({ code: 'ENOENT' })
    expect(await readFile(join(fixture.project, '.gitignore'), 'utf8')).toBe('dist/\n')
  })

  it('refuses symbolic-link targets and reports a fully configured selection', async () => {
    const fixture = await createFixture()
    const outside = join(fixture.root, 'outside.md')
    await writeFile(outside, 'outside')
    await symlink(outside, join(fixture.project, 'AGENTS.md'))
    await expect(fixture.service.preview({ projectRoot: fixture.project, items: ['instructions'] })).rejects.toThrow('symbolic link')
    await rm(join(fixture.project, 'AGENTS.md'))
    await writeFile(join(fixture.project, 'AGENTS.md'), 'existing')
    await expect(fixture.service.preview({ projectRoot: fixture.project, items: ['instructions'] })).rejects.toThrow('already configured')
  })
})

async function createFixture() {
  const root = await mkdtemp(join(tmpdir(), 'mootool-project-starter-'))
  temporaryDirectories.push(root)
  const projectPath = join(root, 'project')
  await mkdir(projectPath)
  const project = await realpath(projectPath)
  const changes = new ConfigChangeService({ snapshotDirectory: join(root, 'snapshots'), protector })
  return { root, project, service: new ProjectStarterService(changes) }
}

const protector: SnapshotProtector = {
  isAvailable: () => true,
  encrypt: (value) => Buffer.from(value).reverse(),
  decrypt: (value) => Buffer.from(value).reverse()
}

// @vitest-environment node
import { mkdtemp, mkdir, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { JsonVaultRepository } from '../../electron/main/jsonVaultRepository'
import { QuickNoteVaultRepository } from '../../electron/main/quickNoteVaultRepository'
import { VaultWorkspaceService } from '../../electron/main/vaultWorkspaceService'

const directories: string[] = []
afterEach(async () => { await Promise.all(directories.splice(0).map((path) => rm(path, { recursive: true, force: true }))) })

async function fixture() {
  const root = await mkdtemp(join(tmpdir(), 'mootool-workspace-'))
  directories.push(root)
  let paths: Record<string, string> = {}
  const storage = { get: () => paths, set: (next: Record<string, string>) => { paths = next } }
  return { root, storage, service: new VaultWorkspaceService(storage) }
}

describe('VaultWorkspaceService', () => {
  it('restores the last opened nested JSON file across service restarts', async () => {
    const { root, storage, service } = await fixture()
    const repository = new JsonVaultRepository(root)
    await repository.save({ relativePath: 'a.json', content: '{"first":true}' })
    await repository.save({ relativePath: 'folder/z.json', content: '{"last":true}' })
    await service.remember({ kind: 'json', rootDirectory: root, relativePath: 'folder/z.json' }, repository)
    const restarted = new VaultWorkspaceService(storage)
    const result = await restarted.open('json', root, repository, () => repository.createDefault())
    expect(result.file).toMatchObject({ relativePath: 'folder/z.json', content: '{"last":true}' })
  })

  it('restores a note rather than choosing the most recently modified note', async () => {
    const { root, storage, service } = await fixture()
    const repository = new QuickNoteVaultRepository(root)
    const lastOpened = await repository.create({ title: 'last opened' })
    await service.remember({ kind: 'quickNote', rootDirectory: root, relativePath: lastOpened.relativePath }, repository)
    await repository.create({ title: 'newer' })
    const result = await new VaultWorkspaceService(storage).open('quickNote', root, repository, () => repository.create({ title: 'Untitled' }))
    expect(result.file.relativePath).toBe(lastOpened.relativePath)
  })

  it('creates exactly one initial JSON file for concurrent empty-vault startups', async () => {
    const { root, service } = await fixture()
    const repository = new JsonVaultRepository(root)
    const results = await Promise.all(Array.from({ length: 8 }, () => service.open('json', root, repository, () => repository.createDefault())))
    expect(new Set(results.map((result) => result.file.relativePath)).size).toBe(1)
    expect(results[0].file.content).toBe('{}')
    expect(await repository.list()).toHaveLength(1)
  })

  it('creates exactly one initial note, including when the vault only contains folders', async () => {
    const { root, service } = await fixture()
    const repository = new QuickNoteVaultRepository(root)
    await repository.createFolder('empty')
    const create = () => repository.create({ title: 'Untitled', fontName: 'Georgia', fontSize: 17, lineWrap: false })
    const results = await Promise.all([service.open('quickNote', root, repository, create), service.open('quickNote', root, repository, create)])
    expect(results[0].file.relativePath).toBe(results[1].file.relativePath)
    expect(results[0].file.metadata).toMatchObject({ fontName: 'Georgia', fontSize: 17, lineWrap: false })
  })

  it('falls back to an existing file after deletion and creates a file only when none remain', async () => {
    const { root, service } = await fixture()
    const repository = new JsonVaultRepository(root)
    await repository.save({ relativePath: 'a.json', content: '{"keep":true}' })
    await repository.save({ relativePath: 'z.json', content: '{}' })
    await service.remember({ kind: 'json', rootDirectory: root, relativePath: 'z.json' }, repository)
    await repository.delete('z.json')
    expect((await service.open('json', root, repository, () => repository.createDefault())).file.relativePath).toBe('a.json')
    await repository.delete('a.json')
    expect((await service.open('json', root, repository, () => repository.createDefault())).file.relativePath).toBe('snippet.json')
  })

  it('keeps selections separate by vault root and follows directory renames', async () => {
    const { root, service } = await fixture()
    const firstRoot = join(root, 'one')
    const secondRoot = join(root, 'two')
    const first = new JsonVaultRepository(firstRoot)
    const second = new JsonVaultRepository(secondRoot)
    await first.save({ relativePath: 'folder/data.json', content: '{"one":true}' })
    await second.save({ relativePath: 'other.json', content: '{"two":true}' })
    await service.remember({ kind: 'json', rootDirectory: firstRoot, relativePath: 'folder/data.json' }, first)
    await service.remember({ kind: 'json', rootDirectory: secondRoot, relativePath: 'other.json' }, second)
    const renamed = await first.renameEntry({ relativePath: 'folder', name: 'renamed' })
    service.relocate('json', firstRoot, 'folder', renamed)
    expect((await service.open('json', firstRoot, first, () => first.createDefault())).file.relativePath).toBe('renamed/data.json')
    expect((await service.open('json', secondRoot, second, () => second.createDefault())).file.relativePath).toBe('other.json')
  })

  it('does not treat ignored files as an empty vault or overwrite existing default names', async () => {
    const { root, service } = await fixture()
    const repository = new JsonVaultRepository(root)
    await repository.save({ relativePath: 'snippet.json', content: '{"original":true}' })
    await writeFile(join(root, '.gitignore'), '*.json\n')
    expect((await service.open('json', root, repository, () => repository.createDefault())).file.content).toBe('{"original":true}')
    await repository.delete('snippet.json')
    await mkdir(join(root, 'snippet.json'))
    const result = await service.open('json', root, repository, () => repository.createDefault())
    expect(result.file.relativePath).not.toBe('snippet.json')
  })

  it('does not silently replace an unreadable remembered file', async () => {
    const { root, service } = await fixture()
    const repository = new JsonVaultRepository(root)
    await repository.save({ relativePath: 'data.json', content: '{}' })
    await service.remember({ kind: 'json', rootDirectory: root, relativePath: 'data.json' }, repository)
    vi.spyOn(repository, 'read').mockRejectedValue(Object.assign(new Error('Permission denied'), { code: 'EACCES' }))
    const create = vi.fn(() => repository.createDefault())
    await expect(service.open('json', root, repository, create)).rejects.toThrow('Permission denied')
    expect(create).not.toHaveBeenCalled()
  })
})

import { mkdtempSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { JsonVaultRepository } from '../../electron/main/jsonVaultRepository'

const tempDirectories: string[] = []

afterEach(() => {
  for (const directory of tempDirectories.splice(0)) rmSync(directory, { recursive: true, force: true })
})

function createRepository(): JsonVaultRepository {
  const directory = mkdtempSync(join(tmpdir(), 'mootool-vault-'))
  tempDirectories.push(directory)
  return new JsonVaultRepository(directory)
}

describe('JsonVaultRepository', () => {
  it('creates nested snippets, lists them as a tree, reads, and deletes them', async () => {
    const repository = createRepository()
    const saved = await repository.save({ relativePath: 'drafts/request', content: '{"ok":true}' })
    expect(saved.relativePath).toBe('drafts/request.json')
    expect((await repository.read(saved.relativePath)).content).toBe('{"ok":true}')
    expect(await repository.list()).toMatchObject([{
      name: 'drafts',
      kind: 'directory',
      children: [{ name: 'request.json', kind: 'file', relativePath: 'drafts/request.json' }]
    }])
    await repository.delete(saved.relativePath)
    expect((await repository.list())[0]?.children).toEqual([])
  })

  it('rejects traversal, hidden paths, and non-JSON reads', async () => {
    const repository = createRepository()
    await expect(repository.save({ relativePath: '../outside.json', content: '{}' })).rejects.toThrow('Invalid Vault path')
    await expect(repository.save({ relativePath: '.hidden/data.json', content: '{}' })).rejects.toThrow('Invalid Vault path')
    await expect(repository.read('/tmp/outside.json')).rejects.toThrow('Invalid Vault path')
  })

  it('creates, renames, moves, duplicates, and deletes folders and snippets', async () => {
    const repository = createRepository()
    await repository.createFolder('drafts')
    await repository.createFolder('archive')
    const source = await repository.save({ relativePath: 'drafts/request', content: '{"ok":true}' })
    const renamed = await repository.renameEntry({ relativePath: source.relativePath, name: 'response.json' })
    expect(renamed).toBe('drafts/response.json')
    const moved = await repository.moveEntry({ relativePath: renamed, targetDirectory: 'archive' })
    expect(moved).toBe('archive/response.json')
    const copy = await repository.duplicate(moved)
    expect(copy.relativePath).toBe('archive/response Copy.json')
    await repository.delete(copy.relativePath)
    await expect(repository.delete('archive')).rejects.toThrow('empty')
    await repository.delete(moved)
    await repository.delete('archive')
    expect((await repository.list()).map((node) => node.name)).toEqual(['drafts'])
  })

  it('filters gitignored entries, sorts by modification time, and rejects symlinks', async () => {
    const directory = mkdtempSync(join(tmpdir(), 'mootool-vault-'))
    tempDirectories.push(directory)
    const repository = new JsonVaultRepository(directory)
    writeFileSync(join(directory, '.gitignore'), 'ignored.json\n')
    writeFileSync(join(directory, 'ignored.json'), '{}')
    await repository.save({ relativePath: 'older.json', content: '{"order":1}' })
    await new Promise((resolve) => setTimeout(resolve, 15))
    await repository.save({ relativePath: 'newer.json', content: '{"order":2}' })
    expect((await repository.list({ hideIgnored: true, sort: 'modified' })).map((node) => node.name)).toEqual(['newer.json', 'older.json'])
    symlinkSync(join(directory, 'newer.json'), join(directory, 'linked.json'))
    await expect(repository.read('linked.json')).rejects.toThrow('Symbolic links')
  })
})

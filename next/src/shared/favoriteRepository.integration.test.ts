import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import { afterEach, describe, expect, it } from 'vitest'
import { FavoriteRepository } from '../../electron/main/favoriteRepository'

const directories: string[] = []

afterEach(async () => {
  await Promise.all(directories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('FavoriteRepository', () => {
  it('stores, updates, lists and deletes favorites by kind', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'mootool-favorite-'))
    directories.push(directory)
    const repository = new FavoriteRepository(join(directory, 'MooToolNext.db'))

    const first = repository.save({ kind: 'regex', name: 'Email', value: 'first' })
    repository.save({ kind: 'regex', name: 'Email', value: 'updated', description: 'sample' })
    repository.save({ kind: 'cron', name: 'Daily', value: '0 0 0 * * *' })

    expect(repository.list('regex')).toMatchObject([{ name: 'Email', value: 'updated', description: 'sample' }])
    expect(repository.list('cron')).toHaveLength(1)
    repository.delete(first.id)
    expect(repository.list('regex')).toEqual([])
    repository.close()
  })

  it('stores colors in multiple folders and cascades folder deletion', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'mootool-favorite-folders-'))
    directories.push(directory)
    const repository = new FavoriteRepository(join(directory, 'MooToolNext.db'))

    const folders = repository.listFolders('color')
    expect(folders).toMatchObject([{ title: '默认收藏夹' }])
    const brand = repository.createFolder({ kind: 'color', title: '品牌色' })
    const product = repository.createFolder({ kind: 'color', title: '产品色' })
    repository.save({ kind: 'color', folderId: brand.id, name: 'Primary', value: '#112233' })
    repository.save({ kind: 'color', folderId: product.id, name: 'Primary', value: '#445566' })

    expect(repository.list('color', brand.id)).toMatchObject([{ folderId: brand.id, value: '#112233' }])
    expect(repository.list('color', product.id)).toMatchObject([{ folderId: product.id, value: '#445566' }])
    expect(repository.renameFolder({ id: product.id, title: '产品主题色' })).toMatchObject({ title: '产品主题色' })
    repository.deleteFolder(brand.id)
    expect(repository.list('color', brand.id)).toEqual([])
    expect(repository.listFolders('color')).toHaveLength(2)
    repository.close()
  })

  it('moves favorites from the flat schema into the default folder', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'mootool-favorite-upgrade-'))
    directories.push(directory)
    const databasePath = join(directory, 'MooToolNext.db')
    const database = new DatabaseSync(databasePath)
    database.exec(`
      CREATE TABLE t_next_favorite (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        kind TEXT NOT NULL,
        name TEXT NOT NULL,
        value TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT '',
        create_time TEXT NOT NULL,
        UNIQUE(kind, name)
      );
      INSERT INTO t_next_favorite (kind, name, value, description, create_time)
      VALUES ('color', 'Legacy color', '#de8f7d', '', '2026-01-01 00:00:00');
    `)
    database.close()

    const repository = new FavoriteRepository(databasePath)
    const folder = repository.listFolders('color')[0]
    expect(folder).toMatchObject({ title: '默认收藏夹' })
    expect(repository.list('color', folder.id)).toMatchObject([{ name: 'Legacy color', value: '#de8f7d', folderId: folder.id }])
    repository.close()
  })
})

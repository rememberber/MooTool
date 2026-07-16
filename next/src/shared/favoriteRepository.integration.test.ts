import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
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
})

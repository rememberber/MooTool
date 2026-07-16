// @vitest-environment node
import { mkdtemp, mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { BackupService } from '../../electron/main/backupService'

const directories: string[] = []

afterEach(async () => {
  await Promise.all(directories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('BackupService', () => {
  it('exports database, config, images and both Vaults into a timestamped folder', async () => {
    const source = await mkdtemp(join(tmpdir(), 'mootool-backup-source-'))
    const target = await mkdtemp(join(tmpdir(), 'mootool-backup-target-'))
    directories.push(source, target)
    await mkdir(join(source, 'images'))
    await mkdir(join(source, 'quick-notes'))
    await mkdir(join(source, 'json-vault'))
    await writeFile(join(source, 'MooToolNext.db'), 'db')
    await writeFile(join(source, 'settings.json'), '{}')
    await writeFile(join(source, 'images', 'moo.png'), 'image')
    await writeFile(join(source, 'quick-notes', 'Note.txt'), 'note')
    await writeFile(join(source, 'json-vault', 'sample.json'), '{}')
    const service = new BackupService({
      dataDirectory: source,
      databasePath: join(source, 'MooToolNext.db'),
      settingsPath: join(source, 'settings.json'),
      imagesPath: join(source, 'images'),
      quickNotePath: join(source, 'quick-notes'),
      jsonVaultPath: join(source, 'json-vault')
    })

    const result = await service.export(target, 'all')
    expect(result.exported).toEqual(expect.arrayContaining(['database/MooToolNext.db', 'config/settings.json', 'images', 'quick-notes', 'json-vault']))
    expect(await readFile(join(result.directory, 'quick-notes', 'Note.txt'), 'utf8')).toBe('note')
  })

  it('can export a single category and skips missing sources', async () => {
    const source = await mkdtemp(join(tmpdir(), 'mootool-backup-source-'))
    const target = await mkdtemp(join(tmpdir(), 'mootool-backup-target-'))
    directories.push(source, target)
    await writeFile(join(source, 'settings.json'), '{}')
    const service = new BackupService({
      dataDirectory: source,
      databasePath: join(source, 'missing.db'),
      settingsPath: join(source, 'settings.json'),
      imagesPath: join(source, 'images'),
      quickNotePath: join(source, 'quick-notes'),
      jsonVaultPath: join(source, 'json-vault')
    })
    const result = await service.export(target, 'settings')
    expect(result.exported).toEqual(['config/settings.json'])
  })

  it('rejects a backup destination inside a copied source directory', async () => {
    const source = await mkdtemp(join(tmpdir(), 'mootool-backup-source-'))
    directories.push(source)
    const imagesPath = join(source, 'images')
    await mkdir(imagesPath)
    const service = new BackupService({
      dataDirectory: source,
      databasePath: join(source, 'MooToolNext.db'),
      settingsPath: join(source, 'settings.json'),
      imagesPath,
      quickNotePath: join(source, 'quick-notes'),
      jsonVaultPath: join(source, 'json-vault')
    })

    await expect(service.export(join(imagesPath, 'backups'), 'all')).rejects.toThrow('inside a source directory')
  })
})

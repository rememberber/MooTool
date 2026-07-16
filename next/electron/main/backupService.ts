import { cp, lstat, mkdir } from 'node:fs/promises'
import { basename, join, relative, resolve, sep } from 'node:path'
import type { BackupExportResult, BackupInfo, BackupKind } from '../../src/shared/contracts/backup'

export class BackupService {
  constructor(private readonly info: BackupInfo) {}

  getInfo(): BackupInfo {
    return { ...this.info }
  }

  async export(targetDirectory: string, kind: BackupKind): Promise<BackupExportResult> {
    await mkdir(targetDirectory, { recursive: true })
    const destination = join(targetDirectory, `MooTool-backup-${backupTimestamp()}`)
    for (const source of [this.info.imagesPath, this.info.quickNotePath, this.info.jsonVaultPath]) {
      if (isInside(source, destination)) throw new Error('Backup destination cannot be inside a source directory')
    }
    await mkdir(destination, { recursive: false })
    const exported: string[] = []

    if (kind === 'all' || kind === 'database') {
      const databaseDirectory = join(destination, 'database')
      for (const path of [this.info.databasePath, `${this.info.databasePath}-wal`, `${this.info.databasePath}-shm`]) {
        if (await copyIfExists(path, join(databaseDirectory, basename(path)))) exported.push(`database/${basename(path)}`)
      }
    }
    if (kind === 'all' || kind === 'settings') {
      const name = basename(this.info.settingsPath)
      if (await copyIfExists(this.info.settingsPath, join(destination, 'config', name))) exported.push(`config/${name}`)
    }
    if (kind === 'all' || kind === 'images') {
      if (await copyIfExists(this.info.imagesPath, join(destination, 'images'))) exported.push('images')
    }
    if (kind === 'all') {
      if (await copyIfExists(this.info.quickNotePath, join(destination, 'quick-notes'))) exported.push('quick-notes')
      if (await copyIfExists(this.info.jsonVaultPath, join(destination, 'json-vault'))) exported.push('json-vault')
    }

    return { directory: destination, exported }
  }
}

function isInside(parent: string, child: string): boolean {
  const path = relative(resolve(parent), resolve(child))
  return path === '' || (!path.startsWith('..') && !path.startsWith(sep))
}

async function copyIfExists(source: string, destination: string): Promise<boolean> {
  try {
    await lstat(source)
  } catch {
    return false
  }
  await mkdir(join(destination, '..'), { recursive: true })
  await cp(source, destination, { recursive: true, force: false, errorOnExist: true })
  return true
}

function backupTimestamp(): string {
  return new Date().toISOString().replace(/[-:]/g, '').replace('T', '-').slice(0, 15)
}

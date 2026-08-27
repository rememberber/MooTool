import { mkdirSync } from 'node:fs'
import { dirname } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import {
  favoriteKinds,
  type FavoriteFolderRecord,
  type FavoriteKind,
  type FavoriteRecord,
  type RenameFavoriteFolderInput,
  type SaveFavoriteFolderInput,
  type SaveFavoriteInput
} from '../../src/shared/contracts/favorites'

const defaultFolderTitle = '默认收藏夹'

export class FavoriteRepository {
  private readonly database: DatabaseSync

  constructor(databasePath: string) {
    mkdirSync(dirname(databasePath), { recursive: true })
    this.database = new DatabaseSync(databasePath)
    this.database.exec(`
      PRAGMA journal_mode = WAL;
      PRAGMA busy_timeout = 3000;
      PRAGMA foreign_keys = ON;
      CREATE TABLE IF NOT EXISTS t_next_favorite_folder (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        kind TEXT NOT NULL,
        title TEXT NOT NULL,
        create_time TEXT NOT NULL,
        UNIQUE(kind, title)
      );
    `)
    this.ensureFavoriteSchema()
    this.database.exec(`
      CREATE INDEX IF NOT EXISTS t_next_favorite_folder_kind_index
        ON t_next_favorite_folder (kind, id);
      CREATE INDEX IF NOT EXISTS t_next_favorite_kind_folder_name_index
        ON t_next_favorite (kind, folder_id, name COLLATE NOCASE);
    `)
    for (const kind of favoriteKinds) this.ensureDefaultFolder(kind)
  }

  list(kind: FavoriteKind, folderId?: number): FavoriteRecord[] {
    const rows = folderId == null
      ? this.database.prepare(`
          SELECT id, kind, folder_id, name, value, description, create_time
          FROM t_next_favorite WHERE kind = ? ORDER BY name COLLATE NOCASE, id
        `).all(kind)
      : this.database.prepare(`
          SELECT id, kind, folder_id, name, value, description, create_time
          FROM t_next_favorite WHERE kind = ? AND folder_id = ? ORDER BY name COLLATE NOCASE, id
        `).all(kind, folderId)
    return rows.map(mapFavorite)
  }

  listFolders(kind: FavoriteKind): FavoriteFolderRecord[] {
    this.ensureDefaultFolder(kind)
    return this.database.prepare(`
      SELECT id, kind, title, create_time
      FROM t_next_favorite_folder WHERE kind = ? ORDER BY id
    `).all(kind).map(mapFavoriteFolder)
  }

  createFolder(input: SaveFavoriteFolderInput): FavoriteFolderRecord {
    const createTime = formatSqliteDate(new Date())
    const result = this.database.prepare(`
      INSERT INTO t_next_favorite_folder (kind, title, create_time) VALUES (?, ?, ?)
    `).run(input.kind, input.title, createTime)
    return this.getFolder(Number(result.lastInsertRowid))
  }

  renameFolder(input: RenameFavoriteFolderInput): FavoriteFolderRecord {
    this.database.prepare('UPDATE t_next_favorite_folder SET title = ? WHERE id = ?').run(input.title, input.id)
    return this.getFolder(input.id)
  }

  deleteFolder(id: number): void {
    const row = this.database.prepare('SELECT kind FROM t_next_favorite_folder WHERE id = ?').get(id) as Record<string, unknown> | undefined
    if (!row) return
    const kind = String(row.kind) as FavoriteKind
    this.database.prepare('DELETE FROM t_next_favorite_folder WHERE id = ?').run(id)
    this.ensureDefaultFolder(kind)
  }

  save(input: SaveFavoriteInput): FavoriteRecord {
    const folderId = input.folderId ?? this.ensureDefaultFolder(input.kind).id
    const folder = this.getFolder(folderId)
    if (folder.kind !== input.kind) throw new Error('Favorite folder does not match favorite kind')
    const createTime = formatSqliteDate(new Date())
    this.database.prepare(`
      INSERT INTO t_next_favorite (kind, folder_id, name, value, description, create_time)
      VALUES (?, ?, ?, ?, ?, ?)
      ON CONFLICT(kind, folder_id, name) DO UPDATE SET
        value = excluded.value,
        description = excluded.description,
        create_time = excluded.create_time
    `).run(input.kind, folderId, input.name, input.value, input.description ?? '', createTime)
    const row = this.database.prepare(`
      SELECT id, kind, folder_id, name, value, description, create_time
      FROM t_next_favorite WHERE kind = ? AND folder_id = ? AND name = ?
    `).get(input.kind, folderId, input.name)
    if (!row) throw new Error('Favorite was not saved')
    return mapFavorite(row)
  }

  delete(id: number): void {
    this.database.prepare('DELETE FROM t_next_favorite WHERE id = ?').run(id)
  }

  close(): void {
    if (this.database.isOpen) this.database.close()
  }

  private ensureFavoriteSchema(): void {
    const exists = Boolean(this.database.prepare(`SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 't_next_favorite'`).get())
    if (!exists) {
      this.createFavoriteTable()
      return
    }
    const columns = this.database.prepare('PRAGMA table_info(t_next_favorite)').all() as Array<Record<string, unknown>>
    if (columns.some((column) => String(column.name) === 'folder_id')) return

    const now = formatSqliteDate(new Date())
    this.database.exec('BEGIN IMMEDIATE')
    try {
      this.database.prepare(`
        INSERT OR IGNORE INTO t_next_favorite_folder (kind, title, create_time)
        SELECT DISTINCT kind, ?, ? FROM t_next_favorite
      `).run(defaultFolderTitle, now)
      this.database.exec(`
        CREATE TABLE t_next_favorite_migrated (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          kind TEXT NOT NULL,
          folder_id INTEGER NOT NULL REFERENCES t_next_favorite_folder(id) ON DELETE CASCADE,
          name TEXT NOT NULL,
          value TEXT NOT NULL,
          description TEXT NOT NULL DEFAULT '',
          create_time TEXT NOT NULL,
          UNIQUE(kind, folder_id, name)
        );
        INSERT INTO t_next_favorite_migrated (id, kind, folder_id, name, value, description, create_time)
        SELECT favorite.id, favorite.kind, folder.id, favorite.name, favorite.value, favorite.description, favorite.create_time
        FROM t_next_favorite favorite
        JOIN t_next_favorite_folder folder ON folder.kind = favorite.kind AND folder.title = '默认收藏夹';
        DROP TABLE t_next_favorite;
        ALTER TABLE t_next_favorite_migrated RENAME TO t_next_favorite;
      `)
      this.database.exec('COMMIT')
    } catch (error) {
      this.database.exec('ROLLBACK')
      throw error
    }
  }

  private createFavoriteTable(): void {
    this.database.exec(`
      CREATE TABLE t_next_favorite (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        kind TEXT NOT NULL,
        folder_id INTEGER NOT NULL REFERENCES t_next_favorite_folder(id) ON DELETE CASCADE,
        name TEXT NOT NULL,
        value TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT '',
        create_time TEXT NOT NULL,
        UNIQUE(kind, folder_id, name)
      );
    `)
  }

  private ensureDefaultFolder(kind: FavoriteKind): FavoriteFolderRecord {
    const existing = this.database.prepare(`
      SELECT id, kind, title, create_time FROM t_next_favorite_folder WHERE kind = ? ORDER BY id LIMIT 1
    `).get(kind)
    if (existing) return mapFavoriteFolder(existing)
    return this.createFolder({ kind, title: defaultFolderTitle })
  }

  private getFolder(id: number): FavoriteFolderRecord {
    const row = this.database.prepare(`
      SELECT id, kind, title, create_time FROM t_next_favorite_folder WHERE id = ?
    `).get(id)
    if (!row) throw new Error('Favorite folder was not found')
    return mapFavoriteFolder(row)
  }
}

function mapFavorite(row: Record<string, unknown>): FavoriteRecord {
  return {
    id: Number(row.id),
    kind: String(row.kind) as FavoriteKind,
    folderId: Number(row.folder_id),
    name: String(row.name ?? ''),
    value: String(row.value ?? ''),
    description: String(row.description ?? ''),
    createTime: String(row.create_time ?? '')
  }
}

function mapFavoriteFolder(row: Record<string, unknown>): FavoriteFolderRecord {
  return {
    id: Number(row.id),
    kind: String(row.kind) as FavoriteKind,
    title: String(row.title ?? ''),
    createTime: String(row.create_time ?? '')
  }
}

function formatSqliteDate(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

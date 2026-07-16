import { mkdirSync } from 'node:fs'
import { dirname } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import type { FavoriteKind, FavoriteRecord, SaveFavoriteInput } from '../../src/shared/contracts/favorites'

export class FavoriteRepository {
  private readonly database: DatabaseSync

  constructor(databasePath: string) {
    mkdirSync(dirname(databasePath), { recursive: true })
    this.database = new DatabaseSync(databasePath)
    this.database.exec(`
      PRAGMA journal_mode = WAL;
      PRAGMA busy_timeout = 3000;
      CREATE TABLE IF NOT EXISTS t_next_favorite (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        kind TEXT NOT NULL,
        name TEXT NOT NULL,
        value TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT '',
        create_time TEXT NOT NULL,
        UNIQUE(kind, name)
      );
      CREATE INDEX IF NOT EXISTS t_next_favorite_kind_name_index
        ON t_next_favorite (kind, name COLLATE NOCASE);
    `)
  }

  list(kind: FavoriteKind): FavoriteRecord[] {
    return this.database.prepare(`
      SELECT id, kind, name, value, description, create_time
      FROM t_next_favorite WHERE kind = ? ORDER BY name COLLATE NOCASE, id
    `).all(kind).map(mapFavorite)
  }

  save(input: SaveFavoriteInput): FavoriteRecord {
    const createTime = formatSqliteDate(new Date())
    this.database.prepare(`
      INSERT INTO t_next_favorite (kind, name, value, description, create_time)
      VALUES (?, ?, ?, ?, ?)
      ON CONFLICT(kind, name) DO UPDATE SET
        value = excluded.value,
        description = excluded.description,
        create_time = excluded.create_time
    `).run(input.kind, input.name, input.value, input.description ?? '', createTime)
    const row = this.database.prepare(`
      SELECT id, kind, name, value, description, create_time
      FROM t_next_favorite WHERE kind = ? AND name = ?
    `).get(input.kind, input.name)
    if (!row) throw new Error('Favorite was not saved')
    return mapFavorite(row)
  }

  delete(id: number): void {
    this.database.prepare('DELETE FROM t_next_favorite WHERE id = ?').run(id)
  }

  close(): void {
    if (this.database.isOpen) this.database.close()
  }
}

function mapFavorite(row: Record<string, unknown>): FavoriteRecord {
  return {
    id: Number(row.id),
    kind: String(row.kind) as FavoriteKind,
    name: String(row.name ?? ''),
    value: String(row.value ?? ''),
    description: String(row.description ?? ''),
    createTime: String(row.create_time ?? '')
  }
}

function formatSqliteDate(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

import { mkdirSync } from 'node:fs'
import { dirname } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import type { FuncHistoryRecord, HistoryQuery, SaveFuncHistoryInput } from '../../src/shared/contracts/history'

const maxHistoryCount = 200

export class HistoryRepository {
  private readonly database: DatabaseSync

  constructor(databasePath: string) {
    mkdirSync(dirname(databasePath), { recursive: true })
    this.database = new DatabaseSync(databasePath)
    this.database.exec(`
      PRAGMA journal_mode = WAL;
      PRAGMA busy_timeout = 3000;
      CREATE TABLE IF NOT EXISTS t_func_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        func_type TEXT NOT NULL,
        summary TEXT,
        input_text TEXT,
        output_text TEXT,
        extra_data TEXT,
        create_time TEXT
      );
      CREATE INDEX IF NOT EXISTS t_func_history_func_type_create_time_index
        ON t_func_history (func_type, create_time DESC);
    `)
  }

  list(query: HistoryQuery): FuncHistoryRecord[] {
    const keyword = query.keyword?.trim()
    const rows = keyword
      ? this.database.prepare(`
          SELECT id, func_type, summary, input_text, output_text, extra_data, create_time
          FROM t_func_history
          WHERE func_type = ? AND (
            summary LIKE ? OR input_text LIKE ? OR output_text LIKE ? OR extra_data LIKE ?
          )
          ORDER BY create_time DESC, id DESC
        `).all(query.funcType, `%${keyword}%`, `%${keyword}%`, `%${keyword}%`, `%${keyword}%`)
      : this.database.prepare(`
          SELECT id, func_type, summary, input_text, output_text, extra_data, create_time
          FROM t_func_history
          WHERE func_type = ?
          ORDER BY create_time DESC, id DESC
        `).all(query.funcType)
    return rows.map(mapHistoryRow)
  }

  save(input: SaveFuncHistoryInput): FuncHistoryRecord | null {
    const funcType = input.funcType.trim()
    if (!funcType || (!input.inputText.trim() && !input.outputText.trim())) {
      return null
    }

    const summary = input.summary?.trim() || previewText(input.inputText, 40)
    const createTime = formatSqliteDate(new Date())
    const result = this.database.prepare(`
      INSERT INTO t_func_history (func_type, summary, input_text, output_text, extra_data, create_time)
      VALUES (?, ?, ?, ?, ?, ?)
    `).run(funcType, summary, input.inputText, input.outputText, input.extraData ?? null, createTime)

    this.database.prepare(`
      DELETE FROM t_func_history
      WHERE func_type = ? AND id NOT IN (
        SELECT id FROM t_func_history
        WHERE func_type = ?
        ORDER BY create_time DESC, id DESC
        LIMIT ?
      )
    `).run(funcType, funcType, maxHistoryCount)

    const row = this.database.prepare(`
      SELECT id, func_type, summary, input_text, output_text, extra_data, create_time
      FROM t_func_history WHERE id = ?
    `).get(result.lastInsertRowid)
    return row ? mapHistoryRow(row) : null
  }

  delete(id: number): void {
    this.database.prepare('DELETE FROM t_func_history WHERE id = ?').run(id)
  }

  clear(funcType: string): void {
    this.database.prepare('DELETE FROM t_func_history WHERE func_type = ?').run(funcType)
  }

  close(): void {
    if (this.database.isOpen) {
      this.database.close()
    }
  }
}

function mapHistoryRow(row: Record<string, unknown>): FuncHistoryRecord {
  return {
    id: Number(row.id),
    funcType: String(row.func_type ?? ''),
    summary: String(row.summary ?? ''),
    inputText: String(row.input_text ?? ''),
    outputText: String(row.output_text ?? ''),
    extraData: row.extra_data == null ? null : String(row.extra_data),
    createTime: String(row.create_time ?? '')
  }
}

function previewText(value: string, maxLength: number): string {
  const normalized = value.replace(/[\r\n]+/g, ' ').trim()
  return normalized.length <= maxLength ? normalized : `${normalized.slice(0, maxLength)}...`
}

function formatSqliteDate(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

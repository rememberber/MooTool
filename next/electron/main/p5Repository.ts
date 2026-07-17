import { mkdirSync } from 'node:fs'
import { dirname } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import type {
  HttpCookieEntry,
  HttpRequestDraft,
  HttpRequestHistory,
  HttpResponseResult,
  KeyValueEntry,
  SaveTranslationHistoryInput,
  SaveTranslationWordInput,
  SavedHttpRequest,
  TranslationHistory,
  TranslationWord
} from '../../src/shared/contracts/network'
import { normalizeTranslationLanguagePair } from '../../src/shared/contracts/network'
import type { HostProfile, SaveHostProfileInput } from '../../src/shared/contracts/system'

export class P5Repository {
  private readonly database: DatabaseSync

  constructor(databasePath: string) {
    mkdirSync(dirname(databasePath), { recursive: true })
    this.database = new DatabaseSync(databasePath)
    this.database.exec(`
      PRAGMA journal_mode = WAL;
      PRAGMA busy_timeout = 3000;
      CREATE TABLE IF NOT EXISTS t_msg_http (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        msg_name TEXT UNIQUE,
        method TEXT,
        url TEXT,
        params TEXT,
        headers TEXT,
        cookies TEXT,
        body TEXT,
        body_type TEXT,
        create_time TEXT,
        modified_time TEXT,
        response_body TEXT,
        response_headers TEXT,
        response_cookies TEXT
      );
      CREATE TABLE IF NOT EXISTS t_http_request_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        request_id INTEGER,
        title TEXT,
        method TEXT,
        url TEXT,
        params TEXT,
        headers TEXT,
        cookies TEXT,
        body TEXT,
        body_type TEXT,
        response_body TEXT,
        response_headers TEXT,
        response_cookies TEXT,
        status TEXT,
        cost_time INTEGER,
        create_time TEXT,
        modified_time TEXT
      );
      CREATE INDEX IF NOT EXISTS t_http_history_create_time_index
        ON t_http_request_history (create_time DESC);
      CREATE TABLE IF NOT EXISTS t_host (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT UNIQUE,
        content TEXT,
        create_time TEXT,
        modified_time TEXT
      );
      CREATE TABLE IF NOT EXISTS t_translation_word (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        source_text TEXT NOT NULL,
        target_text TEXT,
        source_lang TEXT,
        target_lang TEXT,
        remark TEXT,
        create_time TEXT,
        modified_time TEXT
      );
      CREATE INDEX IF NOT EXISTS t_translation_word_modified_time_index
        ON t_translation_word (modified_time DESC);
      CREATE TABLE IF NOT EXISTS t_translation_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        source_text TEXT NOT NULL,
        target_text TEXT,
        source_lang TEXT,
        target_lang TEXT,
        translator_type TEXT,
        create_time TEXT
      );
      CREATE INDEX IF NOT EXISTS t_translation_history_create_time_index
        ON t_translation_history (create_time DESC);
    `)
  }

  listHttpRequests(keyword = ''): SavedHttpRequest[] {
    const rows = keyword.trim()
      ? this.database.prepare('SELECT * FROM t_msg_http WHERE msg_name LIKE ? ORDER BY modified_time DESC, id DESC').all(`%${keyword.trim()}%`)
      : this.database.prepare('SELECT * FROM t_msg_http ORDER BY modified_time DESC, id DESC').all()
    return rows.map(mapHttpRequest)
  }

  saveHttpRequest(request: HttpRequestDraft, response?: HttpResponseResult): SavedHttpRequest {
    const now = sqliteDate()
    const existing = request.id
      ? this.database.prepare('SELECT id, create_time FROM t_msg_http WHERE id = ?').get(request.id)
      : this.database.prepare('SELECT id, create_time FROM t_msg_http WHERE msg_name = ?').get(request.name)
    const payload = httpColumns(request, response)
    let id: number
    if (existing) {
      id = Number(existing.id)
      this.database.prepare(`
        UPDATE t_msg_http SET msg_name = ?, method = ?, url = ?, params = ?, headers = ?, cookies = ?,
          body = ?, body_type = ?, modified_time = ?, response_body = ?, response_headers = ?, response_cookies = ?
        WHERE id = ?
      `).run(request.name, ...payload.slice(0, 7), now, ...payload.slice(7), id)
    } else {
      const result = this.database.prepare(`
        INSERT INTO t_msg_http (msg_name, method, url, params, headers, cookies, body, body_type,
          create_time, modified_time, response_body, response_headers, response_cookies)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `).run(request.name, ...payload.slice(0, 7), now, now, ...payload.slice(7))
      id = Number(result.lastInsertRowid)
    }
    return mapHttpRequest(this.database.prepare('SELECT * FROM t_msg_http WHERE id = ?').get(id)!)
  }

  deleteHttpRequest(id: number): void {
    this.database.prepare('DELETE FROM t_msg_http WHERE id = ?').run(id)
  }

  listHttpHistory(keyword = ''): HttpRequestHistory[] {
    const rows = keyword.trim()
      ? this.database.prepare(`SELECT * FROM t_http_request_history WHERE title LIKE ? OR url LIKE ? ORDER BY id DESC LIMIT 500`).all(`%${keyword.trim()}%`, `%${keyword.trim()}%`)
      : this.database.prepare('SELECT * FROM t_http_request_history ORDER BY id DESC LIMIT 500').all()
    return rows.map(mapHttpHistory)
  }

  saveHttpHistory(request: HttpRequestDraft, response: HttpResponseResult): HttpRequestHistory {
    const now = sqliteDate()
    const result = this.database.prepare(`
      INSERT INTO t_http_request_history (request_id, title, method, url, params, headers, cookies, body,
        body_type, response_body, response_headers, response_cookies, status, cost_time, create_time, modified_time)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(request.id ?? null, request.name, request.method, request.url, serialize(request.params), serialize(request.headers),
      serialize(request.cookies), request.body, request.bodyType, response.body, response.headers, response.cookies,
      `${response.status} ${response.statusText}`.trim(), response.durationMs, now, now)
    this.database.prepare('DELETE FROM t_http_request_history WHERE id NOT IN (SELECT id FROM t_http_request_history ORDER BY id DESC LIMIT 500)').run()
    return mapHttpHistory(this.database.prepare('SELECT * FROM t_http_request_history WHERE id = ?').get(result.lastInsertRowid)!)
  }

  deleteHttpHistory(id: number): void {
    this.database.prepare('DELETE FROM t_http_request_history WHERE id = ?').run(id)
  }

  clearHttpHistory(): void {
    this.database.prepare('DELETE FROM t_http_request_history').run()
  }

  listHosts(keyword = ''): HostProfile[] {
    const rows = keyword.trim()
      ? this.database.prepare('SELECT * FROM t_host WHERE name LIKE ? ORDER BY modified_time DESC, id DESC').all(`%${keyword.trim()}%`)
      : this.database.prepare('SELECT * FROM t_host ORDER BY modified_time DESC, id DESC').all()
    return rows.map(mapHost)
  }

  saveHost(input: SaveHostProfileInput): HostProfile {
    const now = sqliteDate()
    const existing = input.id
      ? this.database.prepare('SELECT id FROM t_host WHERE id = ?').get(input.id)
      : this.database.prepare('SELECT id FROM t_host WHERE name = ?').get(input.name)
    let id: number
    if (existing) {
      id = Number(existing.id)
      this.database.prepare('UPDATE t_host SET name = ?, content = ?, modified_time = ? WHERE id = ?').run(input.name, input.content, now, id)
    } else {
      const result = this.database.prepare('INSERT INTO t_host (name, content, create_time, modified_time) VALUES (?, ?, ?, ?)').run(input.name, input.content, now, now)
      id = Number(result.lastInsertRowid)
    }
    return mapHost(this.database.prepare('SELECT * FROM t_host WHERE id = ?').get(id)!)
  }

  deleteHost(id: number): void {
    this.database.prepare('DELETE FROM t_host WHERE id = ?').run(id)
  }

  listTranslationWords(keyword = ''): TranslationWord[] {
    const rows = keyword.trim()
      ? this.database.prepare(`SELECT * FROM t_translation_word WHERE source_text LIKE ? OR target_text LIKE ? OR remark LIKE ? ORDER BY modified_time DESC, id DESC`).all(...Array(3).fill(`%${keyword.trim()}%`))
      : this.database.prepare('SELECT * FROM t_translation_word ORDER BY modified_time DESC, id DESC').all()
    return rows.map(mapWord)
  }

  saveTranslationWord(input: SaveTranslationWordInput): TranslationWord {
    const now = sqliteDate()
    const existing = input.id
      ? this.database.prepare('SELECT id FROM t_translation_word WHERE id = ?').get(input.id)
      : this.database.prepare('SELECT id FROM t_translation_word WHERE source_text = ? AND source_lang = ? AND target_lang = ?').get(input.sourceText, input.sourceLang, input.targetLang)
    let id: number
    if (existing) {
      id = Number(existing.id)
      this.database.prepare(`UPDATE t_translation_word SET source_text = ?, target_text = ?, source_lang = ?, target_lang = ?, remark = ?, modified_time = ? WHERE id = ?`)
        .run(input.sourceText, input.targetText, input.sourceLang, input.targetLang, input.remark, now, id)
    } else {
      const result = this.database.prepare(`INSERT INTO t_translation_word (source_text, target_text, source_lang, target_lang, remark, create_time, modified_time) VALUES (?, ?, ?, ?, ?, ?, ?)`)
        .run(input.sourceText, input.targetText, input.sourceLang, input.targetLang, input.remark, now, now)
      id = Number(result.lastInsertRowid)
    }
    return mapWord(this.database.prepare('SELECT * FROM t_translation_word WHERE id = ?').get(id)!)
  }

  deleteTranslationWord(id: number): void {
    this.database.prepare('DELETE FROM t_translation_word WHERE id = ?').run(id)
  }

  listTranslationHistory(keyword = ''): TranslationHistory[] {
    const rows = keyword.trim()
      ? this.database.prepare(`SELECT * FROM t_translation_history WHERE source_text LIKE ? OR target_text LIKE ? OR source_lang LIKE ? OR target_lang LIKE ? ORDER BY id DESC LIMIT 500`).all(...Array(4).fill(`%${keyword.trim()}%`))
      : this.database.prepare('SELECT * FROM t_translation_history ORDER BY id DESC LIMIT 500').all()
    return rows.map(mapTranslationHistory)
  }

  saveTranslationHistory(input: SaveTranslationHistoryInput): TranslationHistory {
    const result = this.database.prepare(`INSERT INTO t_translation_history (source_text, target_text, source_lang, target_lang, translator_type, create_time) VALUES (?, ?, ?, ?, ?, ?)`)
      .run(input.sourceText, input.targetText, input.sourceLang, input.targetLang, input.translatorType.toUpperCase(), sqliteDate())
    this.database.prepare('DELETE FROM t_translation_history WHERE id NOT IN (SELECT id FROM t_translation_history ORDER BY id DESC LIMIT 500)').run()
    return mapTranslationHistory(this.database.prepare('SELECT * FROM t_translation_history WHERE id = ?').get(result.lastInsertRowid)!)
  }

  deleteTranslationHistory(id: number): void {
    this.database.prepare('DELETE FROM t_translation_history WHERE id = ?').run(id)
  }

  clearTranslationHistory(): void {
    this.database.prepare('DELETE FROM t_translation_history').run()
  }

  close(): void {
    if (this.database.isOpen) this.database.close()
  }
}

function httpColumns(request: HttpRequestDraft, response?: HttpResponseResult): string[] {
  return [request.method, request.url, serialize(request.params), serialize(request.headers), serialize(request.cookies), request.body,
    request.bodyType, response?.body ?? '', response?.headers ?? '', response?.cookies ?? '']
}

function mapHttpRequest(row: Record<string, unknown>): SavedHttpRequest {
  return {
    id: Number(row.id),
    name: String(row.msg_name ?? ''),
    method: String(row.method ?? 'GET') as SavedHttpRequest['method'],
    url: String(row.url ?? ''),
    params: parseEntries(row.params),
    headers: parseEntries(row.headers),
    cookies: parseCookies(row.cookies),
    body: String(row.body ?? ''),
    bodyType: String(row.body_type ?? 'application/json'),
    responseBody: String(row.response_body ?? ''),
    responseHeaders: String(row.response_headers ?? ''),
    responseCookies: String(row.response_cookies ?? ''),
    createTime: String(row.create_time ?? ''),
    modifiedTime: String(row.modified_time ?? '')
  }
}

function mapHttpHistory(row: Record<string, unknown>): HttpRequestHistory {
  return {
    ...mapHttpRequest({ ...row, msg_name: row.title }),
    requestIdValue: row.request_id == null ? null : Number(row.request_id),
    title: String(row.title ?? ''),
    status: String(row.status ?? ''),
    costTime: Number(row.cost_time ?? 0)
  }
}

function parseEntries(value: unknown): KeyValueEntry[] {
  return parseArray(value).map((entry, index) => ({
    id: stringValue(entry.id) || `entry-${index}`,
    name: stringValue(entry.name),
    value: stringValue(entry.value),
    enabled: entry.enabled !== false
  }))
}

function parseCookies(value: unknown): HttpCookieEntry[] {
  return parseArray(value).map((entry, index) => ({
    id: stringValue(entry.id) || `cookie-${index}`,
    name: stringValue(entry.name),
    value: stringValue(entry.value),
    enabled: entry.enabled !== false,
    domain: stringValue(entry.domain),
    path: stringValue(entry.path),
    expires: stringValue(entry.expires || entry.expiry)
  }))
}

function parseArray(value: unknown): Record<string, unknown>[] {
  try {
    const parsed = JSON.parse(String(value ?? '[]'))
    return Array.isArray(parsed) ? parsed.filter((item): item is Record<string, unknown> => typeof item === 'object' && item !== null) : []
  } catch {
    return []
  }
}

function mapHost(row: Record<string, unknown>): HostProfile {
  return { id: Number(row.id), name: String(row.name ?? ''), content: String(row.content ?? ''), createTime: String(row.create_time ?? ''), modifiedTime: String(row.modified_time ?? '') }
}

function mapWord(row: Record<string, unknown>): TranslationWord {
  const languages = normalizeTranslationLanguagePair(row.source_lang, row.target_lang)
  return { id: Number(row.id), sourceText: String(row.source_text ?? ''), targetText: String(row.target_text ?? ''), sourceLang: languages.sourceLang, targetLang: languages.targetLang, remark: String(row.remark ?? ''), createTime: String(row.create_time ?? ''), modifiedTime: String(row.modified_time ?? '') }
}

function mapTranslationHistory(row: Record<string, unknown>): TranslationHistory {
  const languages = normalizeTranslationLanguagePair(row.source_lang, row.target_lang)
  return { id: Number(row.id), sourceText: String(row.source_text ?? ''), targetText: String(row.target_text ?? ''), sourceLang: languages.sourceLang, targetLang: languages.targetLang, translatorType: String(row.translator_type ?? 'google').toLowerCase() === 'bing' ? 'bing' : 'google', createTime: String(row.create_time ?? '') }
}

function serialize(value: unknown): string {
  return JSON.stringify(value)
}

function stringValue(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function sqliteDate(): string {
  const date = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

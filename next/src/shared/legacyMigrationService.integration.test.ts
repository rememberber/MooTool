// @vitest-environment node
import { createHash } from 'node:crypto'
import { mkdtemp, mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import { afterEach, describe, expect, it } from 'vitest'
import { FavoriteRepository } from '../../electron/main/favoriteRepository'
import { HistoryRepository } from '../../electron/main/historyRepository'
import { LegacyMigrationService, parseLegacySetting } from '../../electron/main/legacyMigrationService'
import { P5Repository } from '../../electron/main/p5Repository'

const directories: string[] = []

afterEach(async () => {
  await Promise.all(directories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('LegacyMigrationService', () => {
  it('previews and imports Java database, Vault and safe settings data without modifying the source', async () => {
    const source = await makeLegacySource()
    const target = await makeTarget()
    const service = new LegacyMigrationService(target)
    const sourceHash = await fileHash(join(source, 'MooTool.db'))

    const preview = await service.preview({ sourceDirectory: source })
    expect(preview.databaseFound).toBe(true)
    expect(preview.configFound).toBe(true)
    expect(preview.alreadyMigrated).toBe(false)
    expect(preview.items).toMatchObject({
      history: 1,
      httpRequests: 1,
      hosts: 1,
      favoriteColors: 1,
      favoriteRegex: 1,
      toolDrafts: 1,
      qrCodes: 1,
      quickNotes: 1,
      jsonItems: 1,
      quickNoteVaultFiles: 2,
      jsonVaultFiles: 1
    })
    expect(preview.warnings).toEqual(expect.arrayContaining(['differentVaultRemotes', 'secretsSkipped']))

    const result = await service.migrate({ sourceDirectory: source }, '/migration-backup')
    expect(result.alreadyMigrated).toBe(false)
    expect(result.backupDirectory).toBe('/migration-backup')
    expect(result.imported.quickNotes).toBe(1)
    expect(result.imported.jsonItems).toBe(1)
    expect(result.settingsPatch).toMatchObject({
      general: { language: 'zh-CN', autoCheckUpdates: false, startMaximized: true },
      appearance: { theme: 'dark', fontFamily: 'PingFang SC', fontSize: 13 },
      network: { proxyEnabled: true, proxyHost: '127.0.0.1', proxyPort: '7890', proxyUsername: 'moo' },
      layout: { compactNavigation: true },
      editor: { sqlDialect: 'MySQL', quickNoteFontSize: 16, jsonFontSize: 15 },
      vault: { gitUsername: 'zhoubo', autoCommit: true, autoCommitIdleSeconds: 25, autoCommitInactiveSeconds: 90, autoPullMinutes: 5 },
      tools: { qrCodeSize: 480, qrErrorCorrection: 'H', randomStringLength: 24, translationProvider: 'bing' }
    })
    expect(result.settingsPatch.layout).not.toHaveProperty('navigationStyle')
    expect(result.settingsPatch.vault).not.toHaveProperty('gitRemote')
    expect(JSON.stringify(result.settingsPatch)).not.toContain('legacy-secret')

    const database = new DatabaseSync(target.databasePath, { readOnly: true })
    try {
      expect(database.prepare('SELECT COUNT(*) AS count FROM t_func_history').get()).toMatchObject({ count: 3 })
      expect(database.prepare('SELECT msg_name, url FROM t_msg_http').get()).toMatchObject({ msg_name: 'Legacy API', url: 'https://example.com' })
      expect(database.prepare('SELECT name, content FROM t_host').get()).toMatchObject({ name: 'Legacy Hosts', content: '127.0.0.1 localhost' })
      expect(database.prepare(`SELECT value FROM t_next_favorite WHERE kind = 'color'`).get()).toMatchObject({ value: '#de8f7d' })
      expect(database.prepare(`SELECT value FROM t_next_favorite WHERE kind = 'regex'`).get()).toMatchObject({ value: '^moo$' })
      expect(database.prepare('SELECT COUNT(*) AS count FROM t_next_migration_run').get()).toMatchObject({ count: 1 })
    } finally {
      database.close()
    }

    expect(await readFile(join(target.quickNotePath, 'Existing.txt'), 'utf8')).toContain('Existing note')
    expect(await readFile(join(target.quickNotePath, 'attachments', 'pixel.png'), 'utf8')).toBe('png')
    expect(await readFile(join(target.quickNotePath, 'Database Note.txt'), 'utf8')).toContain('title: Database Note')
    expect(await readFile(join(target.jsonVaultPath, 'Existing.json'), 'utf8')).toBe('{"existing":true}')
    expect(await readFile(join(target.jsonVaultPath, 'Database JSON.json'), 'utf8')).toBe('{"database":true}')
    expect(await fileHash(join(source, 'MooTool.db'))).toBe(sourceHash)

    const secondPreview = await service.preview({ sourceDirectory: source })
    expect(secondPreview.alreadyMigrated).toBe(true)
    const second = await service.migrate({ sourceDirectory: source }, '/second-backup')
    expect(second.alreadyMigrated).toBe(true)
    expect(Object.values(second.imported).every((count) => count === 0)).toBe(true)
    expect((await readDirectory(target.quickNotePath)).filter((name) => name.startsWith('Database Note'))).toEqual(['Database Note.txt'])
  }, 30_000)

  it('uses a Java migration marker to avoid importing database notes already represented by the old Vault', async () => {
    const source = await makeLegacySource()
    const target = await makeTarget()
    await writeFile(join(source, 'quick-notes', '.migrated-from-db'), 'done')
    await writeFile(join(source, 'json-beauty', '.migrated-from-db'), 'done')
    const service = new LegacyMigrationService(target)

    const preview = await service.preview({ sourceDirectory: source })
    expect(preview.items.quickNotes).toBe(0)
    expect(preview.items.jsonItems).toBe(0)
    const result = await service.migrate({ sourceDirectory: source }, '/backup')
    expect(result.imported.quickNotes).toBe(0)
    expect(result.imported.jsonItems).toBe(0)
    await expect(readFile(join(target.quickNotePath, 'Database Note.txt'), 'utf8')).rejects.toThrow()
    await expect(readFile(join(target.jsonVaultPath, 'Database JSON.json'), 'utf8')).rejects.toThrow()
  })

  it('rejects missing sources and source paths that overlap Next storage', async () => {
    const target = await makeTarget()
    const service = new LegacyMigrationService(target)
    await expect(service.preview({ sourceDirectory: join(tmpdir(), 'missing-mootool-source') })).rejects.toThrow('does not exist')

    const overlapping = await mkdtemp(join(tmpdir(), 'mootool-overlap-'))
    directories.push(overlapping)
    await mkdir(join(overlapping, 'config'))
    await writeFile(join(overlapping, 'config', 'config.setting'), '')
    const overlappingService = new LegacyMigrationService({
      databasePath: join(overlapping, 'MooTool.db'),
      quickNotePath: join(overlapping, 'quick-notes'),
      jsonVaultPath: join(overlapping, 'json-beauty')
    })
    await expect(overlappingService.preview({ sourceDirectory: overlapping })).rejects.toThrow('different locations')
  })

  it('does not overwrite current settings when the legacy config contains no mapped values', async () => {
    const source = await mkdtemp(join(tmpdir(), 'mootool-empty-config-'))
    directories.push(source)
    await mkdir(join(source, 'config'))
    await writeFile(join(source, 'config', 'config.setting'), '# no settings')
    const target = await makeTarget()
    const service = new LegacyMigrationService(target)

    const result = await service.migrate({ sourceDirectory: source }, '/backup')
    expect(result.settingsPatch).toEqual({})
  })
})

describe('parseLegacySetting', () => {
  it('parses Hutool groups, comments and values containing equals signs', () => {
    expect(parseLegacySetting(`
      # MooTool Java
      [setting.http]
      httpProxyHost = 127.0.0.1
      token = "a=b=c"
    `)).toEqual({
      'setting.http': { httpProxyHost: '127.0.0.1', token: 'a=b=c' }
    })
  })
})

async function makeLegacySource(): Promise<string> {
  const root = await mkdtemp(join(tmpdir(), 'mootool-legacy-'))
  directories.push(root)
  await mkdir(join(root, 'config'))
  await mkdir(join(root, 'quick-notes', 'attachments'), { recursive: true })
  await mkdir(join(root, 'json-beauty'))
  await writeFile(join(root, 'quick-notes', 'Existing.txt'), 'Existing note')
  await writeFile(join(root, 'quick-notes', 'attachments', 'pixel.png'), 'png')
  await writeFile(join(root, 'json-beauty', 'Existing.json'), '{"existing":true}')
  await writeFile(join(root, 'config', 'config.setting'), `
[setting.common]
locale = zh_CN
autoCheckUpdate = false

[setting.normal]
defaultMaxWindow = true
unifiedBackground = false
themeColorFollowSystem = false

[setting.appearance]
theme = Flat macOS Dark
font = PingFang SC
fontSize = 13

[setting.http]
httpUseProxy = true
httpProxyHost = 127.0.0.1
httpProxyPort = 7890
httpProxyUserName = moo
httpProxyPassword = legacy-secret

[setting.custom]
tabCompact = true
tabCard = true
funcRecentVisible = true

[setting.quickNote]
sqlDialect = MySQL

[func.quickNote]
quickNoteFontSize = 16
quickNoteGitRemoteUrl = https://example.com/quick.git
quickNoteAutoGitCommit = true
quickNoteAutoGitIdleSeconds = 30
quickNoteAutoGitInactiveSeconds = 120
quickNoteAutoPullIntervalMinutes = 5

[func.jsonBeauty]
jsonBeautyFontSize = 15
jsonBeautyGitRemoteUrl = https://example.com/json.git
jsonBeautyAutoGitIdleSeconds = 25
jsonBeautyAutoGitInactiveSeconds = 90
jsonBeautyAutoPullIntervalMinutes = 10

[func.vaultGit]
vaultGitUsername = zhoubo
vaultGitToken = legacy-secret

[func.qrCode]
qrCodeSize = 480
qrCodeErrorCorrectionLevel = 高

[func.crypto]
randomStringDigit = 24

[func.translation]
translatorType = BING
sourceLanguage = auto
targetLanguage = en
  `)

  const database = new DatabaseSync(join(root, 'MooTool.db'))
  database.exec(`
    CREATE TABLE t_func_history (id INTEGER PRIMARY KEY, func_type TEXT, summary TEXT, input_text TEXT, output_text TEXT, extra_data TEXT, create_time TEXT);
    INSERT INTO t_func_history VALUES (1, 'json', 'Legacy history', '{}', '{ }', NULL, '2026-01-01 00:00:00');
    CREATE TABLE t_msg_http (id INTEGER PRIMARY KEY, msg_name TEXT, method TEXT, url TEXT, params TEXT, headers TEXT, cookies TEXT, body TEXT, body_type TEXT, create_time TEXT, modified_time TEXT);
    INSERT INTO t_msg_http VALUES (1, 'Legacy API', 'GET', 'https://example.com', '[]', '[]', '[]', '', 'none', '2026-01-01', '2026-01-01');
    CREATE TABLE t_http_request_history (id INTEGER PRIMARY KEY, request_id INTEGER, title TEXT, method TEXT, url TEXT, params TEXT, headers TEXT, cookies TEXT, body TEXT, body_type TEXT, response_body TEXT, response_headers TEXT, response_cookies TEXT, status TEXT, cost_time INTEGER, create_time TEXT, modified_time TEXT);
    INSERT INTO t_http_request_history VALUES (1, 1, 'Legacy call', 'GET', 'https://example.com', '[]', '[]', '[]', '', 'none', '{}', '[]', '[]', '200', 12, '2026-01-01', '2026-01-01');
    CREATE TABLE t_host (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT);
    INSERT INTO t_host VALUES (1, 'Legacy Hosts', '127.0.0.1 localhost', '2026-01-01', '2026-01-01');
    CREATE TABLE t_translation_word (id INTEGER PRIMARY KEY, source_text TEXT, target_text TEXT, source_lang TEXT, target_lang TEXT, remark TEXT, create_time TEXT, modified_time TEXT);
    INSERT INTO t_translation_word VALUES (1, 'moo', '哞', 'en', 'zh-CN', '', '2026-01-01', '2026-01-01');
    CREATE TABLE t_translation_history (id INTEGER PRIMARY KEY, source_text TEXT, target_text TEXT, source_lang TEXT, target_lang TEXT, translator_type TEXT, create_time TEXT);
    INSERT INTO t_translation_history VALUES (1, 'hello', '你好', 'en', 'zh-CN', 'BING', '2026-01-01');
    CREATE TABLE t_quick_note (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT, color TEXT, style TEXT, font_name TEXT, font_size TEXT, syntax TEXT, line_wrap TEXT);
    INSERT INTO t_quick_note VALUES (1, 'Database Note', 'Database note body', '2026-01-01', '2026-01-02', 'blue', '', 'Monaco', '15', 'text/plain', '1');
    CREATE TABLE t_json_beauty (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT);
    INSERT INTO t_json_beauty VALUES (1, 'Database JSON', '{"database":true}', '2026-01-01', '2026-01-02');
    CREATE TABLE t_favorite_color_list (id INTEGER PRIMARY KEY, title TEXT, remark TEXT);
    CREATE TABLE t_favorite_color_item (id INTEGER PRIMARY KEY, list_id INTEGER, name TEXT, value TEXT, remark TEXT, create_time TEXT);
    INSERT INTO t_favorite_color_list VALUES (1, '默认收藏夹', 'colors');
    INSERT INTO t_favorite_color_item VALUES (1, 1, 'Coral', '#de8f7d', 'brand', '2026-01-01');
    CREATE TABLE t_favorite_regex_list (id INTEGER PRIMARY KEY, title TEXT, remark TEXT);
    CREATE TABLE t_favorite_regex_item (id INTEGER PRIMARY KEY, list_id INTEGER, name TEXT, value TEXT, remark TEXT, create_time TEXT);
    INSERT INTO t_favorite_regex_list VALUES (1, 'Work', 'regex');
    INSERT INTO t_favorite_regex_item VALUES (1, 1, 'Moo', '^moo$', '', '2026-01-01');
    CREATE TABLE t_func_content (id INTEGER PRIMARY KEY, func TEXT, content TEXT, remark TEXT, create_time TEXT, modified_time TEXT);
    INSERT INTO t_func_content VALUES (1, 'regex', '^draft$', 'Legacy draft', '2026-01-01', '2026-01-02');
    CREATE TABLE t_qr_code (id INTEGER PRIMARY KEY, content TEXT, create_time TEXT, modified_time TEXT);
    INSERT INTO t_qr_code VALUES (1, 'https://mootool.example', '2026-01-01', '2026-01-02');
  `)
  database.close()
  return root
}

async function makeTarget(): Promise<{ databasePath: string; quickNotePath: string; jsonVaultPath: string }> {
  const root = await mkdtemp(join(tmpdir(), 'mootool-next-target-'))
  directories.push(root)
  const databasePath = join(root, 'MooToolNext.db')
  new HistoryRepository(databasePath).close()
  new FavoriteRepository(databasePath).close()
  new P5Repository(databasePath).close()
  return {
    databasePath,
    quickNotePath: join(root, 'quick-notes'),
    jsonVaultPath: join(root, 'json-vault')
  }
}

async function fileHash(path: string): Promise<string> {
  return createHash('sha256').update(await readFile(path)).digest('hex')
}

async function readDirectory(path: string): Promise<string[]> {
  const { readdir } = await import('node:fs/promises')
  return readdir(path)
}

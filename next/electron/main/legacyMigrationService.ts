import { constants } from 'node:fs'
import { access, copyFile, lstat, mkdir, readFile, readdir, realpath, rm, stat, writeFile } from 'node:fs/promises'
import { homedir } from 'node:os'
import { basename, dirname, extname, isAbsolute, join, relative, resolve } from 'node:path'
import { createHash } from 'node:crypto'
import { DatabaseSync } from 'node:sqlite'
import { stringify as stringifyYaml } from 'yaml'
import {
  legacyMigrationCategories,
  type LegacyMigrationCategory,
  type LegacyMigrationCounts,
  type LegacyMigrationInput,
  type LegacyMigrationPreview,
  type LegacyMigrationResult,
  type LegacyMigrationWarning
} from '../../src/shared/contracts/migration'
import type { SettingsPatch } from '../../src/shared/contracts/settings'

type LegacyMigrationTarget = {
  databasePath: string
  quickNotePath: string
  jsonVaultPath: string
}

type ResolvedLegacySource = {
  root: string
  databasePath: string
  configPath: string
  quickNoteVaultPath: string
  jsonVaultPath: string
  config: LegacySetting
  databaseFound: boolean
  configFound: boolean
  quickNoteDatabaseMigrated: boolean
  jsonDatabaseMigrated: boolean
  fingerprint: string
  warnings: LegacyMigrationWarning[]
}

type LegacySetting = Record<string, Record<string, string>>
type SqliteRow = Record<string, unknown>

const sharedTableMigrations: Array<{ table: string; category: LegacyMigrationCategory }> = [
  { table: 't_func_history', category: 'history' },
  { table: 't_msg_http', category: 'httpRequests' },
  { table: 't_http_request_history', category: 'httpHistory' },
  { table: 't_host', category: 'hosts' },
  { table: 't_translation_word', category: 'translationWords' },
  { table: 't_translation_history', category: 'translationHistory' }
]

const favoriteTables: Array<{ list: string; item: string; category: LegacyMigrationCategory; kind: string }> = [
  { list: 't_favorite_color_list', item: 't_favorite_color_item', category: 'favoriteColors', kind: 'color' },
  { list: 't_favorite_regex_list', item: 't_favorite_regex_item', category: 'favoriteRegex', kind: 'regex' },
  { list: 't_favorite_cron_list', item: 't_favorite_cron_item', category: 'favoriteCron', kind: 'cron' }
]

export class LegacyMigrationService {
  constructor(private readonly target: LegacyMigrationTarget) {}

  async preview(input: LegacyMigrationInput): Promise<LegacyMigrationPreview> {
    const source = await this.resolveSource(input)
    await this.assertSeparateTargets(source)
    const items = emptyCounts()
    if (source.databaseFound) {
      const database = new DatabaseSync(source.databasePath, { readOnly: true })
      try {
        for (const migration of sharedTableMigrations) items[migration.category] = countTable(database, migration.table)
        for (const favorite of favoriteTables) items[favorite.category] = countTable(database, favorite.item)
        items.toolDrafts = countTable(database, 't_func_content')
        items.qrCodes = countTable(database, 't_qr_code')
        if (!source.quickNoteDatabaseMigrated) items.quickNotes = countTable(database, 't_quick_note')
        if (!source.jsonDatabaseMigrated) items.jsonItems = countTable(database, 't_json_beauty')
      } finally {
        database.close()
      }
    }
    items.quickNoteVaultFiles = (await collectVaultFiles(source.quickNoteVaultPath, 'quickNote')).length
    items.jsonVaultFiles = (await collectVaultFiles(source.jsonVaultPath, 'json')).length
    const destination = this.openDestination()
    let alreadyMigrated = false
    try {
      alreadyMigrated = Boolean(destination.prepare('SELECT 1 FROM t_next_migration_run WHERE fingerprint = ?').get(source.fingerprint))
    } finally {
      destination.close()
    }
    return {
      sourceDirectory: source.root,
      databasePath: source.databasePath,
      configPath: source.configPath,
      quickNoteVaultPath: source.quickNoteVaultPath,
      jsonVaultPath: source.jsonVaultPath,
      databaseFound: source.databaseFound,
      configFound: source.configFound,
      alreadyMigrated,
      totalItems: Object.values(items).reduce((sum, count) => sum + count, 0),
      items,
      warnings: source.warnings
    }
  }

  async migrate(input: LegacyMigrationInput, backupDirectory: string): Promise<LegacyMigrationResult> {
    const source = await this.resolveSource(input)
    await this.assertSeparateTargets(source)
    const preview = await this.preview(input)
    const imported = emptyCounts()
    const skipped = emptyCounts()
    const settingsPatch = legacySettingsPatch(source.config)
    if (preview.alreadyMigrated) {
      return {
        sourceDirectory: source.root,
        backupDirectory,
        alreadyMigrated: true,
        imported,
        skipped: { ...preview.items },
        settingsPatch,
        warnings: source.warnings
      }
    }

    await mkdir(this.target.quickNotePath, { recursive: true })
    await mkdir(this.target.jsonVaultPath, { recursive: true })
    const destination = this.openDestination()
    const legacy = source.databaseFound ? new DatabaseSync(source.databasePath, { readOnly: true }) : null
    const createdFiles: string[] = []
    destination.exec('BEGIN IMMEDIATE')
    try {
      if (legacy) {
        for (const migration of sharedTableMigrations) {
          migrateSharedTable(legacy, destination, source.databasePath, migration.table, migration.category, imported, skipped)
        }
        for (const favorite of favoriteTables) {
          migrateFavorites(legacy, destination, source.databasePath, favorite, imported, skipped)
        }
        migrateToolDrafts(legacy, destination, source.databasePath, imported, skipped)
        migrateQrCodes(legacy, destination, source.databasePath, imported, skipped)
      }

      await migrateVaultFiles(source.quickNoteVaultPath, this.target.quickNotePath, 'quickNote', destination, imported, skipped, createdFiles)
      await migrateVaultFiles(source.jsonVaultPath, this.target.jsonVaultPath, 'json', destination, imported, skipped, createdFiles)

      if (legacy && !source.quickNoteDatabaseMigrated) {
        await migrateDatabaseNotes(legacy, destination, source.databasePath, this.target.quickNotePath, imported, skipped, createdFiles)
      }
      if (legacy && !source.jsonDatabaseMigrated) {
        await migrateDatabaseJson(legacy, destination, source.databasePath, this.target.jsonVaultPath, imported, skipped, createdFiles)
      }

      destination.prepare(`
        INSERT INTO t_next_migration_run (fingerprint, source_path, migrated_at, report_json)
        VALUES (?, ?, ?, ?)
      `).run(source.fingerprint, source.root, sqliteDate(), JSON.stringify({ imported, skipped }))
      destination.exec('COMMIT')
    } catch (error) {
      try {
        destination.exec('ROLLBACK')
      } catch {
        // Keep the original migration failure.
      }
      await Promise.all(createdFiles.reverse().map((path) => rm(path, { force: true }).catch(() => undefined)))
      throw error
    } finally {
      legacy?.close()
      destination.close()
    }

    return {
      sourceDirectory: source.root,
      backupDirectory,
      alreadyMigrated: false,
      imported,
      skipped,
      settingsPatch,
      warnings: source.warnings
    }
  }

  private async resolveSource(input: LegacyMigrationInput): Promise<ResolvedLegacySource> {
    if (!input || typeof input.sourceDirectory !== 'string' || !input.sourceDirectory.trim()) throw new Error('Legacy source directory is required')
    const requestedRoot = resolve(expandHome(input.sourceDirectory.trim()))
    const rootStat = await lstat(requestedRoot).catch(() => null)
    if (!rootStat?.isDirectory() || rootStat.isSymbolicLink()) throw new Error('Legacy source directory does not exist')
    const root = await realpath(requestedRoot)
    const configPath = join(root, 'config', 'config.setting')
    const configFound = await isFile(configPath)
    const config = configFound ? parseLegacySetting(await readFile(configPath, 'utf8')) : {}
    const configuredDatabaseDirectory = settingValue(config, 'func.advanced', 'dbFilePath')
    const databasePath = configuredDatabaseDirectory
      ? join(resolveConfiguredPath(root, configuredDatabaseDirectory), 'MooTool.db')
      : join(root, 'MooTool.db')
    const quickNoteVaultPath = resolveConfiguredPath(root, settingValue(config, 'func.quickNote', 'quickNoteVaultPath') || 'quick-notes')
    const jsonVaultPath = resolveConfiguredPath(root, settingValue(config, 'func.jsonBeauty', 'jsonBeautyVaultPath') || 'json-beauty')
    const databaseFound = await isFile(databasePath)
    const warnings = legacyWarnings(config)
    const quickNoteDatabaseMigrated = await isFile(join(quickNoteVaultPath, '.migrated-from-db'))
    const jsonDatabaseMigrated = await isFile(join(jsonVaultPath, '.migrated-from-db'))
    const fingerprint = await sourceFingerprint(root, [databasePath, configPath, quickNoteVaultPath, jsonVaultPath])
    if (!databaseFound && !configFound && !(await isDirectory(quickNoteVaultPath)) && !(await isDirectory(jsonVaultPath))) {
      throw new Error('No MooTool Java data was found in the selected directory')
    }
    return {
      root,
      databasePath,
      configPath,
      quickNoteVaultPath,
      jsonVaultPath,
      config,
      databaseFound,
      configFound,
      quickNoteDatabaseMigrated,
      jsonDatabaseMigrated,
      fingerprint,
      warnings
    }
  }

  private async assertSeparateTargets(source: ResolvedLegacySource): Promise<void> {
    const pairs = [
      [source.databasePath, this.target.databasePath],
      [source.quickNoteVaultPath, this.target.quickNotePath],
      [source.jsonVaultPath, this.target.jsonVaultPath]
    ]
    const canonicalPairs = await Promise.all(pairs.map(async ([legacy, target]) => [await canonicalPath(legacy), await canonicalPath(target)]))
    if (canonicalPairs.some(([legacy, target]) => legacy === target)) {
      throw new Error('Legacy data and MooTool Next data must use different locations')
    }
  }

  private openDestination(): DatabaseSync {
    const database = new DatabaseSync(this.target.databasePath)
    database.exec(`
      PRAGMA busy_timeout = 5000;
      CREATE TABLE IF NOT EXISTS t_next_migration_run (
        fingerprint TEXT PRIMARY KEY,
        source_path TEXT NOT NULL,
        migrated_at TEXT NOT NULL,
        report_json TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS t_next_migration_row (
        source_path TEXT NOT NULL,
        source_table TEXT NOT NULL,
        source_id TEXT NOT NULL,
        target_hint TEXT NOT NULL DEFAULT '',
        migrated_at TEXT NOT NULL,
        PRIMARY KEY (source_path, source_table, source_id)
      );
    `)
    return database
  }
}

function migrateSharedTable(
  source: DatabaseSync,
  destination: DatabaseSync,
  sourcePath: string,
  table: string,
  category: LegacyMigrationCategory,
  imported: LegacyMigrationCounts,
  skipped: LegacyMigrationCounts
): void {
  if (!tableExists(source, table) || !tableExists(destination, table)) return
  const sourceColumns = tableColumns(source, table)
  const destinationColumns = new Set(tableColumns(destination, table))
  const columns = sourceColumns.filter((column) => column !== 'id' && destinationColumns.has(column))
  if (!columns.length) return
  const rows = source.prepare(`SELECT * FROM ${quoteIdentifier(table)}`).all() as SqliteRow[]
  const insert = destination.prepare(`INSERT OR IGNORE INTO ${quoteIdentifier(table)} (${columns.map(quoteIdentifier).join(', ')}) VALUES (${columns.map(() => '?').join(', ')})`)
  rows.forEach((row, index) => {
    const sourceId = rowIdentity(row, index)
    if (rowWasMigrated(destination, sourcePath, table, sourceId)) {
      skipped[category] += 1
      return
    }
    const result = insert.run(...columns.map((column) => sqliteValue(row[column])))
    recordMigratedRow(destination, sourcePath, table, sourceId, '')
    if (Number(result.changes) > 0) imported[category] += 1
    else skipped[category] += 1
  })
}

function migrateFavorites(
  source: DatabaseSync,
  destination: DatabaseSync,
  sourcePath: string,
  migration: (typeof favoriteTables)[number],
  imported: LegacyMigrationCounts,
  skipped: LegacyMigrationCounts
): void {
  if (!tableExists(source, migration.item) || !tableExists(destination, 't_next_favorite')) return
  const hasList = tableExists(source, migration.list)
  const rows = source.prepare(hasList
    ? `SELECT item.*, list.title AS list_title, list.remark AS list_remark FROM ${quoteIdentifier(migration.item)} item LEFT JOIN ${quoteIdentifier(migration.list)} list ON list.id = item.list_id ORDER BY item.id`
    : `SELECT item.*, '' AS list_title, '' AS list_remark FROM ${quoteIdentifier(migration.item)} item ORDER BY item.id`).all() as SqliteRow[]
  for (let index = 0; index < rows.length; index += 1) {
    const row = rows[index]
    const sourceId = rowIdentity(row, index)
    if (rowWasMigrated(destination, sourcePath, migration.item, sourceId)) {
      skipped[migration.category] += 1
      continue
    }
    const originalName = cleanText(row.name) || `Legacy ${migration.kind} ${index + 1}`
    const listTitle = cleanText(row.list_title)
    const preferredName = listTitle && !isDefaultFavoriteList(listTitle) ? `${listTitle} / ${originalName}` : originalName
    const name = uniqueFavoriteName(destination, migration.kind, preferredName, listTitle ? `${listTitle} / ${originalName}` : originalName)
    const description = [cleanText(row.remark), cleanText(row.list_remark)].filter(Boolean).join('\n')
    destination.prepare(`INSERT INTO t_next_favorite (kind, name, value, description, create_time) VALUES (?, ?, ?, ?, ?)`)
      .run(migration.kind, name, cleanText(row.value), description, cleanText(row.create_time) || sqliteDate())
    recordMigratedRow(destination, sourcePath, migration.item, sourceId, name)
    imported[migration.category] += 1
  }
}

function migrateToolDrafts(
  source: DatabaseSync,
  destination: DatabaseSync,
  sourcePath: string,
  imported: LegacyMigrationCounts,
  skipped: LegacyMigrationCounts
): void {
  if (!tableExists(source, 't_func_content') || !tableExists(destination, 't_func_history')) return
  const rows = source.prepare('SELECT * FROM t_func_content ORDER BY id').all() as SqliteRow[]
  rows.forEach((row, index) => {
    const sourceId = rowIdentity(row, index)
    if (rowWasMigrated(destination, sourcePath, 't_func_content', sourceId)) {
      skipped.toolDrafts += 1
      return
    }
    destination.prepare(`INSERT INTO t_func_history (func_type, summary, input_text, output_text, extra_data, create_time) VALUES (?, ?, ?, '', ?, ?)`)
      .run(cleanText(row.func) || 'legacy', cleanText(row.remark) || 'Legacy draft', cleanText(row.content), JSON.stringify({ migratedFrom: 't_func_content' }), cleanText(row.modified_time) || cleanText(row.create_time) || sqliteDate())
    recordMigratedRow(destination, sourcePath, 't_func_content', sourceId, cleanText(row.func))
    imported.toolDrafts += 1
  })
}

function migrateQrCodes(
  source: DatabaseSync,
  destination: DatabaseSync,
  sourcePath: string,
  imported: LegacyMigrationCounts,
  skipped: LegacyMigrationCounts
): void {
  if (!tableExists(source, 't_qr_code') || !tableExists(destination, 't_func_history')) return
  const rows = source.prepare('SELECT * FROM t_qr_code ORDER BY id').all() as SqliteRow[]
  rows.forEach((row, index) => {
    const sourceId = rowIdentity(row, index)
    if (rowWasMigrated(destination, sourcePath, 't_qr_code', sourceId)) {
      skipped.qrCodes += 1
      return
    }
    const content = cleanText(row.content)
    destination.prepare(`INSERT INTO t_func_history (func_type, summary, input_text, output_text, extra_data, create_time) VALUES ('qrCode', ?, ?, '', ?, ?)`)
      .run(content.slice(0, 40) || 'Legacy QR Code', content, JSON.stringify({ migratedFrom: 't_qr_code' }), cleanText(row.modified_time) || cleanText(row.create_time) || sqliteDate())
    recordMigratedRow(destination, sourcePath, 't_qr_code', sourceId, '')
    imported.qrCodes += 1
  })
}

async function migrateDatabaseNotes(
  source: DatabaseSync,
  destination: DatabaseSync,
  sourcePath: string,
  targetRoot: string,
  imported: LegacyMigrationCounts,
  skipped: LegacyMigrationCounts,
  createdFiles: string[]
): Promise<void> {
  if (!tableExists(source, 't_quick_note')) return
  const rows = source.prepare('SELECT * FROM t_quick_note ORDER BY id').all() as SqliteRow[]
  for (let index = 0; index < rows.length; index += 1) {
    const row = rows[index]
    const sourceId = rowIdentity(row, index)
    if (rowWasMigrated(destination, sourcePath, 't_quick_note', sourceId)) {
      skipped.quickNotes += 1
      continue
    }
    const title = cleanText(row.name) || `Legacy Note ${index + 1}`
    const target = await uniqueFilePath(targetRoot, `${sanitizeFileName(title)}.txt`)
    const createdAt = cleanText(row.create_time) || sqliteDate()
    const modifiedAt = cleanText(row.modified_time) || createdAt
    const metadata = {
      title,
      style: cleanText(row.style),
      syntax: cleanText(row.syntax) || 'text/plain',
      font_name: cleanText(row.font_name),
      font_size: String(clampNumber(Number(row.font_size), 11, 24, 14)),
      color: cleanText(row.color) || 'default',
      line_wrap: booleanValue(row.line_wrap, false) ? '1' : '0',
      created_at: createdAt,
      modified_at: modifiedAt
    }
    const yaml = stringifyYaml(metadata, { lineWidth: 0 }).trim()
    const content = cleanText(row.content)
    await writeFile(target, `---\n${yaml}\n---${content && !content.startsWith('\n') ? '\n' : ''}${content}`, { encoding: 'utf8', flag: 'wx' })
    createdFiles.push(target)
    recordMigratedRow(destination, sourcePath, 't_quick_note', sourceId, relative(targetRoot, target))
    imported.quickNotes += 1
  }
}

async function migrateDatabaseJson(
  source: DatabaseSync,
  destination: DatabaseSync,
  sourcePath: string,
  targetRoot: string,
  imported: LegacyMigrationCounts,
  skipped: LegacyMigrationCounts,
  createdFiles: string[]
): Promise<void> {
  if (!tableExists(source, 't_json_beauty')) return
  const rows = source.prepare('SELECT * FROM t_json_beauty ORDER BY id').all() as SqliteRow[]
  for (let index = 0; index < rows.length; index += 1) {
    const row = rows[index]
    const sourceId = rowIdentity(row, index)
    if (rowWasMigrated(destination, sourcePath, 't_json_beauty', sourceId)) {
      skipped.jsonItems += 1
      continue
    }
    const title = cleanText(row.name) || `Legacy JSON ${index + 1}`
    const target = await uniqueFilePath(targetRoot, `${sanitizeFileName(title)}.json`)
    await writeFile(target, cleanText(row.content), { encoding: 'utf8', flag: 'wx' })
    createdFiles.push(target)
    recordMigratedRow(destination, sourcePath, 't_json_beauty', sourceId, relative(targetRoot, target))
    imported.jsonItems += 1
  }
}

async function migrateVaultFiles(
  sourceRoot: string,
  targetRoot: string,
  kind: 'quickNote' | 'json',
  destination: DatabaseSync,
  imported: LegacyMigrationCounts,
  skipped: LegacyMigrationCounts,
  createdFiles: string[]
): Promise<void> {
  const category = kind === 'quickNote' ? 'quickNoteVaultFiles' : 'jsonVaultFiles'
  const table = kind === 'quickNote' ? 'quick_note_vault' : 'json_vault'
  for (const file of await collectVaultFiles(sourceRoot, kind)) {
    if (rowWasMigrated(destination, sourceRoot, table, file.relativePath)) {
      skipped[category] += 1
      continue
    }
    const target = await uniqueFilePath(targetRoot, file.relativePath)
    await mkdir(dirname(target), { recursive: true })
    await copyFile(file.absolutePath, target, constants.COPYFILE_EXCL)
    createdFiles.push(target)
    recordMigratedRow(destination, sourceRoot, table, file.relativePath, relative(targetRoot, target))
    imported[category] += 1
  }
}

async function collectVaultFiles(root: string, kind: 'quickNote' | 'json'): Promise<Array<{ absolutePath: string; relativePath: string }>> {
  if (!await isDirectory(root)) return []
  const result: Array<{ absolutePath: string; relativePath: string }> = []
  async function visit(directory: string, depth: number): Promise<void> {
    if (depth > 32) return
    for (const entry of await readdir(directory, { withFileTypes: true })) {
      if (entry.name.startsWith('.') || entry.name === 'node_modules' || entry.isSymbolicLink()) continue
      const absolutePath = join(directory, entry.name)
      if (entry.isDirectory()) {
        await visit(absolutePath, depth + 1)
      } else if (entry.isFile()) {
        const extension = extname(entry.name).toLocaleLowerCase()
        const include = kind === 'json' ? extension === '.json' : extension === '.txt' || isQuickNoteAttachment(extension)
        if (!include) continue
        const fileStat = await stat(absolutePath)
        if (fileStat.size > 20 * 1024 * 1024) continue
        result.push({ absolutePath, relativePath: relative(root, absolutePath).split('\\').join('/') })
      }
    }
  }
  await visit(root, 0)
  return result.sort((left, right) => left.relativePath.localeCompare(right.relativePath))
}

function legacySettingsPatch(config: LegacySetting): SettingsPatch {
  const followSystem = booleanValue(settingValue(config, 'setting.normal', 'themeColorFollowSystem'), true)
  const themeName = settingValue(config, 'setting.appearance', 'theme').toLocaleLowerCase()
  const quickRemote = settingValue(config, 'func.quickNote', 'quickNoteGitRemoteUrl')
  const jsonRemote = settingValue(config, 'func.jsonBeauty', 'jsonBeautyGitRemoteUrl')
  const remotesConflict = Boolean(quickRemote && jsonRemote && quickRemote !== jsonRemote)
  const positivePullIntervals = [
    numberValue(settingValue(config, 'func.quickNote', 'quickNoteAutoPullIntervalMinutes'), 0),
    numberValue(settingValue(config, 'func.jsonBeauty', 'jsonBeautyAutoPullIntervalMinutes'), 0)
  ].filter((value) => value > 0)
  const patch: SettingsPatch = {}
  const general: NonNullable<SettingsPatch['general']> = {}
  if (hasSetting(config, 'setting.common', 'locale')) general.language = legacyLanguage(settingValue(config, 'setting.common', 'locale'))
  if (hasSetting(config, 'setting.common', 'autoCheckUpdate')) general.autoCheckUpdates = booleanValue(settingValue(config, 'setting.common', 'autoCheckUpdate'), true)
  if (hasSetting(config, 'setting.normal', 'defaultMaxWindow')) general.startMaximized = booleanValue(settingValue(config, 'setting.normal', 'defaultMaxWindow'), false)
  if (Object.keys(general).length) patch.general = general

  const appearance: NonNullable<SettingsPatch['appearance']> = {}
  if (hasSetting(config, 'setting.normal', 'themeColorFollowSystem') || hasSetting(config, 'setting.appearance', 'theme')) {
    appearance.theme = followSystem ? 'system' : themeName.includes('dark') || themeName.includes('darcula') ? 'dark' : 'light'
  }
  if (hasSetting(config, 'setting.appearance', 'font')) appearance.fontFamily = settingValue(config, 'setting.appearance', 'font')
  if (hasSetting(config, 'setting.appearance', 'fontSize')) appearance.fontSize = positiveNumber(settingValue(config, 'setting.appearance', 'fontSize'), 13)
  if (hasSetting(config, 'setting.normal', 'unifiedBackground')) appearance.unifiedBackground = booleanValue(settingValue(config, 'setting.normal', 'unifiedBackground'), true)
  if (Object.keys(appearance).length) patch.appearance = appearance

  const layout: NonNullable<SettingsPatch['layout']> = {}
  if (hasSetting(config, 'setting.custom', 'tabCompact')) layout.compactNavigation = booleanValue(settingValue(config, 'setting.custom', 'tabCompact'), false)
  if (hasSetting(config, 'setting.custom', 'tabHideTitle')) layout.hideNavigationTitles = booleanValue(settingValue(config, 'setting.custom', 'tabHideTitle'), false)
  if (hasSetting(config, 'setting.custom', 'tabSeparator')) layout.showSeparators = booleanValue(settingValue(config, 'setting.custom', 'tabSeparator'), false)
  if (hasSetting(config, 'setting.custom', 'funcRecentVisible')) layout.showRecent = booleanValue(settingValue(config, 'setting.custom', 'funcRecentVisible'), false)
  if (Object.keys(layout).length) patch.layout = layout

  const editor: NonNullable<SettingsPatch['editor']> = {}
  if (hasSetting(config, 'setting.quickNote', 'sqlDialect')) editor.sqlDialect = settingValue(config, 'setting.quickNote', 'sqlDialect')
  const quickNoteFontSize = numberValue(settingValue(config, 'func.quickNote', 'quickNoteFontSize'), 0)
  const jsonFontSize = numberValue(settingValue(config, 'func.jsonBeauty', 'jsonBeautyFontSize'), 0)
  if (quickNoteFontSize > 0) editor.quickNoteFontSize = quickNoteFontSize
  if (jsonFontSize > 0) editor.jsonFontSize = jsonFontSize
  if (Object.keys(editor).length) patch.editor = editor

  const network: NonNullable<SettingsPatch['network']> = {}
  if (hasSetting(config, 'setting.http', 'httpUseProxy')) network.proxyEnabled = booleanValue(settingValue(config, 'setting.http', 'httpUseProxy'), false)
  if (hasSetting(config, 'setting.http', 'httpProxyHost')) network.proxyHost = settingValue(config, 'setting.http', 'httpProxyHost')
  if (hasSetting(config, 'setting.http', 'httpProxyPort')) network.proxyPort = settingValue(config, 'setting.http', 'httpProxyPort')
  if (hasSetting(config, 'setting.http', 'httpProxyUserName')) network.proxyUsername = settingValue(config, 'setting.http', 'httpProxyUserName')
  if (Object.keys(network).length) patch.network = network

  const vault: NonNullable<SettingsPatch['vault']> = {}
  if (!remotesConflict && (quickRemote || jsonRemote)) vault.gitRemote = quickRemote || jsonRemote
  if (hasSetting(config, 'func.vaultGit', 'vaultGitUsername')) vault.gitUsername = settingValue(config, 'func.vaultGit', 'vaultGitUsername')
  if (hasSetting(config, 'func.quickNote', 'quickNoteAutoGitCommit') || hasSetting(config, 'func.jsonBeauty', 'jsonBeautyAutoGitCommit')) {
    vault.autoCommit = booleanValue(settingValue(config, 'func.quickNote', 'quickNoteAutoGitCommit'), false)
      || booleanValue(settingValue(config, 'func.jsonBeauty', 'jsonBeautyAutoGitCommit'), false)
  }
  if (hasSetting(config, 'func.quickNote', 'quickNoteAutoPullIntervalMinutes') || hasSetting(config, 'func.jsonBeauty', 'jsonBeautyAutoPullIntervalMinutes')) {
    vault.autoPullMinutes = positivePullIntervals.length ? Math.min(...positivePullIntervals) : 0
  }
  if (hasSetting(config, 'func.quickNote', 'quickNoteHideGitignoredFiles') || hasSetting(config, 'func.jsonBeauty', 'jsonBeautyHideGitignoredFiles')) {
    vault.hideGitignoredFiles = booleanValue(settingValue(config, 'func.quickNote', 'quickNoteHideGitignoredFiles'), true)
      && booleanValue(settingValue(config, 'func.jsonBeauty', 'jsonBeautyHideGitignoredFiles'), true)
  }
  if (Object.keys(vault).length) patch.vault = vault

  const tools: NonNullable<SettingsPatch['tools']> = {}
  if (hasSetting(config, 'func.qrCode', 'qrCodeSize')) tools.qrCodeSize = positiveNumber(settingValue(config, 'func.qrCode', 'qrCodeSize'), 300)
  if (hasSetting(config, 'func.qrCode', 'qrCodeErrorCorrectionLevel')) tools.qrErrorCorrection = legacyQrCorrection(settingValue(config, 'func.qrCode', 'qrCodeErrorCorrectionLevel'))
  if (hasSetting(config, 'func.crypto', 'randomStringDigit')) tools.randomStringLength = positiveNumber(settingValue(config, 'func.crypto', 'randomStringDigit'), 16)
  const exportDirectory = settingValue(config, 'func.quickNote', 'quickNoteExportPath')
    || settingValue(config, 'func.jsonBeauty', 'jsonBeautyExportPath')
    || settingValue(config, 'func.image', 'imageExportPath')
  if (exportDirectory) tools.exportDirectory = exportDirectory
  if (hasSetting(config, 'func.translation', 'translatorType')) tools.translationProvider = settingValue(config, 'func.translation', 'translatorType').toLocaleUpperCase() === 'BING' ? 'bing' : 'google'
  if (hasSetting(config, 'func.translation', 'sourceLanguage')) tools.translationSourceLang = settingValue(config, 'func.translation', 'sourceLanguage')
  if (hasSetting(config, 'func.translation', 'targetLanguage')) tools.translationTargetLang = settingValue(config, 'func.translation', 'targetLanguage')
  if (Object.keys(tools).length) patch.tools = tools
  return patch
}

function legacyWarnings(config: LegacySetting): LegacyMigrationWarning[] {
  const warnings: LegacyMigrationWarning[] = []
  const quickRemote = settingValue(config, 'func.quickNote', 'quickNoteGitRemoteUrl')
  const jsonRemote = settingValue(config, 'func.jsonBeauty', 'jsonBeautyGitRemoteUrl')
  if (quickRemote && jsonRemote && quickRemote !== jsonRemote) warnings.push('differentVaultRemotes')
  if (settingValue(config, 'setting.http', 'httpProxyPassword') || settingValue(config, 'func.vaultGit', 'vaultGitToken')) warnings.push('secretsSkipped')
  return warnings
}

export function parseLegacySetting(raw: string): LegacySetting {
  const result: LegacySetting = {}
  let group = ''
  for (const rawLine of raw.replace(/^\uFEFF/, '').split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#') || line.startsWith(';')) continue
    const groupMatch = /^\[([^\]]+)]$/.exec(line)
    if (groupMatch) {
      group = groupMatch[1].trim()
      result[group] ??= {}
      continue
    }
    const separator = line.indexOf('=')
    if (separator < 1) continue
    const key = line.slice(0, separator).trim()
    const value = line.slice(separator + 1).trim()
    result[group] ??= {}
    result[group][key] = unquote(value)
  }
  return result
}

function emptyCounts(): LegacyMigrationCounts {
  return Object.fromEntries(legacyMigrationCategories.map((category) => [category, 0])) as LegacyMigrationCounts
}

function tableExists(database: DatabaseSync, table: string): boolean {
  return Boolean(database.prepare(`SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?`).get(table))
}

function countTable(database: DatabaseSync, table: string): number {
  if (!tableExists(database, table)) return 0
  const row = database.prepare(`SELECT COUNT(*) AS count FROM ${quoteIdentifier(table)}`).get() as SqliteRow | undefined
  return Number(row?.count ?? 0)
}

function tableColumns(database: DatabaseSync, table: string): string[] {
  return (database.prepare(`PRAGMA table_info(${quoteIdentifier(table)})`).all() as SqliteRow[]).map((row) => String(row.name))
}

function rowWasMigrated(database: DatabaseSync, sourcePath: string, table: string, sourceId: string): boolean {
  return Boolean(database.prepare(`SELECT 1 FROM t_next_migration_row WHERE source_path = ? AND source_table = ? AND source_id = ?`).get(sourcePath, table, sourceId))
}

function recordMigratedRow(database: DatabaseSync, sourcePath: string, table: string, sourceId: string, targetHint: string): void {
  database.prepare(`INSERT OR IGNORE INTO t_next_migration_row (source_path, source_table, source_id, target_hint, migrated_at) VALUES (?, ?, ?, ?, ?)`)
    .run(sourcePath, table, sourceId, targetHint, sqliteDate())
}

function rowIdentity(row: SqliteRow, index: number): string {
  if (row.id != null) return String(row.id)
  return createHash('sha256').update(JSON.stringify(row)).update(String(index)).digest('hex')
}

function uniqueFavoriteName(database: DatabaseSync, kind: string, preferred: string, qualified: string): string {
  const names = preferred === qualified ? [preferred] : [preferred, qualified]
  for (let index = 0; index < 10_000; index += 1) {
    const base = names[Math.min(index, names.length - 1)]
    const candidate = index < names.length ? base : `${qualified} (${index - names.length + 2})`
    if (!database.prepare('SELECT 1 FROM t_next_favorite WHERE kind = ? AND name = ?').get(kind, candidate)) return candidate.slice(0, 240)
  }
  throw new Error('Unable to allocate a favorite name')
}

async function uniqueFilePath(root: string, requestedRelativePath: string): Promise<string> {
  const normalized = requestedRelativePath.replaceAll('\\', '/').replace(/^\/+/, '')
  const parts = normalized.split('/').filter((part) => part && part !== '.' && part !== '..').map(sanitizeFileName)
  const safeRelativePath = parts.join('/') || 'Legacy item'
  const requested = resolve(root, safeRelativePath)
  assertInside(root, requested)
  if (!await pathExists(requested)) return requested
  const extension = extname(requested)
  const stem = basename(requested, extension)
  const parent = dirname(requested)
  for (let index = 1; index < 10_000; index += 1) {
    const suffix = index === 1 ? ' (Legacy)' : ` (Legacy ${index})`
    const candidate = join(parent, `${stem}${suffix}${extension}`)
    if (!await pathExists(candidate)) return candidate
  }
  throw new Error('Unable to allocate a migration file path')
}

function assertInside(root: string, target: string): void {
  const value = relative(resolve(root), resolve(target))
  if (value.startsWith('..') || isAbsolute(value)) throw new Error('Migration path escapes the destination Vault')
}

async function sourceFingerprint(root: string, paths: string[]): Promise<string> {
  const hash = createHash('sha256').update(root)
  for (const path of paths) await addPathFingerprint(hash, path, path, 0)
  return hash.digest('hex')
}

async function addPathFingerprint(hash: ReturnType<typeof createHash>, root: string, path: string, depth: number): Promise<void> {
  const entryStat = await lstat(path).catch(() => null)
  if (!entryStat || entryStat.isSymbolicLink() || depth > 32) return
  hash.update(relative(root, path)).update(String(entryStat.size)).update(String(entryStat.mtimeMs))
  if (!entryStat.isDirectory()) return
  const entries = await readdir(path, { withFileTypes: true })
  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
    if (entry.name === '.git' || entry.isSymbolicLink()) continue
    await addPathFingerprint(hash, root, join(path, entry.name), depth + 1)
  }
}

function resolveConfiguredPath(root: string, configured: string): string {
  const expanded = expandHome(configured.trim())
  return resolve(isAbsolute(expanded) ? expanded : join(root, expanded))
}

function expandHome(value: string): string {
  if (value === '~') return homedir()
  if (value.startsWith('~/') || value.startsWith('~\\')) return join(homedir(), value.slice(2))
  return value
}

function settingValue(config: LegacySetting, group: string, key: string): string {
  return config[group]?.[key]?.trim() ?? ''
}

function hasSetting(config: LegacySetting, group: string, key: string): boolean {
  return Object.hasOwn(config[group] ?? {}, key)
}

function legacyLanguage(value: string): 'zh-CN' | 'en-US' | 'ja-JP' {
  const normalized = value.replace('_', '-').toLocaleLowerCase()
  if (normalized.startsWith('ja')) return 'ja-JP'
  if (normalized.startsWith('zh')) return 'zh-CN'
  return 'en-US'
}

function legacyQrCorrection(value: string): 'L' | 'M' | 'Q' | 'H' {
  const normalized = value.trim().toLocaleUpperCase()
  if (normalized === 'Q' || normalized.includes('中高')) return 'Q'
  if (normalized === 'H' || normalized.includes('高')) return 'H'
  if (normalized === 'L' || normalized === '低') return 'L'
  return 'M'
}

function booleanValue(value: unknown, fallback: boolean): boolean {
  if (typeof value === 'boolean') return value
  if (typeof value === 'number') return value !== 0
  const normalized = String(value ?? '').trim().toLocaleLowerCase()
  if (['true', '1', 'yes', 'on'].includes(normalized)) return true
  if (['false', '0', 'no', 'off'].includes(normalized)) return false
  return fallback
}

function numberValue(value: unknown, fallback: number): number {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function positiveNumber(value: unknown, fallback: number): number {
  const number = numberValue(value, fallback)
  return number > 0 ? number : fallback
}

function clampNumber(value: number, min: number, max: number, fallback: number): number {
  if (!Number.isFinite(value)) return fallback
  return Math.min(max, Math.max(min, Math.round(value)))
}

function cleanText(value: unknown): string {
  return value == null ? '' : String(value)
}

function sqliteValue(value: unknown): null | number | bigint | string | Uint8Array {
  if (value == null) return null
  if (typeof value === 'number' || typeof value === 'bigint' || typeof value === 'string' || value instanceof Uint8Array) return value
  return String(value)
}

function unquote(value: string): string {
  if (value.length >= 2 && ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'")))) {
    return value.slice(1, -1)
  }
  return value
}

function isDefaultFavoriteList(value: string): boolean {
  return ['默认收藏夹', 'default', 'default favorites'].includes(value.trim().toLocaleLowerCase())
}

function sanitizeFileName(value: string): string {
  const sanitized = value.replace(/[\\/:*?"<>|\0]/g, '_').replace(/^\.+/, '').trim()
  return (sanitized || 'Legacy item').slice(0, 180)
}

function isQuickNoteAttachment(extension: string): boolean {
  return ['.png', '.jpg', '.jpeg', '.gif', '.webp', '.bmp'].includes(extension)
}

function quoteIdentifier(value: string): string {
  return `"${value.replaceAll('"', '""')}"`
}

function sqliteDate(): string {
  return new Date().toISOString().replace('T', ' ').slice(0, 19)
}

async function pathExists(path: string): Promise<boolean> {
  return access(path).then(() => true).catch(() => false)
}

async function canonicalPath(path: string): Promise<string> {
  let current = resolve(path)
  const missing: string[] = []
  while (!await pathExists(current)) {
    const parent = dirname(current)
    if (parent === current) return resolve(path)
    missing.unshift(basename(current))
    current = parent
  }
  return resolve(await realpath(current), ...missing)
}

async function isFile(path: string): Promise<boolean> {
  const value = await lstat(path).catch(() => null)
  return Boolean(value?.isFile() && !value.isSymbolicLink())
}

async function isDirectory(path: string): Promise<boolean> {
  const value = await lstat(path).catch(() => null)
  return Boolean(value?.isDirectory() && !value.isSymbolicLink())
}

package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.CustomToolGroup
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.storage.ColorFavoriteStore
import com.rememberber.mootool.next.compose.storage.CronFavorite
import com.rememberber.mootool.next.compose.storage.CronFavoriteStore
import com.rememberber.mootool.next.compose.storage.HistoryPrivacy
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.HostProfile
import com.rememberber.mootool.next.compose.storage.HostProfileStore
import com.rememberber.mootool.next.compose.storage.HttpCollectionStore
import com.rememberber.mootool.next.compose.storage.LegacyHistoryImport
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.RegexFavorite
import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.RegexFavoriteStore
import com.rememberber.mootool.next.compose.storage.SavedHttpRequest
import com.rememberber.mootool.next.compose.storage.TranslationHistoryItem
import com.rememberber.mootool.next.compose.storage.TranslationStore
import com.rememberber.mootool.next.compose.storage.TranslationWord
import com.rememberber.mootool.next.compose.storage.VaultFiles
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.DriverManager
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ImportPreview(
    val sourceKind: String,
    val sourceRoot: String = "",
    val fingerprint: String,
    val notes: Int,
    val jsonItems: Int,
    val customGroups: Int,
    val databaseFound: Boolean,
    val configFound: Boolean,
    val warnings: List<String>,
    val noteFiles: List<Path>,
    val jsonFiles: List<Path>,
    val groups: List<CustomToolGroup>,
    val sqliteNotes: List<ImportedSqliteNote>,
    val sqliteJson: List<ImportedSqliteJson>,
    val regexFavorites: List<ImportedRegexFavorite> = emptyList(),
    val cronFavorites: List<ImportedCronFavorite> = emptyList(),
    val colorFavorites: List<ImportedColorFavorite> = emptyList(),
    val legacyHistory: List<ImportedLegacyHistory> = emptyList(),
    val httpCollections: List<ImportedHttpCollection> = emptyList(),
    val hostProfiles: List<HostProfile> = emptyList(),
    val translationWords: List<TranslationWord> = emptyList(),
    val translationHistory: List<TranslationHistoryItem> = emptyList(),
    val legacyJavaConfigFound: Boolean = false,
    val legacyJavaConfig: LegacyJavaConfig = emptyMap(),
    val legacySettingWarningCodes: List<String> = emptyList(),
    val electronSettings: AppSettings? = null,
    val electronStorePath: Path? = null,
    /** Electron destination DB recorded a full legacy migration for this source directory. */
    val electronMigrationRunComplete: Boolean = false,
    /** Same algorithm as Electron `sourceFingerprint` for Java data roots (when resolvable). */
    val legacyElectronFingerprint: String? = null
)

data class ImportedHttpCollection(
    val dedupeKey: String,
    val request: SavedHttpRequest
)

data class ImportedSqliteNote(
    val id: String,
    val title: String,
    val content: String,
    val fontName: String,
    val fontSize: String,
    val syntax: String,
    val lineWrap: String,
    val color: String,
    val style: String,
    val createdAt: String,
    val modifiedAt: String
)

data class ImportedSqliteJson(
    val id: String,
    val name: String,
    val content: String
)

data class ImportedRegexFavorite(
    val name: String,
    val pattern: String,
    val group: String
)

data class ImportedCronFavorite(
    val name: String,
    val expression: String,
    val group: String
)

data class ImportedColorFavorite(
    val name: String,
    val value: String,
    val group: String
)

data class ImportedLegacyHistory(
    val dedupeKey: String,
    val toolId: String,
    val legacyFunc: String = "",
    val operation: String,
    val summary: String,
    val input: String,
    val output: String,
    val options: String,
    val createdAt: String
)

data class ImportApplyResult(
    val importedNotes: Int,
    val importedJson: Int,
    val importedGroups: Int,
    val importedRegex: Int,
    val importedCron: Int,
    val importedColor: Int,
    val importedHistory: Int,
    val importedHttp: Int,
    val importedHosts: Int,
    val importedTranslationWords: Int,
    val importedTranslationHistory: Int,
    val importedSettings: Boolean,
    val importedElectronSettings: Boolean,
    val skipped: Int,
    val created: List<String>
)

fun ImportPreview.totalItems(): Int =
    notes + jsonItems + customGroups + regexFavorites.size + cronFavorites.size +
        colorFavorites.size + legacyHistory.size + httpCollections.size + hostProfiles.size +
        translationWords.size + translationHistory.size +
        (if (LegacyJavaSettings.hasMigratableSettings(legacyJavaConfig)) 1 else 0) +
        (if (electronSettings != null) 1 else 0)

object CrossProductImporter {
    private const val SQLITE_ROW_LIMIT = 5_000
    private val json = Json { ignoreUnknownKeys = true }
    private val noteNames = setOf("quick-notes", "quick-note", "notes")

    fun inspect(sourceRoot: Path): ImportPreview {
        check(sourceRoot.exists() && sourceRoot.isDirectory()) { "Import source is not a directory" }
        val warnings = ArrayList<String>()
        val electronStorePath = findElectronStore(sourceRoot)
        val electronSettings = electronStorePath?.let { ElectronNextSettingsImport.loadFromStore(it) }
        if (electronStorePath != null && ElectronNextSettingsImport.hasEncryptedSecretsInStore(electronStorePath)) {
            warnings += "electron:${ElectronNextSettingsImport.WARNING_ENCRYPTED_SECRETS_SKIPPED}"
        }
        val legacyConfigFile = LegacyJavaSettings.findConfigFile(sourceRoot)
        val legacyJavaConfig = legacyConfigFile?.readText(Charsets.UTF_8)?.let { LegacyJavaSettings.parse(it) }.orEmpty()
        val legacySettingWarningCodes = LegacyJavaSettings.warningCodes(legacyJavaConfig)
        val legacyJavaConfigFound = legacyConfigFile != null
        val sqlite = findSqlite(sourceRoot)
        val noteDir = findVaultDirectory(sourceRoot, noteNames + setOf("vaults/quick-note"))
        val jsonDir = findVaultDirectory(sourceRoot, setOf("json-vault", "json", "vaults/json"))
        val groups = electronSettings?.layout?.customGroups?.takeIf { it.isNotEmpty() }
            ?: electronStorePath?.let { parseCustomGroups(it) }.orEmpty()
        val noteFiles = noteDir?.let { collectNotes(it) }.orEmpty()
        val jsonFiles = jsonDir?.let { collectJson(it) }.orEmpty()
        val sqliteNotes = sqlite?.let { readSqliteNotes(it, warnings) }.orEmpty()
        val sqliteJson = sqlite?.let { readSqliteJson(it, warnings) }.orEmpty()
        val regexFavorites = sqlite?.let { readSqliteRegexFavorites(it, warnings) }.orEmpty()
        val cronFavorites = sqlite?.let { readSqliteCronFavorites(it, warnings) }.orEmpty()
        val colorFavorites = sqlite?.let { readSqliteColorFavorites(it, warnings) }.orEmpty()
        val httpCollections = sqlite?.let { readSqliteHttpCollections(it, warnings) }.orEmpty()
        val hostProfiles = sqlite?.let { readSqliteHostProfiles(it, warnings) }.orEmpty()
        val translationWords = sqlite?.let { readSqliteTranslationWords(it, warnings) }.orEmpty()
        val translationHistory = sqlite?.let { readSqliteTranslationHistory(it, warnings) }.orEmpty()
        val httpHistory = sqlite?.let { readSqliteHttpHistory(it, warnings) }.orEmpty()
        val legacyHistoryRaw = sqlite?.let { readSqliteLegacyHistory(it, warnings) }.orEmpty() + httpHistory
        val electronMigrationKeys = sqlite?.let { readElectronMigrationDedupeKeys(it, warnings) }.orEmpty()
        val legacyHistory = if (electronMigrationKeys.isEmpty()) {
            legacyHistoryRaw
        } else {
            legacyHistoryRaw.filter { row -> row.dedupeKey.isBlank() || row.dedupeKey !in electronMigrationKeys }
        }
        if (electronMigrationKeys.isNotEmpty() && legacyHistory.size < legacyHistoryRaw.size) {
            warnings += "electron-migration-row:${legacyHistoryRaw.size - legacyHistory.size}"
        }
        val legacyJavaPaths = LegacyJavaDataPaths.resolve(sourceRoot, legacyJavaConfig)
        val legacyElectronFingerprint = runCatching {
            LegacySourceFingerprint.fingerprint(
                sourceRoot,
                listOf(
                    legacyJavaPaths.databasePath,
                    legacyJavaPaths.configPath,
                    legacyJavaPaths.quickNoteVaultPath,
                    legacyJavaPaths.jsonVaultPath
                )
            )
        }.getOrNull()
        val electronMigrationRunComplete = sqlite?.let {
            readElectronMigrationRunComplete(it, sourceRoot, legacyElectronFingerprint, warnings)
        } == true
        if (electronMigrationRunComplete) {
            warnings += "electron-migration-run"
        }
        val kind = when {
            electronStorePath != null -> "electron-next"
            sqlite != null -> "java-sqlite"
            noteFiles.isNotEmpty() || jsonFiles.isNotEmpty() -> "vault-folder"
            else -> "unknown"
        }
        if (
            kind == "unknown" &&
            regexFavorites.isEmpty() &&
            cronFavorites.isEmpty() &&
            colorFavorites.isEmpty() &&
            legacyHistory.isEmpty() &&
            httpCollections.isEmpty() &&
            hostProfiles.isEmpty() &&
            translationWords.isEmpty() &&
            translationHistory.isEmpty() &&
            !LegacyJavaSettings.hasMigratableSettings(legacyJavaConfig)
        ) {
            warnings += "No Electron store, Java SQLite notes, or vault files were found"
        }
        legacySettingWarningCodes.forEach { code ->
            warnings += "legacy:$code"
        }
        val fingerprint = fingerprint(
            sourceRoot,
            noteFiles,
            jsonFiles,
            sqliteNotes,
            sqliteJson,
            groups,
            regexFavorites,
            cronFavorites,
            colorFavorites,
            legacyHistory,
            httpCollections,
            hostProfiles,
            translationWords,
            translationHistory,
            legacyJavaConfig,
            electronStorePath
        )
        return ImportPreview(
            sourceKind = kind,
            sourceRoot = sourceRoot.toAbsolutePath().normalize().pathString,
            fingerprint = fingerprint,
            notes = noteFiles.size + sqliteNotes.size,
            jsonItems = jsonFiles.size + sqliteJson.size,
            customGroups = groups.size,
            databaseFound = sqlite != null,
            configFound = electronStorePath != null,
            warnings = warnings,
            noteFiles = noteFiles,
            jsonFiles = jsonFiles,
            groups = groups,
            sqliteNotes = sqliteNotes,
            sqliteJson = sqliteJson,
            regexFavorites = regexFavorites,
            cronFavorites = cronFavorites,
            colorFavorites = colorFavorites,
            legacyHistory = legacyHistory,
            httpCollections = httpCollections,
            hostProfiles = hostProfiles,
            translationWords = translationWords,
            translationHistory = translationHistory,
            legacyJavaConfigFound = legacyJavaConfigFound,
            legacyJavaConfig = legacyJavaConfig,
            legacySettingWarningCodes = legacySettingWarningCodes,
            electronSettings = electronSettings,
            electronStorePath = electronStorePath,
            electronMigrationRunComplete = electronMigrationRunComplete,
            legacyElectronFingerprint = legacyElectronFingerprint
        )
    }

    fun apply(
        preview: ImportPreview,
        noteVault: NoteVault,
        jsonVault: JsonVault,
        importedFingerprints: Set<String>,
        regexStore: RegexFavoriteStore? = null,
        cronStore: CronFavoriteStore? = null,
        colorStore: ColorFavoriteStore? = null,
        history: HistoryRepository? = null,
        httpStore: HttpCollectionStore? = null,
        hostStore: HostProfileStore? = null,
        translationStore: TranslationStore? = null,
        migrationRows: LegacyMigrationRowRepository? = null
    ): ImportApplyResult {
        if (migrationRows != null && preview.sourceRoot.isNotBlank()) {
            findSqlite(Path.of(preview.sourceRoot))?.let { database ->
                seedElectronMigrationRows(preview.sourceRoot, database, migrationRows, ArrayList())
            }
        }
        if (importAlreadyRecorded(preview, importedFingerprints)) {
            return ImportApplyResult(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                false,
                preview.totalItems(),
                emptyList()
            )
        }
        val created = ArrayList<String>()
        var skipped = 0
        var notes = 0
        var jsonCount = 0
        var regexCount = 0
        var cronCount = 0
        var colorCount = 0
        var historyCount = 0
        var httpCount = 0
        var hostCount = 0
        var wordCount = 0
        var transHistoryCount = 0
        if (preview.electronMigrationRunComplete) {
            skipped += preview.noteFiles.size + preview.sqliteNotes.size + preview.jsonFiles.size +
                preview.sqliteJson.size + preview.regexFavorites.size + preview.cronFavorites.size +
                preview.colorFavorites.size + preview.legacyHistory.size + preview.httpCollections.size +
                preview.hostProfiles.size + preview.translationWords.size + preview.translationHistory.size
        } else {
        preview.noteFiles.forEach { file ->
            val relative = uniqueNoteName(noteVault, file.name)
            runCatching {
                val parsed = NoteFrontmatter.parse(file.readText(Charsets.UTF_8), file.name.substringBeforeLast('.'))
                noteVault.saveNote(relative, parsed.content, parsed.metadata)
                created += relative
                notes += 1
            }.onFailure { skipped += 1 }
        }
        preview.sqliteNotes.forEach { row ->
            val relative = uniqueNoteName(noteVault, "${NoteFrontmatter.sanitizeName(row.title)}.txt")
            runCatching {
                val metadata = NoteMetadata.defaults(row.title).copy(
                    style = row.style,
                    syntax = row.syntax.ifBlank { "text/plain" },
                    fontName = row.fontName,
                    fontSize = NoteFrontmatter.clampFontSize(row.fontSize.toDoubleOrNull() ?: 14.0),
                    color = row.color.ifBlank { "default" },
                    lineWrap = row.lineWrap == "1" || row.lineWrap.equals("true", true),
                    createdAt = row.createdAt,
                    modifiedAt = row.modifiedAt.ifBlank { row.createdAt }
                )
                noteVault.saveNote(relative, row.content, metadata)
                created += relative
                notes += 1
            }.onFailure { skipped += 1 }
        }
        preview.jsonFiles.forEach { file ->
            val relative = uniqueJsonName(jsonVault, file.name)
            runCatching {
                jsonVault.createFile(relative, file.readText(Charsets.UTF_8))
                created += relative
                jsonCount += 1
            }.onFailure { skipped += 1 }
        }
        preview.sqliteJson.forEach { row ->
            val relative = uniqueJsonName(jsonVault, "${row.name.ifBlank { "json" }}.json")
            runCatching {
                jsonVault.createFile(relative, row.content.ifBlank { "{}\n" })
                created += relative
                jsonCount += 1
            }.onFailure { skipped += 1 }
        }
        if (regexStore != null && preview.regexFavorites.isNotEmpty()) {
            val toImport = preview.regexFavorites.map { row ->
                RegexFavorite(
                    id = UUID.randomUUID().toString(),
                    name = row.name.ifBlank { row.pattern.take(32) },
                    pattern = row.pattern,
                    group = row.group
                )
            }
            regexCount = regexStore.mergeImport(toImport)
            skipped += preview.regexFavorites.size - regexCount
        }
        if (cronStore != null && preview.cronFavorites.isNotEmpty()) {
            val toImport = preview.cronFavorites.map { row ->
                CronFavorite(
                    id = UUID.randomUUID().toString(),
                    name = row.name.ifBlank { row.expression.take(32) },
                    expression = row.expression,
                    group = row.group
                )
            }
            cronCount = cronStore.mergeImport(toImport)
            skipped += preview.cronFavorites.size - cronCount
        }
        if (colorStore != null && preview.colorFavorites.isNotEmpty()) {
            val triples = preview.colorFavorites.map { Triple(it.group, it.name, it.value) }
            colorCount = colorStore.mergeImportAll(triples)
            skipped += preview.colorFavorites.size - colorCount
        }
        if (history != null && preview.legacyHistory.isNotEmpty()) {
            val legacyRows = preview.legacyHistory.filter { row ->
                row.dedupeKey.isBlank() ||
                    migrationRows?.wasMigrated(preview.sourceRoot, row.dedupeKey) != true
            }
            val toImport = legacyRows.map { row ->
                LegacyHistoryImport(
                    toolId = row.toolId,
                    operation = row.operation,
                    summary = row.summary,
                    input = row.input,
                    output = row.output,
                    options = LegacyImportOptions.withDedupeKey(row.dedupeKey, row.options),
                    createdAt = row.createdAt,
                    dedupeKey = row.dedupeKey
                )
            }
            historyCount = history.mergeImport(toImport, preview.sourceRoot, migrationRows)
            skipped += preview.legacyHistory.size - historyCount
        }
        if (httpStore != null && preview.httpCollections.isNotEmpty()) {
            httpCount = httpStore.mergeImport(preview.httpCollections.map { it.request })
            skipped += preview.httpCollections.size - httpCount
        }
        if (hostStore != null && preview.hostProfiles.isNotEmpty()) {
            hostCount = hostStore.mergeImport(preview.hostProfiles)
            skipped += preview.hostProfiles.size - hostCount
        }
        if (translationStore != null && preview.translationWords.isNotEmpty()) {
            wordCount = translationStore.mergeImportWords(preview.translationWords)
            skipped += preview.translationWords.size - wordCount
        }
        if (translationStore != null && preview.translationHistory.isNotEmpty()) {
            transHistoryCount = translationStore.mergeImportHistory(preview.translationHistory)
            skipped += preview.translationHistory.size - transHistoryCount
        }
        }
        return ImportApplyResult(
            notes,
            jsonCount,
            preview.groups.size,
            regexCount,
            cronCount,
            colorCount,
            historyCount,
            httpCount,
            hostCount,
            wordCount,
            transHistoryCount,
            LegacyJavaSettings.hasMigratableSettings(preview.legacyJavaConfig),
            preview.electronSettings != null,
            skipped,
            created
        )
    }

    private fun uniqueNoteName(vault: NoteVault, name: String): String {
        if (runCatching { !vault.resolve(name).exists() }.getOrDefault(true)) return name
        val stem = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.', "txt")
        repeat(10_000) { index ->
            val candidate = "$stem imported ${index + 1}.$ext"
            if (runCatching { !vault.resolve(candidate).exists() }.getOrDefault(true)) return candidate
        }
        return "$stem-${UUID.randomUUID().toString().take(8)}.$ext"
    }

    private fun uniqueJsonName(vault: JsonVault, name: String): String {
        val withExt = if (name.contains('.')) name else "$name.json"
        if (runCatching { !vault.resolve(withExt).exists() }.getOrDefault(true)) return withExt
        val stem = withExt.substringBeforeLast('.')
        repeat(10_000) { index ->
            val candidate = "$stem imported ${index + 1}.json"
            if (runCatching { !vault.resolve(candidate).exists() }.getOrDefault(true)) return candidate
        }
        return "$stem-${UUID.randomUUID().toString().take(8)}.json"
    }

    private fun findElectronStore(root: Path): Path? {
        val names = listOf("mootool-next.json", "config.json", "settings.json")
        names.map { root.resolve(it) }.firstOrNull { it.isRegularFile() }?.let { return it }
        return Files.walk(root, 3).use { stream ->
            stream.filter { it.isRegularFile() && it.name == "mootool-next.json" }.findFirst().orElse(null)
        }
    }

    private fun findSqlite(root: Path): Path? {
        val preferred = listOf("MooTool.db", "mootool.db", "MooToolNext.db")
        preferred.map { root.resolve(it) }.firstOrNull { it.isRegularFile() }?.let { return it }
        return Files.walk(root, 2).use { stream ->
            stream.filter { it.isRegularFile() && it.extension.equals("db", true) }.findFirst().orElse(null)
        }
    }

    private fun findVaultDirectory(root: Path, names: Set<String>): Path? {
        names.map { root.resolve(it) }.firstOrNull { it.isDirectory() }?.let { return it }
        return Files.walk(root, 3).use { stream ->
            stream.filter { it.isDirectory() && it.name in setOf("quick-notes", "quick-note", "json-vault", "json") }
                .findFirst()
                .orElse(null)
        }
    }

    private fun collectNotes(directory: Path): List<Path> =
        Files.walk(directory).use { stream ->
            stream.filter { it.isRegularFile() && it.extension.lowercase() in NoteFrontmatter.noteExtensions }
                .filter { !it.fileName.startsWith(".") }
                .limit(VaultFiles.MAX_ENTRIES.toLong())
                .toList()
        }

    private fun collectJson(directory: Path): List<Path> =
        Files.walk(directory).use { stream ->
            stream.filter { it.isRegularFile() && it.extension.lowercase() in setOf("json", "jsonl") }
                .filter { !it.fileName.startsWith(".") }
                .limit(VaultFiles.MAX_ENTRIES.toLong())
                .toList()
        }

    private fun parseCustomGroups(store: Path): List<CustomToolGroup> {
        val root = runCatching { json.parseToJsonElement(store.readText()).jsonObject }.getOrNull() ?: return emptyList()
        val settings = root["settings"]?.jsonObject ?: root
        val groups = settings["layout"]?.jsonObject?.get("customGroups")?.jsonArray ?: return emptyList()
        return groups.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val ids = obj["toolIds"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                .filter { ToolId.fromId(it) != null }
            if (name.isEmpty() || ids.isEmpty()) null
            else CustomToolGroup(obj["id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString(), name, ids)
        }
    }

    private fun readSqliteNotes(database: Path, warnings: MutableList<String>): List<ImportedSqliteNote> {
        return withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_quick_note")) return@withCopiedSqlite emptyList()
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM t_quick_note ORDER BY id").use { rows ->
                    val items = ArrayList<ImportedSqliteNote>()
                    while (rows.next()) {
                        items += ImportedSqliteNote(
                            id = rows.getString("id") ?: items.size.toString(),
                            title = rows.optional("name") ?: "Legacy Note ${items.size + 1}",
                            content = rows.optional("content").orEmpty(),
                            fontName = rows.optional("font_name").orEmpty(),
                            fontSize = rows.optional("font_size") ?: "14",
                            syntax = rows.optional("syntax") ?: "text/plain",
                            lineWrap = rows.optional("line_wrap") ?: "0",
                            color = rows.optional("color") ?: "default",
                            style = rows.optional("style").orEmpty(),
                            createdAt = rows.optional("create_time").orEmpty(),
                            modifiedAt = rows.optional("modified_time").orEmpty()
                        )
                    }
                    items
                }
            }
        }
    }

    private fun readSqliteRegexFavorites(database: Path, warnings: MutableList<String>): List<ImportedRegexFavorite> =
        readSqliteFavoriteItems(database, warnings, "t_favorite_regex_item", "t_favorite_regex_list") { name, value, group ->
            ImportedRegexFavorite(name, value, group)
        }

    private fun readSqliteCronFavorites(database: Path, warnings: MutableList<String>): List<ImportedCronFavorite> =
        readSqliteFavoriteItems(database, warnings, "t_favorite_cron_item", "t_favorite_cron_list") { name, value, group ->
            ImportedCronFavorite(name, value, group)
        }

    private fun readSqliteColorFavorites(database: Path, warnings: MutableList<String>): List<ImportedColorFavorite> =
        readSqliteFavoriteItems(database, warnings, "t_favorite_color_item", "t_favorite_color_list") { name, value, group ->
            ImportedColorFavorite(name, value, group)
        }

    private fun readSqliteLegacyHistory(database: Path, warnings: MutableList<String>): List<ImportedLegacyHistory> {
        val items = ArrayList<ImportedLegacyHistory>()
        items += readSqliteFuncHistory(database, warnings)
        items += readSqliteToolDrafts(database, warnings)
        items += readSqliteQrHistory(database, warnings)
        return items
    }

    private fun readSqliteHttpCollections(database: Path, warnings: MutableList<String>): List<ImportedHttpCollection> =
        withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_msg_http")) return@withCopiedSqlite emptyList()
            val sql = "SELECT * FROM t_msg_http ORDER BY id LIMIT $SQLITE_ROW_LIMIT"
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { rows ->
                    val items = ArrayList<ImportedHttpCollection>()
                    while (rows.next()) {
                        val id = rows.optional("id") ?: items.size.toString()
                        val name = rows.optional("msg_name").orEmpty().ifBlank { "Untitled" }
                        val url = rows.optional("url").orEmpty()
                        if (name.isBlank() && url.isBlank()) continue
                        val created = LegacySqliteParsers.parseLegacyTime(rows.optional("create_time"))
                        val modified = LegacySqliteParsers.parseLegacyTime(rows.optional("modified_time"))
                        val draft = HttpRequestDraft(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            method = LegacySqliteParsers.parseHttpMethod(rows.optional("method")),
                            url = url,
                            params = LegacySqliteParsers.parseHttpPairs(rows.optional("params")),
                            headers = LegacySqliteParsers.parseHttpPairs(rows.optional("headers")),
                            cookies = LegacySqliteParsers.parseHttpCookies(rows.optional("cookies")),
                            body = rows.optional("body").orEmpty(),
                            bodyType = rows.optional("body_type").orEmpty().ifBlank { "application/json" }
                        )
                        items += ImportedHttpCollection(
                            dedupeKey = "t_msg_http:$id",
                            request = SavedHttpRequest(
                                id = draft.id,
                                draft = draft,
                                responseBody = rows.optional("response_body").orEmpty(),
                                responseHeaders = rows.optional("response_headers").orEmpty(),
                                responseCookies = rows.optional("response_cookies").orEmpty(),
                                createdAt = created,
                                modifiedAt = modified
                            )
                        )
                    }
                    items
                }
            }
        }

    private fun readSqliteHttpHistory(database: Path, warnings: MutableList<String>): List<ImportedLegacyHistory> =
        withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_http_request_history")) return@withCopiedSqlite emptyList()
            val sql = "SELECT * FROM t_http_request_history ORDER BY id DESC LIMIT $SQLITE_ROW_LIMIT"
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { rows ->
                    val items = ArrayList<ImportedLegacyHistory>()
                    while (rows.next()) {
                        val id = rows.optional("id") ?: items.size.toString()
                        val url = rows.optional("url").orEmpty()
                        val method = rows.optional("method").orEmpty().ifBlank { "GET" }
                        val title = rows.optional("title").orEmpty().ifBlank { url.take(40).ifBlank { "HTTP" } }
                        val responseBody = rows.optional("response_body").orEmpty().take(8_000)
                        if (url.isBlank() && responseBody.isBlank()) continue
                        items += ImportedLegacyHistory(
                            dedupeKey = "t_http_request_history:$id",
                            toolId = ToolId.Http.id,
                            operation = method,
                            summary = title,
                            input = HistoryPrivacy.httpUrl(url),
                            output = responseBody,
                            options = rows.optional("status").orEmpty(),
                            createdAt = rows.optional("create_time").orEmpty()
                        )
                    }
                    items
                }
            }
        }

    private fun readSqliteHostProfiles(database: Path, warnings: MutableList<String>): List<HostProfile> =
        withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_host")) return@withCopiedSqlite emptyList()
            val sql = "SELECT * FROM t_host ORDER BY id LIMIT $SQLITE_ROW_LIMIT"
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { rows ->
                    val items = ArrayList<HostProfile>()
                    while (rows.next()) {
                        val name = rows.optional("name").orEmpty().trim()
                        if (name.isEmpty()) continue
                        val created = LegacySqliteParsers.parseLegacyTime(rows.optional("create_time"))
                        val modified = LegacySqliteParsers.parseLegacyTime(rows.optional("modified_time"))
                        items += HostProfile(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            content = rows.optional("content").orEmpty(),
                            createdAt = created,
                            modifiedAt = modified
                        )
                    }
                    items
                }
            }
        }

    private fun readSqliteTranslationWords(database: Path, warnings: MutableList<String>): List<TranslationWord> =
        withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_translation_word")) return@withCopiedSqlite emptyList()
            val sql = "SELECT * FROM t_translation_word ORDER BY id LIMIT $SQLITE_ROW_LIMIT"
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { rows ->
                    val items = ArrayList<TranslationWord>()
                    while (rows.next()) {
                        val source = rows.optional("source_text").orEmpty().trim()
                        if (source.isEmpty()) continue
                        val languages = TranslationEngine.normalizeLanguagePair(
                            rows.optional("source_lang").orEmpty(),
                            rows.optional("target_lang").orEmpty()
                        )
                        val created = LegacySqliteParsers.parseLegacyTime(rows.optional("create_time"))
                        val modified = LegacySqliteParsers.parseLegacyTime(rows.optional("modified_time"))
                        items += TranslationWord(
                            id = UUID.randomUUID().toString(),
                            sourceText = source,
                            targetText = rows.optional("target_text").orEmpty(),
                            sourceLang = languages.first,
                            targetLang = languages.second,
                            remark = rows.optional("remark").orEmpty(),
                            createdAt = created,
                            modifiedAt = modified
                        )
                    }
                    items
                }
            }
        }

    private fun readSqliteTranslationHistory(database: Path, warnings: MutableList<String>): List<TranslationHistoryItem> =
        withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_translation_history")) return@withCopiedSqlite emptyList()
            val sql = "SELECT * FROM t_translation_history ORDER BY id DESC LIMIT $SQLITE_ROW_LIMIT"
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { rows ->
                    val items = ArrayList<TranslationHistoryItem>()
                    while (rows.next()) {
                        val source = rows.optional("source_text").orEmpty().trim()
                        if (source.isEmpty()) continue
                        val languages = TranslationEngine.normalizeLanguagePair(
                            rows.optional("source_lang").orEmpty(),
                            rows.optional("target_lang").orEmpty()
                        )
                        val provider = rows.optional("translator_type").orEmpty().ifBlank { "google" }
                        items += TranslationHistoryItem(
                            id = UUID.randomUUID().toString(),
                            sourceText = source,
                            targetText = rows.optional("target_text").orEmpty(),
                            sourceLang = languages.first,
                            targetLang = languages.second,
                            translatorType = provider.lowercase(),
                            createdAt = LegacySqliteParsers.parseLegacyTime(rows.optional("create_time"))
                        )
                    }
                    items
                }
            }
        }

    private fun readSqliteFuncHistory(database: Path, warnings: MutableList<String>): List<ImportedLegacyHistory> =
        withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_func_history")) return@withCopiedSqlite emptyList()
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM t_func_history ORDER BY id").use { rows ->
                    val items = ArrayList<ImportedLegacyHistory>()
                    while (rows.next()) {
                        val id = rows.optional("id") ?: items.size.toString()
                        val input = rows.optional("input_text").orEmpty()
                        val output = rows.optional("output_text").orEmpty()
                        if (input.isBlank() && output.isBlank()) continue
                        items += ImportedLegacyHistory(
                            dedupeKey = "t_func_history:$id",
                            toolId = LegacyToolIdMapper.normalize(rows.optional("func_type").orEmpty()),
                            legacyFunc = rows.optional("func_type").orEmpty(),
                            operation = "legacy",
                            summary = rows.optional("summary").orEmpty().ifBlank { "Legacy history" },
                            input = input,
                            output = output,
                            options = rows.optional("extra_data").orEmpty(),
                            createdAt = rows.optional("create_time").orEmpty()
                        )
                    }
                    items
                }
            }
        }

    private fun readSqliteToolDrafts(database: Path, warnings: MutableList<String>): List<ImportedLegacyHistory> =
        withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_func_content")) return@withCopiedSqlite emptyList()
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM t_func_content ORDER BY id").use { rows ->
                    val items = ArrayList<ImportedLegacyHistory>()
                    while (rows.next()) {
                        val id = rows.optional("id") ?: items.size.toString()
                        val content = rows.optional("content").orEmpty()
                        if (content.isBlank()) continue
                        items += ImportedLegacyHistory(
                            dedupeKey = "t_func_content:$id",
                            toolId = LegacyToolIdMapper.normalize(rows.optional("func").orEmpty()),
                            legacyFunc = rows.optional("func").orEmpty(),
                            operation = "draft",
                            summary = rows.optional("remark").orEmpty().ifBlank { "Legacy draft" },
                            input = content,
                            output = "",
                            options = """{"migratedFrom":"t_func_content"}""",
                            createdAt = rows.optional("modified_time").orEmpty().ifBlank {
                                rows.optional("create_time").orEmpty()
                            }
                        )
                    }
                    items
                }
            }
        }

    private fun readSqliteQrHistory(database: Path, warnings: MutableList<String>): List<ImportedLegacyHistory> =
        withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_qr_code")) return@withCopiedSqlite emptyList()
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM t_qr_code ORDER BY id").use { rows ->
                    val items = ArrayList<ImportedLegacyHistory>()
                    while (rows.next()) {
                        val id = rows.optional("id") ?: items.size.toString()
                        val content = rows.optional("content").orEmpty()
                        if (content.isBlank()) continue
                        val summary = content.take(40).ifBlank { "Legacy QR Code" }
                        items += ImportedLegacyHistory(
                            dedupeKey = "t_qr_code:$id",
                            toolId = ToolId.QrCode.id,
                            operation = "legacy",
                            summary = summary,
                            input = content,
                            output = "",
                            options = """{"migratedFrom":"t_qr_code"}""",
                            createdAt = rows.optional("modified_time").orEmpty().ifBlank {
                                rows.optional("create_time").orEmpty()
                            }
                        )
                    }
                    items
                }
            }
        }

    private fun <T> readSqliteFavoriteItems(
        database: Path,
        warnings: MutableList<String>,
        itemTable: String,
        listTable: String,
        map: (name: String, value: String, group: String) -> T
    ): List<T> {
        return withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, itemTable)) return@withCopiedSqlite emptyList()
            val hasList = tableExists(connection, listTable)
            val sql = if (hasList) {
                """
                SELECT item.name AS fav_name, item.value AS fav_value, list.title AS list_title
                FROM $itemTable item
                LEFT JOIN $listTable list ON list.id = item.list_id
                ORDER BY item.id
                """.trimIndent()
            } else {
                "SELECT name AS fav_name, value AS fav_value, '' AS list_title FROM $itemTable ORDER BY id"
            }
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { rows ->
                    val items = ArrayList<T>()
                    while (rows.next()) {
                        val name = rows.optional("fav_name")?.trim().orEmpty()
                        val value = rows.optional("fav_value")?.trim().orEmpty()
                        val group = rows.optional("list_title")?.trim().orEmpty().ifBlank { "默认收藏夹" }
                        if (value.isEmpty()) continue
                        items += map(
                            name.ifBlank { "Legacy ${items.size + 1}" },
                            value,
                            group
                        )
                    }
                    items
                }
            }
        }
    }

    private fun readElectronMigrationRunComplete(
        database: Path,
        sourceRoot: Path,
        legacyElectronFingerprint: String?,
        warnings: MutableList<String>
    ): Boolean = withCopiedSqlite(database, warnings) { connection ->
        if (!tableExists(connection, "t_next_migration_run")) return@withCopiedSqlite false
        val normalizedRoot = sourceRoot.toAbsolutePath().normalize().pathString
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT source_path, fingerprint FROM t_next_migration_run").use { rows ->
                while (rows.next()) {
                    val path = rows.optional("source_path")?.trim().orEmpty()
                    if (path.isNotEmpty()) {
                        val normalizedPath = runCatching {
                            Path.of(path).toAbsolutePath().normalize().pathString
                        }.getOrDefault(path)
                        if (normalizedPath == normalizedRoot) return@withCopiedSqlite true
                    }
                    val storedFingerprint = rows.optional("fingerprint")?.trim().orEmpty()
                    if (
                        legacyElectronFingerprint != null &&
                        storedFingerprint.isNotEmpty() &&
                        storedFingerprint == legacyElectronFingerprint
                    ) {
                        return@withCopiedSqlite true
                    }
                }
                false
            }
        }
    }

    private fun readElectronMigrationDedupeKeys(database: Path, warnings: MutableList<String>): Set<String> =
        withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_next_migration_row")) return@withCopiedSqlite emptySet()
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT source_table, source_id FROM t_next_migration_row").use { rows ->
                    val keys = LinkedHashSet<String>()
                    while (rows.next()) {
                        val table = rows.optional("source_table")?.trim().orEmpty()
                        val id = rows.optional("source_id")?.trim().orEmpty()
                        if (table.isEmpty() || id.isEmpty()) continue
                        keys += "$table:$id"
                    }
                    keys
                }
            }
        }

    private fun seedElectronMigrationRows(
        sourceRoot: String,
        database: Path,
        migrationRows: LegacyMigrationRowRepository,
        warnings: MutableList<String>
    ) {
        val normalizedRoot = Path.of(sourceRoot).toAbsolutePath().normalize().pathString
        withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_next_migration_row")) return@withCopiedSqlite Unit
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT source_path, source_table, source_id FROM t_next_migration_row").use { rows ->
                    var seeded = 0
                    while (rows.next()) {
                        val path = rows.optional("source_path")?.trim().orEmpty()
                        val table = rows.optional("source_table")?.trim().orEmpty()
                        val id = rows.optional("source_id")?.trim().orEmpty()
                        if (path.isEmpty() || table.isEmpty() || id.isEmpty()) continue
                        val normalizedPath = runCatching {
                            Path.of(path).toAbsolutePath().normalize().pathString
                        }.getOrDefault(path)
                        if (normalizedPath != normalizedRoot) continue
                        migrationRows.record(normalizedRoot, "$table:$id")
                        seeded += 1
                    }
                    if (seeded > 0) warnings += "electron-migration-seed:$seeded"
                }
            }
        }
    }

    private fun readSqliteJson(database: Path, warnings: MutableList<String>): List<ImportedSqliteJson> {
        return withCopiedSqlite(database, warnings) { connection ->
            if (!tableExists(connection, "t_json_beauty")) return@withCopiedSqlite emptyList()
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT * FROM t_json_beauty ORDER BY id").use { rows ->
                    val items = ArrayList<ImportedSqliteJson>()
                    while (rows.next()) {
                        items += ImportedSqliteJson(
                            id = rows.getString("id") ?: items.size.toString(),
                            name = rows.optional("name") ?: "json-${items.size + 1}",
                            content = rows.optional("content") ?: "{}"
                        )
                    }
                    items
                }
            }
        }
    }

    private fun <T> withCopiedSqlite(database: Path, warnings: MutableList<String>, block: (java.sql.Connection) -> T): T {
        val copy = Files.createTempFile("mootool-compose-import-", ".sqlite")
        return try {
            Files.copy(database, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            Class.forName("org.sqlite.JDBC")
            DriverManager.getConnection("jdbc:sqlite:${copy.toAbsolutePath()}").use { connection ->
                block(connection)
            }
        } catch (error: Exception) {
            warnings += "SQLite source could not be read: ${error.message}"
            @Suppress("UNCHECKED_CAST")
            emptyList<Any>() as T
        } finally {
            Files.deleteIfExists(copy)
        }
    }

    internal fun importAlreadyRecorded(preview: ImportPreview, importedFingerprints: Set<String>): Boolean {
        if (preview.fingerprint in importedFingerprints) return true
        val legacy = preview.legacyElectronFingerprint?.trim().orEmpty()
        return legacy.isNotEmpty() && legacy in importedFingerprints
    }

    private fun tableExists(connection: java.sql.Connection, name: String): Boolean =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$name'").use { it.next() }
        }

    private fun java.sql.ResultSet.optional(column: String): String? =
        runCatching { getString(column) }.getOrNull()

    private fun fingerprint(
        root: Path,
        notes: List<Path>,
        jsonFiles: List<Path>,
        sqliteNotes: List<ImportedSqliteNote>,
        sqliteJson: List<ImportedSqliteJson>,
        groups: List<CustomToolGroup>,
        regexFavorites: List<ImportedRegexFavorite>,
        cronFavorites: List<ImportedCronFavorite>,
        colorFavorites: List<ImportedColorFavorite>,
        legacyHistory: List<ImportedLegacyHistory>,
        httpCollections: List<ImportedHttpCollection>,
        hostProfiles: List<HostProfile>,
        translationWords: List<TranslationWord>,
        translationHistory: List<TranslationHistoryItem>,
        legacyJavaConfig: LegacyJavaConfig,
        electronStorePath: Path?
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(root.toAbsolutePath().normalize().pathString.toByteArray())
        notes.forEach { digest.update(it.relativeTo(root).pathString.toByteArray()) }
        jsonFiles.forEach { digest.update(it.relativeTo(root).pathString.toByteArray()) }
        sqliteNotes.forEach { digest.update((it.id + it.title).toByteArray()) }
        sqliteJson.forEach { digest.update((it.id + it.name).toByteArray()) }
        groups.forEach { digest.update((it.id + it.name).toByteArray()) }
        regexFavorites.forEach { digest.update((it.name + it.pattern + it.group).toByteArray()) }
        cronFavorites.forEach { digest.update((it.name + it.expression + it.group).toByteArray()) }
        colorFavorites.forEach { digest.update((it.name + it.value + it.group).toByteArray()) }
        legacyHistory.forEach { digest.update(it.dedupeKey.toByteArray()) }
        httpCollections.forEach { digest.update(it.dedupeKey.toByteArray()) }
        hostProfiles.forEach { digest.update((it.name + it.content.length).toByteArray()) }
        translationWords.forEach { digest.update((it.sourceText + it.targetLang).toByteArray()) }
        translationHistory.forEach { digest.update((it.sourceText + it.createdAt).toByteArray()) }
        legacyJavaConfig.forEach { (group, entries) ->
            digest.update(group.toByteArray())
            entries.forEach { (key, value) -> digest.update((key + value).toByteArray()) }
        }
        electronStorePath?.let { digest.update(it.toAbsolutePath().normalize().pathString.toByteArray()) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

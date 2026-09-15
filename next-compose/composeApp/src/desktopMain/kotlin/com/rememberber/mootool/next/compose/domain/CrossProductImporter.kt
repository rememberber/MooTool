package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.CustomToolGroup
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.storage.NoteVault
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
    val fingerprint: String,
    val notes: Int,
    val jsonItems: Int,
    val customGroups: Int,
    val warnings: List<String>,
    val noteFiles: List<Path>,
    val jsonFiles: List<Path>,
    val groups: List<CustomToolGroup>,
    val sqliteNotes: List<ImportedSqliteNote>,
    val sqliteJson: List<ImportedSqliteJson>
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

data class ImportApplyResult(
    val importedNotes: Int,
    val importedJson: Int,
    val importedGroups: Int,
    val skipped: Int,
    val created: List<String>
)

object CrossProductImporter {
    private val json = Json { ignoreUnknownKeys = true }
    private val noteNames = setOf("quick-notes", "quick-note", "notes")

    fun inspect(sourceRoot: Path): ImportPreview {
        check(sourceRoot.exists() && sourceRoot.isDirectory()) { "Import source is not a directory" }
        val warnings = ArrayList<String>()
        val electronStore = findElectronStore(sourceRoot)
        val sqlite = findSqlite(sourceRoot)
        val noteDir = findVaultDirectory(sourceRoot, noteNames + setOf("vaults/quick-note"))
        val jsonDir = findVaultDirectory(sourceRoot, setOf("json-vault", "json", "vaults/json"))
        val groups = electronStore?.let { parseCustomGroups(it) }.orEmpty()
        val noteFiles = noteDir?.let { collectNotes(it) }.orEmpty()
        val jsonFiles = jsonDir?.let { collectJson(it) }.orEmpty()
        val sqliteNotes = sqlite?.let { readSqliteNotes(it, warnings) }.orEmpty()
        val sqliteJson = sqlite?.let { readSqliteJson(it, warnings) }.orEmpty()
        val kind = when {
            electronStore != null -> "electron-next"
            sqlite != null -> "java-sqlite"
            noteFiles.isNotEmpty() || jsonFiles.isNotEmpty() -> "vault-folder"
            else -> "unknown"
        }
        if (kind == "unknown") warnings += "No Electron store, Java SQLite notes, or vault files were found"
        val fingerprint = fingerprint(sourceRoot, noteFiles, jsonFiles, sqliteNotes, sqliteJson, groups)
        return ImportPreview(
            sourceKind = kind,
            fingerprint = fingerprint,
            notes = noteFiles.size + sqliteNotes.size,
            jsonItems = jsonFiles.size + sqliteJson.size,
            customGroups = groups.size,
            warnings = warnings,
            noteFiles = noteFiles,
            jsonFiles = jsonFiles,
            groups = groups,
            sqliteNotes = sqliteNotes,
            sqliteJson = sqliteJson
        )
    }

    fun apply(preview: ImportPreview, noteVault: NoteVault, jsonVault: JsonVault, importedFingerprints: Set<String>): ImportApplyResult {
        if (preview.fingerprint in importedFingerprints) {
            return ImportApplyResult(0, 0, 0, preview.notes + preview.jsonItems + preview.customGroups, emptyList())
        }
        val created = ArrayList<String>()
        var skipped = 0
        var notes = 0
        var jsonCount = 0
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
        return ImportApplyResult(notes, jsonCount, preview.groups.size, skipped, created)
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
        groups: List<CustomToolGroup>
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(root.toAbsolutePath().normalize().pathString.toByteArray())
        notes.forEach { digest.update(it.relativeTo(root).pathString.toByteArray()) }
        jsonFiles.forEach { digest.update(it.relativeTo(root).pathString.toByteArray()) }
        sqliteNotes.forEach { digest.update((it.id + it.title).toByteArray()) }
        sqliteJson.forEach { digest.update((it.id + it.name).toByteArray()) }
        groups.forEach { digest.update((it.id + it.name).toByteArray()) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

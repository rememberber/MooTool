package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.model.HistoryRecord
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString

class AppDatabase(bootstrap: AppDirectories) : AutoCloseable {
    private val lock = ReentrantLock()
    @Volatile
    private var dataDirectories: AppDirectories = bootstrap
    @Volatile
    var connection: Connection = openConnection()
        private set

    init {
        migrate()
    }

    fun rebindDataDirectories(next: AppDirectories) {
        if (next.databaseFile == dataDirectories.databaseFile) {
            dataDirectories = next
            return
        }
        lock.withLock {
            dataDirectories = next
            runCatching { if (!connection.isClosed) connection.close() }
            connection = openConnection()
            migrate()
        }
    }

    fun checkpoint() {
        lock.withLock {
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA wal_checkpoint(FULL)")
            }
        }
    }

    fun reopen() {
        lock.withLock {
            runCatching { if (!connection.isClosed) connection.close() }
            connection = openConnection()
            migrate()
        }
    }

    private fun openConnection(): Connection {
        dataDirectories.dataRoot.createDirectories()
        Class.forName("org.sqlite.JDBC")
        val next = DriverManager.getConnection("jdbc:sqlite:${dataDirectories.databaseFile.pathString}")
        next.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute("PRAGMA journal_mode = WAL")
            statement.execute("PRAGMA busy_timeout = 5000")
        }
        return next
    }

    fun <T> write(block: (Connection) -> T): T = lock.withLock {
        connection.autoCommit = false
        try {
            val result = block(connection)
            connection.commit()
            result
        } catch (error: Exception) {
            connection.rollback()
            throw error
        } finally {
            connection.autoCommit = true
        }
    }

    private fun migrate() {
        write { db ->
            db.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                      version INTEGER PRIMARY KEY,
                      applied_at TEXT NOT NULL,
                      checksum TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS tool_sessions (
                      tool_id TEXT PRIMARY KEY,
                      schema_version INTEGER NOT NULL,
                      state_json TEXT NOT NULL,
                      updated_at TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS history (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      tool_id TEXT NOT NULL,
                      operation TEXT NOT NULL,
                      summary TEXT NOT NULL,
                      input TEXT,
                      output TEXT,
                      options TEXT,
                      created_at TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute("CREATE INDEX IF NOT EXISTS idx_history_tool ON history(tool_id, created_at DESC)")
            }
            val exists = db.prepareStatement("SELECT 1 FROM schema_migrations WHERE version = 1").use { it.executeQuery().next() }
            if (!exists) {
                db.prepareStatement("INSERT INTO schema_migrations(version, applied_at, checksum) VALUES (1, ?, 'v1-core')")
                    .use { statement ->
                        statement.setString(1, Instant.now().toString())
                        statement.executeUpdate()
                    }
            }
            db.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS import_migration_row (
                      source_path TEXT NOT NULL,
                      source_table TEXT NOT NULL,
                      source_id TEXT NOT NULL,
                      migrated_at TEXT NOT NULL,
                      PRIMARY KEY (source_path, source_table, source_id)
                    )
                    """.trimIndent()
                )
            }
            val v2 = db.prepareStatement("SELECT 1 FROM schema_migrations WHERE version = 2").use { it.executeQuery().next() }
            if (!v2) {
                db.prepareStatement("INSERT INTO schema_migrations(version, applied_at, checksum) VALUES (2, ?, 'v2-import-migration-row')")
                    .use { statement ->
                        statement.setString(1, Instant.now().toString())
                        statement.executeUpdate()
                    }
            }
        }
    }

    override fun close() {
        connection.close()
    }
}

class HistoryRepository(private val database: AppDatabase, private val limit: Int = 200) {
    fun save(toolId: String, operation: String, summary: String, input: String, output: String, options: String = "") {
        if (input.isBlank() && output.isBlank()) return
        database.write { db ->
            db.prepareStatement(
                """
                INSERT INTO history(tool_id, operation, summary, input, output, options, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, toolId)
                statement.setString(2, operation)
                statement.setString(3, summary)
                statement.setString(4, input)
                statement.setString(5, output)
                statement.setString(6, options)
                statement.setString(7, Instant.now().toString())
                statement.executeUpdate()
            }
            db.prepareStatement(
                """
                DELETE FROM history WHERE tool_id = ? AND id NOT IN (
                  SELECT id FROM history WHERE tool_id = ? ORDER BY created_at DESC, id DESC LIMIT ?
                )
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, toolId)
                statement.setString(2, toolId)
                statement.setInt(3, limit)
                statement.executeUpdate()
            }
        }
    }

    fun list(toolId: String, keyword: String = ""): List<HistoryRecord> {
        val sql = if (keyword.isBlank()) {
            "SELECT id, tool_id, operation, summary, input, output, options, created_at FROM history WHERE tool_id = ? ORDER BY created_at DESC, id DESC"
        } else {
            """
            SELECT id, tool_id, operation, summary, input, output, options, created_at
            FROM history
            WHERE tool_id = ? AND (
              summary LIKE ? OR input LIKE ? OR output LIKE ? OR operation LIKE ?
            )
            ORDER BY created_at DESC, id DESC
            """.trimIndent()
        }
        return database.connection.prepareStatement(sql).use { statement ->
            statement.setString(1, toolId)
            if (keyword.isNotBlank()) {
                val like = "%$keyword%"
                statement.setString(2, like)
                statement.setString(3, like)
                statement.setString(4, like)
                statement.setString(5, like)
            }
            statement.executeQuery().use { rows ->
                val items = ArrayList<HistoryRecord>()
                while (rows.next()) {
                    items += HistoryRecord(
                        id = rows.getLong("id"),
                        toolId = rows.getString("tool_id"),
                        operation = rows.getString("operation"),
                        summary = rows.getString("summary"),
                        input = rows.getString("input").orEmpty(),
                        output = rows.getString("output").orEmpty(),
                        options = rows.getString("options").orEmpty(),
                        createdAt = rows.getString("created_at")
                    )
                }
                items
            }
        }
    }

    fun delete(id: Long) {
        database.write { db ->
            db.prepareStatement("DELETE FROM history WHERE id = ?").use { statement ->
                statement.setLong(1, id)
                statement.executeUpdate()
            }
        }
    }

    fun clear(toolId: String) {
        database.write { db ->
            db.prepareStatement("DELETE FROM history WHERE tool_id = ?").use { statement ->
                statement.setString(1, toolId)
                statement.executeUpdate()
            }
        }
    }

    fun mergeImport(
        entries: List<LegacyHistoryImport>,
        sourcePath: String = "",
        migrationRows: LegacyMigrationRowRepository? = null
    ): Int {
        if (entries.isEmpty()) return 0
        var imported = 0
        database.write { db ->
            db.prepareStatement(
                """
                INSERT INTO history(tool_id, operation, summary, input, output, options, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { insert ->
                db.prepareStatement(
                    """
                    SELECT 1 FROM history
                    WHERE json_extract(options, '$.importDedupeKey') = ?
                    LIMIT 1
                    """.trimIndent()
                ).use { dedupeExists ->
                db.prepareStatement(
                    """
                    SELECT 1 FROM history
                    WHERE tool_id = ? AND summary = ? AND input = ? AND created_at = ?
                    LIMIT 1
                    """.trimIndent()
                ).use { exists ->
                    val touched = LinkedHashSet<String>()
                    entries.forEach { entry ->
                        if (entry.input.isBlank() && entry.output.isBlank()) return@forEach
                        val createdAt = entry.createdAt.ifBlank { Instant.now().toString() }
                        if (entry.dedupeKey.isNotBlank()) {
                            if (migrationRows?.wasMigrated(sourcePath, entry.dedupeKey) == true) return@forEach
                            dedupeExists.setString(1, entry.dedupeKey)
                            if (dedupeExists.executeQuery().use { it.next() }) return@forEach
                        }
                        exists.setString(1, entry.toolId)
                        exists.setString(2, entry.summary)
                        exists.setString(3, entry.input)
                        exists.setString(4, createdAt)
                        if (exists.executeQuery().use { it.next() }) return@forEach
                        insert.setString(1, entry.toolId)
                        insert.setString(2, entry.operation)
                        insert.setString(3, entry.summary)
                        insert.setString(4, entry.input)
                        insert.setString(5, entry.output)
                        insert.setString(6, entry.options)
                        insert.setString(7, createdAt)
                        insert.executeUpdate()
                        imported += 1
                        touched += entry.toolId
                        if (sourcePath.isNotBlank() && entry.dedupeKey.isNotBlank()) {
                            migrationRows?.record(sourcePath, entry.dedupeKey, db)
                        }
                    }
                    touched.forEach { toolId -> trimToLimit(db, toolId) }
                }
                }
            }
        }
        return imported
    }

    private fun trimToLimit(db: java.sql.Connection, toolId: String) {
        db.prepareStatement(
            """
            DELETE FROM history WHERE tool_id = ? AND id NOT IN (
              SELECT id FROM history WHERE tool_id = ? ORDER BY created_at DESC, id DESC LIMIT ?
            )
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, toolId)
            statement.setString(2, toolId)
            statement.setInt(3, limit)
            statement.executeUpdate()
        }
    }
}

data class LegacyHistoryImport(
    val toolId: String,
    val operation: String,
    val summary: String,
    val input: String,
    val output: String,
    val options: String = "",
    val createdAt: String = "",
    val dedupeKey: String = ""
)

class LegacyMigrationRowRepository(private val database: AppDatabase) {
    fun wasMigrated(sourcePath: String, dedupeKey: String): Boolean {
        val parts = parseDedupeKey(dedupeKey) ?: return false
        val normalized = normalizeSourcePath(sourcePath)
        if (normalized.isEmpty()) return false
        return database.connection.prepareStatement(
            """
            SELECT 1 FROM import_migration_row
            WHERE source_path = ? AND source_table = ? AND source_id = ?
            LIMIT 1
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, normalized)
            statement.setString(2, parts.first)
            statement.setString(3, parts.second)
            statement.executeQuery().use { it.next() }
        }
    }

    fun record(sourcePath: String, dedupeKey: String, connection: Connection? = null) {
        val parts = parseDedupeKey(dedupeKey) ?: return
        val normalized = normalizeSourcePath(sourcePath)
        if (normalized.isEmpty()) return
        fun insert(db: Connection) {
            db.prepareStatement(
                """
                INSERT OR IGNORE INTO import_migration_row(source_path, source_table, source_id, migrated_at)
                VALUES (?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, normalized)
                statement.setString(2, parts.first)
                statement.setString(3, parts.second)
                statement.setString(4, Instant.now().toString())
                statement.executeUpdate()
            }
        }
        if (connection != null) {
            insert(connection)
        } else {
            database.write { db -> insert(db) }
        }
    }

    private fun parseDedupeKey(dedupeKey: String): Pair<String, String>? {
        val trimmed = dedupeKey.trim()
        val index = trimmed.indexOf(':')
        if (index <= 0 || index >= trimmed.length - 1) return null
        return trimmed.substring(0, index) to trimmed.substring(index + 1)
    }

    private fun normalizeSourcePath(sourcePath: String): String =
        runCatching { java.nio.file.Path.of(sourcePath).toAbsolutePath().normalize().pathString }.getOrDefault(sourcePath.trim())
}

class SessionStore(private val database: AppDatabase) {
    fun load(toolId: String): String? {
        return database.connection.prepareStatement("SELECT state_json FROM tool_sessions WHERE tool_id = ?").use { statement ->
            statement.setString(1, toolId)
            statement.executeQuery().use { rows -> if (rows.next()) rows.getString(1) else null }
        }
    }

    fun save(toolId: String, stateJson: String) {
        database.write { db ->
            db.prepareStatement(
                """
                INSERT INTO tool_sessions(tool_id, schema_version, state_json, updated_at)
                VALUES (?, 1, ?, ?)
                ON CONFLICT(tool_id) DO UPDATE SET state_json = excluded.state_json, updated_at = excluded.updated_at
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, toolId)
                statement.setString(2, stateJson)
                statement.setString(3, Instant.now().toString())
                statement.executeUpdate()
            }
        }
    }
}

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

class AppDatabase(private val directories: AppDirectories) : AutoCloseable {
    private val lock = ReentrantLock()
    @Volatile
    var connection: Connection = openConnection()
        private set

    init {
        migrate()
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
        directories.dataRoot.createDirectories()
        Class.forName("org.sqlite.JDBC")
        val next = DriverManager.getConnection("jdbc:sqlite:${directories.databaseFile.pathString}")
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

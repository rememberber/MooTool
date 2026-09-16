package com.rememberber.mootool.next.compose.domain

import java.sql.DriverManager
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElectronMigrationRowFilterTest {
    @Test
    fun inspectSkipsLegacyRowsRecordedInElectronMigrationTable() {
        val source = createTempDirectory("electron-migration-source-")
        source.resolve("mootool-next.json").writeText("""{"settings":{"general":{"language":"zh-CN"}}}""")
        val db = source.resolve("MooTool.db")
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:${db.toAbsolutePath()}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE t_next_migration_row (
                      source_path TEXT NOT NULL,
                      source_table TEXT NOT NULL,
                      source_id TEXT NOT NULL,
                      target_hint TEXT NOT NULL DEFAULT '',
                      migrated_at TEXT NOT NULL,
                      PRIMARY KEY (source_path, source_table, source_id)
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE t_func_content (
                      id INTEGER PRIMARY KEY, func TEXT, content TEXT, remark TEXT, create_time TEXT, modified_time TEXT
                    )
                    """.trimIndent()
                )
                statement.execute("INSERT INTO t_func_content VALUES (1, 'Regex', '^skip$', 'draft', '2026-01-01', '2026-01-01')")
                statement.execute("INSERT INTO t_func_content VALUES (2, 'Regex', '^keep$', 'draft', '2026-01-02', '2026-01-02')")
                statement.execute(
                    """
                    INSERT INTO t_next_migration_row VALUES (
                      '${source.toAbsolutePath().normalize()}', 't_func_content', '1', '', '2026-01-01'
                    )
                    """.trimIndent()
                )
            }
        }
        val preview = CrossProductImporter.inspect(source)
        assertEquals(1, preview.legacyHistory.size)
        assertEquals("^keep$", preview.legacyHistory.single().input)
        assertTrue(preview.warnings.any { it.startsWith("electron-migration-row:") })
    }

    @Test
    fun inspectMarksElectronMigrationRunForSameSourcePath() {
        val source = createTempDirectory("electron-migration-run-")
        source.resolve("mootool-next.json").writeText("""{"settings":{"general":{"language":"zh-CN"}}}""")
        val db = source.resolve("MooTool.db")
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:${db.toAbsolutePath()}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE t_next_migration_run (
                      fingerprint TEXT PRIMARY KEY,
                      source_path TEXT NOT NULL,
                      migrated_at TEXT NOT NULL,
                      report_json TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    INSERT INTO t_next_migration_run VALUES (
                      'legacy-fp', '${source.toAbsolutePath().normalize()}', '2026-01-01', '{}'
                    )
                    """.trimIndent()
                )
            }
        }
        val preview = CrossProductImporter.inspect(source)
        assertTrue(preview.electronMigrationRunComplete)
        assertTrue(preview.warnings.contains("electron-migration-run"))
    }
}

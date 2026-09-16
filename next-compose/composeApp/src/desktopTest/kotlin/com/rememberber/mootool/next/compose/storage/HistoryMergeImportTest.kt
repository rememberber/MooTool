package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.LegacyImportOptions
import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryMergeImportTest {
    @Test
    fun skipsDuplicateLegacyRowsByImportDedupeKey() {
        val root = createTempDirectory("history-dedupe-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val history = HistoryRepository(database)
        val entry = LegacyHistoryImport(
            toolId = ToolId.Regex.id,
            operation = "draft",
            summary = "Legacy draft",
            input = "^moo$",
            output = "",
            options = LegacyImportOptions.withDedupeKey("t_func_content:1", """{"migratedFrom":"t_func_content"}"""),
            createdAt = "2026-01-01",
            dedupeKey = "t_func_content:1"
        )
        assertEquals(1, history.mergeImport(listOf(entry)))
        assertEquals(0, history.mergeImport(listOf(entry)))
        assertEquals(1, history.list(ToolId.Regex.id).size)
        database.close()
    }

    @Test
    fun recordsImportMigrationRowForSourcePath() {
        val root = createTempDirectory("history-migration-row-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val history = HistoryRepository(database)
        val migrationRows = LegacyMigrationRowRepository(database)
        val source = root.resolve("legacy-source").toAbsolutePath().normalize().pathString
        val entry = LegacyHistoryImport(
            toolId = ToolId.Json.id,
            operation = "legacy",
            summary = "Legacy history",
            input = """{"a":1}""",
            output = "",
            options = LegacyImportOptions.withDedupeKey("t_func_history:9", ""),
            createdAt = "2026-01-01",
            dedupeKey = "t_func_history:9"
        )
        assertEquals(1, history.mergeImport(listOf(entry), source, migrationRows))
        assertTrue(migrationRows.wasMigrated(source, "t_func_history:9"))
        assertEquals(0, history.mergeImport(listOf(entry), source, migrationRows))
        database.close()
    }
}

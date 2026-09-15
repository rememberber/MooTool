package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.storage.NoteVault
import java.sql.DriverManager
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultGitCheckpointSchedulerTest {
    @Test
    fun waitsForIdleThresholdAndCheckpointsOncePerActivity() {
        var now = 1_000L
        var calls = 0
        val scheduler = VaultGitCheckpointScheduler(
            enabled = { true },
            hasUnsavedEditorChanges = { false },
            idleMilliseconds = { 30_000 },
            inactiveMilliseconds = { 120_000 },
            checkpoint = { GitActionResult(true, "Done").also { calls += 1 } },
            now = { now }
        )
        scheduler.recordActivity("Update Quick Note")
        now += 29_999
        assertEquals(false, scheduler.evaluate())
        now += 1
        assertEquals(true, scheduler.evaluate())
        assertEquals(1, calls)
        assertEquals(false, scheduler.evaluate())
        now += 1
        scheduler.recordActivity("Update JSON snippet")
        now += 30_000
        assertEquals(true, scheduler.evaluate())
        assertEquals(2, calls)
    }

    @Test
    fun waitsForEditorAutosaveAndSupportsInactiveThreshold() {
        var now = 10_000L
        var dirty = true
        var calls = 0
        val scheduler = VaultGitCheckpointScheduler(
            enabled = { true },
            hasUnsavedEditorChanges = { dirty },
            idleMilliseconds = { 30_000 },
            inactiveMilliseconds = { 120_000 },
            checkpoint = { GitActionResult(true, "Done").also { calls += 1 } },
            now = { now }
        )
        scheduler.recordActivity("Update Vault")
        scheduler.setWindowActive(false)
        now += 120_000
        assertEquals(false, scheduler.evaluate())
        dirty = false
        assertEquals(true, scheduler.evaluate())
        assertEquals(1, calls)
    }

    @Test
    fun retriesFailedCheckpointInsteadOfConsumingActivity() {
        var now = 1_000L
        var calls = 0
        val scheduler = VaultGitCheckpointScheduler(
            enabled = { true },
            hasUnsavedEditorChanges = { false },
            idleMilliseconds = { 30_000 },
            inactiveMilliseconds = { 120_000 },
            checkpoint = {
                calls += 1
                GitActionResult(calls > 1, if (calls > 1) "Done" else "failed")
            },
            now = { now }
        )
        scheduler.recordActivity("Update Vault")
        now += 30_000
        assertEquals(false, scheduler.evaluate())
        assertEquals(true, scheduler.evaluate())
        assertEquals(2, calls)
    }
}

class CrossProductImporterTest {
    @Test
    fun importsElectronNotesAndJavaSqliteWithoutTouchingSource() {
        val source = createTempDirectory("mootool-import-source-")
        source.resolve("quick-notes").createDirectories()
        source.resolve("quick-notes").resolve("Work").createDirectories()
        source.resolve("quick-notes").resolve("Work").resolve("API ideas.txt").writeText(
            NoteFrontmatter.serialize(
                NoteMetadata.defaults("API ideas").copy(fontName = "PingFang SC", fontSize = 15),
                "# Hello\nneedle"
            )
        )
        source.resolve("mootool-next.json").writeText(
            """{"settings":{"layout":{"customGroups":[{"id":"g1","name":"开发常用","toolIds":["json","http"]}]}}}"""
        )
        val db = source.resolve("MooTool.db")
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:${db.toAbsolutePath()}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE t_quick_note (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT, color TEXT, style TEXT, font_name TEXT, font_size TEXT, syntax TEXT, line_wrap TEXT)")
                statement.execute("INSERT INTO t_quick_note VALUES (1, 'Database Note', 'Database note body', '2026-01-01', '2026-01-02', 'blue', '', 'Monaco', '15', 'text/plain', '1')")
                statement.execute("CREATE TABLE t_json_beauty (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT)")
                statement.execute("""INSERT INTO t_json_beauty VALUES (1, 'Database JSON', '{"database":true}', '2026-01-01', '2026-01-02')""")
            }
        }
        val originalHash = source.resolve("quick-notes/Work/API ideas.txt").readText()
        val preview = CrossProductImporter.inspect(source)
        assertEquals("electron-next", preview.sourceKind)
        assertEquals(1, preview.customGroups)
        assertTrue(preview.notes >= 2)
        assertTrue(preview.jsonItems >= 1)

        val target = createTempDirectory("mootool-import-target-")
        val directories = AppPaths.resolve(target.toString()).also { it.ensureCreated() }
        val notes = NoteVault(directories)
        val json = JsonVault(directories)
        val applied = CrossProductImporter.apply(preview, notes, json, emptySet())
        assertTrue(applied.importedNotes >= 2)
        assertTrue(applied.importedJson >= 1)
        assertEquals(originalHash, source.resolve("quick-notes/Work/API ideas.txt").readText())
        assertTrue(notes.list("needle", includeContent = true).isNotEmpty())
        assertTrue(json.list("database", includeContent = true).isNotEmpty())
        val skipped = CrossProductImporter.apply(preview, notes, json, setOf(preview.fingerprint))
        assertEquals(0, skipped.importedNotes)
    }
}

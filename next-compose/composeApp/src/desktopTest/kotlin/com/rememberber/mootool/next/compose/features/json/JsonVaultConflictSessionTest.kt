package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import javax.swing.SwingUtilities
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsonVaultConflictSessionTest {
    @Test
    fun dirtyOpenFileReportsConflictWhenDiskChangesUnderSamePath() {
        val fixture = openJson("open.json", """{"v":1}""")
        val container = fixture.container
        val session = fixture.session
        try {
            SwingUtilities.invokeAndWait {
                session.editor.setText("""{"v":1,"local":true}""", recordUndo = false)
            }
            container.jsonVault.write("open.json", """{"v":2}""")
            var conflict: VaultConflictState? = null
            SwingUtilities.invokeAndWait {
                val handled = jsonVaultApplyExternalChange(
                    container,
                    session,
                    listOf("open.json"),
                ) { conflict = it }
                assertFalse(handled)
            }
            val pending = assertNotNull(conflict)
            assertEquals("open.json", pending.relativePath)
            assertEquals("""{"v":1,"local":true}""", pending.editorText)
            assertTrue(pending.diskText!!.contains("\"v\":2"))
            assertFalse(pending.deleted)
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    @Test
    fun saveBlockedWhenDiskDivergedFromLastSavedBaseline() {
        val fixture = openJson("open.json", """{"v":1}""")
        val container = fixture.container
        val session = fixture.session
        try {
            SwingUtilities.invokeAndWait {
                session.editor.setText("""{"v":1,"edit":true}""", recordUndo = false)
            }
            container.jsonVault.write("open.json", """{"v":9}""")
            var conflict: VaultConflictState? = null
            SwingUtilities.invokeAndWait {
                val result = saveJsonVault(container, session, monitor = null) { conflict = it }
                assertTrue(result.isFailure)
            }
            assertNotNull(conflict)
            assertEquals("open.json", conflict!!.relativePath)
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    private data class Fixture(val container: AppContainer, val session: JsonSession, val root: java.nio.file.Path)

    private fun openJson(path: String, content: String): Fixture {
        val productRoot = createTempDirectory("mootool-json-conflict-session-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settings = SettingsRepository(directories).also { it.load() }
        val database = AppDatabase(directories)
        val container = AppContainer(
            directories = directories,
            settingsRepository = settings,
            database = database,
            history = HistoryRepository(database),
            migrationRows = LegacyMigrationRowRepository(database),
            sessions = SessionStore(database),
        )
        container.jsonVault.createFile(path, content)
        val session = JsonSession().apply {
            currentFile = path
            savedText = content
            editor.setText(content, recordUndo = false)
        }
        return Fixture(container, session, productRoot)
    }
}

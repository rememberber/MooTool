package com.rememberber.mootool.next.compose.features.vault

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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VaultConflictActionsTest {
    @Test
    fun jsonKeepClearsConflictOnly() {
        val fixture = jsonFixture()
        try {
            val session = fixture.session
            SwingUtilities.invokeAndWait {
                session.editor.setText("""{"keep":true}""", recordUndo = false)
            }
            session.vaultConflict = VaultConflictState("a.json", """{"keep":true}""", """{"disk":1}""", false)
            applyJsonVaultConflictKeep(session)
            assertNull(session.vaultConflict)
            assertEquals("""{"keep":true}""", session.editor.text)
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    @Test
    fun jsonReloadUsesProductionHelper() {
        val fixture = jsonFixture()
        val container = fixture.container
        val session = fixture.session
        try {
            container.jsonVault.createFile("open.json", """{"disk":true}""")
            SwingUtilities.invokeAndWait {
                session.currentFile = "open.json"
                session.editor.setText("""{"local":true}""", recordUndo = false)
                session.pathResult = """["stale"]"""
            }
            val pending = VaultConflictState("open.json", session.editor.text, """{"disk":true}""", deleted = false)
            session.vaultConflict = pending
            SwingUtilities.invokeAndWait { applyJsonVaultConflictReload(container, session, pending) }
            SwingUtilities.invokeAndWait { }
            assertNull(session.vaultConflict)
            SwingUtilities.invokeAndWait { }
            assertEquals("""{"disk":true}""", session.savedText.trim())
            assertEquals("", session.pathResult)
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    @Test
    fun jsonSaveCopyUsesProductionHelper() {
        val fixture = jsonFixture()
        val container = fixture.container
        val session = fixture.session
        try {
            container.jsonVault.createFile("open.json", """{"v":2}""")
            SwingUtilities.invokeAndWait {
                session.currentFile = "open.json"
                session.editor.setText("""{"v":2,"local":true}""", recordUndo = false)
            }
            val pending = VaultConflictState("open.json", session.editor.text, """{"v":2}""", deleted = false)
            session.vaultConflict = pending
            val fixed = 1_700_000_003_000L
            SwingUtilities.invokeAndWait {
                assertTrue(applyJsonVaultConflictSaveCopy(container, session, pending, null, fixed))
            }
            SwingUtilities.invokeAndWait { }
            assertNull(session.vaultConflict)
            assertTrue(container.jsonVault.read("open.local-$fixed.json").contains("local"))
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    private data class JsonFixture(val container: AppContainer, val session: JsonSession, val root: java.nio.file.Path)

    private fun jsonFixture(): JsonFixture {
        val productRoot = createTempDirectory("mootool-vault-conflict-actions-")
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
        val session = JsonSession()
        return JsonFixture(container, session, productRoot)
    }
}

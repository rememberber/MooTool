package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
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

/** 对齐 `JsonVaultConflictOverlay` / `QuickNoteVaultConflictOverlay` 与 Screen 回调链。 */
class VaultConflictOverlayFlowTest {
    @Test
    fun jsonOverlaySaveCopyClearsConflictAndWritesCopy() {
        val productRoot = createTempDirectory("mootool-json-conflict-overlay-")
        val container = appContainer(productRoot)
        val session = JsonSession()
        try {
            container.jsonVault.createFile("doc.json", """{"v":1}""")
            SwingUtilities.invokeAndWait {
                session.currentFile = "doc.json"
                session.editor.setText("""{"v":1,"local":true}""", recordUndo = false)
            }
            val pending = VaultConflictState("doc.json", session.editor.text, """{"v":1}""", deleted = false)
            session.vaultConflict = pending
            val fixed = 1_700_000_004_000L
            SwingUtilities.invokeAndWait {
                assertTrue(applyJsonVaultConflictSaveCopy(container, session, pending, null, fixed))
            }
            assertNull(session.vaultConflict)
            assertTrue(container.jsonVault.read("doc.local-$fixed.json").contains("local"))
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun quickNoteOverlayReloadUsesDiskBody() {
        val productRoot = createTempDirectory("mootool-qn-conflict-overlay-")
        val container = appContainer(productRoot)
        val vault = container.noteVault()
        val session = QuickNoteSession()
        try {
            val path = "note.md"
            vault.write(path, "disk\n")
            SwingUtilities.invokeAndWait {
                session.currentFile = path
                session.editor.setText("local\n", recordUndo = false)
            }
            val pending = VaultConflictState(path, "local\n", "disk\n", deleted = false)
            session.vaultConflict = pending
            SwingUtilities.invokeAndWait {
                applyQuickNoteVaultConflictReload(container, session, vault, pending)
            }
            assertNull(session.vaultConflict)
            assertEquals("disk\n", session.savedText)
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }

    private fun appContainer(productRoot: java.nio.file.Path): AppContainer {
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settings = SettingsRepository(directories).also { it.load() }
        val database = AppDatabase(directories)
        return AppContainer(
            directories = directories,
            settingsRepository = settings,
            database = database,
            history = HistoryRepository(database),
            migrationRows = LegacyMigrationRowRepository(database),
            sessions = SessionStore(database),
        )
    }
}

package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.features.vault.applyJsonVaultConflictKeep
import com.rememberber.mootool.next.compose.features.vault.applyJsonVaultConflictReload
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

/** 对齐 [JsonScreen] `VaultConflictDialog` `onReload` / `onKeep`。 */
class JsonVaultConflictReloadKeepFlowTest {
    @Test
    fun reloadReplacesEditorWithDiskAndClearsConflict() {
        val fixture = openFixture("open.json", """{"v":1}""")
        val container = fixture.container
        val session = fixture.session
        try {
            val local = """{"v":1,"local":true}"""
            val disk = """{"v":9}"""
            container.jsonVault.write("open.json", disk)
            SwingUtilities.invokeAndWait { session.editor.setText(local, recordUndo = false) }
            session.pathResult = """{"stale":true}"""
            val pending = VaultConflictState("open.json", local, disk, deleted = false)
            session.vaultConflict = pending

            SwingUtilities.invokeAndWait { applyJsonVaultConflictReload(container, session, pending) }
            SwingUtilities.invokeAndWait { }

            assertNull(session.vaultConflict)
            assertEquals(disk, session.editor.text)
            assertEquals(disk, session.savedText)
            assertEquals("open.json", session.currentFile)
            assertEquals("", session.pathResult)
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    @Test
    fun reloadWhenDeletedClearsCurrentFileAndKeepsEditorText() {
        val fixture = openFixture("gone.json", """{"v":1}""")
        val session = fixture.session
        try {
            val local = """{"v":1,"x":1}"""
            SwingUtilities.invokeAndWait { session.editor.setText(local, recordUndo = false) }
            val pending = VaultConflictState("gone.json", local, diskText = null, deleted = true)
            session.vaultConflict = pending

            SwingUtilities.invokeAndWait { applyJsonVaultConflictReload(fixture.container, session, pending) }
            SwingUtilities.invokeAndWait { }

            assertNull(session.vaultConflict)
            assertEquals("", session.currentFile)
            assertEquals(local, session.editor.text)
            assertEquals(local, session.savedText)
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    @Test
    fun keepDismissesConflictWithoutChangingEditor() {
        val fixture = openFixture("open.json", """{"v":1}""")
        val session = fixture.session
        try {
            val local = """{"v":1,"keep":true}"""
            SwingUtilities.invokeAndWait { session.editor.setText(local, recordUndo = false) }
            session.vaultConflict = VaultConflictState("open.json", local, """{"v":2}""", deleted = false)

            applyJsonVaultConflictKeep(session)

            assertNull(session.vaultConflict)
            assertEquals(local, session.editor.text)
            assertEquals("open.json", session.currentFile)
        } finally {
            fixture.root.toFile().deleteRecursively()
        }
    }

    private data class Fixture(val container: AppContainer, val session: JsonSession, val root: java.nio.file.Path)

    private fun openFixture(path: String, content: String): Fixture {
        val productRoot = createTempDirectory("mootool-json-conflict-reload-")
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
            SwingUtilities.invokeAndWait { editor.setText(content, recordUndo = false) }
        }
        return Fixture(container, session, productRoot)
    }
}

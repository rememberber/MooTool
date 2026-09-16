package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.domain.VaultRevisionMonitor
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 与 [QuickNoteScreen] `VaultRevisionMonitor(ignoreAttachments = true)` 分支一致。 */
class QuickNoteVaultMonitorConflictTest {
    @Test
    fun pollerSurfacesConflictForDirtyOpenNote() {
        val productRoot = createTempDirectory("mootool-note-monitor-conflict-")
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
        val vault = container.noteVault()
        val path = "daily.md"
        val body = "original body\n"
        vault.write(path, "---\ntitle: Test\n---\n\n$body")
        val session = QuickNoteSession().apply {
            currentFile = path
            savedText = body
            editor.setText("edited locally\n", recordUndo = false)
        }
        val latch = CountDownLatch(1)
        var conflict: VaultConflictState? = null
        val monitor = VaultRevisionMonitor(
            root = vault.root(),
            ignoreAttachments = true,
            intervalMs = 60,
        ) { paths ->
            SwingUtilities.invokeLater {
                quickNoteApplyExternalChange(container, session, vault, paths) { conflict = it }
                latch.countDown()
            }
        }
        monitor.start()
        monitor.rebaseline()
        try {
            Thread.sleep(90)
            vault.write(path, "---\ntitle: Test\n---\n\nremote on disk\n")
            assertTrue(latch.await(3, TimeUnit.SECONDS), "monitor did not deliver paths")
            val pending = assertNotNull(conflict)
            assertEquals(path, pending.relativePath)
            assertEquals("edited locally\n", pending.editorText)
            assertTrue(pending.diskText!!.contains("remote"))
        } finally {
            monitor.close()
            productRoot.toFile().deleteRecursively()
        }
    }
}

package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.features.quicknote.quickNoteApplyExternalChange
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

/** F01 §A：`sample-external.md` 脏编辑 + 磁盘外部改写 → 冲突叠层 Presentation 就绪。 */
class QuickNoteVaultConflictProductEvidenceFlowTest {
    @Test
    fun evidenceScriptSampleExternalMdSurfacesExternalConflict() {
        val productRoot = createTempDirectory("mootool-qn-evidence-flow-")
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
        val path = VaultConflictProductEvidencePresentation.EVIDENCE_QUICKNOTE_RELATIVE_PATH
        vault.write(path, VaultConflictProductEvidencePresentation.evidenceQuickNoteInitialBody)
        val savedBody = "随手记外部冲突走查：打开后编辑不保存，再改写磁盘文件。"
        val dirtyBody = "dirty editor buffer\n"
        val session = QuickNoteSession().apply {
            currentFile = path
            savedText = savedBody
            editor.setText(dirtyBody, recordUndo = false)
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
            vault.write(
                path,
                "---\ntitle: Vault external conflict sample\nsyntax: text/markdown\n---\n\n" +
                    VaultConflictProductEvidencePresentation.EVIDENCE_QUICKNOTE_DISK_REWRITE,
            )
            assertTrue(latch.await(3, TimeUnit.SECONDS), "monitor did not deliver paths")
            val pending = assertNotNull(conflict)
            assertEquals(path, pending.relativePath)
            assertTrue(VaultConflictProductEvidencePresentation.matchesEvidenceWalkthroughPath(pending.relativePath))
            assertTrue(VaultConflictPresentation.showReloadAction(pending.deleted))
            assertEquals(dirtyBody, pending.editorText)
            assertTrue(pending.diskText!!.contains("disk rewrite"))
        } finally {
            monitor.close()
            productRoot.toFile().deleteRecursively()
        }
    }
}

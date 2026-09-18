package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.features.json.jsonVaultApplyExternalChange
import com.rememberber.mootool.next.compose.sessions.JsonSession
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

/** §A：`sample.json` 打开脏编辑 + 磁盘外部改写 → 冲突叠层 Presentation 就绪。 */
class VaultConflictProductEvidenceFlowTest {
    @Test
    fun evidenceScriptSampleJsonSurfacesExternalConflict() {
        val productRoot = createTempDirectory("mootool-vault-evidence-flow-")
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
        val path = VaultConflictProductEvidencePresentation.EVIDENCE_JSON_RELATIVE_PATH
        container.jsonVault.createFile(path, VaultConflictProductEvidencePresentation.evidenceJsonInitialBody)
        val session = JsonSession().apply {
            currentFile = path
            savedText = VaultConflictProductEvidencePresentation.evidenceJsonInitialBody
            editor.setText(
                VaultConflictProductEvidencePresentation.evidenceJsonInitialBody.replace(
                    "open in app then edit without saving",
                    "dirty editor buffer",
                ),
                recordUndo = false,
            )
        }
        val latch = CountDownLatch(1)
        var conflict: VaultConflictState? = null
        val monitor = VaultRevisionMonitor(
            root = container.jsonVault.root(),
            ignoreAttachments = false,
            intervalMs = 60,
        ) { paths ->
            SwingUtilities.invokeLater {
                jsonVaultApplyExternalChange(container, session, paths) { conflict = it }
                latch.countDown()
            }
        }
        monitor.start()
        monitor.rebaseline()
        try {
            Thread.sleep(90)
            container.jsonVault.write(path, VaultConflictProductEvidencePresentation.EVIDENCE_JSON_DISK_REWRITE)
            assertTrue(latch.await(3, TimeUnit.SECONDS), "monitor did not deliver paths")
            val pending = assertNotNull(conflict)
            assertEquals(path, pending.relativePath)
            assertTrue(VaultConflictProductEvidencePresentation.matchesEvidenceWalkthroughPath(pending.relativePath))
            assertTrue(VaultConflictPresentation.showReloadAction(pending.deleted))
            assertEquals("vault.conflict.hint", VaultConflictPresentation.hintMessageKey(pending.deleted))
            assertTrue(pending.editorText.contains("dirty editor buffer"))
            assertEquals(
                VaultConflictProductEvidencePresentation.EVIDENCE_JSON_DISK_REWRITE,
                pending.diskText?.trim(),
            )
        } finally {
            monitor.close()
            productRoot.toFile().deleteRecursively()
        }
    }
}

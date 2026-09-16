package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.domain.VaultRevisionMonitor
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

/** `VaultRevisionMonitor` 回调 → `jsonVaultApplyExternalChange`（与 [JsonScreen] 监视器分支一致）。 */
class JsonVaultMonitorConflictTest {
    @Test
    fun pollerSurfacesConflictForDirtyOpenFile() {
        val productRoot = createTempDirectory("mootool-json-monitor-conflict-")
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
        val path = "open.json"
        container.jsonVault.createFile(path, """{"v":1}""")
        val session = JsonSession().apply {
            currentFile = path
            savedText = """{"v":1}"""
            editor.setText("""{"v":1,"local":true}""", recordUndo = false)
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
            container.jsonVault.write(path, """{"v":9}""")
            assertTrue(latch.await(3, TimeUnit.SECONDS), "monitor did not deliver paths")
            val pending = assertNotNull(conflict)
            assertEquals(path, pending.relativePath)
            assertTrue(pending.editorText.contains("local"))
            assertTrue(pending.diskText!!.contains("\"v\":9"))
        } finally {
            monitor.close()
            productRoot.toFile().deleteRecursively()
        }
    }
}

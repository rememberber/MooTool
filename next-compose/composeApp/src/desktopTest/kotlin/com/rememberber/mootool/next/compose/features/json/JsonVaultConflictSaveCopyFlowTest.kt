package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.features.vault.applyJsonVaultConflictSaveCopy
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
import kotlin.test.assertTrue

/** 对齐 [JsonScreen] `onSaveCopy`：旁路副本 + 编辑器回到磁盘版本。 */
class JsonVaultConflictSaveCopyFlowTest {
    @Test
    fun saveCopyWritesSiblingAndReloadsDiskIntoEditor() {
        val productRoot = createTempDirectory("mootool-json-conflict-savecopy-flow-")
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
        val onDisk = """{"v":2}"""
        val editorText = """{"v":2,"local":true}"""
        container.jsonVault.createFile(path, onDisk)
        val session = JsonSession().apply {
            currentFile = path
            savedText = """{"v":1}"""
            SwingUtilities.invokeAndWait {
                editor.setText(editorText, recordUndo = false)
            }
        }
        val pending = VaultConflictState(path, editorText, onDisk, deleted = false)
        val fixedTime = 1_700_000_001_000L
        try {
            SwingUtilities.invokeAndWait {
                assertTrue(applyJsonVaultConflictSaveCopy(container, session, pending, null, fixedTime))
            }
            SwingUtilities.invokeAndWait { }
            val copy = "open.local-$fixedTime.json"
            assertEquals(onDisk, session.savedText)
            assertEquals(path, session.currentFile)
            assertEquals(editorText, container.jsonVault.read(copy))
            assertEquals(onDisk, container.jsonVault.read(path))
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun externalDeleteWhileDirtyYieldsDeletedConflict() {
        val productRoot = createTempDirectory("mootool-json-conflict-deleted-")
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
        val path = "gone.json"
        container.jsonVault.createFile(path, """{"v":1}""")
        val session = JsonSession().apply {
            currentFile = path
            savedText = """{"v":1}"""
            SwingUtilities.invokeAndWait {
                editor.setText("""{"v":1,"x":1}""", recordUndo = false)
            }
        }
        container.jsonVault.delete(path)
        var conflict: VaultConflictState? = null
        SwingUtilities.invokeAndWait {
            jsonVaultApplyExternalChange(container, session, listOf(path)) { conflict = it }
        }
        val pending = conflict!!
        assertTrue(pending.deleted)
        assertEquals(path, pending.relativePath)
        productRoot.toFile().deleteRecursively()
    }
}

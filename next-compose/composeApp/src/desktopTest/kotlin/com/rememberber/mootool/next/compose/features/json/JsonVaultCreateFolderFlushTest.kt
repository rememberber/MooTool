package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

class JsonVaultCreateFolderFlushTest {
    @Test
    fun folderCreateAbortsWhenFlushConflict() {
        val productRoot = createTempDirectory("mootool-json-folder-flush-")
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
        container.jsonVault.createFile("a.json", """{"saved":true}""")
        val session = JsonSession().apply {
            currentFile = "a.json"
            savedText = """{"saved":true}"""
            editor.setText("""{"dirty":true}""", recordUndo = false)
        }
        container.jsonVault.write("a.json", """{"disk":true}""")
        val thrown = runCatching {
            jsonVaultRequireFlushDirty(container, session, monitor = null) { }
        }.exceptionOrNull()
        assertTrue(thrown is IllegalStateException)
        assertTrue(session.notice.isNotBlank())
    }
}

package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.ai.AiDataAccess
import com.rememberber.mootool.next.compose.ai.AiDataAccessRequest
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VaultPathAiAccessTest {
    @Test
    fun changingVaultPathRevokesAiDataAccess() {
        val productRoot = createTempDirectory("mootool-vault-ai-revoke-")
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
        container.aiIntegration.setDataAccess(AiDataAccessRequest(notes = true, json = true))
        assertEquals(container.noteVault().root().toRealPath().toString(), container.aiIntegration.getDataAccess().notes)
        container.updateSettings { current ->
            current.copy(vault = current.vault.copy(jsonPath = productRoot.resolve("other-json-vault").toString()))
        }
        val access = container.aiIntegration.getDataAccess()
        assertNull(access.notes)
        assertNull(access.json)
        assertEquals(AiDataAccess(), access)
    }

    @Test
    fun changingDataDirectoryRevokesAiDataAccess() {
        val productRoot = createTempDirectory("mootool-data-dir-ai-")
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
        container.aiIntegration.setDataAccess(AiDataAccessRequest(notes = true, json = false))
        container.updateSettings { current ->
            current.copy(data = current.data.copy(directory = productRoot.resolve("alt-data").toString()))
        }
        assertNull(container.aiIntegration.getDataAccess().notes)
    }
}

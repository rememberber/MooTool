package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class VaultTreeRefreshNotifyTest {
    @Test
    fun notifyIncrementsVaultTreeRefreshTicks() {
        val productRoot = createTempDirectory("mootool-vault-notify-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settingsRepo = SettingsRepository(directories).also { it.load() }
        val db = AppDatabase(directories)
        val container = AppContainer(
            directories,
            settingsRepo,
            db,
            HistoryRepository(db),
            LegacyMigrationRowRepository(db),
            SessionStore(db),
        )
        assertEquals(0L, container.jsonVaultAutoPullTick.value)
        assertEquals(0L, container.quickNoteVaultAutoPullTick.value)
        container.notifyJsonVaultTreeChanged()
        container.notifyQuickNoteVaultTreeChanged()
        assertEquals(1L, container.jsonVaultAutoPullTick.value)
        assertEquals(1L, container.quickNoteVaultAutoPullTick.value)
    }

    @Test
    fun reloadToolSessionsFromStoreNotifiesVaultTrees() {
        val productRoot = createTempDirectory("mootool-vault-reload-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settingsRepo = SettingsRepository(directories).also { it.load() }
        val db = AppDatabase(directories)
        val container = AppContainer(
            directories,
            settingsRepo,
            db,
            HistoryRepository(db),
            LegacyMigrationRowRepository(db),
            SessionStore(db),
        )
        container.reloadToolSessionsFromStore()
        assertEquals(1L, container.jsonVaultAutoPullTick.value)
        assertEquals(1L, container.quickNoteVaultAutoPullTick.value)
    }
}

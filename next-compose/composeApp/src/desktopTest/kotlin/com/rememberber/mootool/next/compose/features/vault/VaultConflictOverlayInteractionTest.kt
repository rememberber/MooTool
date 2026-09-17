package com.rememberber.mootool.next.compose.features.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

/** `JsonVaultConflictOverlay` / `QuickNoteVaultConflictOverlay` Compose 回调链。 */
class VaultConflictOverlayInteractionTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun jsonOverlayKeepButtonInvokesRefresh() = runDesktopComposeUiTest(width = 720, height = 480) {
        val zh = Translator(AppLanguage.ZhCN)
        val productRoot = createTempDirectory("mootool-json-conflict-overlay-")
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
        val session = JsonSession()
        session.vaultConflict = VaultConflictState(
            relativePath = "doc.json",
            editorText = """{"local":true}""",
            diskText = """{"disk":true}""",
            deleted = false,
        )
        var refreshCount = 0
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Box(Modifier.fillMaxSize().background(colors.workspace)) {
                    JsonVaultConflictOverlay(
                        container = container,
                        session = session,
                        monitor = null,
                        onRefresh = { refreshCount++ },
                    )
                }
            }
        }
        onNodeWithContentDescription(zh.t("vault.conflict.keep")).performClick()
        waitForIdle()
        assertEquals(1, refreshCount)
        productRoot.toFile().deleteRecursively()
    }
}

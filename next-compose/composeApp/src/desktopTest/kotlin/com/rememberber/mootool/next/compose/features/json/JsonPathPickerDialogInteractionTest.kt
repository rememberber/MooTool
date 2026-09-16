package com.rememberber.mootool.next.compose.features.json

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
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
import kotlin.test.assertFalse

class JsonPathPickerDialogInteractionTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun useButtonAppliesPathWithoutQuery() = runDesktopComposeUiTest(width = 800, height = 520) {
        val zh = Translator(AppLanguage.ZhCN)
        val productRoot = createTempDirectory("mootool-json-path-picker-")
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
        session.editor.setText("""{"store":{"books":[{"title":"One"}]}}""", recordUndo = false)
        session.pathPickerOpen = true
        session.pathPickerSelection = "$.store.books[0].title"
        session.pathResult = """["stale"]"""
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Box(Modifier.fillMaxSize().background(colors.workspace)) {
                    JsonPathPickerDialog(container, session) {}
                }
            }
        }
        waitForIdle()
        onNodeWithContentDescription(zh.t("json.pathPicker.use")).performClick()
        assertFalse(session.pathPickerOpen)
        assertEquals("$.store.books[0].title", session.jsonPath)
        assertEquals("", session.pathResult)
        assertEquals(zh.t("json.notice.pathApplied"), session.notice)
        productRoot.toFile().deleteRecursively()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun doubleTapRowAppliesPathWithoutQuery() = runDesktopComposeUiTest(width = 800, height = 520) {
        val zh = Translator(AppLanguage.ZhCN)
        val productRoot = createTempDirectory("mootool-json-path-picker-dbl-")
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
        session.editor.setText("""{"store":{"books":[{"title":"One"}]}}""", recordUndo = false)
        session.pathPickerOpen = true
        session.pathResult = """["stale"]"""
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Box(Modifier.fillMaxSize().background(colors.workspace)) {
                    JsonPathPickerDialog(container, session) {}
                }
            }
        }
        waitForIdle()
        onNodeWithContentDescription("$.store.books[0].title").performTouchInput { doubleClick() }
        assertFalse(session.pathPickerOpen)
        assertEquals("$.store.books[0].title", session.jsonPath)
        assertEquals("", session.pathResult)
        assertEquals(zh.t("json.notice.pathApplied"), session.notice)
        productRoot.toFile().deleteRecursively()
    }
}

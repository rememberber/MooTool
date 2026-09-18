package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.JsonTranslator
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonInspectorDuplicatePathTest {
    private val translator = JsonTranslator { key, _ -> key }

    @Test
    fun duplicatePathActivation_syncsJsonPathAndPreviewBeforeCopy() {
        val session = JsonSession()
        val input = """{"items":[{"id":1},{"id":1}]}"""
        val path = "$.items[1].id"
        val preview = jsonPathNodePreview(input, path, translator, "")
        val productRoot = createTempDirectory("mootool-json-dup-path-")
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
        var changed = false
        assertTrue(
            jsonInspectorDuplicatePathClick(
                path = path,
                inspectorInput = input,
                session = session,
                container = container,
                pathAppliedNotice = "applied",
                translator = translator,
                onChanged = { changed = true },
            ),
        )
        assertTrue(changed)
        assertEquals(path, session.jsonPath)
        assertEquals(preview, session.pathResult)
        assertEquals("applied", session.notice)
    }
}

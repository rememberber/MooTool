package com.rememberber.mootool.next.compose.sessions

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.SessionStore
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionManagerReloadTest {
    @Test
    fun reloadDiscardsCachedJsonSessionAndReadsStore() {
        val directories = AppPaths.resolve(createTempDirectory("session-reload-").toString()).also { it.ensureCreated() }
        val database = AppDatabase(directories)
        val store = SessionStore(database)
        val manager = SessionManager(store)
        val json = manager.jsonSession()
        val marker = """{"fromStore":true}"""
        json.editor.setText(marker, recordUndo = false)
        manager.persistJson()
        json.editor.setText("stale-in-memory", recordUndo = false)
        manager.reloadAllToolSessionsFromStore()
        assertEquals(json, manager.jsonSession())
        assertEquals(marker, json.editor.text)
        assertTrue(json.editor.text.contains("fromStore"))
        database.close()
    }

    @Test
    fun reloadIncrementsSessionGeneration() {
        val directories = AppPaths.resolve(createTempDirectory("session-gen-").toString()).also { it.ensureCreated() }
        val database = AppDatabase(directories)
        val store = SessionStore(database)
        val manager = SessionManager(store)
        val before = manager.sessionGeneration.value
        manager.reloadAllToolSessionsFromStore()
        assertEquals(before + 1, manager.sessionGeneration.value)
        database.close()
    }

    @Test
    fun reloadResetsJsonVaultScopedOverlays() {
        val directories = AppPaths.resolve(createTempDirectory("session-overlay-").toString()).also { it.ensureCreated() }
        val database = AppDatabase(directories)
        val store = SessionStore(database)
        val manager = SessionManager(store)
        val json = manager.jsonSession()
        json.gitDialogOpen = true
        json.pathPickerOpen = true
        manager.persistJson()
        manager.reloadAllToolSessionsFromStore()
        assertFalse(json.gitDialogOpen)
        assertFalse(json.pathPickerOpen)
        database.close()
    }
}

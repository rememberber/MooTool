package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorSettingsLiveApplyTest {
    @Test
    fun jsonSoftWrapFollowsGlobalSettingsWhenChanged() {
        assertEquals(false, EditorSettingsLiveApply.jsonSoftWrapFromSettings(sessionWrap = true, settingsSoftWrap = false))
        assertEquals(true, EditorSettingsLiveApply.jsonSoftWrapFromSettings(sessionWrap = false, settingsSoftWrap = true))
    }

    @Test
    fun jsonSoftWrapUnchangedWhenAlreadyAligned() {
        assertEquals(true, EditorSettingsLiveApply.jsonSoftWrapFromSettings(sessionWrap = true, settingsSoftWrap = true))
        assertEquals(false, EditorSettingsLiveApply.jsonSoftWrapFromSettings(sessionWrap = false, settingsSoftWrap = false))
    }

    @Test
    fun httpEditorWrapFollowsSettings() {
        assertTrue(EditorSettingsLiveApply.httpEditorWrap(settingsSoftWrap = true))
        assertFalse(EditorSettingsLiveApply.httpEditorWrap(settingsSoftWrap = false))
    }

    @Test
    fun runtimeEditorWrapMatchesElectronFixedOff() {
        assertFalse(EditorSettingsLiveApply.runtimeEditorWrap())
    }

    @Test
    fun newQuickNoteLineWrapUsesGlobalSettings() {
        assertTrue(EditorSettingsLiveApply.newQuickNoteLineWrap(settingsSoftWrap = true))
        assertFalse(EditorSettingsLiveApply.newQuickNoteLineWrap(settingsSoftWrap = false))
    }
}

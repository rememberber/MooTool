package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

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
}

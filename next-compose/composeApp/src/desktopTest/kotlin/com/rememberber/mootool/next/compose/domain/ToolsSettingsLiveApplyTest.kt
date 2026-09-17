package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ToolsSettingsLiveApplyTest {
    @Test
    fun qrCodeSize_clampsToElectronBounds() {
        assertEquals(120, ToolsSettingsLiveApply.qrCodeSize(12))
        assertEquals(2_000, ToolsSettingsLiveApply.qrCodeSize(9_999))
        assertEquals(360, ToolsSettingsLiveApply.qrCodeSize(360))
    }

    @Test
    fun qrErrorCorrection_normalizesLevel() {
        assertEquals("M", ToolsSettingsLiveApply.qrErrorCorrection("m"))
        assertEquals("H", ToolsSettingsLiveApply.qrErrorCorrection("H"))
        assertEquals("M", ToolsSettingsLiveApply.qrErrorCorrection("invalid"))
    }

    @Test
    fun randomStringLength_clampsToElectronBounds() {
        assertEquals(1, ToolsSettingsLiveApply.randomStringLength(0))
        assertEquals(4_096, ToolsSettingsLiveApply.randomStringLength(99_999))
        assertEquals(32, ToolsSettingsLiveApply.randomStringLength(32))
    }
}

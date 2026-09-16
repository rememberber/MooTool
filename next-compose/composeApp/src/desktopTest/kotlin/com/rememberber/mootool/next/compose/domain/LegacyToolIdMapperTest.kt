package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyToolIdMapperTest {
    @Test
    fun mapsJavaFuncTypesToComposeToolIds() {
        assertEquals(ToolId.Json.id, LegacyToolIdMapper.normalize("json"))
        assertEquals(ToolId.QrCode.id, LegacyToolIdMapper.normalize("qrCode"))
        assertEquals(ToolId.QuickNote.id, LegacyToolIdMapper.normalize("quickNote"))
        assertEquals(ToolId.Regex.id, LegacyToolIdMapper.normalize("regex"))
        assertEquals(ToolId.Java.id, LegacyToolIdMapper.normalize("JavaConsole"))
    }
}

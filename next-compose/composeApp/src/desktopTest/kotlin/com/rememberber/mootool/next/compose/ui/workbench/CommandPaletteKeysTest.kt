package com.rememberber.mootool.next.compose.ui.workbench

import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandPaletteKeysTest {
    @Test
    fun arrowsClampInsideResults() {
        assertEquals(0, nextCommandIndex(0, 0, down = true))
        assertEquals(1, nextCommandIndex(0, 3, down = true))
        assertEquals(2, nextCommandIndex(2, 3, down = true))
        assertEquals(1, nextCommandIndex(2, 3, down = false))
        assertEquals(0, nextCommandIndex(0, 3, down = false))
        assertEquals(2, nextCommandIndex(4, 3, stay = true))
    }

    @Test
    fun tabFocusTransition_movesBetweenSearchAndSelectedResult() {
        assertEquals(
            CommandPaletteFocusTarget.Result,
            commandPaletteTabFocusTransition(shift = false, fromResultRow = false, resultCount = 3),
        )
        assertEquals(
            CommandPaletteFocusTarget.Search,
            commandPaletteTabFocusTransition(shift = true, fromResultRow = true, resultCount = 3),
        )
        assertEquals(null, commandPaletteTabFocusTransition(shift = false, fromResultRow = false, resultCount = 0))
        assertEquals(null, commandPaletteTabFocusTransition(shift = true, fromResultRow = false, resultCount = 2))
        assertEquals(null, commandPaletteTabFocusTransition(shift = false, fromResultRow = true, resultCount = 2))
    }

    @Test
    fun searchCopyFollowsElectronKeys() {
        val zh = Translator(AppLanguage.ZhCN)
        assertEquals("搜索工具", zh.t("app.search.title"))
        assertEquals("搜索功能…", zh.t("app.search.placeholder"))
        assertEquals("关闭搜索", zh.t("app.search.close"))
        val en = Translator(AppLanguage.EnUS)
        assertEquals("Search tools", en.t("app.search.title"))
        val ja = Translator(AppLanguage.JaJP)
        assertEquals("ツールを検索", ja.t("app.search.title"))
    }
}

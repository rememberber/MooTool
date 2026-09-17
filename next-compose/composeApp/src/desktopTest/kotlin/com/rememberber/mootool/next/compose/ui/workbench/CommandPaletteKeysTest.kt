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
    fun tabFocusTransition_movesSearchCloseAndResult() {
        assertEquals(
            CommandPaletteFocusTarget.Close,
            commandPaletteTabFocusTransition(
                shift = false,
                from = CommandPaletteFocusTarget.Search,
                resultCount = 3,
            ),
        )
        assertEquals(
            CommandPaletteFocusTarget.Result,
            commandPaletteTabFocusTransition(
                shift = false,
                from = CommandPaletteFocusTarget.Close,
                resultCount = 3,
            ),
        )
        assertEquals(
            CommandPaletteFocusTarget.Close,
            commandPaletteTabFocusTransition(
                shift = true,
                from = CommandPaletteFocusTarget.Result,
                resultCount = 3,
            ),
        )
        assertEquals(
            CommandPaletteFocusTarget.Search,
            commandPaletteTabFocusTransition(
                shift = true,
                from = CommandPaletteFocusTarget.Close,
                resultCount = 3,
            ),
        )
        assertEquals(
            null,
            commandPaletteTabFocusTransition(
                shift = false,
                from = CommandPaletteFocusTarget.Close,
                resultCount = 0,
            ),
        )
        assertEquals(
            null,
            commandPaletteTabFocusTransition(
                shift = true,
                from = CommandPaletteFocusTarget.Search,
                resultCount = 2,
            ),
        )
        assertEquals(
            null,
            commandPaletteTabFocusTransition(
                shift = false,
                from = CommandPaletteFocusTarget.Result,
                resultCount = 2,
            ),
        )
    }

    @Test
    fun tabFocusWalkthrough_searchCloseResultRoundTrip() {
        assertEquals(
            listOf(
                CommandPaletteFocusTarget.Close,
                CommandPaletteFocusTarget.Result,
            ),
            commandPaletteTabForwardWalkthrough(resultCount = 5),
        )
        assertEquals(
            listOf(
                CommandPaletteFocusTarget.Close,
                CommandPaletteFocusTarget.Search,
            ),
            commandPaletteTabBackwardWalkthrough(resultCount = 5),
        )
        assertEquals(
            listOf(CommandPaletteFocusTarget.Close),
            commandPaletteTabForwardWalkthrough(resultCount = 0),
        )
        assertEquals(emptyList(), commandPaletteTabBackwardWalkthrough(resultCount = 0))
    }

    @Test
    fun searchCopyFollowsElectronKeys() {
        val zh = Translator(AppLanguage.ZhCN)
        assertEquals("搜索工具", zh.t("app.search.title"))
        assertEquals("搜索功能…", zh.t("app.search.placeholder"))
        assertEquals("关闭搜索", zh.t("app.search.close"))
        assertEquals("回车确认 · Esc 或右键取消", zh.t("image.captureOverlayKeys"))
        val en = Translator(AppLanguage.EnUS)
        assertEquals("Search tools", en.t("app.search.title"))
        val ja = Translator(AppLanguage.JaJP)
        assertEquals("ツールを検索", ja.t("app.search.title"))
    }
}

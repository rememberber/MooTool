package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

class CloseAskCopyTest {
    @Test
    fun closeAskCopyMatchesElectronMessageBox() {
        val zh = Translator(AppLanguage.ZhCN)
        assertEquals("关闭窗口后如何处理 MooTool？", zh.t("app.close.askBody"))
        assertEquals("隐藏到后台", zh.t("app.close.hide"))
        assertEquals("退出 MooTool", zh.t("app.close.quit"))
        assertEquals("取消", zh.t("app.close.cancel"))
        val en = Translator(AppLanguage.EnUS)
        assertEquals("What should MooTool do when this window closes?", en.t("app.close.askBody"))
        assertEquals("Hide", en.t("app.close.hide"))
        assertEquals("Quit MooTool", en.t("app.close.quit"))
        assertEquals("Cancel", en.t("app.close.cancel"))
        val ja = Translator(AppLanguage.JaJP)
        assertEquals("ウィンドウを閉じるときの動作を選択してください。", ja.t("app.close.askBody"))
        assertEquals("バックグラウンドに隠す", ja.t("app.close.hide"))
        assertEquals("MooTool を終了", ja.t("app.close.quit"))
        assertEquals("キャンセル", ja.t("app.close.cancel"))
    }
}

package com.rememberber.mootool.next.compose.i18n

import com.rememberber.mootool.next.compose.model.AppLanguage
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotEquals

class HttpTimeoutHintI18nTest {
    @Test
    fun timeoutHint_resolves_for_zh_and_en() {
        val zh = Translator(AppLanguage.ZhCN).t("http.timeoutHint")
        val en = Translator(AppLanguage.EnUS).t("http.timeoutHint")
        assertContains(zh, "1000")
        assertContains(en, "1000")
        assertNotEquals(zh, en)
    }
}

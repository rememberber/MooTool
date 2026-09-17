package com.rememberber.mootool.next.compose.features.settings

import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsCategoryI18nTest {
    @Test
    fun categoryLabelsMatchElectronZhAndEn() {
        val zh = Translator(AppLanguage.ZhCN)
        val en = Translator(AppLanguage.EnUS)
        assertEquals("常规", zh.t(SettingsNavCategory.General.categoryLabelKey()))
        assertEquals("布局与习惯", zh.t(SettingsNavCategory.Layout.categoryLabelKey()))
        assertEquals("关于与更新", zh.t(SettingsNavCategory.About.categoryLabelKey()))
        assertEquals("General", en.t(SettingsNavCategory.General.categoryLabelKey()))
        assertEquals("Layout & Habits", en.t(SettingsNavCategory.Layout.categoryLabelKey()))
        assertEquals("About & Updates", en.t(SettingsNavCategory.About.categoryLabelKey()))
    }
}

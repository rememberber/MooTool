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

    @Test
    fun categoryLabelsMatchElectronJa() {
        val ja = Translator(AppLanguage.JaJP)
        assertEquals("一般", ja.t(SettingsNavCategory.General.categoryLabelKey()))
        assertEquals("レイアウトと操作", ja.t(SettingsNavCategory.Layout.categoryLabelKey()))
        assertEquals("情報と更新", ja.t(SettingsNavCategory.About.categoryLabelKey()))
        assertEquals("AI 連携", ja.t(SettingsNavCategory.Ai.categoryLabelKey()))
    }
}

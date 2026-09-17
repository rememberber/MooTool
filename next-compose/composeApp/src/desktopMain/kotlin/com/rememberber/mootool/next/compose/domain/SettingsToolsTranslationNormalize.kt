package com.rememberber.mootool.next.compose.domain

/** 设置页工具默认值 · 翻译语言对失焦提交（对齐 Electron `TextSetting` + `normalizeTranslationLanguagePair`）。 */
object SettingsToolsTranslationNormalize {
    fun commitLanguagePair(sourceDraft: String, targetDraft: String): Pair<String, String> {
        val sourceRaw = sourceDraft.trim().ifBlank { "auto" }
        val targetRaw = targetDraft.trim().ifBlank { "zh-CN" }
        return TranslationEngine.normalizeLanguagePair(sourceRaw, targetRaw)
    }
}

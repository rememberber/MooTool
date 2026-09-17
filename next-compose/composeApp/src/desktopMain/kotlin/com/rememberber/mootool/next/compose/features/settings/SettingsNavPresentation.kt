package com.rememberber.mootool.next.compose.features.settings

/** 设置侧栏/内容区 chrome（对齐 Electron `SettingsWindow` 分类导航）。 */
internal object SettingsNavPresentation {
    fun fromStorageId(id: String): SettingsNavCategory = settingsNavCategoryFromStorageId(id)

    fun step(current: SettingsNavCategory, delta: Int): SettingsNavCategory =
        settingsNavCategoryStep(current, delta)

    fun contentHeaderIcon(category: SettingsNavCategory): String = category.navIcon()

    fun contentHeaderLabelKey(category: SettingsNavCategory): String = category.categoryLabelKey()
}

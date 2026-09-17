package com.rememberber.mootool.next.compose.app

import java.util.Locale

/** 命令盘深链设置分类（对齐 Electron `openSettings(category)` / 侧栏 MCP·Vault Git 入口）。 */
data class CommandSettingsTarget(
    val categoryId: String,
    val labelKey: String,
    val navIcon: String,
    val keywords: List<String>,
)

object CommandSearchCatalog {
    val targets: List<CommandSettingsTarget> = listOf(
        CommandSettingsTarget(
            categoryId = "general",
            labelKey = "settings.category.general",
            navIcon = "⚙",
            keywords = listOf("general", "language", "close", "tray", "常规", "语言", "关闭", "托盘"),
        ),
        CommandSettingsTarget(
            categoryId = "ai",
            labelKey = "settings.category.ai",
            navIcon = "⚡",
            keywords = listOf("ai", "mcp", "cursor", "codex", "claude", "skill", "integration", "接入", "連携"),
        ),
        CommandSettingsTarget(
            categoryId = "vault",
            labelKey = "settings.category.vault",
            navIcon = "⌁",
            keywords = listOf(
                "vault",
                "git",
                "文档库",
                "library",
                "remote",
                "token",
                "username",
                "askpass",
                "credential",
                "checkpoint",
                "autopull",
                "merge",
                "rebase",
                "push",
                "pull",
                "冲突",
            ),
        ),
        CommandSettingsTarget(
            categoryId = "editor",
            labelKey = "settings.category.editor",
            navIcon = "{ }",
            keywords = listOf("editor", "font", "wrap", "json", "note", "sql", "dialect", "编辑器", "字体", "换行", "方言"),
        ),
        CommandSettingsTarget(
            categoryId = "network",
            labelKey = "settings.category.network",
            navIcon = "⬡",
            keywords = listOf("network", "proxy", "http", "timeout", "代理", "网络"),
        ),
        CommandSettingsTarget(
            categoryId = "runtime",
            labelKey = "settings.category.runtime",
            navIcon = ">_",
            keywords = listOf("runtime", "java", "python", "node", "groovy", "运行环境"),
        ),
        CommandSettingsTarget(
            categoryId = "tools",
            labelKey = "settings.category.tools",
            navIcon = "☰",
            keywords = listOf(
                "tools",
                "export",
                "qrcode",
                "qr",
                "random",
                "correction",
                "default",
                "translation",
                "google",
                "bing",
                "translate",
                "工具",
                "导出",
                "默认值",
                "翻译",
                "二维码",
                "随机",
            ),
        ),
        CommandSettingsTarget(
            categoryId = "data",
            labelKey = "settings.category.data",
            navIcon = "▤",
            keywords = listOf("data", "backup", "restore", "migration", "备份", "数据", "迁移"),
        ),
        CommandSettingsTarget(
            categoryId = "appearance",
            labelKey = "settings.category.appearance",
            navIcon = "☼",
            keywords = listOf("appearance", "theme", "accent", "style", "外观", "主题", "强调色"),
        ),
        CommandSettingsTarget(
            categoryId = "layout",
            labelKey = "settings.category.layout",
            navIcon = "▦",
            keywords = listOf(
                "layout",
                "navigation",
                "sidebar",
                "compact",
                "classic",
                "separators",
                "history",
                "favorite",
                "custom",
                "hidden",
                "group",
                "收藏",
                "历史",
                "布局",
                "导航",
                "侧栏",
                "分组",
                "隐藏",
                "紧凑",
                "经典",
            ),
        ),
        CommandSettingsTarget(
            categoryId = "shortcuts",
            labelKey = "settings.category.shortcuts",
            navIcon = "⌘",
            keywords = listOf("shortcut", "hotkey", "keyboard", "快捷键"),
        ),
        CommandSettingsTarget(
            categoryId = "about",
            labelKey = "settings.category.about",
            navIcon = "ℹ",
            keywords = listOf(
                "about",
                "update",
                "version",
                "upgrade",
                "download",
                "release",
                "changelog",
                "notes",
                "check",
                "关于",
                "更新",
                "升级",
                "版本",
                "检查更新",
                "发行说明",
            ),
        ),
    )

    fun search(query: String, translate: (String) -> String): List<CommandSettingsTarget> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        val rootNeedle = needle.lowercase(Locale.ROOT)
        val localeNeedle = needle.lowercase(Locale.getDefault())
        return targets.filter { target -> matchesSearch(rootNeedle, localeNeedle, target, translate) }
    }

    internal fun matchesSearch(
        rootNeedle: String,
        localeNeedle: String,
        target: CommandSettingsTarget,
        translate: (String) -> String,
    ): Boolean {
        if (target.categoryId.lowercase(Locale.ROOT).contains(rootNeedle)) return true
        if (translate(target.labelKey).lowercase(Locale.getDefault()).contains(localeNeedle)) return true
        return target.keywords.any { keyword ->
            val key = keyword.lowercase(Locale.ROOT)
            key.contains(rootNeedle) || keyword.lowercase(Locale.getDefault()).contains(localeNeedle)
        }
    }
}

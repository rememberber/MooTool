package com.rememberber.mootool.next.compose.domain

import java.awt.GraphicsEnvironment

object SystemFonts {
    val fallbackEditorFonts = listOf(
        "ui-monospace",
        "Menlo",
        "Monaco",
        "Consolas",
        "Courier New",
        "JetBrains Mono",
        "PingFang SC",
        "Hiragino Sans GB",
        "Microsoft YaHei",
        "等线",
        "Songti SC",
        "SimSun",
        "Georgia",
        "Times New Roman",
        "SF Pro Text",
        "Helvetica Neue",
        "Arial",
        "system-ui"
    )

    fun list(current: String = ""): List<String> {
        val families = LinkedHashSet<String>()
        families.addAll(fallbackEditorFonts)
        runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.forEach { name ->
                if (name.isNotBlank()) families.add(name.trim())
            }
        }
        val selected = current.trim()
        if (selected.isNotEmpty()) families.add(selected)
        val rest = families
            .filter { it != "ui-monospace" }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
        return if (families.contains("ui-monospace")) listOf("ui-monospace") + rest else rest
    }

    fun displayName(value: String, labels: Map<String, String>, emptyLabel: String?): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return emptyLabel ?: ""
        return labels[trimmed] ?: trimmed
    }
}

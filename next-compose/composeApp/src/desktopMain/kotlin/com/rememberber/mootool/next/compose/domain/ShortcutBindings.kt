package com.rememberber.mootool.next.compose.domain

object ShortcutBindings {
    fun normalize(binding: String): String = binding.trim()

    fun conflict(first: String, second: String): Boolean {
        val left = normalize(first)
        val right = normalize(second)
        return left.isNotEmpty() && right.isNotEmpty() && left.equals(right, ignoreCase = true)
    }

    fun formatDisplay(value: String, mac: Boolean = isMac()): String {
        val meta = if (mac) "⌘" else "Ctrl"
        return normalize(value)
            .replace("CommandOrControl", meta, ignoreCase = true)
            .replace("Meta", meta, ignoreCase = true)
            .replace("Comma", ",", ignoreCase = true)
    }

    fun isMac(): Boolean = System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)
}

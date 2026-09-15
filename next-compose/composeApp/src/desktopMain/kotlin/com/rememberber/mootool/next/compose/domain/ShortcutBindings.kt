package com.rememberber.mootool.next.compose.domain

object ShortcutBindings {
    fun normalize(binding: String): String = binding.trim()

    fun conflict(first: String, second: String): Boolean {
        val left = normalize(first)
        val right = normalize(second)
        return left.isNotEmpty() && right.isNotEmpty() && left.equals(right, ignoreCase = true)
    }
}

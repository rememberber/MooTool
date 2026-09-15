package com.rememberber.mootool.next.compose.domain

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class GitIgnoreMatcher(private val patterns: List<IgnorePattern>) {
    fun ignores(relativePath: String, directory: Boolean = false): Boolean {
        val portable = relativePath.replace('\\', '/').trimStart('/')
        if (portable.isEmpty()) return false
        val candidates = if (directory) listOf(portable, "$portable/") else listOf(portable)
        return candidates.any { path -> patterns.any { it.matches(path) } }
    }

    companion object {
        fun load(root: Path): GitIgnoreMatcher {
            val file = root.resolve(".gitignore")
            if (!file.exists()) return GitIgnoreMatcher(emptyList())
            return parse(runCatching { file.readText(Charsets.UTF_8) }.getOrDefault(""))
        }

        fun parse(source: String): GitIgnoreMatcher {
            val patterns = source.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("!") }
                .map(::IgnorePattern)
                .toList()
            return GitIgnoreMatcher(patterns)
        }
    }
}

data class IgnorePattern(val raw: String) {
    private val directoryOnly = raw.endsWith("/")
    private val body = raw.trimStart('/').removeSuffix("/")
    private val anchored = raw.startsWith("/") || body.contains('/')
    private val regex: Regex = globToRegex(body)

    fun matches(path: String): Boolean {
        val portable = path.removePrefix("/").removeSuffix("/")
        if (portable.isEmpty()) return false
        if (directoryOnly && !path.endsWith("/")) {
            if (!portable.contains('/')) {
                return regex.matches(portable)
            }
        }
        if (anchored) return regex.matches(portable) || portable.startsWith("$body/")
        val name = portable.substringAfterLast('/')
        return regex.matches(name) || regex.matches(portable) || portable.split('/').any { regex.matches(it) } ||
            portable.startsWith("$body/") || portable == body
    }

    private fun globToRegex(pattern: String): Regex {
        val escaped = buildString {
            pattern.forEach { char ->
                when (char) {
                    '*' -> append(".*")
                    '?' -> append('.')
                    '.', '(', ')', '+', '|', '^', '$', '{', '}', '[', ']', '\\' -> {
                        append('\\')
                        append(char)
                    }
                    else -> append(char)
                }
            }
        }
        return Regex("^$escaped$", RegexOption.IGNORE_CASE)
    }
}

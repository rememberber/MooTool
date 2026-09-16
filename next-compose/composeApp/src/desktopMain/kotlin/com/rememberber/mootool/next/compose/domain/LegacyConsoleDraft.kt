package com.rememberber.mootool.next.compose.domain

/**
 * Java Swing tools append console output via [com.luoboduner.moo.tool.util.ConsoleUtil]:
 * blank line, timestamp line, blank line, message. Several `t_func_content` blobs use this shape.
 */
object LegacyConsoleDraft {
    private val timestampLine = Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}\s*$""")

    fun extractMessages(raw: String): List<String> =
        raw.split(Regex("\n\n+"))
            .mapNotNull { block ->
                val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
                val body = lines.filterNot { timestampLine.matches(it) }
                if (body.isEmpty()) null else body.joinToString("\n")
            }

    fun lastMessage(raw: String): String? = extractMessages(raw).lastOrNull()

    fun extractQrGenerateContent(raw: String): String? {
        for (message in extractMessages(raw).asReversed()) {
            val fromLabel = parseGenerateLabel(message)
            if (!fromLabel.isNullOrBlank()) return fromLabel
        }
        return lastMessage(raw)?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
    }

    private fun parseGenerateLabel(message: String): String? {
        val lines = message.lines()
        if (lines.isEmpty()) return null
        val head = lines.first().trim()
        val prefixes = listOf("生成:", "Generate:", "生成：")
        val matched = prefixes.firstOrNull { head.startsWith(it, ignoreCase = true) }
        if (matched != null) {
            val inline = head.substring(matched.length).trim()
            val tail = lines.drop(1).joinToString("\n").trim()
            return tail.ifBlank { inline }.ifBlank { null }
        }
        return null
    }
}

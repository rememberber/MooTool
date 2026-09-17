package com.rememberber.mootool.next.compose.domain

/**
 * Parses Java MooTool `TimeConvert` left-panel log (`t_func_content`) into compose time-convert history rows.
 */
object LegacyTimeConvertDraft {
    private val timestampToLocal = Regex(
        """^(?:时间戳|Timestamp|タイムスタンプ):\s*(\d+)\s*-->\s*(?:时间|Time|時間)\(([^)]+)\):\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val localToTimestamp = Regex(
        """^(?:时间|Time|時間)\s*\(([^)]+)\):\s*(.+?)\s*-->\s*(?:时间戳|Timestamp|タイムスタンプ):\s*(\d+)$""",
        RegexOption.IGNORE_CASE
    )

    data class ParsedEntry(
        val summary: String,
        val input: String,
        val output: String,
        val zone: String,
        val unit: String,
    )

    fun parse(content: String): List<ParsedEntry> {
        val fromConsole = LegacyConsoleDraft.extractMessages(content)
            .mapNotNull { message -> parseLine(message.trim()) }
        if (fromConsole.isNotEmpty()) return fromConsole
        return content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line -> parseLine(line) }
            .toList()
    }

    fun parseLine(line: String): ParsedEntry? {
        timestampToLocal.matchEntire(line)?.let { match ->
            val timestamp = match.groupValues[1]
            val zone = match.groupValues[2].trim()
            val localTime = match.groupValues[3].trim()
            return ParsedEntry(
                summary = line,
                input = timestamp,
                output = localTime,
                zone = zone,
                unit = unitForTimestamp(timestamp),
            )
        }
        localToTimestamp.matchEntire(line)?.let { match ->
            val zone = match.groupValues[1].trim()
            val localTime = match.groupValues[2].trim()
            val timestamp = match.groupValues[3]
            return ParsedEntry(
                summary = line,
                input = localTime,
                output = timestamp,
                zone = zone,
                unit = unitForTimestamp(timestamp),
            )
        }
        return null
    }

    fun optionsJson(zone: String, unit: String): String {
        val unitEnum = if (unit == "millisecond") TimestampUnit.Millisecond else TimestampUnit.Second
        return TimeHistoryMetadata.encode(zone, unitEnum)
    }

    private fun unitForTimestamp(timestamp: String): String =
        if (timestamp.length >= 13) "millisecond" else "second"
}

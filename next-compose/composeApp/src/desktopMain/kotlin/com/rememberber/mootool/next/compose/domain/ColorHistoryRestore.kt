package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.ColorSession

/** F22 调色板历史恢复（主/辅色、格式，不写回历史）。 */
object ColorHistoryRestore {
    sealed class Result {
        data object Ok : Result()
        data object InvalidColor : Result()
    }

    fun apply(session: ColorSession, item: HistoryRecord): Result {
        val meta = ColorHistoryMetadata.decode(item.options)
        ColorHistoryMetadata.parseFormat(meta)?.let { session.format = it }

        val pair = parseColorPair(item.output.ifBlank { item.input })
        if (pair != null) {
            session.primary = ColorEngine.parseColor(pair.first)
            session.secondary = ColorEngine.parseColor(pair.second)
            session.code = ColorEngine.formatColor(session.primary, session.format)
            session.error = ""
            session.notice = item.summary
            return Result.Ok
        }

        val hex = ColorEngine.extractHex(item.output.ifBlank { item.input })
            ?: return Result.InvalidColor
        session.primary = ColorEngine.parseColor(hex)
        session.code = ColorEngine.formatColor(session.primary, session.format)
        session.error = ""
        session.notice = item.summary
        return Result.Ok
    }

    fun parseColorPair(text: String): Pair<String, String>? {
        if (!text.contains('/')) return null
        val parts = text.split('/').map { it.trim() }
        if (parts.size != 2) return null
        val first = ColorEngine.extractHex(parts[0]) ?: return null
        val second = ColorEngine.extractHex(parts[1]) ?: return null
        return first to second
    }
}

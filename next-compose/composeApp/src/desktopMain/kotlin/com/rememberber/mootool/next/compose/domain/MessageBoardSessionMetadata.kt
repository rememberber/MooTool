package com.rememberber.mootool.next.compose.domain

/** F19 留言板会话 wire 规范化。 */
object MessageBoardSessionMetadata {
    fun normalizeTheme(wire: String): String =
        MessageBoardEngine.themeId(wire).name.lowercase()

    fun normalizeAlignment(wire: String): String =
        MessageBoardEngine.alignmentId(wire).name.lowercase()

    fun normalizeSize(wire: Int): Int = MessageBoardEngine.normalizeSize(wire)
}

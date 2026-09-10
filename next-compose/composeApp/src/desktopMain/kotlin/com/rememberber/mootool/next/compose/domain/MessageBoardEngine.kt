package com.rememberber.mootool.next.compose.domain

enum class BoardTheme { Sunbeam, Coral, Cobalt, Forest, Paper, Midnight }

enum class BoardAlignment { Left, Center }

enum class BoardPreset { Away, Closed, Rest, Busy, Meeting, Quiet, Maintenance, Call }

data class BoardThemeColors(val background: String, val foreground: String)

data class BoardPresetSpec(val id: BoardPreset, val messageKey: String, val theme: BoardTheme)

object MessageBoardEngine {
    const val MAX_LENGTH = 80
    const val MIN_SIZE = 70
    const val MAX_SIZE = 130
    const val SIZE_STEP = 5
    const val DEFAULT_SIZE = 100
    const val MIN_FONT_PX = 26
    const val MAX_FONT_PX = 280

    val presets: List<BoardPresetSpec> = listOf(
        BoardPresetSpec(BoardPreset.Away, "messageBoard.preset.away", BoardTheme.Sunbeam),
        BoardPresetSpec(BoardPreset.Closed, "messageBoard.preset.closed", BoardTheme.Coral),
        BoardPresetSpec(BoardPreset.Rest, "messageBoard.preset.rest", BoardTheme.Paper),
        BoardPresetSpec(BoardPreset.Busy, "messageBoard.preset.busy", BoardTheme.Cobalt),
        BoardPresetSpec(BoardPreset.Meeting, "messageBoard.preset.meeting", BoardTheme.Forest),
        BoardPresetSpec(BoardPreset.Quiet, "messageBoard.preset.quiet", BoardTheme.Midnight),
        BoardPresetSpec(BoardPreset.Maintenance, "messageBoard.preset.maintenance", BoardTheme.Cobalt),
        BoardPresetSpec(BoardPreset.Call, "messageBoard.preset.call", BoardTheme.Forest)
    )

    fun clip(message: String): String =
        if (message.length <= MAX_LENGTH) message else message.substring(0, MAX_LENGTH)

    fun normalizeSize(value: Int): Int = value.coerceIn(MIN_SIZE, MAX_SIZE)

    fun snapSize(value: Int): Int = ((value / SIZE_STEP) * SIZE_STEP).coerceIn(MIN_SIZE, MAX_SIZE)

    fun theme(id: BoardTheme): BoardThemeColors = when (id) {
        BoardTheme.Sunbeam -> BoardThemeColors("#F4CE57", "#183832")
        BoardTheme.Coral -> BoardThemeColors("#F36B55", "#FFF7EC")
        BoardTheme.Cobalt -> BoardThemeColors("#3459D4", "#F3F5FF")
        BoardTheme.Forest -> BoardThemeColors("#0F4A3A", "#E8F0C2")
        BoardTheme.Paper -> BoardThemeColors("#EFE9DC", "#29241F")
        BoardTheme.Midnight -> BoardThemeColors("#151821", "#E8F0FF")
    }

    fun themeId(value: String): BoardTheme =
        BoardTheme.entries.find { it.name.equals(value, ignoreCase = true) } ?: BoardTheme.Sunbeam

    fun alignmentId(value: String): BoardAlignment =
        BoardAlignment.entries.find { it.name.equals(value, ignoreCase = true) } ?: BoardAlignment.Center

    fun fitFontSize(availableWidth: Int, availableHeight: Int, sizePercent: Int, fits: (Int) -> Boolean): Int {
        if (availableWidth <= 0 || availableHeight <= 0) return MIN_FONT_PX
        var low = MIN_FONT_PX
        var high = maxOf(low, minOf(MAX_FONT_PX, kotlin.math.round(availableHeight * 0.72 * sizePercent / 100.0).toInt()))
        while (low < high) {
            val candidate = kotlin.math.ceil((low + high) / 2.0).toInt()
            if (fits(candidate)) low = candidate else high = candidate - 1
        }
        return low
    }
}

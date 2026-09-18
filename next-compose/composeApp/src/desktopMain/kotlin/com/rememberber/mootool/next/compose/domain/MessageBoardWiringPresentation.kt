package com.rememberber.mootool.next.compose.domain

/** F19 留言板：展示唤醒、文案裁剪与排版引擎路径（可单测）。 */
object MessageBoardWiringPresentation {
    fun clippedMessage(raw: String): String = MessageBoardEngine.clip(raw)

    fun wakeErrorIfNeeded(displayAwake: Boolean): Boolean = !displayAwake

    fun canEnterPresentation(message: String, displayAwake: Boolean): Boolean =
        message.isNotBlank() && displayAwake

    fun runFitFontSize(
        availableWidth: Int,
        availableHeight: Int,
        sizePercent: Int,
        fits: (Int) -> Boolean,
    ): Int = MessageBoardEngine.fitFontSize(availableWidth, availableHeight, sizePercent, fits)

    fun shouldToastWakeFailure(displayAwake: Boolean): Boolean = !displayAwake
}

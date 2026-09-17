package com.rememberber.mootool.next.compose.domain

/** F19 留言板：展示唤醒与文案裁剪。 */
object MessageBoardWiringPresentation {
    fun clippedMessage(raw: String): String = MessageBoardEngine.clip(raw)

    fun wakeErrorIfNeeded(displayAwake: Boolean): Boolean = !displayAwake
}

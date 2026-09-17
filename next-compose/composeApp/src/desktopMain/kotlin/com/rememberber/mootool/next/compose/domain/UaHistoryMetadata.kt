package com.rememberber.mootool.next.compose.domain

/** F12 UA 历史：对齐 Electron `extraData: 'UaParse'`。 */
object UaHistoryMetadata {
    const val MARKER = "UaParse"

    fun encode(): String = MARKER
}

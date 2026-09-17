package com.rememberber.mootool.next.compose.domain

/** F02 文本对比：自动比较 debounce 与导航索引（对齐 Electron 输入后短延迟重算）。 */
object TextDiffPresentation {
    const val AUTO_COMPARE_DEBOUNCE_MS: Long = 160L

    fun nextNavIndex(current: Int, step: Int, size: Int): Int {
        if (size <= 0) return -1
        return (current + step + size) % size
    }
}

package com.rememberber.mootool.next.compose.domain

/** F14 随机串设置 → 会话长度 clamp（对齐 Electron 工具默认值 + `CryptoEngine` 边界）。 */
object RandomWiringPresentation {
    fun sessionRandomLength(settingsRandomStringLength: Int): Int =
        ToolsSettingsLiveApply.randomStringLength(settingsRandomStringLength)
            .coerceIn(CryptoEngine.MIN_RANDOM_LENGTH, CryptoEngine.MAX_RANDOM_LENGTH)

    fun persistedRandomLength(sessionRandomLength: Int): Int =
        sessionRandomLength.coerceIn(CryptoEngine.MIN_RANDOM_LENGTH, CryptoEngine.MAX_RANDOM_LENGTH)
}

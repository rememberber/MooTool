package com.rememberber.mootool.next.compose.domain

/** F17 设置 → 会话 QR 尺寸/纠错与生成 clamp（可单测，对齐 Electron `qrTools` + 设置默认值）。 */
object QrWiringPresentation {
    data class Defaults(val size: Int, val correction: QrErrorCorrection)

    fun fromSettings(qrCodeSize: Int, qrErrorCorrection: String): Defaults {
        val correctionName = ToolsSettingsLiveApply.qrErrorCorrection(qrErrorCorrection)
        val correction = QrErrorCorrection.entries.find { it.name == correctionName } ?: QrErrorCorrection.M
        val size = QrEngine.normalizeSize(ToolsSettingsLiveApply.qrCodeSize(qrCodeSize))
        return Defaults(size, correction)
    }

    fun parseSizeField(raw: String, fallback: Int): Int {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return QrEngine.normalizeSize(fallback)
        trimmed.toDoubleOrNull()?.let { return QrEngine.normalizeSize(it) }
        trimmed.toIntOrNull()?.let { return QrEngine.normalizeSize(it) }
        return QrEngine.normalizeSize(fallback)
    }

    fun generateSize(sessionSize: Int): Int = QrEngine.normalizeSize(sessionSize)
}

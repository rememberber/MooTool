package com.rememberber.mootool.next.compose.sessions

import com.rememberber.mootool.next.compose.domain.ScreenColorPicker
import com.rememberber.mootool.next.compose.domain.ScreenRegionPicker

/**
 * Electron `jsonSessionState` / `jsonVaultSessionState` 在切工具时保留 Git、历史、JSONPath 选择器、
 * `inputConversion`/`conversionInput`、结果对话框与 Vault 对话框等；遮罩仅在工具页挂载时绘制，切走不会误挡其它页。
 */
internal fun JsonSession.dismissModalOverlays() = Unit

/** 对照 Electron `quickNoteSessionState`：切工具不重置 Git / 历史 / 文档信息 / Vault 操作对话框。 */
internal fun QuickNoteSession.dismissModalOverlays() = Unit

internal fun HttpSession.dismissModalOverlays() {
    historyOpen = false
    curlOpen = false
    saveOpen = false
    deleteConfirm = false
}

internal fun TimeSession.dismissModalOverlays() {
    historyOpen = false
    clockOpen = false
}

internal fun CalculatorSession.dismissModalOverlays() {
    historyOpen = false
}

internal fun EncodeSession.dismissModalOverlays() {
    historyOpen = false
}

internal fun UaSession.dismissModalOverlays() {
    historyOpen = false
}

internal fun RegexSession.dismissModalOverlays() {
    historyOpen = false
    favoritesOpen = false
}

internal fun CronSession.dismissModalOverlays() {
    historyOpen = false
    favoritesOpen = false
}

internal fun DiffSession.dismissModalOverlays() {
    historyOpen = false
}

internal fun ReformatSession.dismissModalOverlays() {
    historyOpen = false
}

internal fun ConfigSession.dismissModalOverlays() {
    historyOpen = false
}

internal fun ProtobufSession.dismissModalOverlays() {
    historyOpen = false
}

internal fun CryptoSession.dismissModalOverlays() {
    historyOpen = false
}

internal fun ColorSession.dismissModalOverlays() {
    historyOpen = false
    favoritesOpen = false
    saveFavoriteOpen = false
    picking = false
    ScreenColorPicker.dismissActive()
}

internal fun PdfSession.dismissModalOverlays() {
    historyOpen = false
    helpOpen = false
    confirmSplit = false
}

internal fun ImageSession.dismissModalOverlays() {
    historyOpen = false
    compressOpen = false
    watermarkOpen = false
    svgOpen = false
    renameOpen = false
    saveOpen = false
    deleteOpen = false
    busy = false
    ScreenRegionPicker.dismissActive()
}

internal fun NetSession.dismissModalOverlays() {
    historyOpen = false
}

internal fun VariablesSession.dismissModalOverlays() {
    editorOpen = false
}

internal fun HostSession.dismissModalOverlays() {
    historyOpen = false
    systemOpen = false
    applyConfirm = false
    deleteConfirm = false
    renameOpen = false
    profileContextMenuId = ""
}

internal fun TranslationSession.dismissModalOverlays() {
    deleteWordConfirm = false
    clearHistoryConfirm = false
}

internal fun CodeRunSession.dismissModalOverlays() {
    historyOpen = false
    optionsOpen = false
}

internal fun MessageBoardSession.dismissModalOverlays() {
    presenting = false
}

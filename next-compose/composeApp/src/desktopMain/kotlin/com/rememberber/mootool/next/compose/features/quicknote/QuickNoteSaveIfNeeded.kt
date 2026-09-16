package com.rememberber.mootool.next.compose.features.quicknote

/** 无脏内容时无需保存；旧版 `session.error` 不应阻塞 Vault 拖放/移动/重命名/Git flush。 */
internal fun quickNoteSaveIfNeeded(dirty: Boolean, save: () -> Boolean): Boolean =
    if (dirty) save() else true

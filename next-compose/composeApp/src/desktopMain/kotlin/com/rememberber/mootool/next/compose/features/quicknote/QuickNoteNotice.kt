package com.rememberber.mootool.next.compose.features.quicknote

/**
 * 用户编辑正文后状态栏 `notice` 策略：列编辑闩锁时保留提示，否则清空（对齐 JSON DIFF-227 / Electron 编辑 onChange）。
 */
internal fun quickNoteNoticeOnUserDocumentChange(columnLatch: Boolean, columnEditHint: String): String =
    if (columnLatch) columnEditHint else ""

/** 用户改正文后清空状态栏 error（含旧保存失败文案；DIFF-335 后 error 不再挡 Vault，但仍不应残留）。 */
internal fun quickNoteErrorOnUserDocumentChange(): String = ""

package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession

/** Vault 根目录切换后关闭仍绑定旧库的对话框（对照换库后 Git/冲突状态失效）。 */
internal fun JsonSession.dismissVaultScopedOverlays() = resetVaultScopedOverlays()

internal fun QuickNoteSession.dismissVaultScopedOverlays() = resetVaultScopedOverlays()

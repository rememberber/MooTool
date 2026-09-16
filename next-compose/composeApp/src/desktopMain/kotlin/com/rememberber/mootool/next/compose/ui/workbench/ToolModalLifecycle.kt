package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.model.ToolId

/** 主窗因分离销毁占位时不应执行切页清理；分离窗关闭会先 reattach，此时 `isDetached` 为 false。 */
internal fun shouldRunToolLeaveCleanup(isDetached: Boolean): Boolean = !isDetached

/**
 * 离开工具页（主窗切页或关闭分离窗）时执行清理；主窗因分离而销毁占位时不执行，避免误清共享会话。
 * 分离窗关闭会先 [com.rememberber.mootool.next.compose.sessions.SessionManager.reattach]，`isDetached` 为 false 后仍会执行。
 */
@Composable
internal fun DismissModalOverlaysOnDispose(
    container: AppContainer,
    toolId: ToolId,
    onDismissUnlessDetached: () -> Unit,
) {
    OnToolLeaveUnlessDetached(container, toolId, onDismissUnlessDetached)
}

/** 切页/关窗时取消在途任务等（与关模态分离，可多次注册）。 */
@Composable
internal fun OnToolLeaveUnlessDetached(
    container: AppContainer,
    toolId: ToolId,
    onLeave: () -> Unit,
) {
    DisposableEffect(toolId) {
        onDispose {
            if (shouldRunToolLeaveCleanup(container.sessionManager.isDetached(toolId))) {
                onLeave()
            }
        }
    }
}

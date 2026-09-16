package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession

/** 设置里切换 Vault 自定义根目录后清空树展开/滚动（对齐 Electron 换库后 `applyTreeExpandMode` / 新树）。 */
internal fun JsonSession.resetVaultTreeOnCustomRootChange() {
    vaultTreeExpanded = emptyMap()
    vaultTreeScrollOffset = 0
}

internal fun QuickNoteSession.resetVaultTreeOnCustomRootChange() {
    vaultTreeExpanded = emptyMap()
    vaultTreeScrollOffset = 0
}

/** 监听解析后的 Vault 根（含 `data.directory` 变更导致的默认路径迁移）。 */
@Composable
internal fun OnVaultEffectiveRootChanged(effectiveRoot: String, onRootChanged: () -> Unit) {
    var previous by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(effectiveRoot) {
        val prior = previous
        previous = effectiveRoot
        if (prior != null && prior != effectiveRoot) onRootChanged()
    }
}

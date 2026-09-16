package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** 备份恢复等导致 `SessionManager.reloadAllToolSessionsFromStore` 后刷新磁盘数据源缓存。 */
@Composable
fun OnSessionStoreReload(generation: Long, onReload: () -> Unit) {
    LaunchedEffect(generation) {
        if (generation == 0L) return@LaunchedEffect
        onReload()
    }
}

package com.rememberber.mootool.next.compose.features.vault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rememberber.mootool.next.compose.domain.VaultRevisionMonitor

/** `sessionGeneration` 或监视器实例变化时同步 rebaseline（早于 `LaunchedEffect`，减轻导入后轮询竞态）。 */
@Composable
fun RebBaselineVaultMonitorOnSessionReload(sessionGeneration: Long, monitor: VaultRevisionMonitor?) {
    var syncedKey by remember { mutableStateOf<Pair<Long, VaultRevisionMonitor?>?>(null) }
    SideEffect {
        val key = sessionGeneration to monitor
        if (key != syncedKey) {
            syncedKey = key
            monitor?.rebaseline()
        }
    }
}

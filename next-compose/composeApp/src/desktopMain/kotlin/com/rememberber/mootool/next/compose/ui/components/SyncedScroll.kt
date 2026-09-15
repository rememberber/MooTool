package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.rememberber.mootool.next.compose.ui.workbench.SyncScrollPolicy

private class ScrollSyncGate {
    var active: Boolean = false
}

@Composable
fun rememberPairedScrollStates(): Pair<ScrollState, ScrollState> {
    val left = rememberScrollState()
    val right = rememberScrollState()
    val gate = remember { ScrollSyncGate() }
    LaunchedEffect(left.value, left.maxValue, right.maxValue) {
        propagateScroll(gate, left, right)
    }
    LaunchedEffect(right.value, right.maxValue, left.maxValue) {
        propagateScroll(gate, right, left)
    }
    return left to right
}

private suspend fun propagateScroll(gate: ScrollSyncGate, source: ScrollState, target: ScrollState) {
    if (gate.active) return
    if (!SyncScrollPolicy.shouldFollowClamped(source.value, source.maxValue, target.value)) return
    val next = SyncScrollPolicy.targetOffset(source.value, target.maxValue)
    if (!SyncScrollPolicy.shouldApply(false, target.value, next)) return
    gate.active = true
    try {
        target.scrollTo(next)
    } finally {
        gate.active = false
    }
}

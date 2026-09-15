package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

object FollowTail {
    const val THRESHOLD_PX = 48

    fun isPinned(value: Int, maxValue: Int, threshold: Int = THRESHOLD_PX): Boolean =
        maxValue <= 0 || maxValue - value <= threshold

    fun shouldResetPin(resetPinKey: Any?): Boolean = when (resetPinKey) {
        null -> false
        is String -> resetPinKey.isNotBlank()
        else -> true
    }
}

@Composable
fun rememberFollowTailScroll(contentKey: Any?, resetPinKey: Any? = null): ScrollState {
    val scroll = rememberScrollState()
    var pinned by remember { mutableStateOf(true) }
    LaunchedEffect(resetPinKey) {
        if (FollowTail.shouldResetPin(resetPinKey)) {
            pinned = true
            scroll.scrollTo(scroll.maxValue)
        }
    }
    LaunchedEffect(scroll.value) {
        pinned = FollowTail.isPinned(scroll.value, scroll.maxValue)
    }
    LaunchedEffect(contentKey, scroll.maxValue) {
        if (pinned) {
            scroll.scrollTo(scroll.maxValue)
        }
    }
    return scroll
}

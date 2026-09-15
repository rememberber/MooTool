package com.rememberber.mootool.next.compose.ui.workbench

object SyncScrollPolicy {
    fun targetOffset(sourceValue: Int, targetMax: Int): Int =
        sourceValue.coerceIn(0, targetMax.coerceAtLeast(0))

    fun shouldApply(syncing: Boolean, currentTarget: Int, nextTarget: Int): Boolean =
        !syncing && currentTarget != nextTarget

    fun shouldFollowClamped(sourceValue: Int, sourceMax: Int, targetValue: Int): Boolean {
        if (sourceMax <= 0) return true
        return !(sourceValue >= sourceMax && targetValue > sourceMax)
    }
}

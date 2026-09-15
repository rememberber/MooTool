package com.rememberber.mootool.next.compose.domain

class VaultGitCheckpointScheduler(
    private val enabled: () -> Boolean,
    private val hasUnsavedEditorChanges: () -> Boolean,
    private val idleMilliseconds: () -> Long,
    private val inactiveMilliseconds: () -> Long,
    private val checkpoint: (String) -> GitActionResult,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private var lastActivityAt = 0L
    private var windowDeactivatedAt = 0L
    private var lastIdleCheckpointForActivity = -1L
    private var lastInactiveCheckpointAt = -1L
    private var message = "Automatic Vault checkpoint"
    private var running = false

    fun recordActivity(nextMessage: String) {
        lastActivityAt = now()
        message = nextMessage
    }

    fun setWindowActive(active: Boolean) {
        windowDeactivatedAt = if (active) 0L else now()
    }

    fun evaluate(): Boolean {
        if (running || !enabled() || hasUnsavedEditorChanges()) return false
        if (lastActivityAt == 0L && windowDeactivatedAt == 0L) return false
        val current = now()
        val idleReady = lastActivityAt > 0L &&
            current - lastActivityAt >= idleMilliseconds() &&
            lastIdleCheckpointForActivity < lastActivityAt
        val inactiveReady = windowDeactivatedAt > 0L &&
            current - windowDeactivatedAt >= inactiveMilliseconds() &&
            lastInactiveCheckpointAt < windowDeactivatedAt
        if (!idleReady && !inactiveReady) return false
        val activitySnapshot = lastActivityAt
        val inactiveSnapshot = windowDeactivatedAt
        running = true
        return try {
            val result = checkpoint(message)
            if (!result.success) return false
            if (idleReady) lastIdleCheckpointForActivity = activitySnapshot
            if (inactiveReady) lastInactiveCheckpointAt = inactiveSnapshot
            true
        } catch (_: Exception) {
            false
        } finally {
            running = false
        }
    }
}

class VaultGitPullScheduler(
    private val enabled: () -> Boolean,
    private val hasUnsavedEditorChanges: () -> Boolean,
    private val intervalMilliseconds: () -> Long,
    private val pull: () -> GitActionResult,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private var lastPullAt = 0L
    private var running = false

    fun evaluate(): Boolean {
        val interval = intervalMilliseconds()
        if (running || !enabled() || interval <= 0L || hasUnsavedEditorChanges()) return false
        val current = now()
        if (lastPullAt != 0L && current - lastPullAt < interval) return false
        running = true
        return try {
            val result = pull()
            lastPullAt = current
            result.success
        } catch (_: Exception) {
            lastPullAt = current
            false
        } finally {
            running = false
        }
    }
}

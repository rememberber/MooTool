package com.rememberber.mootool.next.compose.domain

/** Live wiring between global editor settings and open tool sessions (Quick Note keeps per-note `lineWrap`). */
object EditorSettingsLiveApply {
    fun jsonSoftWrapFromSettings(sessionWrap: Boolean, settingsSoftWrap: Boolean): Boolean =
        if (sessionWrap != settingsSoftWrap) settingsSoftWrap else sessionWrap
}

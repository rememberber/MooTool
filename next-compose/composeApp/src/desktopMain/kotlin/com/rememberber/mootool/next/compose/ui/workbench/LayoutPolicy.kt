package com.rememberber.mootool.next.compose.ui.workbench

object LayoutPolicy {
    const val COMPACT_WIDTH_DP = 960f
    const val COMPACT_HEIGHT_DP = 640f
    const val FULL_TOOLBAR_WIDTH_DP = 1440f
    /** Electron `JsonTool` uses `(min-width: 1321px)` for default / resize-driven inspector visibility. */
    const val JSON_DESKTOP_INSPECTOR_WIDTH_DP = 1321f

    fun isCompact(contentWidthDp: Float): Boolean = contentWidthDp < COMPACT_WIDTH_DP

    fun jsonInspectorDesktopOpen(contentWidthDp: Float): Boolean =
        contentWidthDp >= JSON_DESKTOP_INSPECTOR_WIDTH_DP

    fun showMinSizeHint(contentWidthDp: Float, contentHeightDp: Float): Boolean =
        contentWidthDp < COMPACT_WIDTH_DP || contentHeightDp < COMPACT_HEIGHT_DP

    fun overflowToolbar(contentWidthDp: Float): Boolean = contentWidthDp < FULL_TOOLBAR_WIDTH_DP

    fun collapseNavigation(
        contentWidthDp: Float,
        sidebarCollapsed: Boolean,
        hideTitles: Boolean
    ): Boolean = sidebarCollapsed || hideTitles || isCompact(contentWidthDp)

    fun toggleAux(current: String, target: String): String = if (current == target) "" else target

    fun showVault(compact: Boolean, compactAux: String): Boolean = !compact || compactAux == "vault"

    /** Electron `treeOpen` on wide layout; narrow layout still uses [compactAux]. */
    fun showQuickNoteVault(compact: Boolean, compactAux: String, vaultTreeOpen: Boolean): Boolean =
        vaultTreeOpen && showVault(compact, compactAux)

    fun showInspector(compact: Boolean, compactAux: String, inspectorOpen: Boolean): Boolean =
        if (compact) compactAux == "inspector" else inspectorOpen

    fun showReplace(compact: Boolean, compactAux: String, replaceOpen: Boolean): Boolean =
        if (compact) compactAux == "replace" else replaceOpen
}

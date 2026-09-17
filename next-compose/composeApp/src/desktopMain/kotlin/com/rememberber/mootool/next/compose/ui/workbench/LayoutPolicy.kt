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

    /** Electron `.app-shell--compact-nav .tool-button` / `.recent-item` (min-height 30, font -1sp). */
    fun navigationItemFontSp(compactNavigation: Boolean): Float = if (compactNavigation) 12f else 13f

    fun navigationItemMinHeightDp(compactNavigation: Boolean): Float = if (compactNavigation) 30f else 34f

    /** Electron `.app-shell--compact-nav .tool-group` vs `.app-shell--nav-classic .tool-group { margin-top: 3px }`. */
    fun navigationGroupTopPaddingDp(navigationStyle: String, compactNavigation: Boolean): Int = when {
        navigationStyle == "classic" -> 3
        compactNavigation -> 12
        else -> 10
    }

    fun navigationItemVerticalPaddingDp(compactNavigation: Boolean): Int = if (compactNavigation) 4 else 8

    /** Electron `.app-shell--nav-classic .tool-group h2 { display: none }` but custom groups keep titles. */
    fun showNavigationGroupLabel(navigationStyle: String, showSeparators: Boolean, customGroup: Boolean): Boolean {
        if (!showSeparators) return false
        if (customGroup) return true
        return navigationStyle != "classic"
    }

    /** DIFF-496 follow-up: tool `p5Toolbar` buttons shrink when layout compact navigation is on. */
    fun p5ToolbarDense(compactNavigation: Boolean): Boolean = compactNavigation

    /**
     * Electron `.tool-page .toolbar-button` uses `--tool-control-height` (34px) and modern `font-size: 13px` /
     * `font-weight: 500`. Compact navigation keeps DIFF-497 dense 26px / 11sp.
     */
    fun p5ToolbarButtonHeightDp(dense: Boolean, controlHeightDp: Float): Float =
        if (dense) 26f else controlHeightDp

    fun p5ToolbarFontSp(dense: Boolean, interfaceStyle: String): Float = when {
        dense -> 11f
        interfaceStyle == "modern" || interfaceStyle == "quiet" -> 13f
        else -> 12f
    }

    fun p5ToolbarFontWeightMedium(dense: Boolean, interfaceStyle: String): Boolean =
        dense || interfaceStyle == "modern" || interfaceStyle == "quiet"

    /** Shared by p5 and `.editor-toolbar` non-p5 buttons (DIFF-501/502). */
    fun nonP5ToolbarFontSp(dense: Boolean, interfaceStyle: String): Float? = when {
        dense -> null
        interfaceStyle == "modern" || interfaceStyle == "quiet" -> 13f
        else -> null
    }

    fun nonP5ToolbarFontWeightMedium(dense: Boolean, interfaceStyle: String): Boolean =
        !dense && (interfaceStyle == "modern" || interfaceStyle == "quiet")
}

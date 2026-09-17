package com.rememberber.mootool.next.compose.ui.workbench

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayoutPolicyTest {
    @Test
    fun compactWidthCollapsesNavigationWithoutManualToggle() {
        assertTrue(LayoutPolicy.collapseNavigation(959f, sidebarCollapsed = false, hideTitles = false))
        assertFalse(LayoutPolicy.collapseNavigation(960f, sidebarCollapsed = false, hideTitles = false))
        assertTrue(LayoutPolicy.collapseNavigation(1440f, sidebarCollapsed = true, hideTitles = false))
        assertTrue(LayoutPolicy.collapseNavigation(1440f, sidebarCollapsed = false, hideTitles = true))
        assertTrue(LayoutPolicy.isCompact(959f))
        assertFalse(LayoutPolicy.isCompact(960f))
    }

    @Test
    fun minSizeHintWhenWindowManagerForcesBelowCompact() {
        assertFalse(LayoutPolicy.showMinSizeHint(960f, 640f))
        assertTrue(LayoutPolicy.showMinSizeHint(959f, 640f))
        assertTrue(LayoutPolicy.showMinSizeHint(960f, 639f))
        assertTrue(LayoutPolicy.showMinSizeHint(800f, 500f))
        assertFalse(LayoutPolicy.showMinSizeHint(1440f, 920f))
    }

    @Test
    fun overflowToolbarBelowFullWidth() {
        assertFalse(LayoutPolicy.overflowToolbar(1440f))
        assertTrue(LayoutPolicy.overflowToolbar(1439f))
        assertTrue(LayoutPolicy.overflowToolbar(1080f))
        assertTrue(LayoutPolicy.overflowToolbar(960f))
        assertFalse(LayoutPolicy.overflowToolbar(1920f))
    }

    @Test
    fun jsonInspectorDesktopBreakpointMatchesElectron() {
        assertFalse(LayoutPolicy.jsonInspectorDesktopOpen(1320f))
        assertTrue(LayoutPolicy.jsonInspectorDesktopOpen(1321f))
        assertTrue(LayoutPolicy.jsonInspectorDesktopOpen(1920f))
    }

    @Test
    fun compactNavigationDensityMatchesElectronShell() {
        assertEquals(12f, LayoutPolicy.navigationItemFontSp(compactNavigation = true))
        assertEquals(13f, LayoutPolicy.navigationItemFontSp(compactNavigation = false))
        assertEquals(30f, LayoutPolicy.navigationItemMinHeightDp(compactNavigation = true))
        assertEquals(34f, LayoutPolicy.navigationItemMinHeightDp(compactNavigation = false))
        assertEquals(12, LayoutPolicy.navigationGroupTopPaddingDp(compactNavigation = true))
        assertEquals(10, LayoutPolicy.navigationGroupTopPaddingDp(compactNavigation = false))
    }

    @Test
    fun compactAuxShowsOnePanelAtATime() {
        assertEquals("vault", LayoutPolicy.toggleAux("", "vault"))
        assertEquals("", LayoutPolicy.toggleAux("vault", "vault"))
        assertEquals("inspector", LayoutPolicy.toggleAux("vault", "inspector"))
        assertTrue(LayoutPolicy.showVault(compact = false, compactAux = ""))
        assertFalse(LayoutPolicy.showVault(compact = true, compactAux = ""))
        assertTrue(LayoutPolicy.showVault(compact = true, compactAux = "vault"))
        assertFalse(LayoutPolicy.showInspector(compact = true, compactAux = "", inspectorOpen = true))
        assertTrue(LayoutPolicy.showInspector(compact = true, compactAux = "inspector", inspectorOpen = false))
        assertTrue(LayoutPolicy.showInspector(compact = false, compactAux = "", inspectorOpen = true))
        assertFalse(LayoutPolicy.showReplace(compact = true, compactAux = "vault", replaceOpen = true))
        assertTrue(LayoutPolicy.showReplace(compact = true, compactAux = "replace", replaceOpen = false))
        assertTrue(LayoutPolicy.showReplace(compact = false, compactAux = "", replaceOpen = true))
        assertFalse(LayoutPolicy.showQuickNoteVault(compact = false, compactAux = "", vaultTreeOpen = false))
        assertTrue(LayoutPolicy.showQuickNoteVault(compact = false, compactAux = "", vaultTreeOpen = true))
    }
}

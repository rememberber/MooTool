package com.rememberber.mootool.nextfx.domain;

/**
 * Sidebar geometry from the UI spec. Hidden-title mode uses 84; otherwise 248.
 * Compact mode only reduces row height.
 */
public final class NavigationLayout {

    public static final double EXPANDED_WIDTH = 248;
    public static final double COMPACT_WIDTH = 84;
    public static final double REGULAR_ROW_HEIGHT = 36;
    public static final double COMPACT_ROW_HEIGHT = 32;

    private NavigationLayout() {
    }

    public static double width(boolean hideNavigationTitles) {
        return hideNavigationTitles ? COMPACT_WIDTH : EXPANDED_WIDTH;
    }

    public static double rowHeight(boolean compactNavigation) {
        return compactNavigation ? COMPACT_ROW_HEIGHT : REGULAR_ROW_HEIGHT;
    }
}

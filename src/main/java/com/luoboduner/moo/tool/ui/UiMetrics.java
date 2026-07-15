package com.luoboduner.moo.tool.ui;

import java.awt.Insets;

/**
 * UI 间距与边距常量，避免在业务代码中散落硬编码 Insets。
 */
public final class UiMetrics {

    /** 工具栏相邻图标按钮间距（px） */
    public static final int TOOLBAR_ICON_GAP = 5;

    /** 工具栏按钮组之间的分隔间距（px） */
    public static final int TOOLBAR_GROUP_GAP = 14;

    /** Tab 列表切换按钮与 Tab 栏的间距（px） */
    public static final int TAB_LEADING_INSET = 10;

    /** macOS 透明标题栏下主面板顶部留白（px） */
    public static final int MAC_MAIN_PANEL_MARGIN_TOP = 25;

    /** macOS 左侧 Tab 时主面板顶部留白（px） */
    public static final int MAC_TAB_LEFT_MARGIN_TOP = 15;

    /** macOS 顶部 Tab 时主面板顶部留白（px） */
    public static final int MAC_TAB_TOP_MARGIN_TOP = 25;

    /** 非 macOS 左侧 Tab 时主面板顶部留白（px） */
    public static final int TAB_LEFT_MARGIN_TOP = -10;

    private UiMetrics() {
    }

    public static Insets zero() {
        return new Insets(0, 0, 0, 0);
    }

    public static Insets toolbarButtonSpacingInsets() {
        int leading = TOOLBAR_ICON_GAP / 2;
        int trailing = TOOLBAR_ICON_GAP - leading;
        return new Insets(0, leading, 0, trailing);
    }

    public static Insets tabLeadingInsets(boolean leftTab) {
        if (leftTab) {
            return new Insets(TAB_LEADING_INSET, 0, 0, 0);
        }
        return new Insets(0, TAB_LEADING_INSET, 0, 0);
    }

    public static Insets macMainPanelMarginTop() {
        return new Insets(MAC_MAIN_PANEL_MARGIN_TOP, 0, 0, 0);
    }

    public static Insets macTabLeftPanelMarginTop() {
        return new Insets(MAC_TAB_LEFT_MARGIN_TOP, 0, 0, 0);
    }

    public static Insets macTabTopPanelMarginTop() {
        return new Insets(MAC_TAB_TOP_MARGIN_TOP, 0, 0, 0);
    }

    public static Insets tabLeftPanelMarginTop() {
        return new Insets(TAB_LEFT_MARGIN_TOP, 0, 0, 0);
    }
}

package com.luoboduner.moo.tool.ui.component;

import javax.swing.*;
import java.awt.*;

public final class TabUiUtil {

    public static final String SELECTION_ON_LEADING_EDGE = "MooTool.selectionOnLeadingEdge";

    private static final ThreadLocal<Boolean> INSTALLING = ThreadLocal.withInitial(() -> false);

    private TabUiUtil() {
    }

    public static void applySafeTabbedPaneUi(Component root) {
        applySafeTabbedPaneUi(root, null);
    }

    /**
     * 为子面板中的 JTabbedPane 安装安全 UI；{@code exclude} 用于跳过主功能 Tab（由其自行配置）。
     */
    public static void applySafeTabbedPaneUi(Component root, JTabbedPane exclude) {
        if (root == null) {
            return;
        }
        if (root instanceof JTabbedPane) {
            JTabbedPane tabbedPane = (JTabbedPane) root;
            if (tabbedPane != exclude) {
                installSafeUi(tabbedPane, isSelectionOnLeadingEdge(tabbedPane));
            }
        }
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                applySafeTabbedPaneUi(child, exclude);
            }
        }
    }

    public static boolean isSelectionOnLeadingEdge(JTabbedPane tabbedPane) {
        return Boolean.TRUE.equals(tabbedPane.getClientProperty(SELECTION_ON_LEADING_EDGE));
    }

    public static void installSafeUi(JTabbedPane tabbedPane, boolean leadingEdgeSelection) {
        if (tabbedPane == null || INSTALLING.get()) {
            return;
        }
        if (leadingEdgeSelection) {
            tabbedPane.putClientProperty(SELECTION_ON_LEADING_EDGE, Boolean.TRUE);
        } else {
            tabbedPane.putClientProperty(SELECTION_ON_LEADING_EDGE, null);
        }
        if (tabbedPane.getUI() instanceof MooFlatTabbedPaneUI) {
            ((MooFlatTabbedPaneUI) tabbedPane.getUI()).setSelectionOnLeadingEdge(leadingEdgeSelection);
            tabbedPane.repaint();
            return;
        }
        INSTALLING.set(true);
        try {
            MooFlatTabbedPaneUI ui = new MooFlatTabbedPaneUI();
            ui.setSelectionOnLeadingEdge(leadingEdgeSelection);
            tabbedPane.setUI(ui);
        } finally {
            INSTALLING.set(false);
        }
    }
}

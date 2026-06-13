package com.luoboduner.moo.tool.ui.component;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TAB_WIDTH_MODE;

public final class TabUiUtil {

    public static final String SELECTION_ON_LEADING_EDGE = "MooTool.selectionOnLeadingEdge";

    public static final String HIDE_TAB_STRIP = "MooTool.hideTabStrip";

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

    public static boolean isTabStripHidden(JTabbedPane tabbedPane) {
        return Boolean.TRUE.equals(tabbedPane.getClientProperty(HIDE_TAB_STRIP));
    }

    public static void setTabStripHidden(JTabbedPane tabbedPane, boolean hidden) {
        if (hidden) {
            tabbedPane.putClientProperty(HIDE_TAB_STRIP, Boolean.TRUE);
        } else {
            tabbedPane.putClientProperty(HIDE_TAB_STRIP, null);
        }
        if (tabbedPane.getUI() != null) {
            tabbedPane.revalidate();
            tabbedPane.repaint();
            forceTabContentLayout(tabbedPane);
        }
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
            tabbedPane.revalidate();
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

    /**
     * Tab 栏宽度变化后，强制 JTabbedPane 重新分配内容区 bounds，并刷新当前 Tab 内部布局。
     */
    public static void forceTabContentLayout(JTabbedPane tabbedPane) {
        if (tabbedPane == null) {
            return;
        }
        Runnable layout = () -> {
            tabbedPane.invalidate();
            if (tabbedPane.getWidth() > 0 && tabbedPane.getHeight() > 0) {
                tabbedPane.doLayout();
            }
            Component selected = tabbedPane.getSelectedComponent();
            if (selected instanceof Container container) {
                SplitPaneUtil.relaxHorizontalMinimumDeep(container);
                SplitPaneUtil.normalizeSplitPaneDividers(container);
                validateDeep(container);
            }
            tabbedPane.revalidate();
            tabbedPane.repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            layout.run();
            SwingUtilities.invokeLater(layout);
        } else {
            SwingUtilities.invokeLater(layout);
        }
    }

    public static void relayoutAfterTabStripChanged(JTabbedPane tabbedPane, JComponent contentRoot) {
        forceTabContentLayout(tabbedPane);
        if (contentRoot != null) {
            SplitPaneUtil.relaxHorizontalMinimumDeep(contentRoot);
            SwingUtilities.invokeLater(() -> {
                contentRoot.revalidate();
                contentRoot.repaint();
            });
        }
    }

    public static void relaxTabContentMinimumSizes(JTabbedPane tabbedPane) {
        if (tabbedPane == null) {
            return;
        }
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tab = tabbedPane.getComponentAt(i);
            if (tab instanceof Container container) {
                SplitPaneUtil.relaxHorizontalMinimumDeep(container);
            }
        }
    }

    public static PropertyChangeListener createTabStripLayoutListener(JTabbedPane tabbedPane) {
        return (PropertyChangeEvent event) -> {
            String name = event.getPropertyName();
            if (TABBED_PANE_TAB_WIDTH_MODE.equals(name)
                    || "tabPlacement".equals(name)
                    || "font".equals(name)
                    || "fontSize".equals(name)) {
                forceTabContentLayout(tabbedPane);
            }
        };
    }

    private static void validateDeep(Container container) {
        container.invalidate();
        for (Component child : container.getComponents()) {
            if (child instanceof Container childContainer) {
                validateDeep(childContainer);
            }
        }
        container.validate();
    }
}

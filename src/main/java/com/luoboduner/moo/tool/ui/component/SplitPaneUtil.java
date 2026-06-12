package com.luoboduner.moo.tool.ui.component;

import javax.swing.*;
import java.awt.*;

/**
 * JSplitPane 布局辅助：按面板自身宽度比例设置分隔条，避免用主窗口像素导致内容区被裁切。
 */
public final class SplitPaneUtil {

    /** 左侧列表面板默认占比 */
    public static final double LIST_PANEL_RATIO = 0.18;

    /** 右侧辅助面板默认占比 */
    public static final double SECONDARY_PANEL_RATIO = 0.28;

    private SplitPaneUtil() {
    }

    public static void configureListEditorSplit(JSplitPane splitPane) {
        configureListEditorSplit(splitPane, LIST_PANEL_RATIO);
    }

    public static void configureListEditorSplit(JSplitPane splitPane, double listRatio) {
        splitPane.setResizeWeight(0);
        setDividerProportion(splitPane, listRatio);
    }

    public static void configureEditorSecondarySplit(JSplitPane splitPane) {
        splitPane.setResizeWeight(1.0);
        hideSecondary(splitPane);
    }

    public static void hideSecondary(JSplitPane splitPane) {
        setDividerProportion(splitPane, 1.0);
    }

    public static void showSecondary(JSplitPane splitPane, double secondaryRatio) {
        setDividerProportion(splitPane, Math.max(0, Math.min(1, 1.0 - secondaryRatio)));
    }

    public static void setDividerProportion(JSplitPane splitPane, double proportion) {
        double location = Math.max(0, Math.min(1, proportion));
        Runnable apply = () -> splitPane.setDividerLocation(location);
        if (splitPane.getWidth() > 0) {
            apply.run();
        } else {
            SwingUtilities.invokeLater(apply);
        }
    }

    public static void relaxHorizontalMinimum(JComponent... components) {
        for (JComponent component : components) {
            if (component == null) {
                continue;
            }
            relaxHorizontalMinimum(component);
        }
    }

    public static void relaxHorizontalMinimum(JComponent component) {
        Dimension minimum = component.getMinimumSize();
        int height = minimum.height > 0 ? minimum.height : component.getPreferredSize().height;
        component.setMinimumSize(new Dimension(0, height));
    }

    public static void relaxHorizontalMinimumDeep(Container root) {
        if (root instanceof JComponent jComponent) {
            relaxHorizontalMinimum(jComponent);
        }
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) {
                relaxHorizontalMinimumDeep(container);
            }
        }
    }

    /**
     * Tab 内容区变窄后，将仍按旧像素定位的分隔条折算为当前宽度的比例。
     */
    public static void normalizeSplitPaneDividers(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JSplitPane splitPane) {
                refreshSplitPaneDivider(splitPane);
            }
            if (child instanceof Container container) {
                normalizeSplitPaneDividers(container);
            }
        }
    }

    private static void refreshSplitPaneDivider(JSplitPane splitPane) {
        boolean horizontal = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT;
        int size = horizontal ? splitPane.getWidth() : splitPane.getHeight();
        if (size <= splitPane.getDividerSize() + 1) {
            return;
        }
        int location = splitPane.getDividerLocation();
        int maxLocation = size - splitPane.getDividerSize();
        if (location >= maxLocation - 1) {
            splitPane.setDividerLocation(1.0);
            return;
        }
        if (location <= 1) {
            splitPane.setDividerLocation(0.0);
            return;
        }
        double ratio = (double) location / size;
        if (ratio >= 0.99) {
            splitPane.setDividerLocation(1.0);
        } else if (ratio <= 0.01) {
            splitPane.setDividerLocation(0.0);
        } else {
            splitPane.setDividerLocation(ratio);
        }
    }
}

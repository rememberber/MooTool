package com.luoboduner.moo.tool.ui.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.luoboduner.moo.tool.util.I18n;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;

/**
 * 面板关闭按钮：JToolBar + JButton，提供更大的鼠标热区。
 */
public final class PanelCloseUtil {

    private PanelCloseUtil() {
    }

    public static void installTrailingCloseButton(JButton closeButton, GridConstraints constraints) {
        styleCloseButton(closeButton);
        Container parent = closeButton.getParent();
        if (parent == null) {
            return;
        }
        parent.remove(closeButton);
        constraints.setFill(GridConstraints.FILL_HORIZONTAL);
        constraints.setAnchor(GridConstraints.ANCHOR_CENTER);
        constraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        parent.add(createTrailingCloseBar(closeButton), constraints);
    }

    public static void installCompactCloseButton(JButton closeButton, GridConstraints constraints) {
        styleCloseButton(closeButton);
        Container parent = closeButton.getParent();
        if (parent == null) {
            return;
        }
        parent.remove(closeButton);
        constraints.setFill(GridConstraints.FILL_NONE);
        constraints.setHSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        constraints.setVSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
        parent.add(createCloseToolBar(closeButton), constraints);
    }

    private static JPanel createTrailingCloseBar(JButton closeButton) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.add(createCloseToolBar(closeButton), BorderLayout.EAST);
        return panel;
    }

    private static JToolBar createCloseToolBar(JButton closeButton) {
        JToolBar toolBar = createToolBar();
        toolBar.add(closeButton);
        return toolBar;
    }

    private static void styleCloseButton(JButton closeButton) {
        closeButton.setIcon(new FlatSVGIcon("icon/remove2.svg"));
        closeButton.setText("");
        closeButton.setToolTipText(I18n.get("common.close"));
    }

    private static JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        ToolbarUiUtil.configure(toolBar);
        return toolBar;
    }
}

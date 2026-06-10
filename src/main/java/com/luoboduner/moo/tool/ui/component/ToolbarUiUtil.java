package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.ui.UiMetrics;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.InsetsUIResource;
import java.awt.*;

/**
 * 工具栏统一间距：图标 4–6px、组间 12–16px。
 */
public final class ToolbarUiUtil {

    private ToolbarUiUtil() {
    }

    public static void applyGlobalDefaults() {
        Insets spacing = UiMetrics.toolbarButtonSpacingInsets();
        UIManager.put("Button.toolbar.spacingInsets",
                new InsetsUIResource(spacing.top, spacing.left, spacing.bottom, spacing.right));
        UIManager.put("ToolBar.separatorWidth", UiMetrics.TOOLBAR_GROUP_GAP);
    }

    public static void configure(JToolBar toolBar) {
        toolBar.setFloatable(false);
        toolBar.setBorder(null);
        toolBar.setRollover(true);
    }

    public static void addGroupSeparator(JToolBar toolBar) {
        toolBar.addSeparator();
    }

    public static void add(JToolBar toolBar, Component component) {
        if (needsSpacingWrapper(component)) {
            toolBar.add(wrapCustomComponent(component));
        } else {
            toolBar.add(component);
        }
    }

    private static boolean needsSpacingWrapper(Component component) {
        return !(component instanceof AbstractButton)
                && !(component instanceof JSeparator)
                && !(component instanceof Box.Filler);
    }

    public static JPanel wrapCustomComponent(Component component) {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            public void updateUI() {
                super.updateUI();
                Border border = UIManager.getBorder("ToolBar.spacingBorder");
                if (border != null) {
                    setBorder(border);
                }
            }
        };
        panel.setOpaque(false);
        panel.add(component, BorderLayout.CENTER);
        panel.updateUI();
        return panel;
    }
}

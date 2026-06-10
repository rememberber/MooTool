package com.luoboduner.moo.tool.util;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helpers for binding Swing components to i18n resource keys.
 */
public final class I18nUiUtil {

    private static final List<Runnable> REFRESH_CALLBACKS = new CopyOnWriteArrayList<>();

    private I18nUiUtil() {
    }

    public static void register(Runnable refresh) {
        REFRESH_CALLBACKS.add(refresh);
    }

    public static void refreshAll() {
        for (Runnable callback : REFRESH_CALLBACKS) {
            callback.run();
        }
    }

    public static void setText(JLabel label, String key) {
        if (label != null) {
            label.setText(I18n.get(key));
        }
    }

    public static void setText(AbstractButton button, String key) {
        if (button != null) {
            button.setText(I18n.get(key));
        }
    }

    public static void setToolTip(JComponent component, String key) {
        if (component != null) {
            component.setToolTipText(I18n.get(key));
        }
    }

    public static void setPlaceholder(JTextField field, String key) {
        if (field != null) {
            field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, I18n.get(key));
        }
    }

    public static void setTitledBorder(JComponent panel, String key) {
        if (panel == null || panel.getBorder() == null) {
            return;
        }
        if (panel.getBorder() instanceof TitledBorder titledBorder) {
            titledBorder.setTitle(I18n.get(key));
        }
    }

    public static void setTabTitle(JTabbedPane tabbedPane, int index, String key) {
        if (tabbedPane != null && index >= 0 && index < tabbedPane.getTabCount()) {
            tabbedPane.setTitleAt(index, I18n.get(key));
        }
    }

    /**
     * Match component text against default (designer) strings and apply i18n keys.
     * Keys in the map are the original UI designer strings.
     */
    public static void localizeTree(Container root, Map<String, String> defaultTextToKey) {
        if (root == null) {
            return;
        }
        for (Component component : root.getComponents()) {
            if (component instanceof JLabel label) {
                String key = defaultTextToKey.get(label.getText());
                if (key != null) {
                    label.setText(I18n.get(key));
                }
            } else if (component instanceof AbstractButton button) {
                String text = button.getText();
                if (text != null && !text.isEmpty()) {
                    String key = defaultTextToKey.get(text);
                    if (key != null) {
                        button.setText(I18n.get(key));
                    }
                }
            } else if (component instanceof JComponent jc) {
                if (jc.getBorder() instanceof TitledBorder titledBorder) {
                    String key = defaultTextToKey.get(titledBorder.getTitle());
                    if (key != null) {
                        titledBorder.setTitle(I18n.get(key));
                    }
                }
            }
            if (component instanceof Container container) {
                localizeTree(container, defaultTextToKey);
            }
        }
    }
}

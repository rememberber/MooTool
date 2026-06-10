package com.luoboduner.moo.tool.util;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
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
}

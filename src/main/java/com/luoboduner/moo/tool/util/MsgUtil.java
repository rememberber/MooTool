package com.luoboduner.moo.tool.util;

import javax.swing.*;
import java.awt.*;

/**
 * Internationalized JOptionPane helpers.
 */
public final class MsgUtil {

    private MsgUtil() {
    }

    public static void error(Component parent, String messageKey, Object... args) {
        JOptionPane.showMessageDialog(parent, format(messageKey, args),
                I18n.get("common.failure"), JOptionPane.ERROR_MESSAGE);
    }

    public static void errorWithDetail(Component parent, String failKey, String detail) {
        JOptionPane.showMessageDialog(parent, I18n.format(failKey, detail),
                I18n.get("common.failure"), JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String messageKey, Object... args) {
        JOptionPane.showMessageDialog(parent, format(messageKey, args),
                I18n.get("common.tip"), JOptionPane.INFORMATION_MESSAGE);
    }

    public static void success(Component parent, String messageKey, Object... args) {
        JOptionPane.showMessageDialog(parent, format(messageKey, args),
                I18n.get("common.success"), JOptionPane.INFORMATION_MESSAGE);
    }

    public static int confirm(Component parent, String messageKey) {
        return JOptionPane.showConfirmDialog(parent, I18n.get(messageKey),
                I18n.get("common.confirm"), JOptionPane.YES_NO_OPTION);
    }

    public static String inputName(Component parent, String initial) {
        return (String) JOptionPane.showInputDialog(parent, I18n.get("common.name"), initial);
    }

    public static String input(Component parent, String promptKey, String initial) {
        return (String) JOptionPane.showInputDialog(parent, I18n.get(promptKey), initial);
    }

    public static void warn(Component parent, String messageKey, Object... args) {
        JOptionPane.showMessageDialog(parent, format(messageKey, args),
                I18n.get("common.failure"), JOptionPane.WARNING_MESSAGE);
    }

    public static int confirmWithTitle(Component parent, String messageKey, String titleKey) {
        return JOptionPane.showConfirmDialog(parent, I18n.get(messageKey),
                I18n.get(titleKey), JOptionPane.YES_NO_OPTION);
    }

    public static void errorDetail(Component parent, String titleKey, String detail) {
        JOptionPane.showMessageDialog(parent, detail, I18n.get(titleKey), JOptionPane.ERROR_MESSAGE);
    }

    public static void show(Component parent, String message, String titleKey, int messageType) {
        JOptionPane.showMessageDialog(parent, message, I18n.get(titleKey), messageType);
    }

    private static String format(String messageKey, Object... args) {
        if (args == null || args.length == 0) {
            return I18n.get(messageKey);
        }
        return I18n.format(messageKey, args);
    }
}

package com.luoboduner.moo.tool.ui;


import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMonokaiProIJTheme;
import com.luoboduner.moo.tool.ui.form.MainWindow;

import javax.swing.*;
import java.awt.*;

/**
 * customize Swing component style
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2021/11/17.
 */
public class Style {

    public static final Color YELLOW = new Color(255, 198, 109);

    /**
     * emphatic font for title
     *
     * @param component
     */
    public static void emphaticTitleFont(JComponent component) {
        Font font = MainWindow.getInstance().getMainPanel().getFont();
        component.setFont(new Font(font.getName(), Font.BOLD, font.getSize() + 2));
    }

    /**
     * emphatic font for label
     *
     * @param component
     */
    public static void emphaticLabelFont(JComponent component) {
        Font font = MainWindow.getInstance().getMainPanel().getFont();
        component.setFont(new Font(font.getName(), Font.BOLD, font.getSize()));
    }

    /**
     * emphatic font for indicator
     *
     * @param component
     */
    public static void emphaticIndicatorFont(JComponent component) {
        Font font = MainWindow.getInstance().getMainPanel().getFont();
        component.setFont(new Font(font.getName(), Font.BOLD, font.getSize() + 12));
    }

    public static void blackTextArea(JComponent component) {
        Class<? extends LookAndFeel> lafClass = UIManager.getLookAndFeel().getClass();
        if (FlatLaf.isLafDark() && FlatMonokaiProIJTheme.class != lafClass) {
            Color bgColor = new Color(43, 43, 43);
            component.setBackground(bgColor);
            Color foreColor = new Color(187, 187, 187);
            component.setForeground(foreColor);
        }
    }

}

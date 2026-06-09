package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * 随手记列表项渲染器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/3/26.
 */
public class QuickNoteListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        TQuickNote note = (TQuickNote) value;
        Component component = super.getListCellRendererComponent(list, note.getName(), index, isSelected, cellHasFocus);
        String color = note.getColor();
        if (StringUtils.isNotEmpty(color)) {
            component.setForeground(UIManager.getColor(color));
        } else {
            component.setForeground(MainWindow.getInstance().getMainPanel().getForeground());
        }
        return component;
    }
}

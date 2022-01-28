package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import com.luoboduner.moo.tool.util.MybatisUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * <pre>
 * 自定义颜色块单元格渲染器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/3/26.
 */
public class QuickNoteListTableInCellRenderer extends DefaultTableCellRenderer {
    private static TQuickNoteMapper quickNoteMapper = MybatisUtil.getSqlSession().getMapper(TQuickNoteMapper.class);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        TQuickNote tQuickNote = quickNoteMapper.selectByName((String) value);

        String color = null;
        if (tQuickNote != null) {
            color = tQuickNote.getColor();
        }

        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (StringUtils.isNotEmpty(color)) {
            component.setForeground(UIManager.getColor(color));
        } else {
            component.setForeground(MainWindow.getInstance().getMainPanel().getForeground());
        }

        return component;
    }
}
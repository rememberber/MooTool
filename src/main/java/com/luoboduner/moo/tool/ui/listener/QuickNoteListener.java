package com.luoboduner.moo.tool.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.fun.QuickNoteForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.exceptions.PersistenceException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <pre>
 * 随手记事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/8/15.
 */
public class QuickNoteListener {

    private static TQuickNoteMapper quickNoteMapper = MybatisUtil.getSqlSession().getMapper(TQuickNoteMapper.class);

    private static String selectedName;

    public static void addListeners() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();

        quickNoteForm.getSaveButton().addActionListener(e -> {
            String name = JOptionPane.showInputDialog("请命名", selectedName);
            if (StringUtils.isNotBlank(name)) {
                TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
                if (tQuickNote == null) {
                    tQuickNote = new TQuickNote();
                }
                String now = SqliteUtil.nowDateForSqlite();
                tQuickNote.setName(name);
                tQuickNote.setContent(QuickNoteForm.getInstance().getTextArea().getText());
                tQuickNote.setCreateTime(now);
                tQuickNote.setModifiedTime(now);
                if (tQuickNote.getId() == null) {
                    quickNoteMapper.insert(tQuickNote);
                } else {
                    quickNoteMapper.updateByPrimaryKey(tQuickNote);
                }

                QuickNoteForm.init();
                selectedName = name;
            }
        });

        // 点击左侧表格事件
        quickNoteForm.getNoteListTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ThreadUtil.execute(() -> {
                    int selectedRow = quickNoteForm.getNoteListTable().getSelectedRow();
                    String name = quickNoteForm.getNoteListTable().getValueAt(selectedRow, 1).toString();
                    selectedName = name;
                    TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
                    quickNoteForm.getTextArea().setText(tQuickNote.getContent());
                });
                super.mousePressed(e);
            }
        });
    }
}

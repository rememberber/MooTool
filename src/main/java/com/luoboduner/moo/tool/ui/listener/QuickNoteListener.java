package com.luoboduner.moo.tool.ui.listener;

import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.fun.QuickNoteForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    public static void addListeners() {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        quickNoteForm.getSaveButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog("请命名", "");
                if (StringUtils.isNotBlank(name)) {
                    String now = SqliteUtil.nowDateForSqlite();
                    TQuickNote tQuickNote = new TQuickNote();
                    tQuickNote.setName(name);
                    tQuickNote.setContent(QuickNoteForm.getInstance().getTextArea().getText());
                    tQuickNote.setCreateTime(now);
                    tQuickNote.setModifiedTime(now);

                    quickNoteMapper.insert(tQuickNote);
                    QuickNoteForm.init();
                }
            }
        });
    }
}

package com.luoboduner.moo.tool.ui.listener;


import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.util.SqliteUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Date;

/**
 * <pre>
 * 窗体事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/21.
 */
public class FrameListener {

    private static TQuickNoteMapper quickNoteMapper = MybatisUtil.getSqlSession().getMapper(TQuickNoteMapper.class);

    public static void addListeners() {
        App.mainFrame.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                saveBeforeExit();
                App.mainFrame.setExtendedState(JFrame.HIDE_ON_CLOSE);
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }
        });
    }

    public static void saveBeforeExit() {
        String quickNoteName = QuickNoteListener.selectedName;
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        String now = SqliteUtil.nowDateForSqlite();
        if (StringUtils.isEmpty(quickNoteName)) {
            quickNoteName = "未命名_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_HH-mm-ss");
            TQuickNote tQuickNote = new TQuickNote();
            tQuickNote.setName(quickNoteName);
            tQuickNote.setContent(quickNoteForm.getTextArea().getText());
            tQuickNote.setCreateTime(now);
            tQuickNote.setModifiedTime(now);

            quickNoteMapper.insert(tQuickNote);
        } else {
            TQuickNote tQuickNote = new TQuickNote();
            tQuickNote.setName(quickNoteName);
            tQuickNote.setContent(quickNoteForm.getTextArea().getText());
            tQuickNote.setModifiedTime(now);

            quickNoteMapper.updateByName(tQuickNote);
        }
    }
}

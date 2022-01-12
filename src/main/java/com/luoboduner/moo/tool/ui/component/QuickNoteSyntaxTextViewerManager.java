package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.dao.TQuickNoteMapper;
import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import com.luoboduner.moo.tool.util.MybatisUtil;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义普通纯文本视图管理器
 */
public class QuickNoteSyntaxTextViewerManager {

    private Map<String, RTextScrollPane> viewMap = new HashMap<>();

    private static TQuickNoteMapper quickNoteMapper = MybatisUtil.getSqlSession().getMapper(TQuickNoteMapper.class);

    /**
     * 按名称获取一个实例，若不存在则新建
     *
     * @return
     */
    public RTextScrollPane getSyntaxTextViewer(String name) {
        RTextScrollPane rTextScrollPane = viewMap.get(name);
        if (rTextScrollPane == null) {
            QuickNoteSyntaxTextViewer plainTextViewer = new QuickNoteSyntaxTextViewer();
            TQuickNote tQuickNote = quickNoteMapper.selectByName(name);
            plainTextViewer.setText(tQuickNote.getContent());
            plainTextViewer.setCaretPosition(0);

            rTextScrollPane = new RTextScrollPane(plainTextViewer);
            rTextScrollPane.setMaximumSize(new Dimension(-1, -1));
            rTextScrollPane.setMinimumSize(new Dimension(-1, -1));

            Color defaultBackground = App.mainFrame.getBackground();
            Color defaultForeground = QuickNoteForm.getInstance().getFindTextField().getForeground();

            Gutter gutter = rTextScrollPane.getGutter();
            gutter.setBorderColor(defaultBackground);
            Font font = new Font(App.config.getFont(), Font.PLAIN, App.config.getFontSize());
            gutter.setLineNumberFont(font);
//            gutter.setLineNumberColor(defaultBackground);
            gutter.setFoldBackground(defaultBackground);
            gutter.setArmedFoldBackground(defaultBackground);

            viewMap.put(name, rTextScrollPane);
        }
        return rTextScrollPane;
    }

}

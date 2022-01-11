package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.App;
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

    /**
     * 创建一个新实例，按名称放入map
     *
     * @return
     */
    public RTextScrollPane newPlainTextViewer(String name) {
        QuickNoteSyntaxTextViewer plainTextViewer = new QuickNoteSyntaxTextViewer();
        RTextScrollPane rTextScrollPane = new RTextScrollPane(plainTextViewer);
        rTextScrollPane.setMaximumSize(new Dimension(-1, -1));
        rTextScrollPane.setMinimumSize(new Dimension(-1, -1));

        Gutter gutter = rTextScrollPane.getGutter();
        gutter.setBorderColor(App.mainFrame.getBackground());
        Font font = new Font(App.config.getFont(), Font.PLAIN, App.config.getFontSize());
        gutter.setLineNumberFont(font);

        viewMap.put(name, rTextScrollPane);
        return rTextScrollPane;
    }

    /**
     * 按名称获取一个实例，若不存在则新建
     *
     * @return
     */
    public RTextScrollPane getSyntaxTextViewer(String name) {
        RTextScrollPane rTextScrollPane = viewMap.get(name);
        if (rTextScrollPane == null) {
            QuickNoteSyntaxTextViewer plainTextViewer = new QuickNoteSyntaxTextViewer();
            rTextScrollPane = new RTextScrollPane(plainTextViewer);
            rTextScrollPane.setMaximumSize(new Dimension(-1, -1));
            rTextScrollPane.setMinimumSize(new Dimension(-1, -1));

            Gutter gutter = rTextScrollPane.getGutter();
            gutter.setBorderColor(App.mainFrame.getBackground());
            Font font = new Font(App.config.getFont(), Font.PLAIN, App.config.getFontSize());
            gutter.setLineNumberFont(font);

            viewMap.put(name, rTextScrollPane);
        }
        return rTextScrollPane;
    }

}

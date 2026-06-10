package com.luoboduner.moo.tool.ui.frame;

import com.luoboduner.moo.tool.ui.form.func.FindResultForm;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * 搜索结果展示frame
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/10/20.
 */
public class FindResultFrame extends JFrame {

    private static final long serialVersionUID = 5950950940687769444L;

    private static FindResultFrame findResultFrame;

    public void init() {
        applyI18n();
        FrameUtil.setFrameIcon(this);

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.72, 0.68);
    }

    private void applyI18n() {
        String title = I18n.get("frame.findResult.title");
        setName(title);
        setTitle(title);
    }

    private static void applyI18nStatic() {
        if (findResultFrame != null) {
            findResultFrame.applyI18n();
        }
    }

    public static FindResultFrame getInstance() {
        if (findResultFrame == null) {
            findResultFrame = new FindResultFrame();
            findResultFrame.init();
            I18nUiUtil.register(FindResultFrame::applyI18nStatic);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (screenSize.getWidth() <= 1366) {
                // 低分辨率下自动最大化窗口
                findResultFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            findResultFrame.setContentPane(FindResultForm.getInstance().getFindResultPanel());
            findResultFrame.pack();
        }

        return findResultFrame;
    }

    public static void showResultWindow() {
        getInstance().setVisible(true);
    }
}

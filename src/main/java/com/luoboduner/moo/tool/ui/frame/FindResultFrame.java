package com.luoboduner.moo.tool.ui.frame;

import com.apple.eawt.Application;
import com.luoboduner.moo.tool.ui.UiConsts;
import com.luoboduner.moo.tool.ui.form.func.FindResultForm;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.SystemUtil;

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
        String title = "查找结果";
        this.setName(title);
        this.setTitle(title);
        FrameUtil.setFrameIcon(this);
        // Mac系统Dock图标
        if (SystemUtil.isMacOs()) {
            Application application = Application.getApplication();
            application.setDockIconImage(UiConsts.IMAGE_LOGO_1024);
        }

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.72, 0.68);
    }

    public static FindResultFrame getInstance() {
        if (findResultFrame == null) {
            findResultFrame = new FindResultFrame();
            findResultFrame.init();
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

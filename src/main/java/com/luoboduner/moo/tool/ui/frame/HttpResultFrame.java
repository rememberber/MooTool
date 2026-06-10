package com.luoboduner.moo.tool.ui.frame;

import com.luoboduner.moo.tool.ui.form.func.HttpResultForm;
import com.luoboduner.moo.tool.util.ComponentUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * Http请求响应结果展示frame
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/7/19.
 */
public class HttpResultFrame extends JFrame {

    private static final long serialVersionUID = 5950950940687769444L;

    private static HttpResultFrame httpResultFrame;

    public void init() {
        applyI18n();
        FrameUtil.setFrameIcon(this);

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.6, 0.66);
    }

    private void applyI18n() {
        String title = I18n.get("frame.httpResult.title");
        setName(title);
        setTitle(title);
    }

    private static void applyI18nStatic() {
        if (httpResultFrame != null) {
            httpResultFrame.applyI18n();
        }
    }

    public static HttpResultFrame getInstance() {
        if (httpResultFrame == null) {
            httpResultFrame = new HttpResultFrame();
            httpResultFrame.init();
            I18nUiUtil.register(HttpResultFrame::applyI18nStatic);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (screenSize.getWidth() <= 1366) {
                // 低分辨率下自动最大化窗口
                httpResultFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            httpResultFrame.setContentPane(HttpResultForm.getInstance().getHttpResultPanel());
            httpResultFrame.pack();
        }

        return httpResultFrame;
    }

    public static void showResultWindow() {
        getInstance().setVisible(true);
    }
}

package com.luoboduner.moo.tool.util;

import cn.hutool.core.thread.ThreadUtil;
import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;

import javax.swing.*;
import java.awt.*;

/**
 * 随手记相关指标工具类
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/6.
 */
public class QuickNoteIndicatorTools {

    public static void showTips(String tips, TipsLevel level) {
        ThreadUtil.execute(() -> showTipsLockedMethod(tips, level));
    }

    private synchronized static void showTipsLockedMethod(String tips, TipsLevel level) {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        JPanel indicatorPanel = quickNoteForm.getIndicatorPanel();
        JLabel tipsLabel = quickNoteForm.getTipsLabel();
        if (TipsLevel.SUCCESS.equals(level)) {
            indicatorPanel.setVisible(true);
            tipsLabel.setForeground(TipsLevel.successColor());
            tipsLabel.setText(tips);
        }

        ThreadUtil.safeSleep(1000);
        tipsLabel.setText("");
        indicatorPanel.setVisible(false);
    }

    public static enum TipsLevel {
        INFO,
        SUCCESS,
        WARN,
        ERROR;

        public static Color successColor() {
            return new Color(100, 197, 126);
        }
    }
}

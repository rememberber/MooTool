package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.ui.form.func.QuickNoteForm;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

/**
 * 随手记相关指标工具类
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/6.
 */
public class QuickNoteIndicatorTools {

    public static void showTips(String tips, TipsLevel level) {
        showTipsLockedMethod(tips, level);
    }

    private static void showTipsLockedMethod(String tips, TipsLevel level) {
        QuickNoteForm quickNoteForm = QuickNoteForm.getInstance();
        JLabel tipsLabel = quickNoteForm.getTipsLabel();
        if (TipsLevel.SUCCESS.equals(level)) {
            tipsLabel.setForeground(TipsLevel.successColor());
        }
        StringBuffer tipsBuffer = new StringBuffer();
        tipsBuffer.append(tips);
        tipsBuffer.append(" (").append(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS")).append(")");
        tipsLabel.setText(tipsBuffer.toString());
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

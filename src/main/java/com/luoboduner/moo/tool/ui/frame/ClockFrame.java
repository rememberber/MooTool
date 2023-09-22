package com.luoboduner.moo.tool.ui.frame;

import cn.hutool.core.thread.ThreadUtil;
import com.luoboduner.moo.tool.ui.form.func.ClockForm;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

/**
 * <pre>
 * 大屏时钟展示frame
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2023/9/21
 */
public class ClockFrame extends JFrame {

    private static final long serialVersionUID = 5950950940687769444L;

    private ClockForm cloockForm;

    public ClockFrame() {
        // 最大化窗口
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        cloockForm = new ClockForm();
        setContentPane(cloockForm.getMainPanel());
        this.setName("MooTool");
        this.setTitle("MooTool");
        FrameUtil.setFrameIcon(this);

        // 大号字体
//        Font font1 = cloockForm.getDateLabel().getFont();
//        cloockForm.getDateLabel().setFont(font1.deriveFont(Font.BOLD, 60f));
        cloockForm.getDateLabel().setText("");
        cloockForm.getSecondProgressBar().setMaximum(60);

        // 超大号字体
        Font font = cloockForm.getTimeLabel().getFont();
        cloockForm.getTimeLabel().setFont(font.deriveFont(Font.BOLD, 260f));

        ThreadUtil.execute(() -> {
            while (true) {
                Date now = new Date();
                cloockForm.getTimeLabel().setText(DateFormatUtils.format(now, "HH:mm"));
//                cloockForm.getDateLabel().setText(DateFormatUtils.format(now, "yyyy-MM-dd"));
                cloockForm.getSecondProgressBar().setValue(now.getSeconds());
                ThreadUtil.safeSleep(1000);
            }
        });
    }

}

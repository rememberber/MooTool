package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.ui.form.func.TimeConvertForm;
import com.luoboduner.moo.tool.util.ConsoleUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Date;

/**
 * <pre>
 * TimeConvertListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/10/15.
 */
public class TimeConvertListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        TimeConvertForm timeConvertForm = TimeConvertForm.getInstance();
        timeConvertForm.getToLocalTimeButton().addActionListener(e -> {
            toLocalTime();
        });
        timeConvertForm.getToTimestampButton().addActionListener(e -> {
            toTimestamp();
        });
        timeConvertForm.getTimestampTextField().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    toLocalTime();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        timeConvertForm.getGmtTextField().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    toTimestamp();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        timeConvertForm.getCopyCurrentGmtButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                timeConvertForm.getCopyCurrentGmtButton().setEnabled(false);
                ClipboardUtil.setStr(timeConvertForm.getCurrentGmtLabel().getText());
                JOptionPane.showMessageDialog(timeConvertForm.getTimeConvertPanel(), "已复制！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                logger.error(e1);
            } finally {
                timeConvertForm.getCopyCurrentGmtButton().setEnabled(true);
            }
        }));
        timeConvertForm.getCopyCurrentTimestampButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                timeConvertForm.getCopyCurrentTimestampButton().setEnabled(false);
                ClipboardUtil.setStr(timeConvertForm.getCurrentTimestampLabel().getText());
                JOptionPane.showMessageDialog(timeConvertForm.getTimeConvertPanel(), "已复制！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                logger.error(e1);
            } finally {
                timeConvertForm.getCopyCurrentTimestampButton().setEnabled(true);
            }
        }));
        timeConvertForm.getCopyGeneratedTimestampButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                timeConvertForm.getCopyGeneratedTimestampButton().setEnabled(false);
                ClipboardUtil.setStr(timeConvertForm.getTimestampTextField().getText());
                JOptionPane.showMessageDialog(timeConvertForm.getTimeConvertPanel(), "已复制！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                logger.error(e1);
            } finally {
                timeConvertForm.getCopyGeneratedTimestampButton().setEnabled(true);
            }
        }));
        timeConvertForm.getCopyGeneratedLocalTimeButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                timeConvertForm.getCopyGeneratedLocalTimeButton().setEnabled(false);
                ClipboardUtil.setStr(timeConvertForm.getGmtTextField().getText());
                JOptionPane.showMessageDialog(timeConvertForm.getTimeConvertPanel(), "已复制！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                logger.error(e1);
            } finally {
                timeConvertForm.getCopyGeneratedLocalTimeButton().setEnabled(true);
            }
        }));

        // 文本域按键事件
        timeConvertForm.getTimeHisTextArea().addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent arg0) {
                TimeConvertForm.saveContent();
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_S) {
                    TimeConvertForm.saveContent();
                }
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
            }
        });
    }

    private static void toTimestamp() {
        TimeConvertForm timeConvertForm = TimeConvertForm.getInstance();
        try {
            String localTime = timeConvertForm.getGmtTextField().getText();
            String unit = (String) timeConvertForm.getUnitComboBox().getSelectedItem();
            Date date = DateUtils.parseDate(localTime, TimeConvertForm.TIME_FORMAT);
            long timeStamp = date.getTime();
            if ("秒(s)".equals(unit)) {
                timeStamp = timeStamp / 1000;
            }
            timeConvertForm.getTimestampTextField().setText(String.valueOf(timeStamp));
            timeConvertForm.getTimestampTextField().grabFocus();

            output("本地时间: " + localTime + " --> 时间戳: " + timeStamp);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(ex));
            JOptionPane.showMessageDialog(timeConvertForm.getTimeConvertPanel(), ex.getMessage(), "转换失败！", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void toLocalTime() {
        TimeConvertForm timeConvertForm = TimeConvertForm.getInstance();
        try {
            long timeStamp = Long.parseLong(timeConvertForm.getTimestampTextField().getText());
            if (String.valueOf(timeStamp).length() >= 13) {
                timeConvertForm.getUnitComboBox().setSelectedItem("毫秒(ms)");
            } else {
                timeConvertForm.getUnitComboBox().setSelectedItem("秒(s)");
            }
            String unit = (String) timeConvertForm.getUnitComboBox().getSelectedItem();
            if ("秒(s)".equals(unit)) {
                timeStamp = timeStamp * 1000;
            }
            String localTime = DateFormatUtils.format(new Date(timeStamp), TimeConvertForm.TIME_FORMAT);
            timeConvertForm.getGmtTextField().setText(localTime);
            timeConvertForm.getGmtTextField().grabFocus();

            output("时间戳: " + timeStamp + " --> 本地时间: " + localTime);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(ex));
            JOptionPane.showMessageDialog(timeConvertForm.getTimeConvertPanel(), ex.getMessage(), "转换失败！", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void output(String text) {
        TimeConvertForm timeConvertForm = TimeConvertForm.getInstance();
        ConsoleUtil.consoleOnly(timeConvertForm.getTimeHisTextArea(), text);
        TimeConvertForm.saveContent();
    }
}

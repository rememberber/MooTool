package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.luoboduner.moo.tool.ui.form.func.HardwareInfoForm;
import com.luoboduner.moo.tool.util.HardwareInfoUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;

/**
 * 系统与硬件信息采集监听，按需触发，避免启动时消耗资源。
 */
public class HardwareInfoListener {

    private static final Log logger = LogFactory.get();

    private static volatile boolean loaded;

    private static volatile boolean loading;

    public static void addListeners() {
        HardwareInfoForm form = HardwareInfoForm.getInstance();
        form.getRefreshButton().addActionListener(e -> refresh(true));
    }

    public static void onTabSelected() {
        if (!loaded && !loading) {
            refresh(false);
        }
    }

    public static void refresh(boolean force) {
        if (loading) {
            return;
        }
        if (!force && loaded) {
            return;
        }

        HardwareInfoForm form = HardwareInfoForm.getInstance();
        loading = true;
        form.getRefreshButton().setEnabled(false);
        setLoadingText(form);

        ThreadUtil.execute(() -> {
            try {
                String[] sections = HardwareInfoUtil.collectAll();
                SwingUtilities.invokeLater(() -> {
                    form.getSystemTextArea().setText(sections[0]);
                    form.getCpuTextArea().setText(sections[1]);
                    form.getMemoryTextArea().setText(sections[2]);
                    form.getDiskTextArea().setText(sections[3]);
                    form.getNetworkTextArea().setText(sections[4]);
                    resetCaret(form);
                    loaded = true;
                    loading = false;
                    form.getRefreshButton().setEnabled(true);
                });
            } catch (Exception ex) {
                logger.error(ExceptionUtils.getStackTrace(ex));
                SwingUtilities.invokeLater(() -> {
                    String message = "采集失败: " + ex.getMessage();
                    form.getSystemTextArea().setText(message);
                    form.getCpuTextArea().setText(message);
                    form.getMemoryTextArea().setText(message);
                    form.getDiskTextArea().setText(message);
                    form.getNetworkTextArea().setText(message);
                    loading = false;
                    form.getRefreshButton().setEnabled(true);
                });
            }
        });
    }

    private static void setLoadingText(HardwareInfoForm form) {
        String loadingText = "正在采集硬件信息，请稍候...";
        form.getSystemTextArea().setText(loadingText);
        form.getCpuTextArea().setText(loadingText);
        form.getMemoryTextArea().setText(loadingText);
        form.getDiskTextArea().setText(loadingText);
        form.getNetworkTextArea().setText(loadingText);
    }

    private static void resetCaret(HardwareInfoForm form) {
        form.getSystemTextArea().setCaretPosition(0);
        form.getCpuTextArea().setCaretPosition(0);
        form.getMemoryTextArea().setCaretPosition(0);
        form.getDiskTextArea().setCaretPosition(0);
        form.getNetworkTextArea().setCaretPosition(0);
    }
}

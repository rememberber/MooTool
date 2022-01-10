package com.luoboduner.moo.tool.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import java.util.Date;

/**
 * <pre>
 * 控制台打印相关
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2021/12/15.
 */
@Slf4j
public class ConsoleUtil {

    private static final Log logger = LogFactory.get();

    /**
     * 输出到控制台和log
     *
     * @param log
     */
    public static void consoleWithLog(JTextArea textArea, String log) {
        textArea.append("\n\n");
        textArea.append(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS") + " ");
        textArea.append("\n");
        textArea.append(log);
        textArea.setCaretPosition(textArea.getText().length());
        logger.warn(log);
    }

    /**
     * 仅输出到控制台
     *
     * @param log
     */
    public static void consoleOnly(JTextArea textArea, String log) {
        textArea.append("\n\n");
        textArea.append(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS") + " ");
        textArea.append("\n");
        textArea.append(log);
        textArea.setCaretPosition(textArea.getText().length());
    }

}

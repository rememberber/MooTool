package com.luoboduner.moo.tool.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.text.BadLocationException;

/**
 * <pre>
 * TextArea文本域工具类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/10/30.
 */
@Slf4j
public class TextAreaUtil {

    /**
     * 删除所选行
     *
     * @param jTextArea
     */
    public static void deleteSelectedLine(JTextArea jTextArea) {
        try {
            int startLine = jTextArea.getLineOfOffset(jTextArea.getSelectionStart());
            int endLine = jTextArea.getLineOfOffset(jTextArea.getSelectionEnd());
            jTextArea.replaceRange("", jTextArea.getLineStartOffset(startLine), jTextArea.getLineEndOffset(endLine));
        } catch (BadLocationException e) {
            e.printStackTrace();
            log.error(ExceptionUtils.getStackTrace(e));
        }

    }
}

package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.URLUtil;
import com.luoboduner.moo.tool.ui.FuncConsts;
import com.luoboduner.moo.tool.ui.form.func.EnCodeForm;
import com.luoboduner.moo.tool.util.AsciiUtil;
import com.luoboduner.moo.tool.util.FuncHistoryUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * <pre>
 * EnCodeListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/10/14.
 */
public class EnCodeListener {
    public static void addListeners() {
        EnCodeForm enCodeForm = EnCodeForm.getInstance();
        enCodeForm.getNativeToUnicodeButton().addActionListener(e -> {
            String nativeText1 = enCodeForm.getNativeTextArea().getText();
            String unicodeText = UnicodeUtil.toUnicode(nativeText1);
            enCodeForm.getUnicodeTextArea().setText(unicodeText);
            enCodeForm.getUnicodeTextArea().setCaretPosition(0);
            enCodeForm.getUnicodeTextArea().grabFocus();
            saveHistory("Native/Unicode", "NativeToUnicode", nativeText1, unicodeText);
        });
        enCodeForm.getUnicodeToNativeButton().addActionListener(e -> {
            String unicodeText = enCodeForm.getUnicodeTextArea().getText();
            String nativeText1 = UnicodeUtil.toString(unicodeText);
            enCodeForm.getNativeTextArea().setText(nativeText1);
            enCodeForm.getNativeTextArea().setCaretPosition(0);
            enCodeForm.getNativeTextArea().grabFocus();
            saveHistory("Native/Unicode", "UnicodeToNative", unicodeText, nativeText1);
        });
        enCodeForm.getUrlEncodeButton().addActionListener(e -> {
            String url = enCodeForm.getUrlTextArea().getText();
            String urlEncodeCharset = (String) enCodeForm.getUrlEncodeCharsetComboBox().getSelectedItem();
            String urlEncode = URLUtil.encode(url, CharsetUtil.charset(urlEncodeCharset));
            enCodeForm.getUrlEncodeTextArea().setText(urlEncode);
            enCodeForm.getUrlEncodeTextArea().setCaretPosition(0);
            enCodeForm.getUrlEncodeTextArea().grabFocus();
            saveHistory("URL", "UrlEncode", url, urlEncode, "URL编码(" + urlEncodeCharset + ")");
        });
        enCodeForm.getUrlDecodeButton().addActionListener(e -> {
            String urlEncode = enCodeForm.getUrlEncodeTextArea().getText();
            String urlEncodeCharset = (String) enCodeForm.getUrlEncodeCharsetComboBox().getSelectedItem();
            String urlDecode = URLUtil.decode(urlEncode, urlEncodeCharset);
            enCodeForm.getUrlTextArea().setText(urlDecode);
            enCodeForm.getUrlTextArea().setCaretPosition(0);
            enCodeForm.getUrlTextArea().grabFocus();
            saveHistory("URL", "UrlDecode", urlEncode, urlDecode, "URL解码(" + urlEncodeCharset + ")");
        });
        enCodeForm.getNativeToHexButton().addActionListener(e -> {
            String nativeForHexStr = enCodeForm.getNativeForHexTextArea().getText();
            String hex = HexUtil.encodeHexStr(nativeForHexStr, CharsetUtil.CHARSET_UTF_8);
            enCodeForm.getHexTextArea().setText(hex);
            enCodeForm.getHexTextArea().setCaretPosition(0);
            enCodeForm.getHexTextArea().grabFocus();
            saveHistory("Hex", "NativeToHex", nativeForHexStr, hex);
        });
        enCodeForm.getHexToNativeButton().addActionListener(e -> {
            String hex = enCodeForm.getHexTextArea().getText();
            String nativeStr = HexUtil.decodeHexStr(hex);
            enCodeForm.getNativeForHexTextArea().setText(nativeStr);
            enCodeForm.getNativeForHexTextArea().setCaretPosition(0);
            enCodeForm.getNativeForHexTextArea().grabFocus();
            saveHistory("Hex", "HexToNative", hex, nativeStr);
        });
        enCodeForm.getNativeToAsciiButton().addActionListener(e -> {
            String nativeText = enCodeForm.getNativeForAsciiTextArea().getText();
            String format = (String) enCodeForm.getAsciiFormatComboBox().getSelectedItem();
            String ascii = AsciiUtil.toAscii(nativeText, format);
            enCodeForm.getAsciiTextArea().setText(ascii);
            enCodeForm.getAsciiTextArea().setCaretPosition(0);
            enCodeForm.getAsciiTextArea().grabFocus();
            saveHistory("ASCII", "NativeToAscii", nativeText, ascii, "ASCII编码(" + format + ")");
        });
        enCodeForm.getAsciiToNativeButton().addActionListener(e -> {
            String ascii = enCodeForm.getAsciiTextArea().getText();
            String nativeText = AsciiUtil.fromAscii(ascii);
            enCodeForm.getNativeForAsciiTextArea().setText(nativeText);
            enCodeForm.getNativeForAsciiTextArea().setCaretPosition(0);
            enCodeForm.getNativeForAsciiTextArea().grabFocus();
            saveHistory("ASCII", "AsciiToNative", ascii, nativeText);
        });
    }

    private static void saveHistory(String tab, String operation, String input, String output) {
        saveHistory(tab, operation, input, output, operation);
    }

    private static void saveHistory(String tab, String operation, String input, String output, String summary) {
        if (StringUtils.isAllBlank(input, output)) {
            return;
        }
        FuncHistoryUtil.save(FuncConsts.ENCODE, summary, input, output, tab + "|" + operation);
        if (EnCodeForm.getHistoryPanel() != null) {
            EnCodeForm.getHistoryPanel().refreshListIfVisible();
        }
    }
}

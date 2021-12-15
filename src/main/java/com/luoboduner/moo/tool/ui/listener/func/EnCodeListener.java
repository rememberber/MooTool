package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.URLUtil;
import com.luoboduner.moo.tool.ui.form.func.EnCodeForm;

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
        });
        enCodeForm.getUnicodeToNativeButton().addActionListener(e -> {
            String unicodeText = enCodeForm.getUnicodeTextArea().getText();
            String nativeText1 = UnicodeUtil.toString(unicodeText);
            enCodeForm.getNativeTextArea().setText(nativeText1);
            enCodeForm.getNativeTextArea().setCaretPosition(0);
            enCodeForm.getNativeTextArea().grabFocus();
        });
        enCodeForm.getUrlEncodeButton().addActionListener(e -> {
            String url = enCodeForm.getUrlTextArea().getText();
            String urlEncodeCharset = (String) enCodeForm.getUrlEncodeCharsetComboBox().getSelectedItem();
            String urlEncode = URLUtil.encode(url, CharsetUtil.charset(urlEncodeCharset));
            enCodeForm.getUrlEncodeTextArea().setText(urlEncode);
            enCodeForm.getUrlEncodeTextArea().setCaretPosition(0);
            enCodeForm.getUrlEncodeTextArea().grabFocus();
        });
        enCodeForm.getUrlDecodeButton().addActionListener(e -> {
            String urlEncode = enCodeForm.getUrlEncodeTextArea().getText();
            String urlEncodeCharset = (String) enCodeForm.getUrlEncodeCharsetComboBox().getSelectedItem();
            String urlDecode = URLUtil.decode(urlEncode, urlEncodeCharset);
            enCodeForm.getUrlTextArea().setText(urlDecode);
            enCodeForm.getUrlTextArea().setCaretPosition(0);
            enCodeForm.getUrlTextArea().grabFocus();
        });
        enCodeForm.getNativeToHexButton().addActionListener(e -> {
            String nativeForHexStr = enCodeForm.getNativeForHexTextArea().getText();
            String hex = HexUtil.encodeHexStr(nativeForHexStr, CharsetUtil.CHARSET_UTF_8);
            enCodeForm.getHexTextArea().setText(hex);
            enCodeForm.getHexTextArea().setCaretPosition(0);
            enCodeForm.getHexTextArea().grabFocus();
        });
        enCodeForm.getHexToNativeButton().addActionListener(e -> {
            String hex = enCodeForm.getHexTextArea().getText();
            String nativeStr = HexUtil.decodeHexStr(hex);
            enCodeForm.getNativeForHexTextArea().setText(nativeStr);
            enCodeForm.getNativeForHexTextArea().setCaretPosition(0);
            enCodeForm.getNativeForHexTextArea().grabFocus();
        });
    }
}

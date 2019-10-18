package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.URLUtil;
import com.luoboduner.moo.tool.ui.form.func.EnCodeForm;

/**
 * <pre>
 * Description
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
            enCodeForm.getUnicodeTextArea().grabFocus();
        });
        enCodeForm.getUnicodeToNativeButton().addActionListener(e -> {
            String unicodeText = enCodeForm.getUnicodeTextArea().getText();
            String nativeText1 = UnicodeUtil.toString(unicodeText);
            enCodeForm.getNativeTextArea().setText(nativeText1);
            enCodeForm.getNativeTextArea().grabFocus();
        });
        enCodeForm.getUrlEncodeButton().addActionListener(e -> {
            String url = enCodeForm.getUrlTextArea().getText();
            String urlEncodeCharset = (String) enCodeForm.getUrlEncodeCharsetComboBox().getSelectedItem();
            String urlEncode = URLUtil.encode(url, urlEncodeCharset);
            enCodeForm.getUrlEncodeTextArea().setText(urlEncode);
            enCodeForm.getUrlEncodeTextArea().grabFocus();
        });
        enCodeForm.getUrlDecodeButton().addActionListener(e -> {
            String urlEncode = enCodeForm.getUrlEncodeTextArea().getText();
            String urlEncodeCharset = (String) enCodeForm.getUrlEncodeCharsetComboBox().getSelectedItem();
            String urlDecode = URLUtil.decode(urlEncode, urlEncodeCharset);
            enCodeForm.getUrlTextArea().setText(urlDecode);
            enCodeForm.getUrlTextArea().grabFocus();
        });
    }
}

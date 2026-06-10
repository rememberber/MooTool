package com.luoboduner.moo.tool.ui.form.func;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.domain.TFuncHistory;
import com.luoboduner.moo.tool.ui.FuncConsts;
import com.luoboduner.moo.tool.ui.Style;
import com.luoboduner.moo.tool.ui.component.FuncHistoryPanel;
import com.luoboduner.moo.tool.ui.listener.func.EnCodeListener;
import com.luoboduner.moo.tool.util.FuncHistorySupport;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.I18nUiUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import org.apache.commons.lang3.StringUtils;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * 编码转换
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/9/9.
 */
@Getter
public class EnCodeForm {
    private JTabbedPane tabbedPane1;
    private JPanel enCodePanel;
    private JTextArea nativeTextArea;
    private JTextArea unicodeTextArea;
    private JButton nativeToUnicodeButton;
    private JButton unicodeToNativeButton;
    private JTextArea urlTextArea;
    private JTextArea urlEncodeTextArea;
    private JButton urlEncodeButton;
    private JButton urlDecodeButton;
    private JComboBox urlEncodeCharsetComboBox;
    private JTextArea nativeForHexTextArea;
    private JTextArea hexTextArea;
    private JButton nativeToHexButton;
    private JButton hexToNativeButton;
    private JTextArea nativeForAsciiTextArea;
    private JTextArea asciiTextArea;
    private JButton nativeToAsciiButton;
    private JButton asciiToNativeButton;
    private JComboBox asciiFormatComboBox;

    private static EnCodeForm enCodeForm;

    private static boolean i18nRegistered;

    private static FuncHistoryPanel historyPanel;

    private static final String[] ASCII_FORMAT_KEYS = {"encode.asciiDecimal", "encode.asciiHex"};

    private EnCodeForm() {
        UndoUtil.register(this);
    }

    public static EnCodeForm getInstance() {
        if (enCodeForm == null) {
            enCodeForm = new EnCodeForm();
        }
        return enCodeForm;
    }

    public static void init() {
        enCodeForm = getInstance();
        initUi();
        historyPanel = FuncHistorySupport.attachTab(
                enCodeForm.getTabbedPane1(), FuncConsts.ENCODE, EnCodeForm::applyHistory);
        EnCodeListener.addListeners();

        enCodeForm.applyI18n();
        if (!i18nRegistered) {
            I18nUiUtil.register(EnCodeForm::applyI18nStatic);
            i18nRegistered = true;
        }
    }

    private void applyI18n() {
        I18nUiUtil.setTabTitle(tabbedPane1, 0, "encode.tab.nativeUnicode");
        I18nUiUtil.setTabTitle(tabbedPane1, 1, "encode.tab.url");
        I18nUiUtil.setTabTitle(tabbedPane1, 2, "encode.tab.hex");
        I18nUiUtil.setTabTitle(tabbedPane1, 3, "encode.tab.ascii");
        I18nUiUtil.setText(nativeToUnicodeButton, "encode.nativeToUnicode");
        I18nUiUtil.setText(unicodeToNativeButton, "encode.unicodeToNative");
        I18nUiUtil.setText(urlEncodeButton, "encode.urlEncode");
        I18nUiUtil.setText(urlDecodeButton, "encode.urlDecode");
        I18nUiUtil.setText(nativeToHexButton, "encode.nativeToHex");
        I18nUiUtil.setText(hexToNativeButton, "encode.hexToNative");
        I18nUiUtil.setText(nativeToAsciiButton, "encode.nativeToAscii");
        I18nUiUtil.setText(asciiToNativeButton, "encode.asciiToNative");
        asciiFormatComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index >= 0 && index < ASCII_FORMAT_KEYS.length) {
                    setText(I18n.get(ASCII_FORMAT_KEYS[index]));
                }
                return this;
            }
        });
    }

    private static void applyI18nStatic() {
        if (enCodeForm != null) {
            enCodeForm.applyI18n();
        }
    }

    public static FuncHistoryPanel getHistoryPanel() {
        return historyPanel;
    }

    public static void applyHistory(TFuncHistory history) {
        if (history == null || StringUtils.isBlank(history.getExtraData())) {
            return;
        }
        String[] parts = history.getExtraData().split("\\|", 2);
        if (parts.length < 2) {
            return;
        }
        String tab = parts[0];
        String operation = parts[1];
        for (int i = 0; i < enCodeForm.getTabbedPane1().getTabCount(); i++) {
            if (tab.equals(enCodeForm.getTabbedPane1().getTitleAt(i))) {
                enCodeForm.getTabbedPane1().setSelectedIndex(i);
                break;
            }
        }
        switch (operation) {
            case "NativeToUnicode" -> {
                enCodeForm.getNativeTextArea().setText(history.getInputText());
                enCodeForm.getUnicodeTextArea().setText(history.getOutputText());
            }
            case "UnicodeToNative" -> {
                enCodeForm.getUnicodeTextArea().setText(history.getInputText());
                enCodeForm.getNativeTextArea().setText(history.getOutputText());
            }
            case "UrlEncode" -> {
                enCodeForm.getUrlTextArea().setText(history.getInputText());
                enCodeForm.getUrlEncodeTextArea().setText(history.getOutputText());
            }
            case "UrlDecode" -> {
                enCodeForm.getUrlEncodeTextArea().setText(history.getInputText());
                enCodeForm.getUrlTextArea().setText(history.getOutputText());
            }
            case "NativeToHex" -> {
                enCodeForm.getNativeForHexTextArea().setText(history.getInputText());
                enCodeForm.getHexTextArea().setText(history.getOutputText());
            }
            case "HexToNative" -> {
                enCodeForm.getHexTextArea().setText(history.getInputText());
                enCodeForm.getNativeForHexTextArea().setText(history.getOutputText());
            }
            case "NativeToAscii" -> {
                enCodeForm.getNativeForAsciiTextArea().setText(history.getInputText());
                enCodeForm.getAsciiTextArea().setText(history.getOutputText());
            }
            case "AsciiToNative" -> {
                enCodeForm.getAsciiTextArea().setText(history.getInputText());
                enCodeForm.getNativeForAsciiTextArea().setText(history.getOutputText());
            }
            default -> {
            }
        }
    }

    private static void initUi() {
        Style.blackTextArea(enCodeForm.getNativeTextArea());
        Style.blackTextArea(enCodeForm.getUnicodeTextArea());
        Style.blackTextArea(enCodeForm.getUrlTextArea());
        Style.blackTextArea(enCodeForm.getUrlEncodeTextArea());
        Style.blackTextArea(enCodeForm.getNativeForHexTextArea());
        Style.blackTextArea(enCodeForm.getHexTextArea());
        Style.blackTextArea(enCodeForm.getNativeForAsciiTextArea());
        Style.blackTextArea(enCodeForm.getAsciiTextArea());

        enCodeForm.getUrlEncodeButton().setIcon(new FlatSVGIcon("icon/right_arrow.svg"));
        enCodeForm.getUrlDecodeButton().setIcon(new FlatSVGIcon("icon/left_arrow.svg"));
        enCodeForm.getEnCodePanel().updateUI();
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        enCodePanel = new JPanel();
        enCodePanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        enCodePanel.setMinimumSize(new Dimension(400, 300));
        enCodePanel.setPreferredSize(new Dimension(400, 300));
        tabbedPane1 = new JTabbedPane();
        enCodePanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("Native/Unicode", panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        nativeToUnicodeButton = new JButton();
        nativeToUnicodeButton.setText("Native --> Unicode");
        panel2.add(nativeToUnicodeButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        unicodeToNativeButton = new JButton();
        unicodeToNativeButton.setText("Native <-- Unicode");
        panel2.add(unicodeToNativeButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(31);
        panel3.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        nativeTextArea = new JTextArea();
        nativeTextArea.setLineWrap(true);
        nativeTextArea.setWrapStyleWord(true);
        scrollPane1.setViewportView(nativeTextArea);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setHorizontalScrollBarPolicy(31);
        panel4.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        unicodeTextArea = new JTextArea();
        unicodeTextArea.setLineWrap(true);
        unicodeTextArea.setWrapStyleWord(true);
        scrollPane2.setViewportView(unicodeTextArea);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("URL转码", panel5);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        urlEncodeButton = new JButton();
        urlEncodeButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-right.png")));
        urlEncodeButton.setText("Encode编码");
        panel6.add(urlEncodeButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlDecodeButton = new JButton();
        urlDecodeButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow-left.png")));
        urlDecodeButton.setText("Decode解码");
        panel6.add(urlDecodeButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel6.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel6.add(spacer4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        urlEncodeCharsetComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("utf-8");
        defaultComboBoxModel1.addElement("gb2312");
        urlEncodeCharsetComboBox.setModel(defaultComboBoxModel1);
        panel6.add(urlEncodeCharsetComboBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel5.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        urlTextArea = new JTextArea();
        scrollPane3.setViewportView(urlTextArea);
        final JScrollPane scrollPane4 = new JScrollPane();
        panel5.add(scrollPane4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        urlEncodeTextArea = new JTextArea();
        scrollPane4.setViewportView(urlEncodeTextArea);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("Native/16进制", panel7);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane5 = new JScrollPane();
        panel8.add(scrollPane5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        nativeForHexTextArea = new JTextArea();
        scrollPane5.setViewportView(nativeForHexTextArea);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel9, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane6 = new JScrollPane();
        panel9.add(scrollPane6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        hexTextArea = new JTextArea();
        scrollPane6.setViewportView(hexTextArea);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel10, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        nativeToHexButton = new JButton();
        nativeToHexButton.setText("Native --> Hex");
        panel10.add(nativeToHexButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel10.add(spacer5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel10.add(spacer6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        hexToNativeButton = new JButton();
        hexToNativeButton.setText("Native <-- Hex");
        panel10.add(hexToNativeButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("Native/ASCII", panel11);
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel11.add(panel12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane7 = new JScrollPane();
        panel12.add(scrollPane7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        nativeForAsciiTextArea = new JTextArea();
        scrollPane7.setViewportView(nativeForAsciiTextArea);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel11.add(panel13, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane8 = new JScrollPane();
        panel13.add(scrollPane8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        asciiTextArea = new JTextArea();
        scrollPane8.setViewportView(asciiTextArea);
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel11.add(panel14, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        nativeToAsciiButton = new JButton();
        nativeToAsciiButton.setText("Native --> ASCII");
        panel14.add(nativeToAsciiButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        asciiToNativeButton = new JButton();
        asciiToNativeButton.setText("Native <-- ASCII");
        panel14.add(asciiToNativeButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        asciiFormatComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("十进制");
        defaultComboBoxModel2.addElement("十六进制");
        asciiFormatComboBox.setModel(defaultComboBoxModel2);
        panel14.add(asciiFormatComboBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel14.add(spacer7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        panel14.add(spacer8, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return enCodePanel;
    }

}

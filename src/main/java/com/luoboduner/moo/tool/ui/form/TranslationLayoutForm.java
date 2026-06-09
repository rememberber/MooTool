package com.luoboduner.moo.tool.ui.form;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.util.AlertUtil;
import com.luoboduner.moo.tool.util.ConfigUtil;
import com.luoboduner.moo.tool.util.UndoUtil;
import com.luoboduner.moo.tool.util.translator.Translator;
import com.luoboduner.moo.tool.util.translator.TranslatorFactory;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class TranslationLayoutForm {
    private JTextArea textArea1;
    private JTextArea textArea2;
    private JPanel mainLayoutPanel;
    private JSplitPane splitPane;
    private JPanel leftMenuPanel;

    private JToolBar leftMenuToolBar;
    private JComboBox<String> comboBox1;
    private JButton exchangeButton;
    private JComboBox<String> comboBox2;
    private JComboBox<String> translatorComboBox;
    private JButton copyButton;
    private JButton clearButton;

    private static final AtomicInteger changeCount = new AtomicInteger(0);

    private static final String TRANSLATOR_GOOGLE = "Google翻译";
    private static final String TRANSLATOR_BING = "Bing翻译";

    public TranslationLayoutForm() {
        exchangeButton = new JButton();
        exchangeButton.setIcon(new FlatSVGIcon("icon/exchange.svg"));
        exchangeButton.setToolTipText("交换语言与文本");

        comboBox1 = new JComboBox<>(Translator.getSourceLanguageNames());
        comboBox2 = new JComboBox<>(Translator.getTargetLanguageNames());

        String savedSource = ConfigUtil.getInstance().getTranslationSourceLanguage();
        if (containsItem(comboBox1, savedSource)) {
            comboBox1.setSelectedItem(savedSource);
        }
        String savedTarget = ConfigUtil.getInstance().getTranslationTargetLanguage();
        if (containsItem(comboBox2, savedTarget)) {
            comboBox2.setSelectedItem(savedTarget);
        }

        translatorComboBox = new JComboBox<>();
        final DefaultComboBoxModel<String> translatorComboBoxModel = new DefaultComboBoxModel<>();
        translatorComboBoxModel.addElement(TRANSLATOR_GOOGLE);
        translatorComboBoxModel.addElement(TRANSLATOR_BING);
        translatorComboBox.setModel(translatorComboBoxModel);
        translatorComboBox.setToolTipText("选择翻译源，失败时自动尝试备用翻译源");
        String savedTranslator = ConfigUtil.getInstance().getTranslatorType();
        if ("BING".equals(savedTranslator)) {
            translatorComboBox.setSelectedItem(TRANSLATOR_BING);
        } else {
            translatorComboBox.setSelectedItem(TRANSLATOR_GOOGLE);
        }

        copyButton = new JButton(new FlatSVGIcon("icon/copy.svg"));
        copyButton.setToolTipText("复制译文");
        clearButton = new JButton(new FlatSVGIcon("icon/remove.svg"));
        clearButton.setToolTipText("清空");

        leftMenuToolBar = new JToolBar();
        leftMenuToolBar.add(comboBox1);
        leftMenuToolBar.add(exchangeButton);
        leftMenuToolBar.add(comboBox2);
        leftMenuToolBar.addSeparator();
        leftMenuToolBar.add(new JLabel("翻译源: "));
        leftMenuToolBar.add(translatorComboBox);
        leftMenuToolBar.addSeparator();
        leftMenuToolBar.add(copyButton);
        leftMenuToolBar.add(clearButton);

        leftMenuPanel.add(leftMenuToolBar);

        UndoUtil.register(this);

        textArea1.setLineWrap(true);
        textArea2.setLineWrap(true);
        textArea2.setEditable(false);

        addListeners();
    }

    private static boolean containsItem(JComboBox<String> comboBox, String item) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (item.equals(comboBox.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    public void addListeners() {
        exchangeButton.addActionListener(e -> {
            Object from = comboBox1.getSelectedItem();
            Object to = comboBox2.getSelectedItem();
            if (Translator.AUTO_DETECT.equals(from)) {
                comboBox1.setSelectedItem(to);
            } else {
                comboBox1.setSelectedItem(to);
                comboBox2.setSelectedItem(from);
            }

            String sourceText = textArea1.getText();
            String targetText = textArea2.getText();
            textArea1.setText(targetText);
            textArea2.setText(sourceText);

            saveLanguagePreferences();
            translateControl();
        });

        comboBox1.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                preventSameLanguageSelection();
                saveLanguagePreferences();
                translateControl();
            }
        });

        comboBox2.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                preventSameLanguageSelection();
                saveLanguagePreferences();
                translateControl();
            }
        });

        translatorComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String itemName = e.getItem().toString();
                String translatorType = TRANSLATOR_BING.equals(itemName) ? "BING" : "GOOGLE";
                ConfigUtil.getInstance().setTranslatorType(translatorType);
                if (!StringUtils.isEmpty(textArea1.getText())) {
                    translateControl();
                }
            }
        });

        copyButton.addActionListener(e -> {
            String result = textArea2.getText();
            if (StringUtils.isNotBlank(result)) {
                ClipboardUtil.setStr(result);
                AlertUtil.buttonInfo(copyButton, "", "已复制", 1500);
            }
        });

        clearButton.addActionListener(e -> {
            textArea1.setText("");
            textArea2.setText("");
        });

        textArea1.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                translateControl();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                translateControl();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    private void preventSameLanguageSelection() {
        String sourceLanguage = String.valueOf(comboBox1.getSelectedItem());
        String targetLanguage = String.valueOf(comboBox2.getSelectedItem());
        if (!Translator.AUTO_DETECT.equals(sourceLanguage) && sourceLanguage.equals(targetLanguage)) {
            if ("中文（简体）".equals(targetLanguage)) {
                comboBox2.setSelectedItem("英语");
            } else {
                comboBox2.setSelectedItem("中文（简体）");
            }
        }
    }

    private void saveLanguagePreferences() {
        ConfigUtil.getInstance().setTranslationSourceLanguage(String.valueOf(comboBox1.getSelectedItem()));
        ConfigUtil.getInstance().setTranslationTargetLanguage(String.valueOf(comboBox2.getSelectedItem()));
    }

    public void translateControl() {
        changeCount.incrementAndGet();
        SwingUtilities.invokeLater(() -> {
            if (!StringUtils.isEmpty(textArea1.getText())) {
                textArea2.setText("翻译中...");
            }
        });

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            changeCount.decrementAndGet();
            if (changeCount.get() == 0) {
                translate();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void translate() {
        String sourceLanguage = String.valueOf(comboBox1.getSelectedItem());
        String targetLanguage = String.valueOf(comboBox2.getSelectedItem());
        String text = textArea1.getText();

        if (StringUtils.isEmpty(text)) {
            SwingUtilities.invokeLater(() -> textArea2.setText(""));
            return;
        }

        TranslatorFactory.TranslatorType translatorType = TranslatorFactory.parseType(
                ConfigUtil.getInstance().getTranslatorType());

        String sourceLangCode = Translator.resolveLanguageCode(sourceLanguage, "auto");
        String targetLangCode = Translator.resolveLanguageCode(targetLanguage, "zh-CN");

        String result = TranslatorFactory.translate(text, sourceLangCode, targetLangCode, translatorType);

        SwingUtilities.invokeLater(() -> textArea2.setText(result));
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
        mainLayoutPanel = new JPanel();
        mainLayoutPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainLayoutPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        leftMenuPanel = new JPanel();
        leftMenuPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel1.add(leftMenuPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(224);
        mainLayoutPanel.add(splitPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setLeftComponent(panel2);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textArea1 = new JTextArea();
        scrollPane1.setViewportView(textArea1);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel3);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel3.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textArea2 = new JTextArea();
        scrollPane2.setViewportView(textArea2);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainLayoutPanel;
    }

}

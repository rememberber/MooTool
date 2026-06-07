package com.luoboduner.moo.tool.ui.form;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
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
    private JComboBox comboBox1;
    private JButton exchangeButton;
    private JComboBox comboBox2;
    private JComboBox translatorComboBox;

    private static AtomicInteger changeCount = new AtomicInteger(0);

    // Constants for translator names
    private static final String TRANSLATOR_GOOGLE = "Google翻译";
    private static final String TRANSLATOR_BING = "Bing翻译";
    private static final String TRANSLATOR_MICROSOFT = "微软翻译";

    public TranslationLayoutForm() {
        exchangeButton = new JButton();
        exchangeButton.setIcon(new FlatSVGIcon("icon/exchange.svg"));

        comboBox1 = new JComboBox();
        comboBox1 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("自动检测");
        defaultComboBoxModel1.addElement("中文（简体）");
        defaultComboBoxModel1.addElement("英语");
        comboBox1.setModel(defaultComboBoxModel1);

        comboBox2 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("中文（简体）");
        defaultComboBoxModel2.addElement("英语");
        comboBox2.setModel(defaultComboBoxModel2);

        translatorComboBox = new JComboBox();
        final DefaultComboBoxModel translatorComboBoxModel = new DefaultComboBoxModel();
        translatorComboBoxModel.addElement(TRANSLATOR_GOOGLE);
        translatorComboBoxModel.addElement(TRANSLATOR_BING);
        translatorComboBoxModel.addElement(TRANSLATOR_MICROSOFT);
        translatorComboBox.setModel(translatorComboBoxModel);
        translatorComboBox.setToolTipText("选择翻译源。注意：微软翻译暂时回退到Google翻译");
        // Load saved translator preference
        String savedTranslator = ConfigUtil.getInstance().getTranslatorType();
        if ("MICROSOFT".equals(savedTranslator)) {
            translatorComboBox.setSelectedItem(TRANSLATOR_MICROSOFT);
        } else if ("BING".equals(savedTranslator)) {
            translatorComboBox.setSelectedItem(TRANSLATOR_BING);
        } else {
            translatorComboBox.setSelectedItem(TRANSLATOR_GOOGLE);
        }

        leftMenuToolBar = new JToolBar();
        leftMenuToolBar.add(comboBox1);
        leftMenuToolBar.add(exchangeButton);
        leftMenuToolBar.add(comboBox2);
        leftMenuToolBar.addSeparator();
        leftMenuToolBar.add(new JLabel("翻译源: "));
        leftMenuToolBar.add(translatorComboBox);

        leftMenuPanel.add(leftMenuToolBar);

        UndoUtil.register(this);

        textArea1.setLineWrap(true);
        textArea2.setLineWrap(true);

        addListeners();
    }

    public void addListeners() {
        exchangeButton.addActionListener(e -> {
            String from = comboBox1.getSelectedItem().toString();
            String to = comboBox2.getSelectedItem().toString();
            if ("自动检测".equals(from)) {
                if ("中文（简体）".equals(to)) {
                    comboBox1.setSelectedItem("中文（简体）");
                    comboBox2.setSelectedItem("英语");
                } else {
                    comboBox1.setSelectedItem("中文（简体）");
                    comboBox2.setSelectedItem("英语");
                }
            } else {
                comboBox1.setSelectedItem(to);
                comboBox2.setSelectedItem(from);
            }

            translateControl();
        });

        comboBox1.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String itemName = e.getItem().toString();
                if ("自动检测".equals(itemName)) {
                    comboBox2.setSelectedItem("中文（简体）");
                } else {
                    if ("中文（简体）".equals(itemName)) {
                        comboBox2.setSelectedItem("英语");
                    } else {
                        comboBox2.setSelectedItem("中文（简体）");
                    }
                }
                translateControl();
            }
        });

        comboBox2.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String itemName = e.getItem().toString();
                if ("自动检测".equals(itemName)) {
                    comboBox1.setSelectedItem("中文（简体）");
                } else {
                    if ("中文（简体）".equals(itemName)) {
                        comboBox1.setSelectedItem("英语");
                    } else {
                        comboBox1.setSelectedItem("中文（简体）");
                    }
                }
                translateControl();
            }
        });

        translatorComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String itemName = e.getItem().toString();
                String translatorType;
                if (TRANSLATOR_GOOGLE.equals(itemName)) {
                    translatorType = "GOOGLE";
                } else if (TRANSLATOR_BING.equals(itemName)) {
                    translatorType = "BING";
                } else {
                    translatorType = "MICROSOFT";
                }
                ConfigUtil.getInstance().setTranslatorType(translatorType);
                // Only translate if there's actual text to translate
                if (!StringUtils.isEmpty(textArea1.getText())) {
                    translateControl();
                }
            }
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

    public void translateControl() {
        Thread thread = new Thread(() -> {
            changeCount.incrementAndGet();
            textArea2.setText("^_^……");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            changeCount.decrementAndGet();
            if (changeCount.get() == 0) {
                translate();
            }
        });
        thread.start();
    }

    private void translate() {
        String sourceLanguage = comboBox1.getSelectedItem().toString();
        String targetLanguage = comboBox2.getSelectedItem().toString();
        String text = textArea1.getText();

        // Skip translation if text is empty
        if (StringUtils.isEmpty(text)) {
            textArea2.setText("");
            return;
        }

        // Get the selected translator type from config
        String translatorTypeStr = ConfigUtil.getInstance().getTranslatorType();
        TranslatorFactory.TranslatorType translatorType = TranslatorFactory.TranslatorType.GOOGLE;
        try {
            translatorType = TranslatorFactory.TranslatorType.valueOf(translatorTypeStr);
        } catch (IllegalArgumentException e) {
            // Default to GOOGLE if invalid
            translatorType = TranslatorFactory.TranslatorType.GOOGLE;
        }

        // Get language codes, with fallback for null values
        String sourceLangCode = Translator.languageNameToCodeMap.get(sourceLanguage);
        String targetLangCode = Translator.languageNameToCodeMap.get(targetLanguage);
        
        if (sourceLangCode == null) {
            sourceLangCode = "auto"; // Default to auto-detect
        }
        if (targetLangCode == null) {
            targetLangCode = "zh-CN"; // Default to Simplified Chinese
        }

        String result = TranslatorFactory.getTranslator(translatorType).translate(text, sourceLangCode, targetLangCode);

        textArea2.setText(result);
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

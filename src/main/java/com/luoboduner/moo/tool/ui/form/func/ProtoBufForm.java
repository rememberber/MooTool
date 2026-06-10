package com.luoboduner.moo.tool.ui.form.func;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.luoboduner.moo.tool.domain.TFuncHistory;
import com.luoboduner.moo.tool.ui.FuncConsts;
import com.luoboduner.moo.tool.ui.Style;
import com.luoboduner.moo.tool.ui.component.FuncHistoryPanel;
import com.luoboduner.moo.tool.ui.listener.func.ProtoBufListener;
import com.luoboduner.moo.tool.util.FuncHistorySupport;
import com.luoboduner.moo.tool.util.UndoUtil;
import org.apache.commons.lang3.StringUtils;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * Protobuf 工具
 */
@Getter
public class ProtoBufForm {
    private JTabbedPane tabbedPane1;
    private JPanel protoBufPanel;
    private JTextArea protoDefinitionTextArea;
    private JTextField messageNameTextField;
    private JComboBox<String> binaryFormatComboBox;
    private JTextArea jsonTextArea;
    private JTextArea binaryTextArea;
    private JButton jsonToBinaryButton;
    private JButton binaryToJsonButton;
    private JButton formatProtoButton;
    private JTextArea wireInputTextArea;
    private JTextArea wireOutputTextArea;
    private JButton wireDecodeButton;
    private JComboBox<String> wireFormatComboBox;
    private JTextArea hexTextArea;
    private JTextArea base64TextArea;
    private JButton hexToBase64Button;
    private JButton base64ToHexButton;

    private static ProtoBufForm protoBufForm;

    private static FuncHistoryPanel historyPanel;

    private ProtoBufForm() {
        UndoUtil.register(this);
    }

    public static ProtoBufForm getInstance() {
        if (protoBufForm == null) {
            protoBufForm = new ProtoBufForm();
        }
        return protoBufForm;
    }

    public static void init() {
        protoBufForm = getInstance();
        initUi();
        historyPanel = FuncHistorySupport.attachTab(
                protoBufForm.getTabbedPane1(), FuncConsts.PROTOBUF, ProtoBufForm::applyHistory);
        ProtoBufListener.addListeners();
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
        for (int i = 0; i < protoBufForm.getTabbedPane1().getTabCount(); i++) {
            if (tab.equals(protoBufForm.getTabbedPane1().getTitleAt(i))) {
                protoBufForm.getTabbedPane1().setSelectedIndex(i);
                break;
            }
        }
        switch (operation) {
            case "JsonToBinary" -> {
                protoBufForm.getJsonTextArea().setText(history.getInputText());
                protoBufForm.getBinaryTextArea().setText(history.getOutputText());
            }
            case "BinaryToJson" -> {
                protoBufForm.getBinaryTextArea().setText(history.getInputText());
                protoBufForm.getJsonTextArea().setText(history.getOutputText());
            }
            case "WireDecode" -> {
                protoBufForm.getWireInputTextArea().setText(history.getInputText());
                protoBufForm.getWireOutputTextArea().setText(history.getOutputText());
            }
            case "HexToBase64" -> {
                protoBufForm.getHexTextArea().setText(history.getInputText());
                protoBufForm.getBase64TextArea().setText(history.getOutputText());
            }
            case "Base64ToHex" -> {
                protoBufForm.getBase64TextArea().setText(history.getInputText());
                protoBufForm.getHexTextArea().setText(history.getOutputText());
            }
            case "FormatProto" -> {
                protoBufForm.getProtoDefinitionTextArea().setText(history.getOutputText());
            }
            default -> {
            }
        }
    }

    private static void initUi() {
        Style.blackTextArea(protoBufForm.getProtoDefinitionTextArea());
        Style.blackTextArea(protoBufForm.getJsonTextArea());
        Style.blackTextArea(protoBufForm.getBinaryTextArea());
        Style.blackTextArea(protoBufForm.getWireInputTextArea());
        Style.blackTextArea(protoBufForm.getWireOutputTextArea());
        Style.blackTextArea(protoBufForm.getHexTextArea());
        Style.blackTextArea(protoBufForm.getBase64TextArea());

        protoBufForm.getJsonToBinaryButton().setIcon(new FlatSVGIcon("icon/right_arrow.svg"));
        protoBufForm.getBinaryToJsonButton().setIcon(new FlatSVGIcon("icon/left_arrow.svg"));
        protoBufForm.getHexToBase64Button().setIcon(new FlatSVGIcon("icon/right_arrow.svg"));
        protoBufForm.getBase64ToHexButton().setIcon(new FlatSVGIcon("icon/left_arrow.svg"));

        protoBufForm.getProtoBufPanel().updateUI();
    }

    {
        $$$setupUI$$$();
    }

    private void $$$setupUI$$$() {
        protoBufPanel = new JPanel();
        protoBufPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        protoBufPanel.setMinimumSize(new Dimension(400, 300));
        protoBufPanel.setPreferredSize(new Dimension(400, 300));
        tabbedPane1 = new JTabbedPane();
        protoBufPanel.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));

        // Tab 1: JSON/二进制
        final JPanel jsonTab = new JPanel();
        jsonTab.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("JSON/二进制", jsonTab);

        final JPanel protoConfigPanel = new JPanel();
        protoConfigPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 8, 0), -1, -1));
        jsonTab.add(protoConfigPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JLabel protoLabel = new JLabel(".proto 定义:");
        protoConfigPanel.add(protoLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        formatProtoButton = new JButton("格式化");
        protoConfigPanel.add(formatProtoButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JScrollPane protoScroll = new JScrollPane();
        protoScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        protoConfigPanel.add(protoScroll, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(-1, 100), null, 0, false));
        protoDefinitionTextArea = new JTextArea();
        protoDefinitionTextArea.setLineWrap(true);
        protoDefinitionTextArea.setWrapStyleWord(true);
        protoDefinitionTextArea.setText("syntax = \"proto3\";\n\nmessage Person {\n  string name = 1;\n  int32 age = 2;\n}");
        protoScroll.setViewportView(protoDefinitionTextArea);

        final JPanel configRow = new JPanel();
        configRow.setLayout(new GridLayoutManager(1, 4, new Insets(4, 0, 0, 0), 8, -1));
        protoConfigPanel.add(configRow, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JLabel messageLabel = new JLabel("Message:");
        configRow.add(messageLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        messageNameTextField = new JTextField();
        messageNameTextField.setText("Person");
        configRow.add(messageNameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, -1), null, 0, false));

        final JLabel formatLabel = new JLabel("二进制格式:");
        configRow.add(formatLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        binaryFormatComboBox = new JComboBox<>();
        binaryFormatComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Hex", "Base64"}));
        configRow.add(binaryFormatComboBox, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));

        final JPanel convertPanel = new JPanel();
        convertPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jsonTab.add(convertPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        final JPanel jsonPanel = new JPanel();
        jsonPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(jsonPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel jsonLabel = new JLabel("JSON");
        jsonPanel.add(jsonLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane jsonScroll = new JScrollPane();
        jsonScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsonPanel.add(jsonScroll, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        jsonTextArea = new JTextArea();
        jsonTextArea.setLineWrap(true);
        jsonTextArea.setWrapStyleWord(true);
        jsonTextArea.setText("{\n  \"name\": \"张三\",\n  \"age\": 25\n}");
        jsonScroll.setViewportView(jsonTextArea);

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(buttonPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonPanel.add(new Spacer(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        jsonToBinaryButton = new JButton("JSON → 二进制");
        buttonPanel.add(jsonToBinaryButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        binaryToJsonButton = new JButton("JSON ← 二进制");
        buttonPanel.add(binaryToJsonButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonPanel.add(new Spacer(), new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));

        final JPanel binaryPanel = new JPanel();
        binaryPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        convertPanel.add(binaryPanel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel binaryLabel = new JLabel("二进制");
        binaryPanel.add(binaryLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane binaryScroll = new JScrollPane();
        binaryScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        binaryPanel.add(binaryScroll, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        binaryTextArea = new JTextArea();
        binaryTextArea.setLineWrap(true);
        binaryTextArea.setWrapStyleWord(true);
        binaryScroll.setViewportView(binaryTextArea);

        // Tab 2: Wire 解码
        final JPanel wireTab = new JPanel();
        wireTab.setLayout(new GridLayoutManager(2, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("Wire解码", wireTab);

        final JPanel wireInputPanel = new JPanel();
        wireInputPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        wireTab.add(wireInputPanel, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel wireInputLabel = new JLabel("二进制输入");
        wireInputPanel.add(wireInputLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane wireInputScroll = new JScrollPane();
        wireInputPanel.add(wireInputScroll, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        wireInputTextArea = new JTextArea();
        wireInputTextArea.setLineWrap(true);
        wireInputTextArea.setWrapStyleWord(true);
        wireInputScroll.setViewportView(wireInputTextArea);

        final JPanel wireButtonPanel = new JPanel();
        wireButtonPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        wireTab.add(wireButtonPanel, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        wireButtonPanel.add(new Spacer(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        wireDecodeButton = new JButton("解码");
        wireButtonPanel.add(wireDecodeButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wireFormatComboBox = new JComboBox<>();
        wireFormatComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Hex", "Base64"}));
        wireButtonPanel.add(wireFormatComboBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wireButtonPanel.add(new Spacer(), new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));

        final JPanel wireOutputPanel = new JPanel();
        wireOutputPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        wireTab.add(wireOutputPanel, new GridConstraints(0, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel wireOutputLabel = new JLabel("Wire 格式");
        wireOutputPanel.add(wireOutputLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane wireOutputScroll = new JScrollPane();
        wireOutputPanel.add(wireOutputScroll, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        wireOutputTextArea = new JTextArea();
        wireOutputTextArea.setEditable(false);
        wireOutputTextArea.setLineWrap(true);
        wireOutputTextArea.setWrapStyleWord(true);
        wireOutputScroll.setViewportView(wireOutputTextArea);

        // Tab 3: Hex/Base64
        final JPanel hexTab = new JPanel();
        hexTab.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("Hex/Base64", hexTab);

        final JPanel hexPanel = new JPanel();
        hexPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        hexTab.add(hexPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane hexScroll = new JScrollPane();
        hexPanel.add(hexScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        hexTextArea = new JTextArea();
        hexTextArea.setLineWrap(true);
        hexTextArea.setWrapStyleWord(true);
        hexScroll.setViewportView(hexTextArea);

        final JPanel hexButtonPanel = new JPanel();
        hexButtonPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        hexTab.add(hexButtonPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        hexButtonPanel.add(new Spacer(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        hexToBase64Button = new JButton("Hex → Base64");
        hexButtonPanel.add(hexToBase64Button, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        base64ToHexButton = new JButton("Hex ← Base64");
        hexButtonPanel.add(base64ToHexButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hexButtonPanel.add(new Spacer(), new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));

        final JPanel base64Panel = new JPanel();
        base64Panel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        hexTab.add(base64Panel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane base64Scroll = new JScrollPane();
        base64Panel.add(base64Scroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        base64TextArea = new JTextArea();
        base64TextArea.setLineWrap(true);
        base64TextArea.setWrapStyleWord(true);
        base64Scroll.setViewportView(base64TextArea);
    }

    public JComponent $$$getRootComponent$$$() {
        return protoBufPanel;
    }
}

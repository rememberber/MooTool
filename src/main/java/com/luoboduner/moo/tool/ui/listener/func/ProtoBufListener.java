package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.luoboduner.moo.tool.ui.FuncConsts;
import com.luoboduner.moo.tool.ui.form.func.ProtoBufForm;
import com.luoboduner.moo.tool.util.FuncHistoryUtil;
import com.luoboduner.moo.tool.util.ProtoBufUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * Protobuf 工具事件监听
 */
@Slf4j
public class ProtoBufListener {

    public static void addListeners() {
        ProtoBufForm form = ProtoBufForm.getInstance();

        form.getJsonToBinaryButton().addActionListener(e -> {
            try {
                String proto = form.getProtoDefinitionTextArea().getText();
                String messageName = form.getMessageNameTextField().getText();
                String json = form.getJsonTextArea().getText();
                if (StrUtil.isBlank(json)) {
                    return;
                }
                String format = (String) form.getBinaryFormatComboBox().getSelectedItem();
                byte[] binary = ProtoBufUtil.jsonToBinary(proto, messageName, json);
                String output = ProtoBufUtil.formatBinaryOutput(binary, format);
                form.getBinaryTextArea().setText(output);
                form.getBinaryTextArea().setCaretPosition(0);
                saveHistory("JSON转Protobuf", "JSON/二进制|JsonToBinary", json, output, messageName + " (" + format + ")");
            } catch (Exception ex) {
                log.error("JSON转Protobuf失败", ex);
                JOptionPane.showMessageDialog(form.getProtoBufPanel(),
                        "转换失败:\n\n" + ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
            }
        });

        form.getBinaryToJsonButton().addActionListener(e -> {
            try {
                String proto = form.getProtoDefinitionTextArea().getText();
                String messageName = form.getMessageNameTextField().getText();
                String binaryText = form.getBinaryTextArea().getText();
                if (StrUtil.isBlank(binaryText)) {
                    return;
                }
                String format = (String) form.getBinaryFormatComboBox().getSelectedItem();
                byte[] binary = ProtoBufUtil.parseBinaryInput(binaryText, format);
                String json = ProtoBufUtil.binaryToJson(proto, messageName, binary);
                form.getJsonTextArea().setText(json);
                form.getJsonTextArea().setCaretPosition(0);
                saveHistory("Protobuf转JSON", "JSON/二进制|BinaryToJson", binaryText, json, messageName + " (" + format + ")");
            } catch (Exception ex) {
                log.error("Protobuf转JSON失败", ex);
                JOptionPane.showMessageDialog(form.getProtoBufPanel(),
                        "转换失败:\n\n" + ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
            }
        });

        form.getFormatProtoButton().addActionListener(e -> {
            String proto = form.getProtoDefinitionTextArea().getText();
            if (StrUtil.isBlank(proto)) {
                return;
            }
            String formatted = ProtoBufUtil.formatProto(proto);
            form.getProtoDefinitionTextArea().setText(formatted);
            form.getProtoDefinitionTextArea().setCaretPosition(0);
            saveHistory("Proto格式化", "Proto定义|FormatProto", proto, formatted, "格式化");
        });

        form.getWireDecodeButton().addActionListener(e -> {
            try {
                String input = form.getWireInputTextArea().getText();
                if (StrUtil.isBlank(input)) {
                    return;
                }
                String format = (String) form.getWireFormatComboBox().getSelectedItem();
                byte[] data = ProtoBufUtil.parseBinaryInput(input, format);
                String output = ProtoBufUtil.decodeWireFormat(data);
                form.getWireOutputTextArea().setText(output);
                form.getWireOutputTextArea().setCaretPosition(0);
                saveHistory("Wire解码", "Wire解码|WireDecode", input, output, format);
            } catch (Exception ex) {
                log.error("Wire格式解码失败", ex);
                JOptionPane.showMessageDialog(form.getProtoBufPanel(),
                        "解码失败:\n\n" + ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
            }
        });

        form.getHexToBase64Button().addActionListener(e -> {
            try {
                String hex = form.getHexTextArea().getText();
                if (StrUtil.isBlank(hex)) {
                    return;
                }
                byte[] data = HexUtil.decodeHex(hex.replaceAll("\\s+", ""));
                String base64 = Base64.encode(data);
                form.getBase64TextArea().setText(base64);
                form.getBase64TextArea().setCaretPosition(0);
                saveHistory("Hex转Base64", "Hex/Base64|HexToBase64", hex, base64, "Hex转Base64");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(form.getProtoBufPanel(),
                        "转换失败:\n\n" + ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
            }
        });

        form.getBase64ToHexButton().addActionListener(e -> {
            try {
                String base64 = form.getBase64TextArea().getText();
                if (StrUtil.isBlank(base64)) {
                    return;
                }
                byte[] data = Base64.decode(base64.replaceAll("\\s+", ""));
                String hex = HexUtil.encodeHexStr(data);
                form.getHexTextArea().setText(hex);
                form.getHexTextArea().setCaretPosition(0);
                saveHistory("Base64转Hex", "Hex/Base64|Base64ToHex", base64, hex, "Base64转Hex");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(form.getProtoBufPanel(),
                        "转换失败:\n\n" + ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void saveHistory(String summary, String extraData, String input, String output, String remark) {
        if (StringUtils.isAllBlank(input, output)) {
            return;
        }
        String displaySummary = StringUtils.isBlank(remark) ? summary : summary + " - " + remark;
        FuncHistoryUtil.save(FuncConsts.PROTOBUF, displaySummary, input, output, extraData);
        if (ProtoBufForm.getHistoryPanel() != null) {
            ProtoBufForm.getHistoryPanel().refreshListIfVisible();
        }
    }
}

package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.luoboduner.moo.tool.ui.form.func.ProtoBufForm;
import com.luoboduner.moo.tool.util.ProtoBufUtil;
import lombok.extern.slf4j.Slf4j;

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
                form.getBinaryTextArea().setText(ProtoBufUtil.formatBinaryOutput(binary, format));
                form.getBinaryTextArea().setCaretPosition(0);
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
            form.getProtoDefinitionTextArea().setText(ProtoBufUtil.formatProto(proto));
            form.getProtoDefinitionTextArea().setCaretPosition(0);
        });

        form.getWireDecodeButton().addActionListener(e -> {
            try {
                String input = form.getWireInputTextArea().getText();
                if (StrUtil.isBlank(input)) {
                    return;
                }
                String format = (String) form.getWireFormatComboBox().getSelectedItem();
                byte[] data = ProtoBufUtil.parseBinaryInput(input, format);
                form.getWireOutputTextArea().setText(ProtoBufUtil.decodeWireFormat(data));
                form.getWireOutputTextArea().setCaretPosition(0);
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
                form.getBase64TextArea().setText(Base64.encode(data));
                form.getBase64TextArea().setCaretPosition(0);
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
                form.getHexTextArea().setText(HexUtil.encodeHexStr(data));
                form.getHexTextArea().setCaretPosition(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(form.getProtoBufPanel(),
                        "转换失败:\n\n" + ex.getMessage(), "失败", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

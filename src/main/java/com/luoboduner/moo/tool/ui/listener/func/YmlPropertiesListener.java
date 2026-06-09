package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.util.StrUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.form.func.YmlPropertiesForm;
import com.luoboduner.moo.tool.util.YamlValidateUtil;
import com.luoboduner.moo.tool.util.YmlAndPropUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

/**
 * <pre>
 * EnCodeListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/10/14.
 */
@Slf4j
public class YmlPropertiesListener {
    public static void addListeners() {
        YmlPropertiesForm ymlForm = YmlPropertiesForm.getInstance();

        // 点击转换按钮
        ymlForm.getProperties2ymlButton().addActionListener(e -> {

            String propStr = ymlForm.getPropertiesTextArea().getText();
            if (StrUtil.isBlank(propStr)) {
                return;
            }

            // Properties 转 yml
            String result = YmlAndPropUtil.convertProp2Yml(propStr);
            ymlForm.getYmlTextArea().setText(result);
        });

        // 点击转换按钮
        ymlForm.getYml2propertiesButton().addActionListener(e -> {
            String ymlStr = ymlForm.getYmlTextArea().getText();
            if (StrUtil.isBlank(ymlStr)) {
                // 为空不操作
                return;
            }

            // yml 转 Properties
            String result = YmlAndPropUtil.convertYml2Prop(ymlStr);
            ymlForm.getPropertiesTextArea().setText(result);
        });

        ymlForm.getYamlValidateButton().addActionListener(e -> {
            String yamlStr = ymlForm.getYamlValidateInputTextArea().getText();
            YamlValidateUtil.ValidateResult result = YamlValidateUtil.validate(yamlStr);
            if (result.valid()) {
                ymlForm.getYamlValidateResultTextArea().setText(YamlValidateUtil.formatSuccessMessage(result));
            } else {
                ymlForm.getYamlValidateResultTextArea().setText("✗ " + YamlValidateUtil.formatErrorMessage(result));
            }
            ymlForm.getYamlValidateResultTextArea().setCaretPosition(0);
        });

        ymlForm.getYamlFormatButton().addActionListener(e -> {
            String yamlStr = ymlForm.getYamlValidateInputTextArea().getText();
            if (StrUtil.isBlank(yamlStr)) {
                return;
            }
            try {
                String formatted = YamlValidateUtil.format(yamlStr);
                ymlForm.getYamlValidateInputTextArea().setText(formatted);
                ymlForm.getYamlValidateInputTextArea().setCaretPosition(0);
                ymlForm.getYamlValidateResultTextArea().setText("✓ 格式化成功");
            } catch (Exception ex) {
                ymlForm.getYamlValidateResultTextArea().setText("✗ 格式化失败：\n" + ex.getMessage());
                JOptionPane.showMessageDialog(App.mainFrame, "格式化失败！\n\n" + ex.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                log.error("YAML格式化失败", ex);
            }
        });
    }

}

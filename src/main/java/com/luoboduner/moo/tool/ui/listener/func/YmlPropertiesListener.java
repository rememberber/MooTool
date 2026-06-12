package com.luoboduner.moo.tool.ui.listener.func;

import cn.hutool.core.util.StrUtil;
import com.luoboduner.moo.tool.App;
import com.luoboduner.moo.tool.ui.FuncConsts;
import com.luoboduner.moo.tool.ui.form.func.YmlPropertiesForm;
import com.luoboduner.moo.tool.util.FuncHistoryUtil;
import com.luoboduner.moo.tool.util.YamlValidateUtil;
import com.luoboduner.moo.tool.util.I18n;
import com.luoboduner.moo.tool.util.MsgUtil;
import com.luoboduner.moo.tool.util.YmlAndPropUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * <pre>
 * YmlPropertiesListener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/10/14.
 */
@Slf4j
public class YmlPropertiesListener {
    public static void addListeners() {
        YmlPropertiesForm ymlForm = YmlPropertiesForm.getInstance();

        ymlForm.getProperties2ymlButton().addActionListener(e -> {
            String propStr = ymlForm.getPropertiesTextArea().getText();
            if (StrUtil.isBlank(propStr)) {
                return;
            }
            String result = YmlAndPropUtil.convertProp2Yml(propStr);
            ymlForm.getYmlTextArea().setText(result);
            saveHistory("Properties转YAML", "Prop2Yml", propStr, result);
        });

        ymlForm.getYml2propertiesButton().addActionListener(e -> {
            String ymlStr = ymlForm.getYmlTextArea().getText();
            if (StrUtil.isBlank(ymlStr)) {
                return;
            }
            String result = YmlAndPropUtil.convertYml2Prop(ymlStr);
            ymlForm.getPropertiesTextArea().setText(result);
            saveHistory("YAML转Properties", "Yml2Prop", ymlStr, result);
        });

        ymlForm.getYamlValidateButton().addActionListener(e -> {
            String yamlStr = ymlForm.getYamlValidateInputTextArea().getText();
            YamlValidateUtil.ValidateResult result = YamlValidateUtil.validate(yamlStr);
            String output;
            if (result.valid()) {
                output = YamlValidateUtil.formatSuccessMessage(result);
            } else {
                output = "✗ " + YamlValidateUtil.formatErrorMessage(result);
            }
            ymlForm.getYamlValidateResultTextArea().setText(output);
            ymlForm.getYamlValidateResultTextArea().setCaretPosition(0);
            saveHistory("YAML校验", "YamlValidate", yamlStr, output);
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
                ymlForm.getYamlValidateResultTextArea().setText(I18n.get("yml.formatSuccess"));
                saveHistory("YAML格式化", "YamlFormat", yamlStr, formatted);
            } catch (Exception ex) {
                ymlForm.getYamlValidateResultTextArea().setText(I18n.format("yml.formatFailed", ex.getMessage()));
                MsgUtil.errorWithDetail(App.mainFrame, "msg.formatFailed", ex.getMessage());
                log.error("YAML格式化失败", ex);
            }
        });
    }

    private static void saveHistory(String summary, String operation, String input, String output) {
        if (StringUtils.isAllBlank(input, output)) {
            return;
        }
        FuncHistoryUtil.save(FuncConsts.YML_PROPERTIES, summary, input, output, operation);
        if (YmlPropertiesForm.getHistoryPanel() != null) {
            YmlPropertiesForm.getHistoryPanel().refreshListIfVisible();
        }
    }
}

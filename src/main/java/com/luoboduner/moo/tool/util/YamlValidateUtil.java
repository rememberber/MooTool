package com.luoboduner.moo.tool.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.StringReader;
import java.util.Map;

/**
 * YAML 格式校验与格式化
 */
public class YamlValidateUtil {

    public record ValidateResult(boolean valid, String message, int line, int column) {
    }

    private YamlValidateUtil() {
    }

    public static ValidateResult validate(String yamlStr) {
        if (yamlStr == null || yamlStr.trim().isEmpty()) {
            return new ValidateResult(false, "YAML 内容为空", -1, -1);
        }
        try {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(new StringReader(yamlStr));
            if (loaded == null) {
                return new ValidateResult(true, "YAML 格式正确（空文档）", -1, -1);
            }
            String type = loaded instanceof Map ? "映射(Map)" : loaded.getClass().getSimpleName();
            return new ValidateResult(true, "YAML 格式正确（根节点类型：" + type + "）", -1, -1);
        } catch (MarkedYAMLException e) {
            Mark mark = e.getProblemMark();
            if (mark != null) {
                return new ValidateResult(false, e.getMessage(), mark.getLine() + 1, mark.getColumn() + 1);
            }
            return new ValidateResult(false, e.getMessage(), -1, -1);
        } catch (YAMLException e) {
            return new ValidateResult(false, e.getMessage(), -1, -1);
        }
    }

    public static String format(String yamlStr) {
        ValidateResult result = validate(yamlStr);
        if (!result.valid()) {
            throw new IllegalArgumentException(formatErrorMessage(result));
        }
        Yaml parser = new Yaml();
        Object loaded = parser.load(new StringReader(yamlStr));

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setAllowUnicode(true);
        options.setIndent(2);
        options.setPrettyFlow(true);

        Yaml dumper = new Yaml(options);
        if (loaded == null) {
            return "";
        }
        return dumper.dump(loaded);
    }

    public static String formatErrorMessage(ValidateResult result) {
        if (result.line() > 0 && result.column() > 0) {
            return String.format("第 %d 行，第 %d 列：%s", result.line(), result.column(), result.message());
        }
        return result.message();
    }

    public static String formatSuccessMessage(ValidateResult result) {
        return "✓ " + result.message();
    }
}

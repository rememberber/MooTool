package com.luoboduner.moo.tool.util;

import org.junit.Assert;
import org.junit.Test;

public class YamlValidateUtilTest {

    @Test
    public void validateValidYaml() {
        String yaml = """
                server:
                  port: 8080
                  name: demo
                """;
        YamlValidateUtil.ValidateResult result = YamlValidateUtil.validate(yaml);
        Assert.assertTrue(result.valid());
    }

    @Test
    public void validateInvalidYaml() {
        String yaml = """
                server:
                  port: 8080
                    name: demo
                """;
        YamlValidateUtil.ValidateResult result = YamlValidateUtil.validate(yaml);
        Assert.assertFalse(result.valid());
        Assert.assertTrue(result.line() > 0);
    }

    @Test
    public void validateEmptyYaml() {
        YamlValidateUtil.ValidateResult result = YamlValidateUtil.validate("");
        Assert.assertFalse(result.valid());
    }

    @Test
    public void formatYaml() {
        String yaml = "server:\n  port: 8080\n";
        String formatted = YamlValidateUtil.format(yaml);
        Assert.assertTrue(formatted.contains("server:"));
        Assert.assertTrue(formatted.contains("port: 8080"));
    }

    @Test
    public void formatErrorMessageWithLine() {
        YamlValidateUtil.ValidateResult result = new YamlValidateUtil.ValidateResult(false, "bad indentation", 3, 5);
        Assert.assertEquals("第 3 行，第 5 列：bad indentation", YamlValidateUtil.formatErrorMessage(result));
    }
}

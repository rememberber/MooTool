package com.luoboduner.moo.tool.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class UaParseUtilTest {

    @Test
    public void parseChromeWindows() {
        Map<String, String> result = UaParseUtil.parse(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        Assert.assertEquals("Chrome", result.get("浏览器"));
        Assert.assertEquals("131.0.0.0", result.get("浏览器版本"));
        Assert.assertEquals("Windows", result.get("操作系统"));
        Assert.assertEquals("否", result.get("是否移动端"));
    }

    @Test
    public void parseSafariIPhone() {
        Map<String, String> result = UaParseUtil.parse(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1");
        Assert.assertEquals("Safari", result.get("浏览器"));
        Assert.assertEquals("iOS", result.get("操作系统"));
        Assert.assertEquals("是", result.get("是否移动端"));
        Assert.assertEquals("手机", result.get("设备类型"));
    }

    @Test
    public void parseCurl() {
        Map<String, String> result = UaParseUtil.parse("curl/8.7.1");
        Assert.assertEquals("curl", result.get("浏览器"));
        Assert.assertEquals("Bot/爬虫", result.get("设备类型"));
    }

    @Test
    public void parseEmpty() {
        Map<String, String> result = UaParseUtil.parse("");
        Assert.assertTrue(result.containsKey("错误"));
    }
}

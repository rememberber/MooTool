package com.luoboduner.moo.tool.util;

import org.junit.Assert;
import org.junit.Test;

public class I18nTest {

    @Test
    public void englishBundleLoads() {
        I18n.setLocale(I18n.LOCALE_EN);
        Assert.assertEquals("Settings", I18n.get("setting.title"));
        Assert.assertEquals("English", I18n.displayLanguage(I18n.LOCALE_EN));
    }

    @Test
    public void chineseBundleLoads() {
        I18n.setLocale(I18n.LOCALE_ZH_CN);
        Assert.assertEquals("设置", I18n.get("setting.title"));
        Assert.assertEquals("简体中文", I18n.displayLanguage(I18n.LOCALE_ZH_CN));
    }
}

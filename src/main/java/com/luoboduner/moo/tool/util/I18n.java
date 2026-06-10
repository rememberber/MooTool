package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.App;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Internationalization helper. Default locale is English.
 */
public final class I18n {

    public static final String LOCALE_EN = "en";
    public static final String LOCALE_ZH_CN = "zh_CN";
    public static final String LOCALE_JA = "ja";

    private static final String BUNDLE_BASE = "i18n.messages";

    private static Locale currentLocale = Locale.ENGLISH;
    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
    private static final ResourceBundle ENGLISH_BUNDLE = ResourceBundle.getBundle(BUNDLE_BASE, Locale.ENGLISH);

    private I18n() {
    }

    public static void init() {
        setLocale(App.config.getLocale());
    }

    public static void setLocale(String localeTag) {
        currentLocale = toLocale(localeTag);
        bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
    }

    public static void refreshUi() {
        I18nUiUtil.refreshAll();
    }

    public static String getLocaleTag() {
        String tag = currentLocale.toLanguageTag();
        if (LOCALE_ZH_CN.equals(tag) || "zh-CN".equals(tag)) {
            return LOCALE_ZH_CN;
        }
        if (LOCALE_JA.equals(tag) || "ja-JP".equals(tag)) {
            return LOCALE_JA;
        }
        return LOCALE_EN;
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static boolean isChinese() {
        return LOCALE_ZH_CN.equals(getLocaleTag());
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            try {
                return ENGLISH_BUNDLE.getString(key);
            } catch (MissingResourceException ignored) {
                return key;
            }
        }
    }

    public static String format(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    public static String displayLanguage(String localeTag) {
        if (LOCALE_ZH_CN.equals(localeTag)) {
            return get("language.zhCN");
        }
        if (LOCALE_JA.equals(localeTag)) {
            return get("language.ja");
        }
        return get("language.en");
    }

    public static String localeFromDisplay(String display) {
        if (getRaw(LOCALE_ZH_CN, "language.zhCN").equals(display)) {
            return LOCALE_ZH_CN;
        }
        if (getRaw(LOCALE_JA, "language.ja").equals(display)) {
            return LOCALE_JA;
        }
        return LOCALE_EN;
    }

    public static String[] supportedLocaleTags() {
        return new String[]{LOCALE_EN, LOCALE_ZH_CN, LOCALE_JA};
    }

    private static String getRaw(String localeTag, String key) {
        try {
            return ResourceBundle.getBundle(BUNDLE_BASE, toLocale(localeTag)).getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    private static Locale toLocale(String localeTag) {
        if (LOCALE_ZH_CN.equals(localeTag) || "zh-CN".equals(localeTag)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        if (LOCALE_JA.equals(localeTag) || "ja-JP".equals(localeTag)) {
            return Locale.JAPANESE;
        }
        return Locale.ENGLISH;
    }
}

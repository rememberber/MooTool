package com.luoboduner.moo.tool.util.translator;

import com.luoboduner.moo.tool.util.I18n;
import org.apache.commons.lang3.StringUtils;

/**
 * Localized translation error messages and detection.
 */
public final class TranslationErrorUtil {

    private static final String[] ERROR_KEYS = {
            "translation.error.google.network",
            "translation.error.google.timeout",
            "translation.error.google.exception",
            "translation.error.bing.httpStatus",
            "translation.error.bing.captcha",
            "translation.error.bing.network",
            "translation.error.bing.timeout",
            "translation.error.bing.exception",
            "translation.error.bing.emptyResult",
            "translation.error.bing.parseFormat",
            "translation.error.bing.parseException",
            "translation.error.bing.fetchPage"
    };

    private static final String[] LEGACY_ZH_PREFIXES = {
            "访问", "Bing翻译", "解析翻译", "翻译返回", "Google翻译", "翻译失败", "网络", "翻译中", "获取Bing", "无法从Bing"
    };

    private static final String[] LEGACY_EN_PREFIXES = {
            "Network error accessing Google",
            "Google Translate request timed out",
            "Google Translate error",
            "Bing Translate returned error",
            "Bing Translate CAPTCHA",
            "Network error accessing Bing",
            "Bing Translate request timed out",
            "Bing Translate error",
            "Translation result is empty",
            "Failed to parse translation",
            "Failed to fetch Bing Translate page",
            "Failed to parse IG parameter",
            "Failed to parse token/key"
    };

    private TranslationErrorUtil() {
    }

    public static String error(String key, Object... args) {
        return I18n.format(key, args);
    }

    public static boolean isErrorResult(String result) {
        if (StringUtils.isBlank(result)) {
            return true;
        }
        String translating = I18n.get("translation.translating");
        if (result.startsWith(translating)) {
            return true;
        }
        for (String legacy : LEGACY_ZH_PREFIXES) {
            if (result.startsWith(legacy)) {
                return true;
            }
        }
        for (String legacy : LEGACY_EN_PREFIXES) {
            if (result.startsWith(legacy)) {
                return true;
            }
        }
        for (String key : ERROR_KEYS) {
            String prefix = templatePrefix(I18n.get(key));
            if (!prefix.isEmpty() && result.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSuccessfulTranslation(String result) {
        return !isErrorResult(result);
    }

    private static String templatePrefix(String template) {
        int idx = template.indexOf("{0}");
        if (idx < 0) {
            return template;
        }
        return template.substring(0, idx);
    }
}

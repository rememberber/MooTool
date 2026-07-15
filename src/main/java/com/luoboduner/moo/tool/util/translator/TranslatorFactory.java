package com.luoboduner.moo.tool.util.translator;

import org.apache.commons.lang3.StringUtils;

public class TranslatorFactory {
    public enum TranslatorType {
        GOOGLE,
        BING
    }

    private static final GoogleTranslatorUtil GOOGLE_TRANSLATOR = new GoogleTranslatorUtil();
    private static final BingTranslatorUtil BING_TRANSLATOR = new BingTranslatorUtil();

    public static Translator getTranslator(TranslatorType type) {
        switch (type) {
            case GOOGLE:
                return GOOGLE_TRANSLATOR;
            case BING:
                return BING_TRANSLATOR;
            default:
                throw new IllegalArgumentException("Unknown translator type: " + type);
        }
    }

    public static String translate(String text, String from, String to, TranslatorType preferredType) {
        Translator primary = getTranslator(preferredType);
        String result = primary.translate(text, from, to);
        if (!TranslationErrorUtil.isErrorResult(result)) {
            return result;
        }

        TranslatorType fallbackType = preferredType == TranslatorType.GOOGLE
                ? TranslatorType.BING
                : TranslatorType.GOOGLE;
        String fallbackResult = getTranslator(fallbackType).translate(text, from, to);
        if (!TranslationErrorUtil.isErrorResult(fallbackResult)) {
            return fallbackResult;
        }
        return result;
    }

    public static TranslatorType parseType(String translatorTypeStr) {
        if (StringUtils.isBlank(translatorTypeStr)) {
            return TranslatorType.GOOGLE;
        }
        if ("MICROSOFT".equalsIgnoreCase(translatorTypeStr)) {
            return TranslatorType.GOOGLE;
        }
        try {
            return TranslatorType.valueOf(translatorTypeStr);
        } catch (IllegalArgumentException e) {
            return TranslatorType.GOOGLE;
        }
    }

}

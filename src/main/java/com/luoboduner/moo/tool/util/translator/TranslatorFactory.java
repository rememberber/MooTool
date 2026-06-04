package com.luoboduner.moo.tool.util.translator;

public class TranslatorFactory {
    public enum TranslatorType {
        GOOGLE,
        BING,
        MICROSOFT
    }

    public static Translator getTranslator(TranslatorType type) {
        switch (type) {
            case GOOGLE:
                return new GoogleTranslatorUtil();
            case BING:
                return new BingTranslatorUtil();
            case MICROSOFT:
                // Microsoft translator requires API key configuration
                // Falling back to Google translator for now
                return new GoogleTranslatorUtil();
            default:
                throw new IllegalArgumentException("Unknown translator type: " + type);
        }
    }
}

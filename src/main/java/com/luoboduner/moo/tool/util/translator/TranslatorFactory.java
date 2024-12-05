package com.luoboduner.moo.tool.util.translator;

public class TranslatorFactory {
    public enum TranslatorType {
        GOOGLE,
        MICROSOFT
    }

    public static Translator getTranslator(String type) {
        switch (type) {
            case "GOOGLE":
                return new GoogleTranslatorUtil();
            case "MICROSOFT":
//                return new MicrosoftTranslatorUtil();
            default:
                throw new IllegalArgumentException("Unknown translator type: " + type);
        }
    }
}

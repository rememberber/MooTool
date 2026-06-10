package com.luoboduner.moo.tool.util.translator;

public interface Translator {

    /**
     * @deprecated use {@link TranslatorLangUtil#getAutoDetectLabel()}
     */
    @Deprecated
    String AUTO_DETECT = "自动检测";

    String translate(String text, String from, String to);

    static String[] getSourceLanguageNames() {
        return TranslatorLangUtil.getSourceLanguageNames();
    }

    static String[] getTargetLanguageNames() {
        return TranslatorLangUtil.getTargetLanguageNames();
    }

    static String resolveLanguageCode(String languageName, String defaultCode) {
        return TranslatorLangUtil.resolveCode(languageName, defaultCode);
    }
}

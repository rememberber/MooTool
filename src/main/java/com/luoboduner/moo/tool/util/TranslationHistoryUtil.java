package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.dao.TTranslationHistoryMapper;
import com.luoboduner.moo.tool.domain.TTranslationHistory;
import com.luoboduner.moo.tool.util.translator.TranslatorFactory;
import org.apache.commons.lang3.StringUtils;

public final class TranslationHistoryUtil {

    private static final int MAX_HISTORY_COUNT = 500;

    private TranslationHistoryUtil() {
    }

    private static TTranslationHistoryMapper historyMapper() {
        return MybatisUtil.getSqlSession().getMapper(TTranslationHistoryMapper.class);
    }

    public static boolean isSuccessfulTranslation(String result) {
        if (StringUtils.isBlank(result)) {
            return false;
        }
        String translating = I18n.get("translation.translating");
        if (result.startsWith(translating) || result.startsWith("翻译中")) {
            return false;
        }
        return !result.startsWith("访问")
                && !result.startsWith("Bing翻译")
                && !result.startsWith("解析翻译")
                && !result.startsWith("Google翻译")
                && !result.startsWith("翻译失败")
                && !result.startsWith("网络");
    }

    public static void save(String sourceText,
                            String targetText,
                            String sourceLang,
                            String targetLang,
                            TranslatorFactory.TranslatorType translatorType) {
        if (StringUtils.isBlank(sourceText) || !isSuccessfulTranslation(targetText)) {
            return;
        }

        TTranslationHistory history = new TTranslationHistory();
        history.setSourceText(sourceText.trim());
        history.setTargetText(StringUtils.defaultString(targetText));
        history.setSourceLang(sourceLang);
        history.setTargetLang(targetLang);
        history.setTranslatorType(translatorType != null ? translatorType.name() : null);
        history.setCreateTime(SqliteUtil.nowDateForSqlite());
        historyMapper().insert(history);

        if (historyMapper().count() > MAX_HISTORY_COUNT) {
            historyMapper().deleteBeyondLimit(MAX_HISTORY_COUNT);
        }
    }

    public static String previewText(String text, int maxLength) {
        return TranslationWordBookUtil.previewText(text, maxLength);
    }

    public static String formatTranslatorType(String translatorType) {
        if ("BING".equalsIgnoreCase(translatorType)) {
            return "Bing";
        }
        if ("GOOGLE".equalsIgnoreCase(translatorType)) {
            return "Google";
        }
        return StringUtils.defaultIfBlank(translatorType, "-");
    }
}

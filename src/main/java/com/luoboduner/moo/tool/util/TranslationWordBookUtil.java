package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.dao.TTranslationWordMapper;
import com.luoboduner.moo.tool.domain.TTranslationWord;
import com.luoboduner.moo.tool.util.translator.Translator;
import com.luoboduner.moo.tool.util.translator.TranslatorFactory;
import org.apache.commons.lang3.StringUtils;

public final class TranslationWordBookUtil {

    private static TTranslationWordMapper wordMapper() {
        return MybatisUtil.getSqlSession().getMapper(TTranslationWordMapper.class);
    }

    private TranslationWordBookUtil() {
    }

    public static TTranslationWord saveOrUpdate(String sourceText,
                                                String targetText,
                                                String sourceLang,
                                                String targetLang,
                                                String remark) {
        if (StringUtils.isBlank(sourceText)) {
            return null;
        }

        String now = SqliteUtil.nowDateForSqlite();
        TTranslationWord existing = wordMapper().selectBySourceAndLang(
                sourceText.trim(), sourceLang, targetLang);

        if (existing == null) {
            TTranslationWord word = new TTranslationWord();
            word.setSourceText(sourceText.trim());
            word.setTargetText(StringUtils.defaultString(targetText));
            word.setSourceLang(sourceLang);
            word.setTargetLang(targetLang);
            word.setRemark(remark);
            word.setCreateTime(now);
            word.setModifiedTime(now);
            wordMapper().insert(word);
            return wordMapper().selectBySourceAndLang(sourceText.trim(), sourceLang, targetLang);
        }

        existing.setTargetText(StringUtils.defaultString(targetText));
        existing.setRemark(remark);
        existing.setModifiedTime(now);
        wordMapper().updateByPrimaryKey(existing);
        return existing;
    }

    public static String retranslate(TTranslationWord word) {
        String sourceLangCode = Translator.resolveLanguageCode(word.getSourceLang(), "auto");
        String targetLangCode = Translator.resolveLanguageCode(word.getTargetLang(), "zh-CN");
        TranslatorFactory.TranslatorType type = TranslatorFactory.parseType(
                ConfigUtil.getInstance().getTranslatorType());
        return TranslatorFactory.translate(word.getSourceText(), sourceLangCode, targetLangCode, type);
    }

    public static String previewText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\n', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}

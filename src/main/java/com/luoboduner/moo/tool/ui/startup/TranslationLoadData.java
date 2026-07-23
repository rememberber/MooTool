package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.domain.TTranslationHistory;
import com.luoboduner.moo.tool.domain.TTranslationWord;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.dao.TTranslationHistoryMapper;
import com.luoboduner.moo.tool.dao.TTranslationWordMapper;
import lombok.Getter;

import java.util.List;

/**
 * 翻译页后台加载快照。
 */
@Getter
public final class TranslationLoadData {

    private final List<TTranslationWord> words;
    private final List<TTranslationHistory> histories;

    public TranslationLoadData(List<TTranslationWord> words, List<TTranslationHistory> histories) {
        this.words = words == null ? List.of() : List.copyOf(words);
        this.histories = histories == null ? List.of() : List.copyOf(histories);
    }

    public static TranslationLoadData loadInitial() {
        EdtGuard.assertNotEdt();
        TTranslationWordMapper wordMapper = MybatisUtil.getSqlSession().getMapper(TTranslationWordMapper.class);
        TTranslationHistoryMapper historyMapper = MybatisUtil.getSqlSession().getMapper(TTranslationHistoryMapper.class);
        return new TranslationLoadData(wordMapper.selectAll(), historyMapper.selectAll());
    }
}

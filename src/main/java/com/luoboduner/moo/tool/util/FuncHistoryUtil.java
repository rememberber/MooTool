package com.luoboduner.moo.tool.util;

import com.luoboduner.moo.tool.dao.TFuncHistoryMapper;
import com.luoboduner.moo.tool.domain.TFuncHistory;
import org.apache.commons.lang3.StringUtils;

public final class FuncHistoryUtil {

    private static final int MAX_HISTORY_COUNT = 200;

    private FuncHistoryUtil() {
    }

    private static TFuncHistoryMapper historyMapper() {
        return MybatisUtil.getSqlSession().getMapper(TFuncHistoryMapper.class);
    }

    public static void save(String funcType,
                            String summary,
                            String inputText,
                            String outputText,
                            String extraData) {
        if (StringUtils.isBlank(funcType) || StringUtils.isAllBlank(inputText, outputText)) {
            return;
        }

        TFuncHistory history = new TFuncHistory();
        history.setFuncType(funcType);
        history.setSummary(StringUtils.defaultIfBlank(summary, previewText(inputText, 40)));
        history.setInputText(StringUtils.defaultString(inputText));
        history.setOutputText(StringUtils.defaultString(outputText));
        history.setExtraData(extraData);
        history.setCreateTime(SqliteUtil.nowDateForSqlite());
        historyMapper().insert(history);

        if (historyMapper().countByFuncType(funcType) > MAX_HISTORY_COUNT) {
            historyMapper().deleteBeyondLimit(funcType, MAX_HISTORY_COUNT);
        }
    }

    public static String previewText(String text, int maxLength) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String normalized = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}

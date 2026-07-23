package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.dao.THttpRequestHistoryMapper;
import com.luoboduner.moo.tool.dao.TMsgHttpMapper;
import com.luoboduner.moo.tool.domain.THttpRequestHistory;
import com.luoboduner.moo.tool.domain.TMsgHttp;
import com.luoboduner.moo.tool.util.MybatisUtil;
import lombok.Getter;

import java.util.List;

/**
 * HTTP 请求页后台加载快照。
 */
@Getter
public final class HttpRequestLoadData {

    private final List<TMsgHttp> messages;
    private final TMsgHttp firstMessage;
    private final List<THttpRequestHistory> firstHistory;

    public HttpRequestLoadData(List<TMsgHttp> messages,
                               TMsgHttp firstMessage,
                               List<THttpRequestHistory> firstHistory) {
        this.messages = messages == null ? List.of() : List.copyOf(messages);
        this.firstMessage = firstMessage;
        this.firstHistory = firstHistory == null ? List.of() : List.copyOf(firstHistory);
    }

    public static HttpRequestLoadData loadInitial() {
        EdtGuard.assertNotEdt();
        TMsgHttpMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgHttpMapper.class);
        THttpRequestHistoryMapper historyMapper = MybatisUtil.getSqlSession().getMapper(THttpRequestHistoryMapper.class);
        List<TMsgHttp> messages = msgMapper.selectByFilter("%%");
        TMsgHttp first = messages.isEmpty() ? null : messages.get(0);
        List<THttpRequestHistory> history = List.of();
        if (first != null && first.getId() != null) {
            history = historyMapper.selectByRequestId(first.getId());
        }
        return new HttpRequestLoadData(messages, first, history);
    }
}

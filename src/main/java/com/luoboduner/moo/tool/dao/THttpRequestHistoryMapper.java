package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.THttpRequestHistory;

public interface THttpRequestHistoryMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(THttpRequestHistory record);

    int insertSelective(THttpRequestHistory record);

    THttpRequestHistory selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(THttpRequestHistory record);

    int updateByPrimaryKey(THttpRequestHistory record);
}
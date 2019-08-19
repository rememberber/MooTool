package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TJsonBeauty;

public interface TJsonBeautyMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TJsonBeauty record);

    int insertSelective(TJsonBeauty record);

    TJsonBeauty selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TJsonBeauty record);

    int updateByPrimaryKey(TJsonBeauty record);
}
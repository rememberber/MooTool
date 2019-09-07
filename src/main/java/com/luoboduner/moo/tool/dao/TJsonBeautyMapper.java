package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TJsonBeauty;

import java.util.List;

public interface TJsonBeautyMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TJsonBeauty record);

    int insertSelective(TJsonBeauty record);

    TJsonBeauty selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TJsonBeauty record);

    int updateByPrimaryKey(TJsonBeauty record);

    List<TJsonBeauty> selectAll();

    TJsonBeauty selectByName(String name);

    int updateByName(TJsonBeauty tJsonBeauty);
}
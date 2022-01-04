package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TFuncContent;

public interface TFuncContentMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TFuncContent record);

    int insertSelective(TFuncContent record);

    TFuncContent selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TFuncContent record);

    int updateByPrimaryKey(TFuncContent record);
}
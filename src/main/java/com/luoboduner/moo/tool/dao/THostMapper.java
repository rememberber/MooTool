package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.THost;

public interface THostMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(THost record);

    int insertSelective(THost record);

    THost selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(THost record);

    int updateByPrimaryKey(THost record);
}
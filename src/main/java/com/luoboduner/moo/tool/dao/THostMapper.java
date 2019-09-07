package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.THost;

import java.util.List;

public interface THostMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(THost record);

    int insertSelective(THost record);

    THost selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(THost record);

    int updateByPrimaryKey(THost record);

    List<THost> selectAll();

    THost selectByName(String name);

    int updateByName(THost tHost);
}
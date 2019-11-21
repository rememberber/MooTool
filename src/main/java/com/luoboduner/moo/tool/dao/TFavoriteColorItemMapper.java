package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TFavoriteColorItem;

public interface TFavoriteColorItemMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TFavoriteColorItem record);

    int insertSelective(TFavoriteColorItem record);

    TFavoriteColorItem selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TFavoriteColorItem record);

    int updateByPrimaryKey(TFavoriteColorItem record);
}
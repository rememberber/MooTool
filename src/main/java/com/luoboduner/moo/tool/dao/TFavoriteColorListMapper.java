package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TFavoriteColorList;

public interface TFavoriteColorListMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TFavoriteColorList record);

    int insertSelective(TFavoriteColorList record);

    TFavoriteColorList selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TFavoriteColorList record);

    int updateByPrimaryKey(TFavoriteColorList record);
}
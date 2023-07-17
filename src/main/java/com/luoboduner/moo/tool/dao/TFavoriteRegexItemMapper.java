package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TFavoriteRegexItem;

import java.util.List;

public interface TFavoriteRegexItemMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TFavoriteRegexItem record);

    int insertSelective(TFavoriteRegexItem record);

    TFavoriteRegexItem selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TFavoriteRegexItem record);

    int updateByPrimaryKey(TFavoriteRegexItem record);

    List<TFavoriteRegexItem> selectByListId(Integer listId);
}
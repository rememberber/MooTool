package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TQuickNote;

import java.util.List;

public interface TQuickNoteMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TQuickNote record);

    int insertSelective(TQuickNote record);

    TQuickNote selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TQuickNote record);

    int updateByPrimaryKey(TQuickNote record);

    List<TQuickNote> selectAll();
}
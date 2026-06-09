package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TTranslationHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TTranslationHistoryMapper {

    int insert(TTranslationHistory record);

    TTranslationHistory selectByPrimaryKey(Integer id);

    List<TTranslationHistory> selectAll();

    List<TTranslationHistory> selectByFilter(@Param("keyword") String keyword);

    int deleteByPrimaryKey(Integer id);

    int deleteAll();

    int count();

    int deleteBeyondLimit(@Param("maxCount") int maxCount);
}

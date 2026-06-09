package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TTranslationWord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TTranslationWordMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTranslationWord record);

    TTranslationWord selectByPrimaryKey(Integer id);

    int updateByPrimaryKey(TTranslationWord record);

    List<TTranslationWord> selectAll();

    List<TTranslationWord> selectByFilter(@Param("keyword") String keyword);

    TTranslationWord selectBySourceAndLang(@Param("sourceText") String sourceText,
                                           @Param("sourceLang") String sourceLang,
                                           @Param("targetLang") String targetLang);
}

package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TFuncHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TFuncHistoryMapper {

    int insert(TFuncHistory record);

    TFuncHistory selectByPrimaryKey(Integer id);

    List<TFuncHistory> selectByFuncType(@Param("funcType") String funcType);

    List<TFuncHistory> selectByFuncTypeAndFilter(@Param("funcType") String funcType,
                                                 @Param("keyword") String keyword);

    int deleteByPrimaryKey(Integer id);

    int deleteAllByFuncType(@Param("funcType") String funcType);

    int countByFuncType(@Param("funcType") String funcType);

    int deleteBeyondLimit(@Param("funcType") String funcType, @Param("maxCount") int maxCount);
}

package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TMsgHttp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TMsgHttpMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TMsgHttp record);

    int insertSelective(TMsgHttp record);

    TMsgHttp selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TMsgHttp record);

    int updateByPrimaryKey(TMsgHttp record);

    List<TMsgHttp> selectByMsgTypeAndMsgName(@Param("msgType") int msgType, @Param("msgName") String msgName);

    int updateByMsgTypeAndMsgName(TMsgHttp tMsgHttp);

    List<TMsgHttp> selectByMsgType(int msgType);
}
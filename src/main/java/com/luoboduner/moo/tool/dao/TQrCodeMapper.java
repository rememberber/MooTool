package com.luoboduner.moo.tool.dao;

import com.luoboduner.moo.tool.domain.TQrCode;

public interface TQrCodeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TQrCode record);

    int insertSelective(TQrCode record);

    TQrCode selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TQrCode record);

    int updateByPrimaryKey(TQrCode record);
}
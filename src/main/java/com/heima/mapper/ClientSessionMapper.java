package com.heima.mapper;

import com.heima.entity.ClientSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClientSessionMapper {

    int insert(ClientSession row);

    ClientSession selectByToken(@Param("token") String token);

    int deleteByToken(@Param("token") String token);

    int deleteExpired();
}

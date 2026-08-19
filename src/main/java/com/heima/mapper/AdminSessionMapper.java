package com.heima.mapper;

import com.heima.entity.AdminSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminSessionMapper {

    int insert(AdminSession row);

    AdminSession selectByToken(@Param("token") String token);

    int deleteByToken(@Param("token") String token);

    int deleteExpired();
}

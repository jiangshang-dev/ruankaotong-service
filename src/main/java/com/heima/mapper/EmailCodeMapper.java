package com.heima.mapper;

import com.heima.entity.EmailCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmailCodeMapper {

    int insert(EmailCode row);

    EmailCode selectLatest(@Param("email") String email);

    int markUsed(@Param("id") Long id);

    int countRecent(@Param("email") String email, @Param("seconds") int seconds);
}

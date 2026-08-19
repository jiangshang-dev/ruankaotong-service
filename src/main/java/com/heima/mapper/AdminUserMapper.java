package com.heima.mapper;

import com.heima.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminUserMapper {

    AdminUser selectByAccount(@Param("account") String account);

    AdminUser selectById(@Param("id") Long id);

    int insert(AdminUser row);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int countAll();
}

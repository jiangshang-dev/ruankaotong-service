package com.heima.mapper;

import com.heima.entity.ClientUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClientUserMapper {

    ClientUser selectByEmail(@Param("email") String email);

    ClientUser selectById(@Param("id") Long id);

    int insert(ClientUser row);

    int touchLogin(@Param("id") Long id);
}

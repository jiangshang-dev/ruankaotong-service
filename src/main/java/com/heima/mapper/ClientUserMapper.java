package com.heima.mapper;

import com.heima.dto.AdminDtos.ClientUserRow;
import com.heima.entity.ClientUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClientUserMapper {

    ClientUser selectByEmail(@Param("email") String email);

    ClientUser selectById(@Param("id") Long id);

    int insert(ClientUser row);

    int touchLogin(@Param("id") Long id);

    int updateName(@Param("id") Long id, @Param("name") String name);

    int updateEnabled(@Param("id") Long id, @Param("enabled") int enabled);

    List<ClientUserRow> selectAdminList(@Param("q") String q);
}

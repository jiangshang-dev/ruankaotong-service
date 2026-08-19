package com.heima.mapper;

import com.heima.dto.AdminDtos.AiQaQuery;
import com.heima.dto.AdminDtos.ClientIpRow;
import com.heima.dto.AdminDtos.DashboardStats;
import com.heima.entity.AiQaLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiQaMapper {

    int insert(AiQaLog row);

    AiQaLog selectById(@Param("id") Long id);

    long countList(@Param("q") AiQaQuery q);

    List<AiQaLog> selectList(@Param("q") AiQaQuery q, @Param("offset") long offset, @Param("limit") long limit);

    List<ClientIpRow> selectClients(@Param("ip") String ip);

    DashboardStats selectDashboard();

    List<AiQaLog> selectRecent(@Param("limit") int limit);
}

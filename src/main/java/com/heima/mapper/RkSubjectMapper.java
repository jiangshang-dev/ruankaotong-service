package com.heima.mapper;

import com.heima.entity.RkSubject;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RkSubjectMapper {

    List<RkSubject> selectAll();

    List<RkSubject> selectEnabled();

    RkSubject selectById(@Param("id") String id);

    int insert(RkSubject row);

    int update(RkSubject row);

    int countAll();
}

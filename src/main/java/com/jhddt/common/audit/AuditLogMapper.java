package com.jhddt.common.audit;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuditLogMapper {
    int insert(AuditLogEntity entity);

    List<AuditLogEntity> selectRecent(@Param("limit") Integer limit);
}

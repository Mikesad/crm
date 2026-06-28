package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmRecordMigrationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 跟进记录迁移日志 Mapper
 */
@Mapper
public interface CrmRecordMigrationLogMapper extends BaseMapper<CrmRecordMigrationLog> {
}
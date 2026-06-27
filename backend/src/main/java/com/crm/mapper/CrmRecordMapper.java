package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 跟进记录 Mapper
 */
@Mapper
public interface CrmRecordMapper extends BaseMapper<CrmRecord> {
}

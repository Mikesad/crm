package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmLead;
import org.apache.ibatis.annotations.Mapper;

/**
 * 线索 Mapper
 *
 * <p>基础 CRUD 由 MyBatis-Plus {@code BaseMapper} 提供；复杂查询（按 dataScope 拼接条件、
 * 跨表 join）放在 XML 文件中。</p>
 */
@Mapper
public interface CrmLeadMapper extends BaseMapper<CrmLead> {
}

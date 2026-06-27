package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmContact;
import org.apache.ibatis.annotations.Mapper;

/**
 * 联系人 Mapper
 */
@Mapper
public interface CrmContactMapper extends BaseMapper<CrmContact> {
}

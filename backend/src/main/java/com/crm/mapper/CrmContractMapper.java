package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmContract;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同 Mapper
 *
 * <p>有 owner_user_id,受 {@code CrmDataPermissionHandler} 拦截。
 * sales 看自己,lead 看本部门,director 看本部门及以下,finance 看全部。</p>
 */
@Mapper
public interface CrmContractMapper extends BaseMapper<CrmContract> {
}

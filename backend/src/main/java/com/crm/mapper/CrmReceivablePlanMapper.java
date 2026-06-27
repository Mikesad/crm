package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmReceivablePlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 回款计划 Mapper
 *
 * <p>无 owner_user_id,V1 不入 CrmDataPermissionHandler.MANAGED_TABLES,
 * 权限靠 {@code @SaCheckPermission} 兜底。</p>
 */
@Mapper
public interface CrmReceivablePlanMapper extends BaseMapper<CrmReceivablePlan> {
}

package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmApproval;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同审批 Mapper
 *
 * <p>无 owner_user_id,V1 不进 CrmDataPermissionHandler.MANAGED_TABLES,
 * 仅靠 {@code @SaCheckPermission("crm:contract:approve")} 兜底。</p>
 */
@Mapper
public interface CrmApprovalMapper extends BaseMapper<CrmApproval> {
}

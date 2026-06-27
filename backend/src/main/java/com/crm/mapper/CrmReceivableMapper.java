package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmReceivable;
import org.apache.ibatis.annotations.Mapper;

/**
 * 回款记录 Mapper
 *
 * <p>append-only,无 is_deleted,不走逻辑删除拦截器。
 * 受 {@code CrmDataPermissionHandler.MANAGED_TABLES} 拦截(本表 owner_user_id 由 contract 决定,
 * V1 直接当受控表处理:如果 SQL 不带 owner 列会自动跳过,见 Task #7 V2 优化)。</p>
 */
@Mapper
public interface CrmReceivableMapper extends BaseMapper<CrmReceivable> {
}

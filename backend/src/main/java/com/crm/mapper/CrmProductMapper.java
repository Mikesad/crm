package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品 Mapper
 *
 * <p>产品为公共资源，无 owner_user_id 字段，不入
 * {@code CrmDataPermissionHandler.MANAGED_TABLES}，所有角色可访问。</p>
 */
@Mapper
public interface CrmProductMapper extends BaseMapper<CrmProduct> {
}

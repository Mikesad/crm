package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.CrmContractProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同明细 Mapper
 *
 * <p>append-only,无 is_deleted,删除时跟随合同主表走逻辑删除;
 * 实际查询通过 contract_id 关联,无需数据权限拦截。</p>
 */
@Mapper
public interface CrmContractProductMapper extends BaseMapper<CrmContractProduct> {
}

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

    /**
     * 按分类ID统计产品数(用于产品分类页"关联产品数"列展示,
     * 以及删除分类前的引用校验,product_category.list & product_category.delete)。
     */
    default Long countByCategoryId(Long categoryId) {
        return selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CrmProduct>()
            .eq("category_id", categoryId)
            .eq("is_deleted", 0)
        );
    }
}


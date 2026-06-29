package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 产品分类
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_product_category} 表。</p>
 *
 * <p>V1 仅一级分类,parentId=0 表示顶级;阶段六 commit 2 新增 entity/Service/Controller。
 * 删除前需校验是否被 {@code crm_product.category_id} 引用,引用数 > 0 不允许删除。</p>
 */
@Data
@TableName("crm_product_category")
public class CrmProductCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 父分类ID(V1 暂不开放多级,固定填 0) */
    private Long parentId;

    /** 分类名称 */
    private String categoryName;

    /** 关联产品数(VO 层填充,不在表里) */
    @TableField(exist = false)
    private Integer productCount;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}

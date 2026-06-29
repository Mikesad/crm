package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_product} 表。</p>
 *
 * <p>产品为公共资源，无 owner_user_id，不受数据权限拦截。状态：0 下架 / 1 上架。</p>
 */
@Data
@TableName("crm_product")
public class CrmProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 产品分类 ID */
    private Long categoryId;

    /** 产品编码 / SKU，全局唯一 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 规格型号 */
    private String spec;

    /** 标准售价 */
    private BigDecimal price;

    /** 单位 (个/套/人天/班 等) */
    private String unit;

    /** 状态：0 下架 / 1 上架 */
    private Integer status;

    // v0.7:移除套餐线 / 计费周期字段(撤销 D4 中度 SaaS 升级)

    private String createBy;

    private LocalDateTime createTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}

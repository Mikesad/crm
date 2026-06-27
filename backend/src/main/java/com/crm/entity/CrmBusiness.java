package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商机
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_business} 表。</p>
 *
 * <p>阶段严格单向流转（由 {@code BusinessService.updateStage()} 强制）：</p>
 * <pre>
 *   需求分析 → 方案报价 → 商务谈判 → 赢单
 *                                 ↘ 输单（任意阶段可转输单，但不可逆）
 * </pre>
 */
@Data
@TableName("crm_business")
public class CrmBusiness implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联客户 ID */
    private Long customerId;

    /** 商机名称/项目名 */
    private String businessName;

    /** 预计金额，使用 BigDecimal 禁止浮点 */
    private BigDecimal expectedAmount;

    /** 预计结单日期 */
    private LocalDate expectedDealDate;

    /**
     * 商机阶段
     * <p>存中文：需求分析 / 方案报价 / 商务谈判 / 赢单 / 输单</p>
     * <p>阶段校验在 Service 层做，不依赖数据库 CHECK。</p>
     */
    private String stage;

    /** 商机负责人 ID */
    private Long ownerUserId;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}

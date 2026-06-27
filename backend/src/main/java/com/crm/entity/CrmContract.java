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
 * 合同
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_contract} 表。</p>
 *
 * <p>受数据权限拦截（{@code CrmDataPermissionHandler.MANAGED_TABLES} 含本表）。</p>
 *
 * <p>状态：0 审批中 / 1 执行中 / 2 已结束 / 3 已作废。
 * 折扣 &lt; 8.5 折时 status=0 需总监审批；审批通过改 1，回款全清改 2。</p>
 */
@Data
@TableName("crm_contract")
public class CrmContract implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 合同编号,全局唯一,业务层生成 HT-YYYYMMDD-XXXX */
    private String contractNum;

    /** 合同名称 */
    private String contractName;

    /** 客户 ID */
    private Long customerId;

    /** 商机 ID,可空(从商机赢单跳转时填入) */
    private Long businessId;

    /** 合同总金额,后端按明细重算后落库 */
    private BigDecimal totalAmount;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 状态:0 审批中 / 1 执行中 / 2 已结束 / 3 已作废 */
    private Integer status;

    /** 签约人/负责人 ID */
    private Long ownerUserId;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}

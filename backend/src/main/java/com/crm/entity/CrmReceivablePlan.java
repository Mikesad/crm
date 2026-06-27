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
 * 回款计划
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_receivable_plan} 表。</p>
 *
 * <p>状态：0 未到期 / 1 催款中 / 2 已回款。状态 0→2 由
 * {@code ReceivableEventListener} 监听 {@code ReceivableRecordedEvent} 自动联动。</p>
 *
 * <p>无 owner_user_id,跟随 contract 走权限,V1 不入数据权限拦截,靠
 * {@code @SaCheckPermission} 兜底。</p>
 */
@Data
@TableName("crm_receivable_plan")
public class CrmReceivablePlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 合同 ID */
    private Long contractId;

    /** 期数(第几期付款,1/2/3...) */
    private Integer period;

    /** 预计回款金额 */
    private BigDecimal expectedAmount;

    /** 预计回款日期 */
    private LocalDate expectedDate;

    /** 状态:0 未到期 / 1 催款中 / 2 已回款 */
    private Integer status;

    /** 备注 */
    private String remark;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}

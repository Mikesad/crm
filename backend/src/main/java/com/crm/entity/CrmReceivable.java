package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回款记录 (append-only,无 is_deleted)
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_receivable} 表。</p>
 *
 * <p>录入时发 {@code ReceivableRecordedEvent} 事件,由 {@code ReceivableEventListener}
 * 异步联动 crm_receivable_plan.status 与 crm_contract.status。</p>
 *
 * <p>{@code plan_id} 允许 NULL（计划外回款,如押金）,不影响 plan/contract 状态联动。</p>
 */
@Data
@TableName("crm_receivable")
public class CrmReceivable implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 回款编号/流水号,业务层生成 SK-YYYYMMDD-XXXX */
    private String receivableNum;

    /** 合同 ID */
    private Long contractId;

    /** 对应回款计划 ID,可空 */
    private Long planId;

    /** 实际回款金额 */
    private BigDecimal actualAmount;

    /** 实际回款日期 */
    private LocalDate returnDate;

    /** 支付方式:银行转账/微信/支付宝/现金 */
    private String paymentMethod;

    private String createBy;
    private LocalDateTime createTime;
}

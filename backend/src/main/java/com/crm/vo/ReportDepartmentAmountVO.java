package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门业绩单元(报表 Tab ① 部门柱状 + Tab ② 占比饼图共用)
 *
 * <p>阶段八 commit 2 升级(C2-D4):拆 2 个口径——</p>
 * <ul>
 *   <li>{@link #amount} / {@link #percent} —— 合同业绩(销售口径,crm_contract.total_amount)</li>
 *   <li>{@link #receivedAmount} / {@link #receivedPercent} —— 实际回款(财务口径,crm_receivable.actual_amount)</li>
 * </ul>
 *
 * <p>前端 chip tab 切换"合同业绩 / 实际回款"展示对应字段。两种口径都按合同 status IN (1,2) 过滤(排除审批中/已作废)。</p>
 */
@Data
@Schema(description = "部门业绩单元")
public class ReportDepartmentAmountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 部门 ID(可空,公海/无主业绩时为 null) */
    private Long deptId;

    /** 部门名(真实名称,来自 sys_dept.dept_name,阶段八 commit 2·C2-D1) */
    private String deptName;

    /** 合同业绩金额(BigDecimal → toPlainString),对应 crm_contract.total_amount WHERE status IN (1,2) */
    private String amount;

    /** 合同业绩占比百分比字符串,如 "47.0%" */
    private String percent;

    /** 实际回款金额(阶段八 commit 2·C2-D4 新增),对应 crm_receivable.actual_amount */
    private String receivedAmount;

    /** 实际回款占比百分比字符串,如 "32.5%" */
    private String receivedPercent;
}
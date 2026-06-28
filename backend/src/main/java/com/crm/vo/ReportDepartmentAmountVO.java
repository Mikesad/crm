package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门业绩单元(报表 Tab ① 部门柱状 + Tab ② 占比饼图共用)
 *
 * <p>用于横向柱状图(部门名 / 总额)和环形饼图(部门占比)。</p>
 */
@Data
@Schema(description = "部门业绩单元")
public class ReportDepartmentAmountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 部门 ID(可空,公海/无主业绩时为 null) */
    private Long deptId;

    /** 部门名 */
    private String deptName;

    /** 销售总额(BigDecimal → toPlainString) */
    private String amount;

    /** 占比百分比字符串,如 "47.0%" */
    private String percent;
}

package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报表中心通用查询条件（阶段五 commit 2）
 *
 * <p>14 个接口共用同一组筛选维度：时间范围 + 部门 + 人员。Service 层
 * 据此计算 [start, end] 闭区间再走各业务表聚合,避免 SQL 内 DATE_FORMAT
 * 走函数索引。</p>
 *
 * <p>范围枚举 {@code range}：today / week / month / quarter / year / custom,
 * 选 custom 时 {@code startDate} 与 {@code endDate} 必填。</p>
 */
@Data
@Schema(description = "报表通用查询条件")
public class ReportQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 时间范围枚举:today / week / month / quarter / year / custom */
    @Schema(description = "时间范围", example = "month",
            allowableValues = {"today", "week", "month", "quarter", "year", "custom"})
    private String range = "month";

    /** 自定义开始日期(yyyy-MM-dd),range=custom 时必填 */
    @Schema(description = "自定义开始日期", example = "2026-06-01")
    private String startDate;

    /** 自定义结束日期(yyyy-MM-dd),range=custom 时必填 */
    @Schema(description = "自定义结束日期", example = "2026-06-28")
    private String endDate;

    /** 部门 ID(可空,空=全部部门;data_scope=5 时被强制覆盖为当前用户) */
    @Schema(description = "部门 ID", example = "2")
    private Long deptId;

    /** 销售/用户 ID(可空,空=全部人员;data_scope=5 时被强制覆盖为当前用户) */
    @Schema(description = "用户 ID", example = "4")
    private Long userId;

    /** 趋势粒度:day / month(仅 trend 接口使用,默认 day) */
    @Schema(description = "趋势粒度", example = "day",
            allowableValues = {"day", "month"})
    private String granularity = "day";

    /** 维度枚举(仅 customer.distribution 接口使用):industry / region / level / source */
    @Schema(description = "客户分布维度", example = "industry",
            allowableValues = {"industry", "region", "level", "source"})
    private String dim;

    /** 排行 TopN(仅 performer / top 接口使用,默认 5,最大 50) */
    @Schema(description = "TopN", example = "5")
    private Integer topN = 5;

    /** 报表类型(仅 trend 接口使用):contract / lead / record / receivable */
    @Schema(description = "趋势指标类型", example = "contract",
            allowableValues = {"contract", "lead", "record", "receivable"})
    private String metric;
}

package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报表 Tab ③ 4 个切换统计指标(阶段八 commit 4·2026-06-30)
 *
 * <p>前端 4 个 tab 切换,每 tab 取一个 {@link ConversionMetric} 展示;</p>
 * <p>分子分母都给,方便做"xx / yy (xx%)"展示。</p>
 */
@Data
@Schema(description = "跟进与转化率 4 个切换指标")
public class ReportConversionMetricsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 跟进率:期内 last_follow_time ∈ [start,end] 的客户数 / 全量客户数 */
    private ConversionMetric followRate;

    /** 客户转换率:期内 status=3(已转客户)的线索数 / 期内创建的线索总数(线索 → 客户) */
    private ConversionMetric leadToCustomerRate;

    /** 商机转换率:期内新增商机数 / 期内新增客户数(客户 → 商机) */
    private ConversionMetric leadToBusinessRate;

    /**
     * 单个指标:分子 / 分母 / 比率 / 描述
     */
    @Data
    public static class ConversionMetric implements Serializable {
        private static final long serialVersionUID = 1L;

        /** key:followRate / leadToCustomerRate / leadToBusinessRate */
        private String key;

        /** 比率(字符串,保留 1 位小数 + %,如 "23.5%") */
        private String rate;

        /** 分子数值 */
        private long numerator;

        /** 分子含义(如"被跟进客户数") */
        private String numeratorLabel;

        /** 分母数值 */
        private long denominator;

        /** 分母含义(如"客户总数") */
        private String denominatorLabel;

        /** 一句话描述口径(给前端副标题) */
        private String description;
    }
}
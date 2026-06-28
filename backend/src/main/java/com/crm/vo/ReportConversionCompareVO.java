package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 团队 vs 全公司 转化率对比(报表 Tab ③)
 *
 * <p>用于柱状图(双 series)。{@code group} 取 team / company,每个 group
 * 5 个阶段各有一个 {@code value} 字段(阶段转化率百分比)。</p>
 */
@Data
@Schema(description = "团队 vs 全公司 转化率对比")
public class ReportConversionCompareVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分组:team / company */
    private String group;

    /** 阶段 1(新建线索)转化率(全公司默认 100) */
    private String stage1Lead;

    /** 阶段 2(需求分析)转化率 */
    private String stage2Analysis;

    /** 阶段 3(方案报价)转化率 */
    private String stage3Quote;

    /** 阶段 4(商务谈判)转化率 */
    private String stage4Negotiate;

    /** 阶段 5(赢单)转化率 */
    private String stage5Win;
}

package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报表中心趋势点(跨 Tab 复用)
 *
 * <p>折线/柱状图/堆叠图每个数据点都走这个 VO。{@code date} 格式
 * 由 {@code granularity} 决定(粒度 day → "MM-dd";month → "yyyy-MM"),
 * 前端直接当 x 轴标签用。</p>
 */
@Data
@Schema(description = "趋势点")
public class ReportTrendPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据点标签(粒度 day → "MM-dd";month → "yyyy-MM") */
    private String date;

    /** 数值(字符串形式,BigDecimal → toPlainString 防止科学计数) */
    private String value;

    /** 维度 key(可选,堆叠/多 series 时用,如 "received" / "predicted") */
    private String seriesKey;
}

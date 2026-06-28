package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报表中心 KPI 单元(跨 Tab 复用)
 *
 * <p>6 KPI 密集条 + 单个 KPI 卡都走这个 VO。前端用 {@code value} 直接渲染,
 * {@code delta} 显示 ↑/↓ 同比,正绿负红(不染色仅靠符号),单位由前端拼接
 * "¥" / "%" / "单"。</p>
 */
@Data
@Schema(description = "KPI 单元")
public class ReportKpiVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 指标 ID(前端 i18n key,如 totalAmount / contractCount / winRate) */
    private String key;

    /** 指标名(中文 label,如"销售总额" / "赢单率") */
    private String label;

    /** 当前值(字符串形式,前端按格式解析;BigDecimal → toPlainString 防止科学计数) */
    private String value;

    /** 单位(可空;前端按 value 拼接,如"单" / "%") */
    private String unit;

    /** 同比字符串,如 "↑ 18.6%" / "↓ 3.2%" / "↑ 2.1 pp" / null(无对比) */
    private String delta;

    /** 同比方向:up / down / null(无对比) */
    private String deltaDir;

    /** 脚注说明(可空,如"vs 上月") */
    private String footnote;
}

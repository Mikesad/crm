package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 维度分布项(报表 Tab ① 客户来源 / Tab ② 行业-地区-等级 共用)
 *
 * <p>用于饼图 / 横向柱状图 / 漏斗辅助列表。</p>
 */
@Data
@Schema(description = "维度分布项")
public class ReportDistItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 维度 key(行业/地区/等级/来源 名) */
    private String key;

    /** 数量 */
    private Long count;

    /** 金额(BigDecimal → toPlainString;可空,客户来源分布无金额) */
    private String amount;

    /** 占比百分比字符串 */
    private String percent;
}

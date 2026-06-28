package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 账龄分布桶(报表 Tab ④)
 *
 * <p>按计划回款日期与今天的差值分桶:</p>
 * <ul>
 *   <li>0-30 天:正常</li>
 *   <li>31-60 天:关注</li>
 *   <li>61-90 天:警告</li>
 *   <li>90+ 天:严重逾期</li>
 * </ul>
 *
 * <p>用 {@code key} 给前端配色(0-30 用 accent 绿,90+ 用 danger 红)。</p>
 */
@Data
@Schema(description = "账龄分布桶")
public class ReportAgingBucketVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 桶 key(0-30 / 31-60 / 61-90 / 90+) */
    private String key;

    /** 桶显示名 */
    private String label;

    /** 应收笔数 */
    private Long count;

    /** 应收金额(BigDecimal → toPlainString) */
    private String amount;

    /** 占比百分比字符串 */
    private String percent;
}

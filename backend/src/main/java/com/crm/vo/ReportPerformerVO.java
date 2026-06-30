package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报表中心排行(销售个人榜 / 客户贡献榜共用)
 *
 * <p>Tab ① 销售榜:rank / name / count / amount<br>
 * Tab ④ 客户榜:rank / name / industry / count / amount / overdue</p>
 */
@Data
@Schema(description = "排行项")
public class ReportPerformerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排名(从 1 开始) */
    private Integer rank;

    /** 名称(销售名 / 客户名) */
    private String name;

    /** 副标题(可空,客户榜为"行业名") */
    private String subtitle;

    /** 单数(销售榜:合同数;客户榜:合同数) */
    private Integer count;

    /** 金额(BigDecimal → toPlainString;销售榜为销售总额,客户榜为累计合同金额) */
    private String amount;

    /** 实际回款金额(阶段八 P3·2026-06-29 新增,销售榜 chip tab 切换"实际回款"口径时使用) */
    private String receivedAmount;

    /** 转化率(可空,仅销售榜) */
    private String convRate;

    /** 逾期金额(可空,仅客户榜) */
    private String overdueAmount;
}

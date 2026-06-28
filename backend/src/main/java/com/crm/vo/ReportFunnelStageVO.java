package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报表 Tab ① 漏斗 5 阶段单元
 *
 * <p>阶段固定 5 个:① 新建线索 → ② 需求分析 → ③ 方案报价 → ④ 商务谈判 → ⑤ 赢单。
 * 漏斗图用 {@code count} 做宽度,平均金额(可选)用于 tooltip 富化。</p>
 */
@Data
@Schema(description = "漏斗阶段")
public class ReportFunnelStageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 阶段 key(固定 5 个,前端按 key 渲染阶段名) */
    private String stage;

    /** 阶段显示名(中文) */
    private String stageName;

    /** 商机数 */
    private Long count;

    /** 阶段累计金额(BigDecimal → toPlainString,可空) */
    private String amount;

    /** 阶段转化率(相对上一阶段;首阶段=null) */
    private String convRate;
}

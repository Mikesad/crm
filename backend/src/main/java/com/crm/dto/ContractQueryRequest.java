package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 合同分页查询请求
 */
@Data
@Schema(description = "合同分页查询请求")
public class ContractQueryRequest extends PageQuery {

    @Schema(description = "关键字 (合同编号/合同名称 模糊匹配)", example = "HT-2026")
    private String keyword;

    @Schema(description = "客户 ID 精确过滤")
    private Long customerId;

    @Schema(description = "状态:0 审批中 / 1 执行中 / 2 已结束 / 3 已作废")
    private Integer status;
}

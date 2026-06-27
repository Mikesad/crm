package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同响应（详情包含明细 items）
 */
@Data
@Schema(description = "合同响应")
public class ContractVO {

    private Long id;
    private String contractNum;
    private String contractName;

    private Long customerId;
    private String customerName;

    private Long businessId;

    private BigDecimal totalAmount;
    private LocalDate startDate;
    private LocalDate endDate;

    @Schema(description = "状态:0 审批中 / 1 执行中 / 2 已结束 / 3 已作废")
    private Integer status;
    private String statusText;

    private Long ownerUserId;
    private String ownerName;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Schema(description = "合同明细(仅详情接口返回)")
    private List<ContractItemVO> items;
}

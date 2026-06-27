package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 线索导入结果
 */
@Data
@Schema(description = "线索 Excel 导入结果")
public class LeadImportResultVO {

    @Schema(description = "本次读取的总行数")
    private Integer totalRows;

    @Schema(description = "成功导入条数")
    private Integer successRows;

    @Schema(description = "失败条数(空行/字段缺失/状态错误等)")
    private Integer failRows;

    @Schema(description = "失败明细,行号 -> 原因")
    private java.util.Map<Integer, String> errors;
}

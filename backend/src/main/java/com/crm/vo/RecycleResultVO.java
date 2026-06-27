package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 公海池回收结果
 */
@Data
@Schema(description = "公海池回收结果")
public class RecycleResultVO {

    @Schema(description = "本次使用的阈值秒数")
    private Long thresholdSeconds;

    @Schema(description = "本次使用的 limit")
    private Integer limit;

    @Schema(description = "本次是否为 dry run")
    private Boolean dryRun;

    @Schema(description = "扫描到符合回收条件的客户数")
    private Integer scanned;

    @Schema(description = "实际回收的客户数(dryRun 时为 0)")
    private Integer recycled;

    @Schema(description = "耗时毫秒")
    private Long durationMs;

    @Schema(description = "本次扫描到的客户详情(最多 limit 条)")
    private List<RecycledCustomerVO> details;
}

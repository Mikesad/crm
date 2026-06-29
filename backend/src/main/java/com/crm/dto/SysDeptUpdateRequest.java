package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新部门请求
 *
 * <p>{@code parentId} 可变,但变更会触发祖先链重建(事务内刷所有后代 ancestors)。</p>
 */
@Data
@Schema(description = "更新部门请求")
public class SysDeptUpdateRequest {

    @Schema(description = "部门 ID", example = "1")
    @NotNull(message = "部门 ID 不能为空")
    private Long id;

    @Schema(description = "部门名称", example = "华北销售部")
    private String deptName;

    @Schema(description = "上级部门 ID,变更会触发祖先链重建", example = "1")
    private Long parentId;

    @Schema(description = "显示顺序", example = "5")
    private Integer orderNum;

    @Schema(description = "状态 0 停用 / 1 正常", example = "1")
    private Integer status;
}

package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新建部门请求
 *
 * <p>{@code parentId=0} 表示顶级(总公司);V1 暂不开放普通用户创建顶级,仅总公司下挂子部门。</p>
 */
@Data
@Schema(description = "新建部门请求")
public class SysDeptCreateRequest {

    @Schema(description = "部门名称", example = "华北销售部")
    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    @Schema(description = "上级部门 ID,0 表示顶级", example = "1")
    @NotNull(message = "上级部门不能为空")
    private Long parentId;

    @Schema(description = "显示顺序(同级从小到大)", example = "5")
    @NotNull(message = "排序不能为空")
    private Integer orderNum;

    @Schema(description = "状态 0 停用 / 1 正常", example = "1")
    private Integer status;
}

package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新角色请求
 *
 * <p>注：{@code roleKey} 不允许修改（作为角色代码稳定不变）。</p>
 */
@Data
@Schema(description = "更新角色请求")
public class SysRoleUpdateRequest {

    @Schema(description = "角色 ID", example = "1")
    @NotNull(message = "角色 ID 不能为空")
    private Long id;

    @Schema(description = "角色名称", example = "大区经理")
    private String roleName;

    @Schema(description = "数据范围 1-5", example = "3")
    private Integer dataScope;

    @Schema(description = "状态 0 停用 / 1 正常", example = "1")
    private Integer status;

    @Schema(description = "绑定的菜单 ID 列表（为 null 表示不修改菜单）")
    private List<Long> menuIds;
}

package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 新建角色请求
 */
@Data
@Schema(description = "新建角色请求")
public class SysRoleCreateRequest {

    @Schema(description = "角色名称", example = "大区经理")
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @Schema(description = "角色权限字符串,全局唯一,如 regional_manager", example = "regional_manager")
    @NotBlank(message = "roleKey 不能为空")
    private String roleKey;

    @Schema(description = "数据范围 1-5", example = "3")
    @NotNull(message = "dataScope 不能为空")
    private Integer dataScope;

    @Schema(description = "状态 0 停用 / 1 正常", example = "1")
    private Integer status;

    @Schema(description = "绑定的菜单 ID 列表")
    private List<Long> menuIds;
}

package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新菜单请求
 */
@Data
@Schema(description = "更新菜单请求")
public class SysMenuUpdateRequest {

    @Schema(description = "菜单 ID", example = "25")
    @NotNull(message = "菜单 ID 不能为空")
    private Long id;

    @Schema(description = "菜单名称", example = "用户管理")
    private String menuName;

    @Schema(description = "父菜单 ID", example = "24")
    private Long parentId;

    @Schema(description = "显示顺序", example = "1")
    private Integer orderNum;

    @Schema(description = "路由地址", example = "system/user")
    private String path;

    @Schema(description = "组件路径", example = "system/user")
    private String component;

    @Schema(description = "菜单类型 M 目录 / C 菜单 / F 按钮")
    private String menuType;

    @Schema(description = "权限标识", example = "sys:user:list")
    private String perms;

    @Schema(description = "状态 0 隐藏 / 1 显示", example = "1")
    private Integer status;
}

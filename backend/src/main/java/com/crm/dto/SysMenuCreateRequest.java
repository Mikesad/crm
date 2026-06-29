package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 新建菜单请求
 */
@Data
@Schema(description = "新建菜单请求")
public class SysMenuCreateRequest {

    @Schema(description = "菜单名称", example = "用户管理")
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    @Schema(description = "父菜单 ID,0 表示顶级", example = "0")
    @NotNull(message = "parentId 不能为空")
    private Long parentId;

    @Schema(description = "显示顺序", example = "1")
    private Integer orderNum;

    @Schema(description = "路由地址(M/C 类型必填)", example = "system/user")
    private String path;

    @Schema(description = "组件路径(C 类型必填)", example = "system/user")
    private String component;

    @Schema(description = "菜单类型 M 目录 / C 菜单 / F 按钮", example = "C",
            allowableValues = {"M", "C", "F"})
    @NotBlank(message = "menuType 不能为空")
    @Pattern(regexp = "^[MCF]$", message = "menuType 只能为 M/C/F")
    private String menuType;

    @Schema(description = "权限标识,F 按钮必填,如 sys:user:edit", example = "sys:user:list")
    private String perms;

    @Schema(description = "状态 0 隐藏 / 1 显示", example = "1")
    private Integer status;
}

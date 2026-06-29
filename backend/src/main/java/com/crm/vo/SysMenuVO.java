package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单响应
 */
@Data
@Schema(description = "菜单响应")
public class SysMenuVO {

    private Long id;
    private String menuName;
    private Long parentId;
    private Integer orderNum;
    private String path;
    private String component;

    /** M 目录 / C 菜单 / F 按钮 */
    private String menuType;
    private String menuTypeText;

    private String perms;

    /** 0 隐藏 / 1 显示 */
    private Integer status;
    private String statusText;

    /** 子菜单(树形用) */
    private List<SysMenuVO> children = new ArrayList<>();
}

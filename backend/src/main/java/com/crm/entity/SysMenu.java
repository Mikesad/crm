package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜单与功能权限
 *
 * <p>{@code menuType}：M 目录 / C 菜单 / F 按钮；
 * 仅 {@code perms} 非空的记录参与 {@code @SaCheckPermission} 鉴权。</p>
 *
 * <p>注：sys_menu 表无 is_deleted 字段，不启用逻辑删除。</p>
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String menuName;

    private Long parentId;

    private Integer orderNum;

    private String path;

    private String component;

    /** M 目录 / C 菜单 / F 按钮 */
    private String menuType;

    /** 权限标识，如 {@code crm:customer:delete} */
    private String perms;

    /** 0 隐藏 / 1 显示 */
    private Integer status;
}
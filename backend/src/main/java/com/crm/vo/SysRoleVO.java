package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色响应
 */
@Data
@Schema(description = "角色响应")
public class SysRoleVO {

    private Long id;
    private String roleName;
    private String roleKey;
    private Integer dataScope;
    private String dataScopeText;
    private Integer status;
    private String statusText;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 绑定此角色的用户数 */
    private Integer userCount;

    /** 已绑定的菜单 ID 列表(角色详情/编辑用) */
    private List<Long> menuIds = new ArrayList<>();
}

package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户响应（前端用）
 */
@Data
@Schema(description = "用户响应")
public class SysUserVO {

    private Long id;
    private Long deptId;
    private String deptName;

    private String username;
    private String nickname;
    private String phone;
    private String email;

    /** 0 男 / 1 女 / 2 未知 */
    private Integer sex;
    private String sexText;

    /** 0 停用 / 1 正常 */
    private Integer status;
    private String statusText;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 角色 ID 列表 */
    private List<Long> roleIds = new ArrayList<>();

    /** 角色名列表（前端 chip 展示用） */
    private List<String> roleNames = new ArrayList<>();
}

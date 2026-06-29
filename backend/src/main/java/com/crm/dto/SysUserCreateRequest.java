package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新建用户请求
 */
@Data
@Schema(description = "新建用户请求")
public class SysUserCreateRequest {

    @Schema(description = "账号（全局唯一,不可含空格）", example = "sales_zhao")
    @NotBlank(message = "账号不能为空")
    @Size(max = 30, message = "账号长度不能超过 30")
    private String username;

    @Schema(description = "昵称", example = "赵销售")
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "初始密码（可空,默认 123456）", example = "123456")
    private String password;

    @Schema(description = "手机号", example = "13800000010")
    private String phone;

    @Schema(description = "邮箱", example = "zhao@zencrm.local")
    private String email;

    @Schema(description = "性别：0 男 / 1 女 / 2 未知", example = "0")
    private Integer sex;

    @Schema(description = "状态：0 停用 / 1 正常", example = "1")
    private Integer status;

    @Schema(description = "绑定的角色 ID 列表")
    private List<Long> roleIds;
}

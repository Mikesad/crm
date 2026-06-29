package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新用户请求
 *
 * <p>注：{@code username} 不允许修改（登录账号稳定不变）。</p>
 */
@Data
@Schema(description = "更新用户请求")
public class SysUserUpdateRequest {

    @Schema(description = "用户 ID", example = "1")
    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    @Schema(description = "昵称", example = "赵销售")
    private String nickname;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "手机号", example = "13800000010")
    private String phone;

    @Schema(description = "邮箱", example = "zhao@zencrm.local")
    private String email;

    @Schema(description = "性别：0 男 / 1 女 / 2 未知", example = "0")
    private Integer sex;

    @Schema(description = "状态：0 停用 / 1 正常", example = "1")
    private Integer status;

    @Schema(description = "绑定的角色 ID 列表（为 null 表示不修改角色）")
    private List<Long> roleIds;
}

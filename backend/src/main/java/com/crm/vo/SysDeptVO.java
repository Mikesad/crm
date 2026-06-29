package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门响应(前端用)
 *
 * <p>每个字段都从 sys_dept 直出或衍生,前端无论 tree/list/detail 用同一 VO 减少类型转换。</p>
 */
@Data
@Schema(description = "部门响应")
public class SysDeptVO {

    private Long id;
    private Long parentId;

    /** 上级部门名称(联 sys_dept 派生,V1 仅 list/tree 用) */
    private String parentName;

    /** 祖级路径,逗号分隔,如 0,1,2 */
    private String ancestors;

    private String deptName;
    private Integer orderNum;

    /** 0 停用 / 1 正常 */
    private Integer status;
    private String statusText;

    /** 直接子部门数 */
    private Integer childCount;

    /** 启用中的用户数 */
    private Integer userCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

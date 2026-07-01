package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统角色
 *
 * <p>{@code dataScope} 取值：1 全部 / 2 自定义 / 3 本部门组 / 4 已废弃 / 5 仅本人（phase8 commit1 拆档）。</p>
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String roleName;

    /** 角色权限字符串，如 admin / sales / finance */
    private String roleKey;

    /**
     * 数据范围
     * <ul>
     *   <li>1 - 全部</li>
     *   <li>2 - 自定义（暂未实现，留扩展）</li>
     *   <li>3 - 本部门组（我的部门 + 同 parent_id 的所有兄弟部门）</li>
     *   <li>4 - 已废弃（phase8 commit1 拆档，DB 历史值会被代码兜底为仅本人）</li>
     *   <li>5 - 仅本人</li>
     * </ul>
     */
    private Integer dataScope;

    /** 0 停用 / 1 正常 */
    private Integer status;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
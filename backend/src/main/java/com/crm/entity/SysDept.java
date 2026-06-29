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
 * 系统部门
 *
 * <p>{@code ancestors} 字段以逗号分隔记录从顶级到当前节点的 ID 路径,
 * 例: {@code 0,1,2}。
 * 阶段七 commit:补齐 entity + 后续 ancestors 维护 / 子部门查询。</p>
 *
 * <p>{@code parentId=0} 表示顶级(总公司等)。</p>
 */
@Data
@TableName("sys_dept")
public class SysDept implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 父部门 ID,0 表示顶级 */
    private Long parentId;

    /** 祖级列表,逗号分隔,例 0,1,2 */
    private String ancestors;

    private String deptName;

    /** 同级显示顺序,从小到大 */
    private Integer orderNum;

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

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
 * 线索
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_lead} 表。</p>
 *
 * <p>状态机：1 未跟进 → 2 跟进中 → 3 已转客户 → 4 已死线索（单向流转，转客户后不可恢复）。</p>
 */
@Data
@TableName("crm_lead")
public class CrmLead implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 线索名称/公司暂定名 */
    private String leadName;

    /** 联系人姓名 */
    private String contactName;

    /** 电话/手机 */
    private String phone;

    /** 线索来源（如：广告/展会/线上留单） */
    private String source;

    /** 状态：1 未跟进 / 2 跟进中 / 3 已转客户 / 4 已死线索 */
    private Integer status;

    /** 负责人 ID（逻辑关联 sys_user.id，受 dataScope 约束） */
    private Long ownerUserId;

    /** 备注描述 */
    private String remark;

    /** 死线索原因(可选,阶段五新增) */
    private String deadReason;

    /** 死线索标记时间(阶段五新增) */
    private LocalDateTime deadTime;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}

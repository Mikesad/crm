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
 * 合同审批
 *
 * <p>状态：0 待审 / 1 通过 / 2 驳回 / 3 撤回。</p>
 *
 * <p>无 owner_user_id 字段,权限语义跟随 contract 走;在 V1 仅靠
 * {@code @SaCheckPermission("crm:contract:approve")} 兜底,DataPermissionHandler 暂不扩展。</p>
 */
@Data
@TableName("crm_approval")
public class CrmApproval implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 合同 ID */
    private Long contractId;

    /** 申请人(销售)ID */
    private Long applicantId;

    /** 审批人(总监)ID */
    private Long approverId;

    /** 状态:0 待审 / 1 通过 / 2 驳回 / 3 撤回 */
    private Integer status;

    /** 触发原因(如:折扣 8.4 折,低于 8.5 折审批线) */
    private String triggerReason;

    /** 审批意见/驳回原因 */
    private String comment;

    /** 审批完成时间 */
    private LocalDateTime finishTime;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}

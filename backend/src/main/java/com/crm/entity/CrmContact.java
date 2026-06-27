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
 * 联系人
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_contact} 表。</p>
 *
 * <p>线索转客户时，{@code LeadService.convertToCustomer()} 会同时创建一条主联系人记录。</p>
 */
@Data
@TableName("crm_contact")
public class CrmContact implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属客户 ID（逻辑关联 crm_customer.id） */
    private Long customerId;

    /** 联系人姓名 */
    private String contactName;

    /** 职务/职位 */
    private String post;

    /** 手机号 */
    private String phone;

    /** 是否为主联系人：0 否 / 1 是 */
    private Integer isMaster;

    /**
     * 决策权重
     * <p>1 核心决策者 / 2 弱影响者 / 3 普通职员</p>
     */
    private Integer decisionWeight;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}

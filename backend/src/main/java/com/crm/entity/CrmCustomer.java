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
 * 客户
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_customer} 表。</p>
 *
 * <p>公海规则：ownerUserId 为 NULL 且 isPublic=1 表示在公海池，阶段四定时回收时填入。</p>
 */
@Data
@TableName("crm_customer")
public class CrmCustomer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 客户公司/主体名称 */
    private String customerName;

    /** 所属行业 */
    private String industry;

    /**
     * 客户级别
     * <p>A 重要客户 / B 普通客户 / C 意向客户</p>
     */
    private String level;

    /** 归属销售 ID；为 NULL 且 isPublic=1 表示在公海 */
    private Long ownerUserId;

    /** 是否为公海客户：0 私海 / 1 公海 */
    private Integer isPublic;

    /** 最后跟进时间，用于公海回收判定 */
    private LocalDateTime lastFollowTime;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}

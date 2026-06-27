package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户共享表
 *
 * <p>阶段四新增;让主销售把私海客户共享给其他同事,支持只读/读写两档权限。</p>
 */
@Data
@TableName("crm_customer_share")
public class CrmCustomerShare implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 客户 ID */
    private Long customerId;

    /** 被共享人 ID */
    private Long userId;

    /** 权限类型:1 只读 / 2 读写 */
    private Integer authType;

    /** 发起人(主销售 username) */
    private String createBy;

    /** 共享时间 */
    private LocalDateTime createTime;
}

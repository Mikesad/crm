package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 跟进记录
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_record} 表。</p>
 *
 * <p><b>该表无 is_deleted 字段，无 update_by/update_time 字段</b>——跟进记录只能新增，不能修改或删除，
 * 这是 CRM 行业惯例（审计追溯）。</p>
 *
 * <p>关联主体由 {@code relatedType} + {@code relatedId} 锁定，支持：</p>
 * <ul>
 *   <li>lead 线索</li>
 *   <li>customer 客户</li>
 *   <li>business 商机</li>
 * </ul>
 */
@Data
@TableName("crm_record")
public class CrmRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联类型：lead / customer / business */
    private String relatedType;

    /** 对应的关联主体 ID */
    private Long relatedId;

    /** 跟进内容/沟通纪要 */
    private String content;

    /** 跟进方式：电话 / 微信 / 上门拜访 / 邮件 */
    private String followType;

    /** 下次跟进时间 */
    private LocalDateTime nextFollowTime;

    /** 跟进人（销售昵称/账号） */
    private String createBy;

    /** 跟进时间 */
    private LocalDateTime createTime;
}

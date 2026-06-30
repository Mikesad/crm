package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户活跃度单元(报表 Tab ②)
 *
 * <p>阶段八 commit 8(2026-06-30)重构:只展示 <strong>总数 / 活跃 / 公海</strong> 三维,
 * 移除"沉睡"维度(用户反馈:沉睡数据不准,与公海归并含义有重叠)。</p>
 *
 * <ul>
 *   <li>{@code total}    总在管客户数(owner_user_id ∈ ownerIds, 不含公海)</li>
 *   <li>{@code active}   期内被跟进客户数(last_follow_time ∈ [start, end] 且 is_public=0)</li>
 *   <li>{@code publicPool} 期内新增到公海客户数(is_public=1 且 create_time ∈ [start, end])</li>
 * </ul>
 */
@Data
@Schema(description = "客户活跃度")
public class ReportActivityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总在管客户数 */
    private Long total;

    /** 期内活跃数 */
    private Long active;

    /** 期内公海数 */
    private Long publicPool;

    /** 活跃占比 */
    private String activePercent;

    /** 公海占比 */
    private String publicPercent;
}
package com.crm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 跟进待办视图 VO（阶段五新增）
 *
 * <p>用于跟进中心 3 个 Tab 的列表渲染,聚合了 crm_record 与关联主体(crm_lead / crm_customer / crm_business / crm_contract)
 * 的关键信息,避免前端 N+1 查询。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code subjectName} 主体名(从关联表 JOIN 出来,如"蓝海科技")</li>
 *   <li>{@code subjectStatusText} 主体状态文字(如"方案报价"/"执行中"/"已死线索")</li>
 *   <li>{@code overdue} 是否逾期(nextFollowTime < 当前时间)</li>
 *   <li>{@code leadStatus} 仅 lead 类型:1未跟进 2跟进中 4已死,用于前端红边判断</li>
 * </ul>
 */
@Data
@Schema(description = "跟进待办视图")
public class RecordTodoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 跟进记录 ID */
    private Long recordId;

    /** 关联类型:lead / customer / business / contract */
    private String relatedType;

    /** 关联主体 ID */
    private Long relatedId;

    /** 主体名(关联表 JOIN 出来) */
    private String subjectName;

    /** 主体状态文字(可空) */
    private String subjectStatusText;

    /** 主体金额/预计金额(可空,仅 business / contract 有值) */
    private String subjectAmount;

    /** 跟进内容 */
    private String content;

    /** 跟进方式 */
    private String followType;

    /** 下次跟进时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime nextFollowTime;

    /** 跟进人 */
    private String createBy;

    /** 跟进时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createTime;

    /** 距下次跟进天数(正数未来,负数逾期,0=今天) */
    private Long daysUntilNext;

    /** 是否逾期(nextFollowTime < 当前时间) */
    private Boolean overdue;

    /** 线索状态(仅 lead 类型):1未跟进 2跟进中 4已死,用于前端红边判断 */
    private Integer leadStatus;
}
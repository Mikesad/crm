package com.crm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报表中心筛选下拉项(部门/人员)
 *
 * <p>前端顶部 2 个下拉直接渲染:全部部门 / 销售一部 / 销售二部,以及
 * 全部销售 / 李销售 / 陈销售。data_scope=5 (仅本人) 时只返回当前用户 1 项。</p>
 */
@Data
@Schema(description = "筛选下拉项")
public class ReportFilterOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 选项 ID */
    private Long id;

    /** 显示名 */
    private String name;
}

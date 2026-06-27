package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询基类
 *
 * <p>所有模块的列表查询都继承该类，统一 {@code pageNum=1} / {@code pageSize=10} 默认值。</p>
 */
@Data
public class PageQuery {

    @Schema(description = "页码（从 1 开始）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（最大 200）", example = "10")
    private Integer pageSize = 10;

    /**
     * 归一化为 MyBatis-Plus {@code Page} 所需的 current/size
     */
    public long normalizeCurrent() {
        return pageNum == null || pageNum < 1 ? 1L : pageNum.longValue();
    }

    public long normalizeSize() {
        if (pageSize == null || pageSize < 1) return 10L;
        return Math.min(pageSize.longValue(), 200L);
    }
}

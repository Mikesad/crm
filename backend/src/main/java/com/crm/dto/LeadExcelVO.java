package com.crm.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索 Excel 导入导出 VO
 *
 * <p>用 EasyExcel 3.3.4 的 {@code @ExcelProperty} + {@code @DateTimeFormat} 注解,
 * 字段顺序 = Excel 列顺序(导出从左到右,导入从左到右)。</p>
 */
@Data
@Schema(description = "线索 Excel 行")
public class LeadExcelVO {

    @ExcelProperty(value = "ID", index = 0)
    private Long id;

    @ExcelProperty(value = "线索名称", index = 1)
    private String leadName;

    @ExcelProperty(value = "联系人", index = 2)
    private String contactName;

    @ExcelProperty(value = "电话", index = 3)
    private String phone;

    @ExcelProperty(value = "线索来源", index = 4)
    private String source;

    @ExcelProperty(value = "状态", index = 5)
    private String statusText;        // 导出:未跟进/跟进中/已转客户/已死线索;导入:也按这 4 个文字解析

    @ExcelProperty(value = "负责人", index = 6)
    private String ownerName;         // 导出显示昵称,导入按 username/nickname 匹配

    @ExcelProperty(value = "备注", index = 7)
    private String remark;

    @ExcelProperty(value = "创建时间", index = 8)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

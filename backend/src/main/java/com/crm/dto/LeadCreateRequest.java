package com.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建线索请求
 */
@Data
@Schema(description = "创建线索请求")
public class LeadCreateRequest {

    @NotBlank(message = "线索名称不能为空")
    @Size(max = 100, message = "线索名称最长 100 字符")
    private String leadName;

    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 30, message = "姓名最长 30 字符")
    private String contactName;

    @Size(max = 20, message = "电话最长 20 字符")
    private String phone;

    @Size(max = 50, message = "来源最长 50 字符")
    private String source;

    @Size(max = 500, message = "备注最长 500 字符")
    private String remark;
}

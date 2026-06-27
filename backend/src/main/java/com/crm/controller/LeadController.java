package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.LeadConvertRequest;
import com.crm.dto.LeadCreateRequest;
import com.crm.dto.LeadImportResultVO;
import com.crm.dto.LeadQueryRequest;
import com.crm.dto.LeadUpdateRequest;
import com.crm.service.LeadService;
import com.crm.vo.LeadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 线索接口
 *
 * <p>路径前缀 /api/crm/lead（application.yml 已配置 context-path=/api）。</p>
 */
@Tag(name = "02. 线索管理", description = "线索 CRUD + 一键转客户")
@RestController
@RequestMapping("/crm/lead")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @Operation(summary = "分页查询线索")
    @SaCheckPermission("crm:lead:list")
    @GetMapping("/page")
    public Result<IPage<LeadVO>> page(LeadQueryRequest query) {
        return Result.success(leadService.page(query));
    }

    @Operation(summary = "线索详情")
    @SaCheckPermission("crm:lead:list")
    @GetMapping("/{id}")
    public Result<LeadVO> detail(@Parameter(description = "线索 ID") @PathVariable Long id) {
        return Result.success(leadService.detail(id));
    }

    @Operation(summary = "创建线索")
    @SaCheckPermission("crm:lead:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody LeadCreateRequest req) {
        return Result.success(leadService.create(req));
    }

    @Operation(summary = "更新线索")
    @SaCheckPermission("crm:lead:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody LeadUpdateRequest req) {
        leadService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除线索", description = "逻辑删除，is_deleted 置 1")
    @SaCheckPermission("crm:lead:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        leadService.delete(id);
        return Result.success();
    }

    @Operation(summary = "线索转客户",
        description = "事务内双写 crm_customer + crm_contact，并把原线索 status 置 3-已转客户")
    @SaCheckPermission("crm:lead:edit")
    @PostMapping("/{id}/convert")
    public Result<Long> convert(@PathVariable Long id, @Valid @RequestBody LeadConvertRequest req) {
        return Result.success(leadService.convertToCustomer(id, req));
    }

    @Operation(summary = "导出线索 Excel",
        description = "下载当前用户可见的全部线索(受 dataScope 过滤);xlsx 格式")
    @SaCheckPermission("crm:lead:list")
    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response) throws IOException {
        byte[] bytes = leadService.exportExcel();
        String filename = URLEncoder.encode("线索列表_" + System.currentTimeMillis() / 1000 + ".xlsx",
                StandardCharsets.UTF_8);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename);
        response.setContentLength(bytes.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
        }
    }

    @Operation(summary = "导入线索 Excel",
        description = "xlsx 格式;行级容错,失败行记入 errors 不影响其他行")
    @SaCheckPermission("crm:lead:edit")
    @PostMapping("/import")
    public Result<LeadImportResultVO> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.fail(1001, "请上传 xlsx 文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            return Result.fail(1001, "仅支持 .xlsx 格式");
        }
        return Result.success(leadService.importExcel(file));
    }
}

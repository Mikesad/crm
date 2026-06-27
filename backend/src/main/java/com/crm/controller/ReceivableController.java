package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.ReceivableCreateRequest;
import com.crm.dto.ReceivableQueryRequest;
import com.crm.service.ReceivableService;
import com.crm.vo.ReceivableVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 回款管理接口
 *
 * <p>财务录入实际回款,提交后异步发 {@code ReceivableRecordedEvent} 联动 plan/contract 状态。
 * 财务有 crm:receivable:list / crm:receivable:edit 权限,其他角色可见 list 但不能录入。</p>
 */
@Tag(name = "11. 回款管理", description = "财务录入实际回款,Spring Event 联动 plan/contract 状态")
@RestController
@RequestMapping("/crm/receivable")
@RequiredArgsConstructor
public class ReceivableController {

    private final ReceivableService receivableService;

    @Operation(summary = "分页查询回款记录")
    @SaCheckPermission("crm:receivable:list")
    @GetMapping("/page")
    public Result<IPage<ReceivableVO>> page(ReceivableQueryRequest query) {
        return Result.success(receivableService.page(query));
    }

    @Operation(summary = "回款详情")
    @SaCheckPermission("crm:receivable:list")
    @GetMapping("/{id}")
    public Result<ReceivableVO> detail(@PathVariable Long id) {
        return Result.success(receivableService.detail(id));
    }

    @Operation(summary = "录入回款",
        description = "校验合同 status=1, planId 可空(计划外回款),提交后 Spring Event 异步联动 plan/contract 状态")
    @SaCheckPermission("crm:receivable:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ReceivableCreateRequest req) {
        return Result.success(receivableService.create(req));
    }
}

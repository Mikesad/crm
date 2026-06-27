package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.crm.common.result.Result;
import com.crm.dto.ReceivablePlanCreateRequest;
import com.crm.dto.ReceivablePlanQueryRequest;
import com.crm.dto.ReceivablePlanUpdateRequest;
import com.crm.service.ReceivablePlanService;
import com.crm.vo.ReceivablePlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 回款计划接口
 *
 * <p>合同审批通过后(contract.status=1),销售在合同详情页录入。
 * 状态 0→2 由 {@code ReceivableEventListener} 监听 {@code ReceivableRecordedEvent} 自动联动。</p>
 */
@Tag(name = "10. 回款计划", description = "合同回款计划 CRUD,销售在合同详情录入")
@RestController
@RequestMapping("/crm/receivable-plan")
@RequiredArgsConstructor
public class ReceivablePlanController {

    private final ReceivablePlanService planService;

    @Operation(summary = "按合同查询回款计划",
        description = "V1 用 list 替代 page(单合同计划数通常 3~10)")
    @SaCheckPermission("crm:contract:list")
    @GetMapping("/list")
    public Result<List<ReceivablePlanVO>> list(ReceivablePlanQueryRequest query) {
        return Result.success(planService.list(query));
    }

    @Operation(summary = "回款计划详情")
    @SaCheckPermission("crm:contract:list")
    @GetMapping("/{id}")
    public Result<ReceivablePlanVO> detail(@PathVariable Long id) {
        return Result.success(planService.detail(id));
    }

    @Operation(summary = "批量创建回款计划",
        description = "一次传入多条,要求合同 status=1 (执行中);期数(period) 不能重复")
    @SaCheckPermission("crm:receivable_plan:edit")
    @PostMapping
    public Result<Void> createBatch(@Valid @RequestBody ReceivablePlanCreateRequest req) {
        planService.createBatch(req);
        return Result.success();
    }

    @Operation(summary = "更新回款计划",
        description = "已回款(status=2) 的计划不能修改")
    @SaCheckPermission("crm:receivable_plan:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ReceivablePlanUpdateRequest req) {
        planService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除回款计划", description = "逻辑删除,is_deleted 置 1;已回款不能删")
    @SaCheckPermission("crm:receivable_plan:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return Result.success();
    }
}

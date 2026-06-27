package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.BusinessCreateRequest;
import com.crm.dto.BusinessQueryRequest;
import com.crm.dto.BusinessStageUpdateRequest;
import com.crm.dto.BusinessUpdateRequest;
import com.crm.service.BusinessService;
import com.crm.vo.BusinessVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商机接口
 */
@Tag(name = "05. 商机管理", description = "商机 CRUD + 阶段严格单向流转")
@RestController
@RequestMapping("/crm/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @Operation(summary = "分页查询商机")
    @SaCheckPermission("crm:business:list")
    @GetMapping("/page")
    public Result<IPage<BusinessVO>> page(BusinessQueryRequest query) {
        return Result.success(businessService.page(query));
    }

    @Operation(summary = "商机详情")
    @SaCheckPermission("crm:business:list")
    @GetMapping("/{id}")
    public Result<BusinessVO> detail(@PathVariable Long id) {
        return Result.success(businessService.detail(id));
    }

    @Operation(summary = "创建商机", description = "默认阶段为「需求分析」")
    @SaCheckPermission("crm:business:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody BusinessCreateRequest req) {
        return Result.success(businessService.create(req));
    }

    @Operation(summary = "更新商机", description = "阶段字段不在此处修改，走 /stage 端点")
    @SaCheckPermission("crm:business:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody BusinessUpdateRequest req) {
        businessService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除商机", description = "逻辑删除，is_deleted 置 1")
    @SaCheckPermission("crm:business:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        businessService.delete(id);
        return Result.success();
    }

    @Operation(summary = "商机阶段变更",
        description = "严格单向：需求分析→方案报价→商务谈判→赢单；任意阶段可转输单；赢单/输单为终态")
    @SaCheckPermission("crm:business:edit")
    @PutMapping("/{id}/stage")
    public Result<Void> updateStage(@PathVariable Long id, @Valid @RequestBody BusinessStageUpdateRequest req) {
        businessService.updateStage(id, req);
        return Result.success();
    }
}

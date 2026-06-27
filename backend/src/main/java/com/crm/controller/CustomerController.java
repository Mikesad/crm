package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.CustomerCreateRequest;
import com.crm.dto.CustomerQueryRequest;
import com.crm.dto.CustomerUpdateRequest;
import com.crm.service.CustomerService;
import com.crm.vo.CustomerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户接口
 */
@Tag(name = "03. 客户管理", description = "客户 CRUD + 私海/公海查询")
@RestController
@RequestMapping("/crm/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "分页查询客户",
        description = "isPublic=1 时返回公海池（owner_user_id IS NULL），否则受 dataScope 拦截")
    @SaCheckPermission("crm:customer:list")
    @GetMapping("/page")
    public Result<IPage<CustomerVO>> page(CustomerQueryRequest query) {
        return Result.success(customerService.page(query));
    }

    @Operation(summary = "客户详情")
    @SaCheckPermission("crm:customer:list")
    @GetMapping("/{id}")
    public Result<CustomerVO> detail(@PathVariable Long id) {
        return Result.success(customerService.detail(id));
    }

    @Operation(summary = "创建客户")
    @SaCheckPermission("crm:customer:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CustomerCreateRequest req) {
        return Result.success(customerService.create(req));
    }

    @Operation(summary = "更新客户")
    @SaCheckPermission("crm:customer:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody CustomerUpdateRequest req) {
        customerService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除客户", description = "逻辑删除，is_deleted 置 1")
    @SaCheckPermission("crm:customer:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.success();
    }

    @Operation(summary = "公海认领",
        description = "把公海池里的客户捞到自己名下;需要 customer 当前是 is_public=1 且 owner_user_id IS NULL")
    @SaCheckPermission("crm:customer:public_pool")
    @PostMapping("/public-pool/claim/{id}")
    public Result<Void> claim(@PathVariable Long id) {
        customerService.claim(id);
        return Result.success();
    }
}

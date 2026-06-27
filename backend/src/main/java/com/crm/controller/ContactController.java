package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.crm.common.result.Result;
import com.crm.dto.ContactCreateRequest;
import com.crm.dto.ContactQueryRequest;
import com.crm.dto.ContactUpdateRequest;
import com.crm.service.ContactService;
import com.crm.vo.ContactVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 联系人接口
 */
@Tag(name = "04. 联系人", description = "按客户 ID 查询与维护联系人")
@RestController
@RequestMapping("/crm/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @Operation(summary = "按客户查询联系人列表", description = "强依赖 customerId")
    @SaCheckPermission("crm:contact:list")
    @GetMapping("/list")
    public Result<List<ContactVO>> list(@Valid ContactQueryRequest query) {
        return Result.success(contactService.listByCustomer(query));
    }

    @Operation(summary = "创建联系人")
    @SaCheckPermission("crm:contact:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ContactCreateRequest req) {
        return Result.success(contactService.create(req));
    }

    @Operation(summary = "更新联系人")
    @SaCheckPermission("crm:contact:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ContactUpdateRequest req) {
        contactService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除联系人", description = "逻辑删除，is_deleted 置 1")
    @SaCheckPermission("crm:contact:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return Result.success();
    }
}

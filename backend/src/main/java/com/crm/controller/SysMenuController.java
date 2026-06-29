package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.crm.common.result.Result;
import com.crm.dto.SysMenuCreateRequest;
import com.crm.dto.SysMenuUpdateRequest;
import com.crm.service.SysMenuService;
import com.crm.vo.SysMenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统菜单接口
 *
 * <p>admin + 销售总监 可访问;类型校验严格;关键能力菜单不可删。</p>
 */
@Tag(name = "97. 系统 - 菜单权限", description = "系统菜单树 CRUD + 类型校验")
@RestController
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "查询全量菜单(平铺,前端组装树)")
    @SaCheckPermission("sys:menu:list")
    @GetMapping("/all")
    public Result<List<SysMenuVO>> listAll() {
        return Result.success(menuService.listAll());
    }

    @Operation(summary = "查询菜单树")
    @SaCheckPermission("sys:menu:list")
    @GetMapping("/tree")
    public Result<List<SysMenuVO>> tree() {
        return Result.success(menuService.tree());
    }

    @Operation(summary = "菜单详情")
    @SaCheckPermission("sys:menu:list")
    @GetMapping("/{id}")
    public Result<SysMenuVO> detail(@PathVariable Long id) {
        return Result.success(menuService.detail(id));
    }

    @Operation(summary = "新建菜单", description = "M/C/F 类型严格校验")
    @SaCheckPermission("sys:menu:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysMenuCreateRequest req) {
        return Result.success(menuService.create(req));
    }

    @Operation(summary = "更新菜单")
    @SaCheckPermission("sys:menu:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysMenuUpdateRequest req) {
        menuService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除菜单", description = "存在子菜单/角色绑定/关键能力菜单时拒绝")
    @SaCheckPermission("sys:menu:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.success();
    }
}

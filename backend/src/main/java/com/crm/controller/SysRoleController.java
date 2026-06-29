package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.SysRoleCreateRequest;
import com.crm.dto.SysRoleUpdateRequest;
import com.crm.service.SysRoleService;
import com.crm.vo.SysRoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统角色接口
 *
 * <p>admin + 销售总监 可访问;内置 5 角色不可删;菜单绑定全量重绑。</p>
 */
@Tag(name = "96. 系统 - 角色", description = "系统角色 CRUD + 菜单绑定")
@RestController
@RequestMapping("/sys/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "角色分页")
    @SaCheckPermission("sys:role:list")
    @GetMapping("/page")
    public Result<IPage<SysRoleVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(roleService.page(keyword, pageNum, pageSize));
    }

    @Operation(summary = "全部启用角色下拉")
    @SaCheckPermission("sys:role:list")
    @GetMapping("/all")
    public Result<List<SysRoleVO>> listAll() {
        return Result.success(roleService.listAll());
    }

    @Operation(summary = "角色详情(含 menuIds)")
    @SaCheckPermission("sys:role:list")
    @GetMapping("/{id}")
    public Result<SysRoleVO> detail(@PathVariable Long id) {
        return Result.success(roleService.detail(id));
    }

    @Operation(summary = "新建角色")
    @SaCheckPermission("sys:role:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysRoleCreateRequest req) {
        return Result.success(roleService.create(req));
    }

    @Operation(summary = "更新角色", description = "roleKey 不可修改;传 menuIds 时全量重绑 + 踢下线")
    @SaCheckPermission("sys:role:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysRoleUpdateRequest req) {
        roleService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除角色", description = "内置 5 角色不可删;存在用户绑定时拒绝")
    @SaCheckPermission("sys:role:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "分配菜单", description = "全量重绑 sys_role_menu + 踢所有绑定用户下线")
    @SaCheckPermission("sys:role:assign_menu")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(
            @PathVariable Long id,
            @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.success();
    }

    // ========== 阶段六 commit 1 收尾:成员管理 3 接口 ==========

    @Operation(summary = "添加成员",
        description = "批量把 userId 加到本角色;已绑定走 INSERT IGNORE 跳过;踢受影响用户下线")
    @SaCheckPermission("sys:role:assign_menu")
    @PostMapping("/{id}/members")
    public Result<Integer> addMembers(
            @PathVariable Long id,
            @RequestBody List<Long> userIds) {
        roleService.addMembers(id, userIds);
        return Result.success(userIds == null ? 0 : userIds.size());
    }

    @Operation(summary = "移除成员",
        description = "admin 角色至少保留 1 人兜底;踢该用户下线")
    @SaCheckPermission("sys:role:assign_menu")
    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId) {
        roleService.removeMember(id, userId);
        return Result.success();
    }

    @Operation(summary = "成员列表",
        description = "分页列出本角色下的用户;roleNames 字段不含当前 roleId(由前端'其他角色'列展示)")
    @SaCheckPermission("sys:role:list")
    @GetMapping("/{id}/members")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<com.crm.vo.SysUserVO>> listMembers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(roleService.listMembers(id, pageNum, pageSize));
    }
}

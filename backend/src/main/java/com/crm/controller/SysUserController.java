package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.SysUserCreateRequest;
import com.crm.dto.SysUserUpdateRequest;
import com.crm.entity.SysUser;
import com.crm.mapper.SysUserMapper;
import com.crm.service.SysUserService;
import com.crm.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统用户接口
 *
 * <p>阶段六 commit 1: 完整 CRUD + 重置密码 + 分配角色。
 * 既有的 {@code GET /list} (轻量版,被共享人下拉用) 保留,标记 {@code @SaIgnore} 复用同一权限链。</p>
 */
@Tag(name = "95. 系统 - 用户", description = "系统用户 CRUD + 重置密码 + 角色分配;admin + 销售总监 可访问")
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;
    private final SysUserMapper userMapper;  // 仅供轻量 list 使用

    @Operation(summary = "用户分页(联 deptName + roleNames)")
    @SaCheckPermission("sys:user:list")
    @GetMapping("/page")
    public Result<IPage<SysUserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(userService.page(keyword, deptId, status, pageNum, pageSize));
    }

    @Operation(summary = "用户详情(含 roles)")
    @SaCheckPermission("sys:user:list")
    @GetMapping("/{id}")
    public Result<SysUserVO> detail(@PathVariable Long id) {
        return Result.success(userService.detail(id));
    }

    @Operation(summary = "新建用户", description = "默认密码 123456;admin 自保护 + 唯一性校验")
    @SaCheckPermission("sys:user:add")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysUserCreateRequest req) {
        return Result.success(userService.create(req));
    }

    @Operation(summary = "更新用户", description = "username 不可修改;传 roleIds 时全量重绑")
    @SaCheckPermission("sys:user:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysUserUpdateRequest req) {
        userService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除用户", description = "逻辑删除 + 踢下线;admin 自保护 + 至少 1 人校验")
    @SaCheckPermission("sys:user:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @Operation(summary = "重置密码", description = "默认重置为 123456;重置后立即踢下线")
    @SaCheckPermission("sys:user:reset_pwd")
    @PostMapping("/{id}/resetPassword")
    public Result<Void> resetPassword(
            @PathVariable Long id,
            @RequestParam(required = false) String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success();
    }

    @Operation(summary = "分配角色", description = "全量重绑 sys_user_role;admin 自保护")
    @SaCheckPermission("sys:user:assign_role")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(
            @PathVariable Long id,
            @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return Result.success();
    }

    @Operation(summary = "启停用", description = "status=0 时踢下线;admin 自保护 + 至少 1 人校验")
    @SaCheckPermission("sys:user:edit")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        userService.toggleStatus(id, status);
        return Result.success();
    }

    // ============= 既有轻量接口(被共享人下拉用)=============

    @Operation(summary = "查询用户列表(供前端下拉/选择器用,最大 200 条)")
    @SaIgnore  // 已登录即可,由 Sa-Token 拦截器兜底
    @GetMapping("/list")
    public Result<List<LiteUserVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword));
        }
        if (deptId != null) {
            wrapper.eq(SysUser::getDeptId, deptId);
        }
        wrapper.orderByAsc(SysUser::getId).last("LIMIT 200");
        List<SysUser> users = userMapper.selectList(wrapper);
        return Result.success(users.stream().map(LiteUserVO::of).toList());
    }

    @Data
    public static class LiteUserVO {
        private Long id;
        private String username;
        private String nickname;
        private Long deptId;

        public static LiteUserVO of(SysUser u) {
            LiteUserVO vo = new LiteUserVO();
            BeanUtil.copyProperties(u, vo);
            return vo;
        }
    }
}

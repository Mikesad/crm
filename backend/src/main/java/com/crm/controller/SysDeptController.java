package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.SysDeptCreateRequest;
import com.crm.dto.SysDeptUpdateRequest;
import com.crm.service.SysDeptService;
import com.crm.vo.SysDeptVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统部门接口
 *
 * <p>admin + 销售总监 可访问;3 类删除保护(顶级 / 有子 / 有用户);
 * 父变更事务内刷后代 ancestors。</p>
 */
@Tag(name = "97. 系统 - 部门", description = "系统部门 CRUD + 树形 + 详情;admin + 销售总监 可访问")
@RestController
@RequestMapping("/sys/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @Operation(summary = "部门全量列表(树形 / 平铺共用,前端 el-tree 自组织)")
    @SaCheckPermission("sys:dept:list")
    @GetMapping("/all")
    public Result<List<SysDeptVO>> listAll() {
        return Result.success(deptService.listAll());
    }

    @Operation(summary = "部门分页(搜索)")
    @SaCheckPermission("sys:dept:list")
    @GetMapping("/page")
    public Result<IPage<SysDeptVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(deptService.page(keyword, status, parentId, pageNum, pageSize));
    }

    @Operation(summary = "部门详情(右栏详情卡)")
    @SaCheckPermission("sys:dept:list")
    @GetMapping("/{id}")
    public Result<SysDeptVO> detail(@PathVariable Long id) {
        return Result.success(deptService.detail(id));
    }

    @Operation(summary = "新建部门", description = "V1 暂不允许新建顶级;同级 deptName 唯一")
    @SaCheckPermission("sys:dept:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysDeptCreateRequest req) {
        return Result.success(deptService.create(req));
    }

    @Operation(summary = "更新部门", description = "父变更会触发祖先链重建(事务内刷所有后代 ancestors)")
    @SaCheckPermission("sys:dept:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysDeptUpdateRequest req) {
        deptService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除部门", description = "顶级/有子部门/有用户 三类拒绝")
    @SaCheckPermission("sys:dept:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.success();
    }

    @Operation(summary = "启停用", description = "顶级不可停用;不踢下线(部门不在 Sa-Token session 缓存)")
    @SaCheckPermission("sys:dept:edit")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        deptService.toggleStatus(id, status);
        return Result.success();
    }
}

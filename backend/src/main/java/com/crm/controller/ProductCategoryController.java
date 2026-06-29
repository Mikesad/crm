package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.ProductCategoryCreateRequest;
import com.crm.dto.ProductCategoryQueryRequest;
import com.crm.dto.ProductCategoryUpdateRequest;
import com.crm.service.ProductCategoryService;
import com.crm.vo.ProductCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品分类接口
 *
 * <p>阶段六 commit 2 新增。公共资源,所有角色可读(D7 v0.4 全员可见)。</p>
 */
@Tag(name = "06. 产品管理 - 产品分类", description = "产品分类 CRUD,公共资源,所有角色可访问")
@RestController
@RequestMapping("/crm/product/category")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @Operation(summary = "分页查询产品分类",
        description = "按关键字(名称模糊匹配);每条记录含 productCount 关联产品数")
    @SaCheckPermission("crm:product:category:list")
    @GetMapping("/page")
    public Result<IPage<ProductCategoryVO>> page(ProductCategoryQueryRequest query) {
        return Result.success(productCategoryService.page(query));
    }

    @Operation(summary = "全量查询产品分类",
        description = "产品表单下拉用;不分页,按 id 升序")
    @SaCheckPermission("crm:product:category:list")
    @GetMapping("/all")
    public Result<List<ProductCategoryVO>> all() {
        return Result.success(productCategoryService.all());
    }

    @Operation(summary = "创建产品分类")
    @SaCheckPermission("crm:product:category:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductCategoryCreateRequest req) {
        return Result.success(productCategoryService.create(req));
    }

    @Operation(summary = "更新产品分类")
    @SaCheckPermission("crm:product:category:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ProductCategoryUpdateRequest req) {
        productCategoryService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除产品分类",
        description = "若被产品引用(>0),返回 DATA_EXISTS,提示先迁移产品")
    @SaCheckPermission("crm:product:category:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productCategoryService.delete(id);
        return Result.success();
    }
}

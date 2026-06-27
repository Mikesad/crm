package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.ProductCreateRequest;
import com.crm.dto.ProductQueryRequest;
import com.crm.dto.ProductUpdateRequest;
import com.crm.service.ProductService;
import com.crm.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 产品接口
 */
@Tag(name = "06. 产品管理", description = "产品库 CRUD,公共资源,无数据权限拦截")
@RestController
@RequestMapping("/crm/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "分页查询产品",
        description = "按关键字(编码/名称)/分类/状态过滤;产品为公共资源,所有角色可访问")
    @SaCheckPermission("crm:product:list")
    @GetMapping("/page")
    public Result<IPage<ProductVO>> page(ProductQueryRequest query) {
        return Result.success(productService.page(query));
    }

    @Operation(summary = "产品详情")
    @SaCheckPermission("crm:product:list")
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }

    @Operation(summary = "创建产品")
    @SaCheckPermission("crm:product:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductCreateRequest req) {
        return Result.success(productService.create(req));
    }

    @Operation(summary = "更新产品")
    @SaCheckPermission("crm:product:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ProductUpdateRequest req) {
        productService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除产品", description = "逻辑删除,is_deleted 置 1")
    @SaCheckPermission("crm:product:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }
}

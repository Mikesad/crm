package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.crm.common.result.Result;
import com.crm.dto.CustomerShareCreateRequest;
import com.crm.dto.CustomerShareListRequest;
import com.crm.service.CustomerShareService;
import com.crm.vo.CustomerShareVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户共享接口
 */
@Tag(name = "03. 客户管理 - 客户共享", description = "客户共享/撤销/查询")
@RestController
@RequestMapping("/customer/share")
@RequiredArgsConstructor
public class CustomerShareController {

    private final CustomerShareService shareService;

    @Operation(summary = "发起共享(已存在则覆盖 authType)")
    @SaCheckPermission("crm:customer:share")
    @PostMapping
    public Result<Long> share(@Valid @RequestBody CustomerShareCreateRequest req) {
        return Result.success(shareService.share(req));
    }

    @Operation(summary = "撤销共享(仅 owner)")
    @SaCheckPermission("crm:customer:share")
    @DeleteMapping("/{id}")
    public Result<Void> revoke(@PathVariable Long id) {
        shareService.revoke(id);
        return Result.success();
    }

    @Operation(summary = "查看某客户的共享名单(仅 owner)")
    @SaCheckPermission("crm:customer:share")
    @GetMapping("/list")
    public Result<List<CustomerShareVO>> list(@Valid @ModelAttribute CustomerShareListRequest req) {
        return Result.success(shareService.list(req.getCustomerId()));
    }
}

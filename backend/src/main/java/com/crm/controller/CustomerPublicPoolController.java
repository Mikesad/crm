package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.crm.common.result.Result;
import com.crm.dto.RecycleRequest;
import com.crm.service.CustomerPublicPoolService;
import com.crm.vo.RecycleResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公海池接口
 *
 * <p>目前只有手动回收一个接口(认领接口在 {@link CustomerController} 里)。</p>
 */
@Tag(name = "03. 客户管理 - 公海池", description = "公海池手动回收/认领")
@RestController
@RequestMapping("/customer/public-pool")
@RequiredArgsConstructor
public class CustomerPublicPoolController {

    private final CustomerPublicPoolService poolService;

    @Operation(summary = "手动触发公海回收",
        description = "供开发/演示/运维使用,与凌晨 2 点的 @Scheduled 任务规则一致;支持秒级阈值与 dryRun")
    @SaCheckPermission("crm:customer:public_pool")
    @PostMapping("/recycle")
    public Result<RecycleResultVO> recycle(@Valid @RequestBody(required = false) RecycleRequest req) {
        // Service 层做角色二次校验,即使权限码被错配也兜底
        poolService.requireRecyclePermission();
        if (req == null) {
            req = new RecycleRequest();
        }
        return Result.success(poolService.recycle(
                req.getThresholdSeconds(), req.getLimit(), req.getDryRun()));
    }
}

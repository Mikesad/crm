package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.ApprovalApproveRequest;
import com.crm.dto.ApprovalQueryRequest;
import com.crm.dto.ApprovalRejectRequest;
import com.crm.service.ApprovalService;
import com.crm.vo.ApprovalVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 合同审批接口
 *
 * <p>仅销售总监 / admin 可访问,业务流：合同 create() 检测折扣 &lt; 8.5 折 → 自动写 crm_approval
 * → 总监在此 approve/reject → 联动 crm_contract.status 流转。</p>
 */
@Tag(name = "09. 合同审批", description = "销售总监审批合同(折扣 < 8.5 折自动触发)")
@RestController
@RequestMapping("/crm/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @Operation(summary = "分页查询审批单",
        description = "按状态/合同/申请人/审批人过滤;默认 status 升序(待审在前) + createTime 降序")
    @SaCheckPermission("crm:contract:approve")
    @GetMapping("/page")
    public Result<IPage<ApprovalVO>> page(ApprovalQueryRequest query) {
        return Result.success(approvalService.page(query));
    }

    @Operation(summary = "审批通过",
        description = "状态 0→1,同时 crm_contract.status 置为 1 (执行中)")
    @SaCheckPermission("crm:contract:approve")
    @PostMapping("/approve")
    public Result<Void> approve(@Valid @RequestBody ApprovalApproveRequest req) {
        approvalService.approve(req);
        return Result.success();
    }

    @Operation(summary = "审批驳回",
        description = "状态 0→2 + comment 必填,同时 crm_contract.status 置为 3 (已作废)")
    @SaCheckPermission("crm:contract:approve")
    @PostMapping("/reject")
    public Result<Void> reject(@Valid @RequestBody ApprovalRejectRequest req) {
        approvalService.reject(req);
        return Result.success();
    }
}

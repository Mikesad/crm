package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.ContractCreateRequest;
import com.crm.dto.ContractQueryRequest;
import com.crm.dto.ContractUpdateRequest;
import com.crm.service.ContractService;
import com.crm.vo.ContractVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 合同接口
 *
 * <p>状态机: 0 审批中 → 1 执行中 → 2 已结束; 0 → 3 已作废(驳回)
 * 由本接口创建 → {@code ApprovalService.approve/reject} 流转 → {@code ReceivableEventListener} 收尾。</p>
 */
@Tag(name = "08. 合同管理", description = "合同 CRUD + 金额重算 + 折扣审批触发")
@RestController
@RequestMapping("/crm/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "分页查询合同",
        description = "受 dataScope 拦截:sales 仅看自己,finance 看全部")
    @SaCheckPermission("crm:contract:list")
    @GetMapping("/page")
    public Result<IPage<ContractVO>> page(ContractQueryRequest query) {
        return Result.success(contractService.page(query));
    }

    @Operation(summary = "合同详情", description = "返回主表 + 明细 items 列表")
    @SaCheckPermission("crm:contract:list")
    @GetMapping("/{id}")
    public Result<ContractVO> detail(@PathVariable Long id) {
        return Result.success(contractService.detail(id));
    }

    @Operation(summary = "创建合同",
        description = "后端按明细实时核算金额,折扣 < 8.5 折时自动进入审批中 + 写 crm_approval")
    @SaCheckPermission("crm:contract:edit")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ContractCreateRequest req) {
        return Result.success(contractService.create(req));
    }

    @Operation(summary = "更新合同",
        description = "V1 仅允许修改合同名称/起止日期;已结束/已作废合同不能改")
    @SaCheckPermission("crm:contract:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ContractUpdateRequest req) {
        contractService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除合同", description = "逻辑删除,is_deleted 置 1")
    @SaCheckPermission("crm:contract:edit")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return Result.success();
    }
}

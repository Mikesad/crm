package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.ApprovalApproveRequest;
import com.crm.dto.ApprovalQueryRequest;
import com.crm.dto.ApprovalRejectRequest;
import com.crm.entity.CrmApproval;
import com.crm.entity.CrmContract;
import com.crm.entity.CrmContractProduct;
import com.crm.mapper.CrmApprovalMapper;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmContractProductMapper;
import com.crm.vo.ApprovalVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合同审批服务
 *
 * <p>完整审批流（V1）：审批单由 {@code ContractService.create()} 在折扣 < 8.5 折时自动创建。
 * 销售总监在审批中心手动 approve/reject,状态联动 {@code crm_contract.status}。</p>
 *
 * <p><b>权限</b>：所有接口需 {@code crm:contract:approve}（仅销售总监 / admin）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final CrmApprovalMapper approvalMapper;
    private final CrmContractMapper contractMapper;
    private final CrmContractProductMapper contractProductMapper;

    public IPage<ApprovalVO> page(ApprovalQueryRequest query) {
        Page<CrmApproval> page = new Page<>(query.normalizeCurrent(), query.normalizeSize());
        LambdaQueryWrapper<CrmApproval> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) wrapper.eq(CrmApproval::getStatus, query.getStatus());
        if (query.getContractId() != null) wrapper.eq(CrmApproval::getContractId, query.getContractId());
        if (query.getApplicantId() != null) wrapper.eq(CrmApproval::getApplicantId, query.getApplicantId());
        if (query.getApproverId() != null) wrapper.eq(CrmApproval::getApproverId, query.getApproverId());
        wrapper.orderByAsc(CrmApproval::getStatus) // 待审(status=0)排前面
                .orderByDesc(CrmApproval::getCreateTime);
        IPage<CrmApproval> result = approvalMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    @Transactional
    public void approve(ApprovalApproveRequest req) {
        CrmApproval approval = approvalMapper.selectById(req.getId());
        if (approval == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "审批单不存在");
        }
        if (approval.getStatus() != 0) {
            throw new BusinessException(ResultCode.APPROVAL_NOT_PENDING, "该审批单已处理,不能重复操作");
        }
        // 1) 更新审批单
        approval.setStatus(1); // 通过
        approval.setApproverId(UserContext.requireUserId());
        approval.setComment(req.getComment());
        approval.setFinishTime(java.time.LocalDateTime.now());
        approval.setUpdateBy(UserContext.currentUsername());
        approvalMapper.updateById(approval);
        // 2) 联动合同 status = 1 (执行中)
        CrmContract contract = contractMapper.selectById(approval.getContractId());
        if (contract == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "关联合同不存在");
        }
        contract.setStatus(1);
        contract.setUpdateBy(UserContext.currentUsername());
        contractMapper.updateById(contract);
        log.info("审批通过: approvalId={}, contractId={}, approver={}",
                approval.getId(), contract.getId(), UserContext.currentUsername());
    }

    @Transactional
    public void reject(ApprovalRejectRequest req) {
        CrmApproval approval = approvalMapper.selectById(req.getId());
        if (approval == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "审批单不存在");
        }
        if (approval.getStatus() != 0) {
            throw new BusinessException(ResultCode.APPROVAL_NOT_PENDING, "该审批单已处理,不能重复操作");
        }
        // 1) 更新审批单
        approval.setStatus(2); // 驳回
        approval.setApproverId(UserContext.requireUserId());
        approval.setComment(req.getComment());
        approval.setFinishTime(java.time.LocalDateTime.now());
        approval.setUpdateBy(UserContext.currentUsername());
        approvalMapper.updateById(approval);
        // 2) 联动合同 status = 3 (已作废)
        CrmContract contract = contractMapper.selectById(approval.getContractId());
        if (contract == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "关联合同不存在");
        }
        contract.setStatus(3);
        contract.setUpdateBy(UserContext.currentUsername());
        contractMapper.updateById(contract);
        log.info("审批驳回: approvalId={}, contractId={}, approver={}, reason={}",
                approval.getId(), contract.getId(), UserContext.currentUsername(), req.getComment());
    }

    // ---------- helpers ----------

    private ApprovalVO toVO(CrmApproval a) {
        ApprovalVO vo = new ApprovalVO();
        BeanUtils.copyProperties(a, vo);
        vo.setStatusText(statusText(a.getStatus()));
        // 关联合同信息
        CrmContract c = contractMapper.selectById(a.getContractId());
        if (c != null) {
            vo.setContractNum(c.getContractNum());
            vo.setContractName(c.getContractName());
            vo.setContractTotalAmount(c.getTotalAmount());
            // 查合同明细最低折扣
            List<CrmContractProduct> items = contractProductMapper.selectList(
                    new LambdaQueryWrapper<CrmContractProduct>().eq(CrmContractProduct::getContractId, c.getId()));
            BigDecimal minDiscount = items.stream()
                    .map(CrmContractProduct::getDiscount)
                    .filter(java.util.Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
            vo.setMinDiscount(minDiscount);
        }
        return vo;
    }

    private String statusText(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 0 -> "待审";
            case 1 -> "已通过";
            case 2 -> "已驳回";
            case 3 -> "已撤回";
            default -> "-";
        };
    }
}

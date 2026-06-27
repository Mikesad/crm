package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.ReceivablePlanCreateRequest;
import com.crm.dto.ReceivablePlanItemRequest;
import com.crm.dto.ReceivablePlanQueryRequest;
import com.crm.dto.ReceivablePlanUpdateRequest;
import com.crm.entity.CrmContract;
import com.crm.entity.CrmReceivable;
import com.crm.entity.CrmReceivablePlan;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmReceivableMapper;
import com.crm.mapper.CrmReceivablePlanMapper;
import com.crm.vo.ReceivablePlanVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 回款计划服务
 *
 * <p>合同审批通过(contract.status=1)后,销售可手动录入多条回款计划。
 * 状态 0→2 由 {@code ReceivableEventListener} 监听 {@code ReceivableRecordedEvent} 自动联动。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivablePlanService {

    private final CrmReceivablePlanMapper planMapper;
    private final CrmContractMapper contractMapper;
    private final CrmReceivableMapper receivableMapper;

    public List<ReceivablePlanVO> list(ReceivablePlanQueryRequest query) {
        if (query.getContractId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "contractId 必传");
        }
        LambdaQueryWrapper<CrmReceivablePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmReceivablePlan::getContractId, query.getContractId());
        if (query.getStatus() != null) {
            wrapper.eq(CrmReceivablePlan::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(CrmReceivablePlan::getPeriod);
        List<CrmReceivablePlan> plans = planMapper.selectList(wrapper);
        return enrichWithReceivedAmount(plans);
    }

    public ReceivablePlanVO detail(Long id) {
        CrmReceivablePlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "回款计划不存在");
        }
        List<ReceivablePlanVO> list = enrichWithReceivedAmount(Collections.singletonList(plan));
        return list.get(0);
    }

    @Transactional
    public void createBatch(ReceivablePlanCreateRequest req) {
        // 1) 校验合同存在 + 状态为执行中
        CrmContract contract = contractMapper.selectById(req.getContractId());
        if (contract == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "合同不存在");
        }
        if (contract.getStatus() == null || contract.getStatus() != 1) {
            throw new BusinessException(ResultCode.CONTRACT_NOT_IN_EXECUTION, "只有执行中的合同(status=1)才能录入回款计划");
        }
        // 2) 校验期数不重复
        Set<Integer> periodSet = new HashSet<>();
        for (ReceivablePlanItemRequest item : req.getPlans()) {
            if (!periodSet.add(item.getPeriod())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "期数 " + item.getPeriod() + " 重复");
            }
        }
        // 3) 校验该合同已存在的期数(防止覆盖)
        List<CrmReceivablePlan> existing = planMapper.selectList(
                new LambdaQueryWrapper<CrmReceivablePlan>().eq(CrmReceivablePlan::getContractId, req.getContractId()));
        Set<Integer> existingPeriods = existing.stream()
                .map(CrmReceivablePlan::getPeriod).collect(Collectors.toSet());
        for (ReceivablePlanItemRequest item : req.getPlans()) {
            if (existingPeriods.contains(item.getPeriod())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "第 " + item.getPeriod() + " 期计划已存在");
            }
        }
        // 4) 批量插入
        for (ReceivablePlanItemRequest item : req.getPlans()) {
            CrmReceivablePlan plan = new CrmReceivablePlan();
            plan.setContractId(req.getContractId());
            plan.setPeriod(item.getPeriod());
            plan.setExpectedAmount(item.getExpectedAmount());
            plan.setExpectedDate(item.getExpectedDate());
            plan.setStatus(0); // 未到期
            plan.setRemark(item.getRemark());
            plan.setCreateBy(UserContext.currentUsername());
            plan.setUpdateBy(UserContext.currentUsername());
            planMapper.insert(plan);
        }
        log.info("批量创建回款计划: contractId={}, count={}", req.getContractId(), req.getPlans().size());
    }

    @Transactional
    public void update(ReceivablePlanUpdateRequest req) {
        CrmReceivablePlan plan = planMapper.selectById(req.getId());
        if (plan == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "回款计划不存在");
        }
        if (plan.getStatus() != null && plan.getStatus() == 2) {
            throw new BusinessException(ResultCode.PLAN_ALREADY_PAID, "已回款的计划不能修改");
        }
        if (req.getExpectedAmount() != null) plan.setExpectedAmount(req.getExpectedAmount());
        if (req.getExpectedDate() != null) plan.setExpectedDate(req.getExpectedDate());
        if (req.getStatus() != null) plan.setStatus(req.getStatus());
        if (StringUtils.hasText(req.getRemark())) plan.setRemark(req.getRemark());
        plan.setUpdateBy(UserContext.currentUsername());
        planMapper.updateById(plan);
    }

    @Transactional
    public void delete(Long id) {
        CrmReceivablePlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "回款计划不存在");
        }
        if (plan.getStatus() != null && plan.getStatus() == 2) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "已回款的计划不能删除");
        }
        planMapper.deleteById(id);
        log.info("逻辑删除回款计划: id={}", id);
    }

    // ---------- helpers ----------

    /**
     * 用 crm_receivable 实际回款累加 receivedAmount 字段
     * <p>SQL: SELECT plan_id, SUM(actual_amount) FROM crm_receivable
     *        WHERE plan_id IS NOT NULL AND contract_id = ? GROUP BY plan_id</p>
     */
    private List<ReceivablePlanVO> enrichWithReceivedAmount(List<CrmReceivablePlan> plans) {
        if (plans == null || plans.isEmpty()) return Collections.emptyList();
        Set<Long> planIds = plans.stream().map(CrmReceivablePlan::getId).collect(Collectors.toSet());
        Set<Long> contractIds = plans.stream().map(CrmReceivablePlan::getContractId).collect(Collectors.toSet());
        // 用 contract_id 查所有回款,V1 一次性拉,后续可优化
        Map<Long, BigDecimal> receivedMap = new HashMap<>();
        for (Long contractId : contractIds) {
            List<CrmReceivable> recs = receivableMapper.selectList(
                    new LambdaQueryWrapper<CrmReceivable>().eq(CrmReceivable::getContractId, contractId));
            for (CrmReceivable r : recs) {
                if (r.getPlanId() != null) {
                    receivedMap.merge(r.getPlanId(), r.getActualAmount(), BigDecimal::add);
                }
            }
        }
        return plans.stream().map(p -> {
            ReceivablePlanVO vo = new ReceivablePlanVO();
            BeanUtils.copyProperties(p, vo);
            vo.setStatusText(statusText(p.getStatus()));
            vo.setReceivedAmount(receivedMap.getOrDefault(p.getId(), BigDecimal.ZERO));
            return vo;
        }).collect(Collectors.toList());
    }

    private String statusText(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 0 -> "未到期";
            case 1 -> "催款中";
            case 2 -> "已回款";
            default -> "-";
        };
    }
}

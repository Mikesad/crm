package com.crm.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.entity.CrmContract;
import com.crm.entity.CrmReceivable;
import com.crm.entity.CrmReceivablePlan;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmReceivableMapper;
import com.crm.mapper.CrmReceivablePlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;

/**
 * 回款事件监听器
 *
 * <p>监听 {@link ReceivableRecordedEvent}（{@code AFTER_COMMIT} 阶段）：
 * <ol>
 *   <li>若 {@code planId} 非空,累加该计划下的 {@code crm_receivable.actual_amount},
 *       {@code >= expected_amount} 时将 plan.status 置为 2 (已回款)</li>
 *   <li>扫描该合同下所有 plan,若全部 status=2,则将 {@code crm_contract.status} 置为 2 (已结束)</li>
 * </ol>
 * </p>
 *
 * <p><b>事务</b>：监听器使用独立事务 ({@code REQUIRES_NEW}),
 * 即使计划/合同状态更新失败也不会回滚 receivable 写入（财务记录已生效）。</p>
 *
 * <p><b>容错</b>：所有异常 {@code try-catch} 兜底,仅 {@code log.error},不抛。
 * V2 阶段考虑加对账 Job 自动修复丢的事件。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceivableEventListener {

    private final CrmReceivableMapper receivableMapper;
    private final CrmReceivablePlanMapper planMapper;
    private final CrmContractMapper contractMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReceivableRecorded(ReceivableRecordedEvent event) {
        Long contractId = event.getContractId();
        Long planId = event.getPlanId();
        try {
            log.info("收到回款事件: contractId={}, receivableId={}, planId={}",
                    contractId, event.getReceivableId(), planId);
            if (planId != null) {
                updatePlanStatus(planId);
            } else {
                log.info("planId 为空,跳过 plan 状态联动(计划外回款)");
            }
            // 不管 planId 是否为空,都要尝试更新合同状态
            updateContractStatus(contractId);
        } catch (Exception e) {
            // 永不抛:监听器异常会污染调用方事务
            log.error("处理回款事件失败: contractId={}, receivableId={}, planId={}",
                    contractId, event.getReceivableId(), planId, e);
        }
    }

    /**
     * 更新单条 plan 状态:实收累计 >= 预计 → status=2
     */
    private void updatePlanStatus(Long planId) {
        CrmReceivablePlan plan = planMapper.selectById(planId);
        if (plan == null) {
            log.warn("plan 不存在: planId={}", planId);
            return;
        }
        if (plan.getStatus() != null && plan.getStatus() == 2) {
            log.debug("plan 已是已回款,跳过: planId={}", planId);
            return;
        }
        // 累加该 plan 下的实收
        List<CrmReceivable> recs = receivableMapper.selectList(
                new LambdaQueryWrapper<CrmReceivable>().eq(CrmReceivable::getPlanId, planId));
        BigDecimal received = recs.stream()
                .map(CrmReceivable::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (received.compareTo(plan.getExpectedAmount()) >= 0) {
            plan.setStatus(2);
            planMapper.updateById(plan);
            log.info("plan 状态联动: planId={}, 实收={}, 预计={}, 状态置为 2 (已回款)",
                    planId, received, plan.getExpectedAmount());
        } else {
            log.debug("plan 实收 {} < 预计 {},暂不更新状态: planId={}", received, plan.getExpectedAmount(), planId);
        }
    }

    /**
     * 更新合同状态:所有 plan 都已回款 → contract.status=2
     */
    private void updateContractStatus(Long contractId) {
        CrmContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            log.warn("contract 不存在: contractId={}", contractId);
            return;
        }
        if (contract.getStatus() != null && contract.getStatus() == 2) {
            log.debug("contract 已是已结束,跳过: contractId={}", contractId);
            return;
        }
        // 查该合同下所有未逻辑删除的 plan
        List<CrmReceivablePlan> plans = planMapper.selectList(
                new LambdaQueryWrapper<CrmReceivablePlan>().eq(CrmReceivablePlan::getContractId, contractId));
        if (plans.isEmpty()) {
            log.debug("contract 无 plan,跳过状态联动: contractId={}", contractId);
            return;
        }
        boolean allCompleted = plans.stream()
                .allMatch(p -> p.getStatus() != null && p.getStatus() == 2);
        if (allCompleted) {
            contract.setStatus(2);
            contractMapper.updateById(contract);
            log.info("contract 状态联动: contractId={}, 共 {} 期 plan 已全部回款,状态置为 2 (已结束)",
                    contractId, plans.size());
        }
    }
}

package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.ReceivableCreateRequest;
import com.crm.dto.ReceivableQueryRequest;
import com.crm.entity.CrmContract;
import com.crm.entity.CrmReceivable;
import com.crm.entity.CrmReceivablePlan;
import com.crm.event.ReceivableRecordedEvent;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmReceivableMapper;
import com.crm.mapper.CrmReceivablePlanMapper;
import com.crm.vo.ReceivableVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 回款服务
 *
 * <p>财务录入实际回款,V1 仅支持单条录入（多次到账分多次录）。</p>
 *
 * <p>核心业务 {@link #create(ReceivableCreateRequest)}:
 * <ol>
 *   <li>校验合同存在 + status=1 (执行中)</li>
 *   <li>若 planId 非空,校验 plan 属于该合同</li>
 *   <li>insert crm_receivable (append-only,无 is_deleted)</li>
 *   <li>{@code applicationEventPublisher.publishEvent(ReceivableRecordedEvent)}</li>
 * </ol>
 * </p>
 *
 * <p>事件监听器在事务提交后异步更新 plan.status / contract.status(见 {@code ReceivableEventListener})。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final CrmReceivableMapper receivableMapper;
    private final CrmContractMapper contractMapper;
    private final CrmReceivablePlanMapper planMapper;
    private final ApplicationEventPublisher eventPublisher;

    public IPage<ReceivableVO> page(ReceivableQueryRequest query) {
        Page<CrmReceivable> page = new Page<>(query.normalizeCurrent(), query.normalizeSize());
        LambdaQueryWrapper<CrmReceivable> wrapper = new LambdaQueryWrapper<>();
        if (query.getContractId() != null) wrapper.eq(CrmReceivable::getContractId, query.getContractId());
        if (query.getPlanId() != null) wrapper.eq(CrmReceivable::getPlanId, query.getPlanId());
        if (StringUtils.hasText(query.getPaymentMethod())) wrapper.eq(CrmReceivable::getPaymentMethod, query.getPaymentMethod());
        if (query.getReturnDateStart() != null) wrapper.ge(CrmReceivable::getReturnDate, query.getReturnDateStart());
        if (query.getReturnDateEnd() != null) wrapper.le(CrmReceivable::getReturnDate, query.getReturnDateEnd());
        wrapper.orderByDesc(CrmReceivable::getCreateTime);
        IPage<CrmReceivable> result = receivableMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    public ReceivableVO detail(Long id) {
        CrmReceivable rec = receivableMapper.selectById(id);
        if (rec == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "回款记录不存在");
        }
        return toVO(rec);
    }

    @Transactional
    public Long create(ReceivableCreateRequest req) {
        // 1) 校验合同
        CrmContract contract = contractMapper.selectById(req.getContractId());
        if (contract == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "合同不存在");
        }
        if (contract.getStatus() == null || contract.getStatus() != 1) {
            throw new BusinessException(ResultCode.CONTRACT_NOT_IN_EXECUTION, "只有执行中的合同(status=1)才能录入回款");
        }
        // 2) 校验 plan(若有)
        if (req.getPlanId() != null) {
            CrmReceivablePlan plan = planMapper.selectById(req.getPlanId());
            if (plan == null) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "回款计划不存在");
            }
            if (!plan.getContractId().equals(contract.getId())) {
                throw new BusinessException(ResultCode.PLAN_NOT_BELONG_CONTRACT, "回款计划不属于该合同");
            }
        }
        // 3) 写回款记录
        CrmReceivable rec = new CrmReceivable();
        rec.setReceivableNum(generateReceivableNum());
        rec.setContractId(req.getContractId());
        rec.setPlanId(req.getPlanId());
        rec.setActualAmount(req.getActualAmount());
        rec.setReturnDate(req.getReturnDate());
        rec.setPaymentMethod(StringUtils.hasText(req.getPaymentMethod()) ? req.getPaymentMethod() : "银行转账");
        rec.setCreateBy(UserContext.currentUsername());
        receivableMapper.insert(rec);
        log.info("录入回款: id={}, num={}, contractId={}, planId={}, amount={}",
                rec.getId(), rec.getReceivableNum(), rec.getContractId(), rec.getPlanId(), rec.getActualAmount());
        // 4) 发事件(AFTER_COMMIT 监听器会更新 plan/contract 状态)
        eventPublisher.publishEvent(new ReceivableRecordedEvent(
                this, rec.getContractId(), rec.getId(), rec.getPlanId()));
        return rec.getId();
    }

    // ---------- helpers ----------

    /** 生成回款编号 SK-YYYYMMDD-XXXXXX */
    private String generateReceivableNum() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("SK-%s-%06d", date, System.currentTimeMillis() % 1000000);
    }

    private ReceivableVO toVO(CrmReceivable rec) {
        ReceivableVO vo = new ReceivableVO();
        BeanUtils.copyProperties(rec, vo);
        // 关联合同信息
        CrmContract c = contractMapper.selectById(rec.getContractId());
        if (c != null) {
            vo.setContractNum(c.getContractNum());
            vo.setContractName(c.getContractName());
        }
        // 关联计划期数
        if (rec.getPlanId() != null) {
            CrmReceivablePlan p = planMapper.selectById(rec.getPlanId());
            vo.setPlanPeriod(p != null ? p.getPeriod() : null);
            vo.setPlanExtra(false);
        } else {
            vo.setPlanExtra(true);
        }
        return vo;
    }
}

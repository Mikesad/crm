package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.BusinessCreateRequest;
import com.crm.dto.BusinessQueryRequest;
import com.crm.dto.BusinessStageUpdateRequest;
import com.crm.dto.BusinessUpdateRequest;
import com.crm.entity.CrmBusiness;
import com.crm.entity.CrmCustomer;
import com.crm.entity.CrmRecord;
import com.crm.mapper.CrmBusinessMapper;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.mapper.CrmRecordMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.vo.BusinessVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 商机服务
 *
 * <p>核心业务：{@link #updateStage(Long, BusinessStageUpdateRequest)} 严格校验阶段单向流转，
 * 阶段变更时同步写入 crm_record 时间轴。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessService {

    /** 阶段顺序定义 */
    private static final Map<String, Integer> STAGE_ORDER = Map.of(
            "需求分析", 1,
            "方案报价", 2,
            "商务谈判", 3,
            "赢单",    4,
            "输单",    5
    );

    private final CrmBusinessMapper businessMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmRecordMapper recordMapper;
    private final SysUserMapper userMapper;

    public IPage<BusinessVO> page(BusinessQueryRequest query) {
        Page<CrmBusiness> page = new Page<>(query.normalizeCurrent(), query.normalizeSize());
        LambdaQueryWrapper<CrmBusiness> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(CrmBusiness::getBusinessName, query.getKeyword());
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(CrmBusiness::getCustomerId, query.getCustomerId());
        }
        if (StringUtils.hasText(query.getStage())) {
            wrapper.eq(CrmBusiness::getStage, query.getStage());
        }
        // 排序:前端可控 sortBy + order,空时默认 updateTime desc
        boolean asc = "asc".equalsIgnoreCase(query.getOrder());
        switch (query.getSortBy() == null ? "" : query.getSortBy()) {
            case "expectedDealDate":
                if (asc) wrapper.orderByAsc(CrmBusiness::getExpectedDealDate);
                else wrapper.orderByDesc(CrmBusiness::getExpectedDealDate);
                break;
            case "createTime":
                if (asc) wrapper.orderByAsc(CrmBusiness::getCreateTime);
                else wrapper.orderByDesc(CrmBusiness::getCreateTime);
                break;
            case "expectedAmount":
                if (asc) wrapper.orderByAsc(CrmBusiness::getExpectedAmount);
                else wrapper.orderByDesc(CrmBusiness::getExpectedAmount);
                break;
            case "businessName":
                if (asc) wrapper.orderByAsc(CrmBusiness::getBusinessName);
                else wrapper.orderByDesc(CrmBusiness::getBusinessName);
                break;
            default:
                wrapper.orderByDesc(CrmBusiness::getUpdateTime);
        }
        IPage<CrmBusiness> result = businessMapper.selectPage(page, wrapper);
        return result.convert(b -> {
            BusinessVO vo = toVO(b);
            // 关联客户名（简单 N+1 接受，列表用 inner join 可优化）
            if (b.getCustomerId() != null) {
                CrmCustomer c = customerMapper.selectById(b.getCustomerId());
                if (c != null) vo.setCustomerName(c.getCustomerName());
            }
            return vo;
        });
    }

    public BusinessVO detail(Long id) {
        CrmBusiness b = businessMapper.selectById(id);
        if (b == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "商机不存在");
        }
        BusinessVO vo = toVO(b);
        if (b.getCustomerId() != null) {
            CrmCustomer c = customerMapper.selectById(b.getCustomerId());
            if (c != null) vo.setCustomerName(c.getCustomerName());
        }
        return vo;
    }

    @Transactional
    public Long create(BusinessCreateRequest req) {
        CrmCustomer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        CrmBusiness b = new CrmBusiness();
        BeanUtils.copyProperties(req, b);
        b.setStage("需求分析"); // 新建默认需求分析
        b.setOwnerUserId(UserContext.requireUserId());
        b.setCreateBy(UserContext.currentUsername());
        b.setUpdateBy(UserContext.currentUsername());
        businessMapper.insert(b);
        log.info("创建商机: id={}, name={}", b.getId(), b.getBusinessName());
        return b.getId();
    }

    @Transactional
    public void update(BusinessUpdateRequest req) {
        CrmBusiness b = businessMapper.selectById(req.getId());
        if (b == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "商机不存在");
        }
        if (StringUtils.hasText(req.getBusinessName())) b.setBusinessName(req.getBusinessName());
        if (req.getExpectedAmount() != null) b.setExpectedAmount(req.getExpectedAmount());
        if (req.getExpectedDealDate() != null) b.setExpectedDealDate(req.getExpectedDealDate());
        b.setUpdateBy(UserContext.currentUsername());
        businessMapper.updateById(b);
    }

    @Transactional
    public void delete(Long id) {
        CrmBusiness b = businessMapper.selectById(id);
        if (b == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "商机不存在");
        }
        businessMapper.deleteById(id);
    }

    /**
     * 商机阶段变更（核心业务）
     *
     * <p>严格单向校验：</p>
     * <ul>
     *   <li>赢单 / 输单 → 不可再变更（终态）</li>
     *   <li>任意阶段 → 输单：允许（放弃）</li>
     *   <li>需求分析 → 方案报价 → 商务谈判 → 赢单：单向推进，不允许跳级（如 需求分析 → 赢单）</li>
     * </ul>
     *
     * <p>阶段变更时同步写一条 crm_record（relatedType=business），形成时间轴。</p>
     */
    @Transactional
    public void updateStage(Long id, BusinessStageUpdateRequest req) {
        CrmBusiness b = businessMapper.selectById(id);
        if (b == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "商机不存在");
        }
        String currentStage = b.getStage() == null ? "需求分析" : b.getStage();
        String targetStage = req.getStage();

        // 校验目标阶段合法
        if (!STAGE_ORDER.containsKey(targetStage)) {
            throw new BusinessException("目标阶段不合法：" + targetStage);
        }
        // 终态不能再变
        if ("赢单".equals(currentStage) || "输单".equals(currentStage)) {
            throw new BusinessException("商机已进入「" + currentStage + "」终态，不可再变更阶段");
        }
        // 目标与当前相同：no-op
        if (currentStage.equals(targetStage)) {
            return;
        }
        // 任意 → 输单：允许
        if ("输单".equals(targetStage)) {
            // 跳过其他校验
        } else {
            // 严格 +1 推进
            int cur = STAGE_ORDER.get(currentStage);
            int tgt = STAGE_ORDER.get(targetStage);
            if (tgt != cur + 1) {
                throw new BusinessException("商机阶段必须按「需求分析 → 方案报价 → 商务谈判 → 赢单」顺序推进，不允许跳级");
            }
        }

        b.setStage(targetStage);
        b.setUpdateBy(UserContext.currentUsername());
        businessMapper.updateById(b);

        // 时间轴埋点
        CrmRecord record = new CrmRecord();
        record.setRelatedType("business");
        record.setRelatedId(b.getId());
        record.setContent("商机「" + b.getBusinessName() + "」阶段从「" + currentStage
                + "」推进到「" + targetStage + "」" +
                (StringUtils.hasText(req.getFollowContent()) ? "\n跟进内容：" + req.getFollowContent() : ""));
        record.setFollowType("系统");
        record.setCreateBy(UserContext.currentAuthor());
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);

        log.info("商机阶段变更: id={}, {} → {}, operator={}",
                b.getId(), currentStage, targetStage, UserContext.currentUsername());
    }

    private BusinessVO toVO(CrmBusiness b) {
        BusinessVO vo = new BusinessVO();
        BeanUtils.copyProperties(b, vo);
        if (b.getOwnerUserId() != null) {
            com.crm.entity.SysUser u = userMapper.selectById(b.getOwnerUserId());
            if (u != null) vo.setOwnerName(u.getNickname());
        }
        return vo;
    }
}

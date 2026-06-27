package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.LeadConvertRequest;
import com.crm.dto.LeadCreateRequest;
import com.crm.dto.LeadQueryRequest;
import com.crm.dto.LeadUpdateRequest;
import com.crm.entity.CrmContact;
import com.crm.entity.CrmCustomer;
import com.crm.entity.CrmLead;
import com.crm.entity.CrmRecord;
import com.crm.mapper.CrmContactMapper;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.mapper.CrmLeadMapper;
import com.crm.mapper.CrmRecordMapper;
import com.crm.vo.LeadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 线索服务
 *
 * <p>核心业务：{@link #convertToCustomer(Long, LeadConvertRequest)} 事务内双写
 * {@code crm_customer} + {@code crm_contact}，并将原线索 status 置为 3（已转客户）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final CrmLeadMapper leadMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmContactMapper contactMapper;
    private final CrmRecordMapper recordMapper;

    public IPage<LeadVO> page(LeadQueryRequest query) {
        Page<CrmLead> page = new Page<>(query.normalizeCurrent(), query.normalizeSize());
        LambdaQueryWrapper<CrmLead> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(CrmLead::getLeadName, query.getKeyword())
                    .or().like(CrmLead::getContactName, query.getKeyword())
                    .or().like(CrmLead::getPhone, query.getKeyword()));
        }
        if (query.getStatus() != null) {
            wrapper.eq(CrmLead::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getSource())) {
            wrapper.eq(CrmLead::getSource, query.getSource());
        }
        // dataScope 拦截器自动加 owner_user_id 条件（仅本部门 / 仅本人）
        wrapper.orderByDesc(CrmLead::getCreateTime);
        IPage<CrmLead> result = leadMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    public LeadVO detail(Long id) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "线索不存在");
        }
        return toVO(lead);
    }

    @Transactional
    public Long create(LeadCreateRequest req) {
        CrmLead lead = new CrmLead();
        BeanUtils.copyProperties(req, lead);
        lead.setStatus(1); // 默认未跟进
        lead.setOwnerUserId(UserContext.requireUserId());
        lead.setCreateBy(UserContext.currentUsername());
        lead.setUpdateBy(UserContext.currentUsername());
        leadMapper.insert(lead);
        log.info("创建线索: id={}, name={}", lead.getId(), lead.getLeadName());
        return lead.getId();
    }

    @Transactional
    public void update(LeadUpdateRequest req) {
        CrmLead lead = leadMapper.selectById(req.getId());
        if (lead == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "线索不存在");
        }
        if (lead.getStatus() != null && lead.getStatus() == 3) {
            throw new BusinessException("已转客户的线索不可修改");
        }
        // 防止通过此接口把 status 偷偷改成 3
        if (req.getStatus() != null && req.getStatus() == 3) {
            throw new BusinessException("请使用'线索转客户'接口推进到 3-已转客户");
        }
        if (StringUtils.hasText(req.getLeadName())) lead.setLeadName(req.getLeadName());
        if (StringUtils.hasText(req.getContactName())) lead.setContactName(req.getContactName());
        if (StringUtils.hasText(req.getPhone())) lead.setPhone(req.getPhone());
        if (StringUtils.hasText(req.getSource())) lead.setSource(req.getSource());
        if (req.getStatus() != null) lead.setStatus(req.getStatus());
        if (req.getRemark() != null) lead.setRemark(req.getRemark());
        lead.setUpdateBy(UserContext.currentUsername());
        leadMapper.updateById(lead);
    }

    @Transactional
    public void delete(Long id) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "线索不存在");
        }
        // @TableLogic 自动转 UPDATE is_deleted=1
        leadMapper.deleteById(id);
    }

    /**
     * 线索转客户（核心业务）
     *
     * <p>{@code @Transactional} 内三步：</p>
     * <ol>
     *   <li>校验线索 status != 3，避免重复转</li>
     *   <li>创建 crm_customer（owner = 当前用户，isPublic=0）</li>
     *   <li>创建 crm_contact（isMaster=1，沿用线索联系人信息）</li>
     *   <li>更新线索 status = 3（已转客户）</li>
     *   <li>追加 crm_record 时间轴（type=线索转客户）</li>
     * </ol>
     */
    @Transactional
    public Long convertToCustomer(Long leadId, LeadConvertRequest req) {
        CrmLead lead = leadMapper.selectById(leadId);
        if (lead == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "线索不存在");
        }
        if (lead.getStatus() != null && lead.getStatus() == 3) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "该线索已转客户，不可重复转化");
        }
        Long currentUserId = UserContext.requireUserId();
        String currentUser = UserContext.currentUsername();

        // 1) 创建客户
        CrmCustomer customer = new CrmCustomer();
        customer.setCustomerName(req.getCustomerName());
        customer.setIndustry(req.getIndustry());
        customer.setLevel(StringUtils.hasText(req.getLevel()) ? req.getLevel() : "C");
        customer.setOwnerUserId(currentUserId);
        customer.setIsPublic(0);
        customer.setCreateBy(currentUser);
        customer.setUpdateBy(currentUser);
        customerMapper.insert(customer);

        // 2) 创建主联系人
        CrmContact contact = new CrmContact();
        contact.setCustomerId(customer.getId());
        contact.setContactName(lead.getContactName());
        contact.setPost(req.getPost());
        contact.setPhone(StringUtils.hasText(req.getPhone()) ? req.getPhone() : lead.getPhone());
        contact.setIsMaster(1);
        contact.setDecisionWeight(req.getDecisionWeight() == null ? 1 : req.getDecisionWeight());
        contact.setCreateBy(currentUser);
        contact.setUpdateBy(currentUser);
        contactMapper.insert(contact);

        // 3) 标记线索已转化
        lead.setStatus(3);
        lead.setUpdateBy(currentUser);
        leadMapper.updateById(lead);

        // 4) 时间轴埋点
        CrmRecord record = new CrmRecord();
        record.setRelatedType("lead");
        record.setRelatedId(lead.getId());
        record.setContent("线索已转化为客户「" + customer.getCustomerName() + "」");
        record.setFollowType("系统");
        record.setCreateBy(currentUser);
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);

        log.info("线索转客户成功: leadId={}, customerId={}, contactId={}, operator={}",
                lead.getId(), customer.getId(), contact.getId(), currentUser);
        return customer.getId();
    }

    private LeadVO toVO(CrmLead lead) {
        LeadVO vo = new LeadVO();
        BeanUtils.copyProperties(lead, vo);
        vo.setStatusText(switch (lead.getStatus() == null ? 0 : lead.getStatus()) {
            case 1 -> "未跟进";
            case 2 -> "跟进中";
            case 3 -> "已转客户";
            case 4 -> "已死线索";
            default -> "未知";
        });
        return vo;
    }
}

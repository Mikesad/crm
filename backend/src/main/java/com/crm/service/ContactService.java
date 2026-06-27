package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.ContactCreateRequest;
import com.crm.dto.ContactQueryRequest;
import com.crm.dto.ContactUpdateRequest;
import com.crm.entity.CrmContact;
import com.crm.entity.CrmCustomer;
import com.crm.mapper.CrmContactMapper;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.vo.ContactVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 联系人服务
 *
 * <p>联系人查询强依赖 customerId，返回的是平铺列表（非分页）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final CrmContactMapper contactMapper;
    private final CrmCustomerMapper customerMapper;

    public List<ContactVO> listByCustomer(ContactQueryRequest query) {
        // 校验客户存在
        CrmCustomer customer = customerMapper.selectById(query.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        LambdaQueryWrapper<CrmContact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmContact::getCustomerId, query.getCustomerId());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(CrmContact::getContactName, query.getKeyword())
                    .or().like(CrmContact::getPhone, query.getKeyword()));
        }
        // 主联系人排前面
        wrapper.orderByDesc(CrmContact::getIsMaster);
        wrapper.orderByDesc(CrmContact::getCreateTime);
        return contactMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Transactional
    public Long create(ContactCreateRequest req) {
        CrmCustomer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        CrmContact contact = new CrmContact();
        BeanUtils.copyProperties(req, contact);
        contact.setIsMaster(req.getIsMaster() == null ? 0 : req.getIsMaster());
        contact.setDecisionWeight(req.getDecisionWeight() == null ? 3 : req.getDecisionWeight());
        contact.setCreateBy(UserContext.currentUsername());
        contact.setUpdateBy(UserContext.currentUsername());
        contactMapper.insert(contact);
        log.info("创建联系人: id={}, customerId={}, name={}",
                contact.getId(), contact.getCustomerId(), contact.getContactName());
        return contact.getId();
    }

    @Transactional
    public void update(ContactUpdateRequest req) {
        CrmContact contact = contactMapper.selectById(req.getId());
        if (contact == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "联系人不存在");
        }
        if (StringUtils.hasText(req.getContactName())) contact.setContactName(req.getContactName());
        if (req.getPost() != null) contact.setPost(req.getPost());
        if (req.getPhone() != null) contact.setPhone(req.getPhone());
        if (req.getIsMaster() != null) contact.setIsMaster(req.getIsMaster());
        if (req.getDecisionWeight() != null) contact.setDecisionWeight(req.getDecisionWeight());
        contact.setUpdateBy(UserContext.currentUsername());
        contactMapper.updateById(contact);
    }

    @Transactional
    public void delete(Long id) {
        CrmContact contact = contactMapper.selectById(id);
        if (contact == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "联系人不存在");
        }
        contactMapper.deleteById(id);
    }

    private ContactVO toVO(CrmContact contact) {
        ContactVO vo = new ContactVO();
        BeanUtils.copyProperties(contact, vo);
        vo.setDecisionWeightText(switch (contact.getDecisionWeight() == null ? 0 : contact.getDecisionWeight()) {
            case 1 -> "核心决策者";
            case 2 -> "弱影响者";
            case 3 -> "普通职员";
            default -> "-";
        });
        return vo;
    }
}

package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.CustomerCreateRequest;
import com.crm.dto.CustomerQueryRequest;
import com.crm.dto.CustomerUpdateRequest;
import com.crm.entity.CrmCustomer;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.vo.CustomerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 客户服务
 *
 * <p>阶段二实现私海/公海查询过滤由 {@code CrmDataPermissionHandler} 接管，
 * 当 {@code isPublic=1} 时附加 {@code owner_user_id IS NULL} 条件。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CrmCustomerMapper customerMapper;

    public IPage<CustomerVO> page(CustomerQueryRequest query) {
        Page<CrmCustomer> page = new Page<>(query.normalizeCurrent(), query.normalizeSize());
        LambdaQueryWrapper<CrmCustomer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(CrmCustomer::getCustomerName, query.getKeyword());
        }
        if (StringUtils.hasText(query.getLevel())) {
            wrapper.eq(CrmCustomer::getLevel, query.getLevel());
        }
        if (StringUtils.hasText(query.getIndustry())) {
            wrapper.eq(CrmCustomer::getIndustry, query.getIndustry());
        }
        if (query.getIsPublic() != null && query.getIsPublic() == 1) {
            // 公海：仅看 owner_user_id IS NULL 的
            wrapper.isNull(CrmCustomer::getOwnerUserId);
        } else {
            // 私海：dataScope 拦截器自动加 owner_user_id 条件
        }
        wrapper.orderByDesc(CrmCustomer::getCreateTime);
        IPage<CrmCustomer> result = customerMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    public CustomerVO detail(Long id) {
        CrmCustomer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        return toVO(customer);
    }

    @Transactional
    public Long create(CustomerCreateRequest req) {
        CrmCustomer customer = new CrmCustomer();
        BeanUtils.copyProperties(req, customer);
        customer.setOwnerUserId(UserContext.requireUserId());
        customer.setIsPublic(0);
        customer.setCreateBy(UserContext.currentUsername());
        customer.setUpdateBy(UserContext.currentUsername());
        customerMapper.insert(customer);
        log.info("创建客户: id={}, name={}", customer.getId(), customer.getCustomerName());
        return customer.getId();
    }

    @Transactional
    public void update(CustomerUpdateRequest req) {
        CrmCustomer customer = customerMapper.selectById(req.getId());
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        if (StringUtils.hasText(req.getCustomerName())) customer.setCustomerName(req.getCustomerName());
        if (req.getIndustry() != null) customer.setIndustry(req.getIndustry());
        if (StringUtils.hasText(req.getLevel())) customer.setLevel(req.getLevel());
        customer.setUpdateBy(UserContext.currentUsername());
        customerMapper.updateById(customer);
    }

    @Transactional
    public void delete(Long id) {
        CrmCustomer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        // @TableLogic 自动转 UPDATE is_deleted=1
        customerMapper.deleteById(id);
    }

    private CustomerVO toVO(CrmCustomer customer) {
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(customer, vo);
        vo.setLevelText(switch (customer.getLevel() == null ? "" : customer.getLevel()) {
            case "A" -> "重要客户";
            case "B" -> "普通客户";
            case "C" -> "意向客户";
            default -> "-";
        });
        return vo;
    }
}

package com.crm.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.CustomerShareCreateRequest;
import com.crm.entity.CrmCustomer;
import com.crm.entity.CrmCustomerShare;
import com.crm.entity.SysUser;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.mapper.CrmCustomerShareMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.vo.CustomerShareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户共享服务
 *
 * <p>阶段四新增:主销售把私海客户共享给其他同事,支持只读/读写两档权限。</p>
 *
 * <p><b>写操作差异化拦截</b>:
 * {@link #requireWriteAccess(Long)} 供 CustomerService.update/delete、
 * RecordService.append 调用,若当前用户是只读共享身份则抛
 * {@link ResultCode#READONLY_SHARE_CANNOT_EDIT}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerShareService {

    /** 只读 */
    public static final int AUTH_READONLY = 1;
    /** 读写 */
    public static final int AUTH_READWRITE = 2;

    private final CrmCustomerShareMapper shareMapper;
    private final CrmCustomerMapper customerMapper;
    private final SysUserMapper userMapper;

    /**
     * 发起共享(已存在则覆盖 auth_type)
     */
    @Transactional
    public Long share(CustomerShareCreateRequest req) {
        Long currentUserId = UserContext.requireUserId();
        CrmCustomer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        // 1. 必须是 owner
        if (customer.getOwnerUserId() == null || !customer.getOwnerUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.NOT_CUSTOMER_OWNER, "仅 owner 可发起共享");
        }
        // 2. 公海客户不能共享
        if (customer.getIsPublic() != null && customer.getIsPublic() == 1) {
            throw new BusinessException(ResultCode.PUBLIC_CUSTOMER_CANNOT_SHARE,
                    "公海客户不能被共享,请先认领再共享");
        }
        // 3. 不校验"不能共享给自己":前端 ShareDialog 已在 listUsers 过滤掉当前用户,
        //    防御深度由前端保证;若未来后端要恢复校验,加回这一段即可
        // 4. authType 校验
        if (req.getAuthType() == null
                || (req.getAuthType() != AUTH_READONLY && req.getAuthType() != AUTH_READWRITE)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "authType 必须为 1(只读) 或 2(读写)");
        }
        // 5. 被共享人必须存在
        SysUser target = userMapper.selectById(req.getUserId());
        if (target == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "被共享人不存在");
        }

        // 6. UPSERT:同 (customer, user) 已存在则更新
        LambdaQueryWrapper<CrmCustomerShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmCustomerShare::getCustomerId, req.getCustomerId())
               .eq(CrmCustomerShare::getUserId, req.getUserId());
        CrmCustomerShare existing = shareMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setAuthType(req.getAuthType());
            existing.setCreateBy(UserContext.currentUsername());
            shareMapper.updateById(existing);
            log.info("更新共享: customer={} user={} authType={}",
                    req.getCustomerId(), req.getUserId(), req.getAuthType());
            return existing.getId();
        }
        CrmCustomerShare share = new CrmCustomerShare();
        share.setCustomerId(req.getCustomerId());
        share.setUserId(req.getUserId());
        share.setAuthType(req.getAuthType());
        share.setCreateBy(UserContext.currentUsername());
        shareMapper.insert(share);
        log.info("新建共享: id={} customer={} user={} authType={}",
                share.getId(), share.getCustomerId(), share.getUserId(), share.getAuthType());
        return share.getId();
    }

    /**
     * 撤销共享(仅 owner 可撤销)
     */
    @Transactional
    public void revoke(Long shareId) {
        Long currentUserId = UserContext.requireUserId();
        CrmCustomerShare share = shareMapper.selectById(shareId);
        if (share == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "共享记录不存在");
        }
        CrmCustomer customer = customerMapper.selectById(share.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        if (customer.getOwnerUserId() == null || !customer.getOwnerUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.NOT_CUSTOMER_OWNER, "仅 owner 可撤销共享");
        }
        shareMapper.deleteById(shareId);
        log.info("撤销共享: id={} customer={} user={}",
                shareId, share.getCustomerId(), share.getUserId());
    }

    /**
     * 查看某客户的共享名单(仅 owner 可看)
     */
    public List<CustomerShareVO> list(Long customerId) {
        Long currentUserId = UserContext.requireUserId();
        CrmCustomer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        if (customer.getOwnerUserId() == null || !customer.getOwnerUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.NOT_CUSTOMER_OWNER, "仅 owner 可查看共享名单");
        }
        LambdaQueryWrapper<CrmCustomerShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmCustomerShare::getCustomerId, customerId)
               .orderByDesc(CrmCustomerShare::getCreateTime);
        List<CrmCustomerShare> shares = shareMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(shares)) {
            return Collections.emptyList();
        }
        // 批量加载 user 信息
        Set<Long> userIds = shares.stream().map(CrmCustomerShare::getUserId).collect(Collectors.toSet());
        List<SysUser> users = userMapper.selectBatchIds(userIds);
        Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
        return shares.stream().map(s -> toVO(s, userMap)).toList();
    }

    /**
     * 写权限校验:供 CustomerService.update/delete、RecordService.append 等调用
     * <p>规则:① 当前用户是 owner → 放行;② 不在 owner 列表也不在 share 表 → 抛
     * {@link ResultCode#NOT_CUSTOMER_OWNER};③ 在 share 表但 auth_type=1 → 抛
     * {@link ResultCode#READONLY_SHARE_CANNOT_EDIT};④ 在 share 表且 auth_type=2 → 放行;
     * ⑤ 公海客户(本就是公开)→ 放行(让所有人能改公海元数据)。</p>
     */
    public void requireWriteAccess(Long customerId) {
        Long currentUserId = UserContext.requireUserId();
        CrmCustomer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        // 公海客户:无 owner,谁都能改
        if (customer.getIsPublic() != null && customer.getIsPublic() == 1
                && customer.getOwnerUserId() == null) {
            return;
        }
        // owner 直接放行
        if (customer.getOwnerUserId() != null && customer.getOwnerUserId().equals(currentUserId)) {
            return;
        }
        // 查 share 表
        LambdaQueryWrapper<CrmCustomerShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmCustomerShare::getCustomerId, customerId)
               .eq(CrmCustomerShare::getUserId, currentUserId)
               .last("LIMIT 1");
        CrmCustomerShare share = shareMapper.selectOne(wrapper);
        if (share == null) {
            throw new BusinessException(ResultCode.NOT_CUSTOMER_OWNER, "无权操作该客户");
        }
        if (share.getAuthType() != null && share.getAuthType() == AUTH_READONLY) {
            throw new BusinessException(ResultCode.READONLY_SHARE_CANNOT_EDIT,
                    "只读共享身份不能编辑该客户");
        }
        // 读写共享,放行
    }

    private CustomerShareVO toVO(CrmCustomerShare s, Map<Long, SysUser> userMap) {
        CustomerShareVO vo = new CustomerShareVO();
        BeanUtil.copyProperties(s, vo);
        SysUser u = userMap.get(s.getUserId());
        if (u != null) {
            vo.setUserNickname(u.getNickname());
        }
        vo.setAuthTypeText(s.getAuthType() != null
                ? (s.getAuthType() == AUTH_READONLY ? "只读" : "读写")
                : "-");
        return vo;
    }
}

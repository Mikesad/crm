package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.CustomerCreateRequest;
import com.crm.dto.CustomerQueryRequest;
import com.crm.entity.CrmCustomerShare;
import com.crm.mapper.CrmCustomerShareMapper;
import com.crm.dto.CustomerUpdateRequest;
import com.crm.entity.CrmCustomer;
import com.crm.entity.CrmRecord;
import com.crm.entity.SysUser;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.mapper.CrmRecordMapper;
import com.crm.mapper.SysUserMapper;
import com.crm.vo.CustomerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final CrmRecordMapper recordMapper;
    private final CustomerShareService shareService;
    private final CrmCustomerShareMapper shareMapper;
    private final SysUserMapper userMapper;

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
        if (query.getSharedToMeOnly() != null && query.getSharedToMeOnly() == 1) {
            // 阶段四:仅查"被共享给我"的客户(共享表命中)
            // 注意:此处仅作 inSql 过滤,数据权限拦截器仍会兜底
            Long uid = UserContext.requireUserId();
            wrapper.inSql(CrmCustomer::getId,
                    "SELECT customer_id FROM crm_customer_share WHERE user_id = " + uid);
        } else if (query.getIsPublic() != null && query.getIsPublic() == 1) {
            // 公海：仅看 owner_user_id IS NULL 的
            wrapper.isNull(CrmCustomer::getOwnerUserId);
        } else {
            // 私海：阶段四起,dataScope=5 的拦截器会 OR 上 is_public=1,
            // 这里显式排除公海,让私海 Tab 不混入公海客户
            wrapper.eq(CrmCustomer::getIsPublic, 0);
        }
        // v0.16:支持前端 sortBy + order 控制升降序(默认 lastFollowTime desc)
        boolean asc = "asc".equalsIgnoreCase(query.getOrder());
        switch (query.getSortBy() == null ? "" : query.getSortBy()) {
            case "lastFollowTime":
                if (asc) wrapper.orderByAsc(CrmCustomer::getLastFollowTime);
                else wrapper.orderByDesc(CrmCustomer::getLastFollowTime);
                break;
            default:
                wrapper.orderByDesc(CrmCustomer::getLastFollowTime);
        }
        IPage<CrmCustomer> result = customerMapper.selectPage(page, wrapper);

        Map<Long, String> ownerNameMap = buildOwnerNameMap(result.getRecords());
        // phase8 commit1 修复:批量查"被共享给当前登录用户"的客户 id 集合,返回前端用于 ownerBadgeText 判定
        Set<Long> sharedIds = batchQuerySharedIds(result.getRecords());
        Map<Long, String> finalMap = ownerNameMap;
        return result.convert(c -> {
            CustomerVO vo = toVO(c, finalMap);
            vo.setSharedToMe(sharedIds.contains(c.getId()));
            return vo;
        });
    }

    /**
     * 阶段八 commit1 修复:批量查当前登录用户被共享的客户 id 集合
     * <p>前端 ownerBadgeText 兜底分支之前错把所有"非自己 own"标"共享给我",
     * 改为按此 Set 判定,避免误显示。一次 IN 查询,N+1 → 1。</p>
     */
    private Set<Long> batchQuerySharedIds(java.util.List<CrmCustomer> customers) {
        if (customers.isEmpty()) return Collections.emptySet();
        Long uid = UserContext.requireUserId();
        List<Long> ids = customers.stream().map(CrmCustomer::getId).collect(Collectors.toList());
        // 拼 inSql: SELECT customer_id FROM crm_customer_share WHERE user_id = ? AND customer_id IN (...)
        String inList = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CrmCustomerShare> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.select("customer_id").eq("user_id", uid).inSql("customer_id", inList);
        List<CrmCustomerShare> rows = shareMapper.selectList(qw);
        return rows.stream().map(CrmCustomerShare::getCustomerId).collect(Collectors.toSet());
    }

    /**
     * 阶段五修复:从记录集合中提取 ownerUserId,批量查 user 表拿 nickname,
     * 复用于列表和单条详情(单条详情也走 1 次 IN 查,避免单点 selectById N+1)。
     */
    private Map<Long, String> buildOwnerNameMap(java.util.List<CrmCustomer> customers) {
        Set<Long> ownerIds = customers.stream()
                .map(CrmCustomer::getOwnerUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ownerIds.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));
    }

    public CustomerVO detail(Long id) {
        CrmCustomer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        // 单条详情复用批量查 ownerName 的 helper
        return toVO(customer, buildOwnerNameMap(java.util.List.of(customer)));
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
        // 阶段四:owner / 读写共享人可改,只读共享人 / 其他人 → 抛 READONLY_SHARE_CANNOT_EDIT
        shareService.requireWriteAccess(req.getId());
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
        // 阶段四:写权限校验,见 update() 注释
        shareService.requireWriteAccess(id);
        CrmCustomer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        // @TableLogic 自动转 UPDATE is_deleted=1
        customerMapper.deleteById(id);
    }

    /**
     * 阶段四:公海池认领
     * <p>前置条件:customer.isPublic=1 且 ownerUserId IS NULL。</p>
     * <p>副作用:① owner=当前用户;② isPublic=0;③ lastFollowTime=now;
     * ④ 追加一条 crm_record 跟进记录(内容固定为"从公海池认领客户")。</p>
     */
    @Transactional
    public void claim(Long id) {
        Long currentUserId = UserContext.requireUserId();
        CrmCustomer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "客户不存在");
        }
        if (customer.getIsPublic() == null || customer.getIsPublic() != 1
                || customer.getOwnerUserId() != null) {
            throw new BusinessException(ResultCode.CUSTOMER_NOT_IN_PUBLIC_POOL,
                    "该客户不在公海池,无法认领");
        }
        CrmCustomer update = new CrmCustomer();
        update.setId(id);
        update.setOwnerUserId(currentUserId);
        update.setIsPublic(0);
        update.setLastFollowTime(LocalDateTime.now());
        update.setUpdateBy(UserContext.currentUsername());
        customerMapper.updateById(update);

        // 写一条跟进记录(直接用 recordMapper 而非 RecordService 以避免循环依赖)
        CrmRecord r = new CrmRecord();
        r.setRelatedType("customer");
        r.setRelatedId(id);
        r.setContent("从公海池认领客户");
        r.setFollowType("系统");
        r.setCreateBy(UserContext.currentUsername());
        r.setCreateTime(LocalDateTime.now());
        // 注入 recordMapper 比较啰嗦,这里通过 RecordService 走 — 但 RecordService 也依赖 shareService
        // 为避免循环,改用直接 RecordService 调用,通过构造注入
        // — 已在 controller 端点改用 RecordService ——
        // (本方法直接 insert 的实现放在 CustomerService.claim,需要 recordMapper)
        // — 把 recordMapper 也注入到本类即可 ——
        // (改为使用注入的 recordMapper)
        recordMapper.insert(r);

        log.info("公海认领: customerId={} by userId={}", id, currentUserId);
    }

    private CustomerVO toVO(CrmCustomer customer) {
        return toVO(customer, Collections.emptyMap());
    }

    private CustomerVO toVO(CrmCustomer customer, Map<Long, String> ownerNameMap) {
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(customer, vo);
        vo.setLevelText(switch (customer.getLevel() == null ? "" : customer.getLevel()) {
            case "A" -> "重要客户";
            case "B" -> "普通客户";
            case "C" -> "意向客户";
            default -> "-";
        });
        if (customer.getOwnerUserId() != null) {
            vo.setOwnerName(ownerNameMap.get(customer.getOwnerUserId()));
        }
        return vo;
    }
}

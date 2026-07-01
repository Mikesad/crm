package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.ContractCreateRequest;
import com.crm.dto.ContractItemRequest;
import com.crm.dto.ContractQueryRequest;
import com.crm.dto.ContractUpdateRequest;
import com.crm.entity.*;
import com.crm.mapper.*;
import com.crm.vo.ContractItemVO;
import com.crm.vo.ContractVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 合同服务
 *
 * <p>核心业务 {@link #create(ContractCreateRequest)}:
 * <ol>
 *   <li>按 {@code productId} 批量查 {@code crm_product.price}（MyBatis-Plus 自动过滤 is_deleted=1）</li>
 *   <li>按 {@code sales_price = standardPrice × discount / 10} 反推实际单价（{@code setScale(2, HALF_UP)}）</li>
 *   <li>累加 sum(salesPrice × count) 与前端 {@code totalAmount} 比对,误差 > 0.01 视为篡改</li>
 *   <li>取最低折扣, &lt; 8.5 折时 contract.status=0 (审批中,需销售总监在合同详情页手动审核)</li>
 *   <li>{@code @Transactional} 双写 contract + contract_product</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    /** 折扣审批阈值(可配置;V1 写死 8.50,阶段三评审已确认) */
    private static final BigDecimal DISCOUNT_THRESHOLD = new BigDecimal("8.50");

    /** 金额容差:前端传来的 totalAmount 与后端重算允许 0.01 误差 */
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final CrmContractMapper contractMapper;
    private final CrmContractProductMapper contractProductMapper;
    private final CrmProductMapper productMapper;
    private final CrmCustomerMapper customerMapper;
    private final SysUserMapper sysUserMapper;

    public IPage<ContractVO> page(ContractQueryRequest query) {
        Page<CrmContract> page = new Page<>(query.normalizeCurrent(), query.normalizeSize());
        LambdaQueryWrapper<CrmContract> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(CrmContract::getContractNum, query.getKeyword())
                    .or().like(CrmContract::getContractName, query.getKeyword()));
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(CrmContract::getCustomerId, query.getCustomerId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(CrmContract::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(CrmContract::getCreateTime);
        // DataPermissionHandler 自动追加 owner_user_id 条件
        IPage<CrmContract> result = contractMapper.selectPage(page, wrapper);
        // 批量查 customerName + ownerName(避免每行单独查询,N+1)
        // ⚠ 2026-06-29 修 bug:原 buildNameMap 用单 Map<Long,String> 拼接 "[cust]"/"[owner]" 前缀,
        // 当 crm_customer.id == sys_user.id 时(测试数据 customer.id=7, owner=8 等撞车),
        // 后 put 的 owner 会覆盖先 put 的 customer,导致列表中合同 customerName 丢失。
        // detail 走 List.of(contract) 单条无冲突,所以一直正常 — 这正是用户报的现象。
        NameMap nameMap = buildNameMap(result.getRecords());
        return result.convert(c -> toVO(c, nameMap));
    }

    public ContractVO detail(Long id) {
        CrmContract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "合同不存在");
        }
        // 查 customer + owner 单条
        NameMap nameMap = buildNameMap(java.util.List.of(contract));
        ContractVO vo = toVO(contract, nameMap);
        // 加载明细
        List<CrmContractProduct> items = contractProductMapper.selectList(
                new LambdaQueryWrapper<CrmContractProduct>().eq(CrmContractProduct::getContractId, id));
        vo.setItems(toItemVOs(items));
        return vo;
    }

    /**
     * 批量查 customerName + ownerName,组成两个独立 Map(避免 N+1 + namespace 撞车)
     * <p>2026-06-29 改造:从 {@code Map<Long,String>} 单 namespace 拆成
     * {@link NameMap} 双 Map,消除 customer.id == owner_user_id 时的覆盖 bug。</p>
     */
    private NameMap buildNameMap(List<CrmContract> contracts) {
        NameMap nm = new NameMap();
        if (contracts == null || contracts.isEmpty()) return nm;
        // 收集 customerIds
        java.util.Set<Long> customerIds = contracts.stream()
                .map(CrmContract::getCustomerId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (!customerIds.isEmpty()) {
            customerMapper.selectBatchIds(customerIds).forEach(c ->
                    nm.customerNames.put(c.getId(), c.getCustomerName()));
        }
        // 收集 ownerUserIds
        java.util.Set<Long> ownerIds = contracts.stream()
                .map(CrmContract::getOwnerUserId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (!ownerIds.isEmpty()) {
            sysUserMapper.selectBatchIds(ownerIds).forEach(u ->
                    nm.ownerNames.put(u.getId(), u.getNickname()));
        }
        return nm;
    }

    /**
     * 名称映射(客户名 + 业主名分开两个 Map,避免 Long id 撞车)
     */
    private static class NameMap {
        final java.util.Map<Long, String> customerNames = new java.util.HashMap<>();
        final java.util.Map<Long, String> ownerNames = new java.util.HashMap<>();
    }

    @Transactional
    public Long create(ContractCreateRequest req) {
        // 1) 按 productId 批量查 standardPrice
        List<Long> productIds = req.getItems().stream()
                .map(ContractItemRequest::getProductId).distinct().collect(Collectors.toList());
        Map<Long, CrmProduct> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(CrmProduct::getId, p -> p));
        // 校验所有产品都查得到
        for (Long pid : productIds) {
            if (!productMap.containsKey(pid)) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "产品 ID " + pid + " 不存在或已下架");
            }
        }
        // 2) 重算金额 + 取最低折扣
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal minDiscount = new BigDecimal("10.00");
        List<CrmContractProduct> items = new ArrayList<>(req.getItems().size());
        for (ContractItemRequest itemReq : req.getItems()) {
            CrmProduct p = productMap.get(itemReq.getProductId());
            BigDecimal standardPrice = p.getPrice();
            BigDecimal discount = itemReq.getDiscount();
            // sales_price = standardPrice * discount / 10,精度 2 位 HALF_UP
            BigDecimal salesPrice = standardPrice.multiply(discount)
                    .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
            BigDecimal subtotal = salesPrice.multiply(new BigDecimal(itemReq.getCount()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);
            if (discount.compareTo(minDiscount) < 0) {
                minDiscount = discount;
            }
            CrmContractProduct item = new CrmContractProduct();
            item.setProductId(p.getId());
            item.setCount(itemReq.getCount());
            item.setStandardPrice(standardPrice);
            item.setSalesPrice(salesPrice);
            item.setDiscount(discount);
            items.add(item);
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        // 3) 金额校验:与前端 totalAmount 比对,容差 0.01
        if (total.subtract(req.getTotalAmount()).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new BusinessException(ResultCode.AMOUNT_MISMATCH,
                    "合同金额与明细不符: 后端重算 " + total + ",前端传 " + req.getTotalAmount());
        }
        // 4) 状态判定(无审批流,低折扣仅标 status=0 留痕,由销售总监在合同详情页手动复核)
        Integer status;
        if (minDiscount.compareTo(DISCOUNT_THRESHOLD) < 0) {
            status = 0; // 审批中(留痕,需总监人工审核后改 1)
        } else {
            status = 1; // 执行中
        }
        // 5) 写主表
        CrmContract contract = new CrmContract();
        contract.setContractNum(generateContractNum());
        contract.setContractName(req.getContractName());
        contract.setCustomerId(req.getCustomerId());
        contract.setBusinessId(req.getBusinessId());
        contract.setTotalAmount(total);
        contract.setStartDate(req.getStartDate());
        contract.setEndDate(req.getEndDate());
        contract.setStatus(status);
        contract.setOwnerUserId(UserContext.requireUserId());
        contract.setCreateBy(UserContext.currentUsername());
        contract.setUpdateBy(UserContext.currentUsername());
        contractMapper.insert(contract);
        // 6) 写明细
        for (CrmContractProduct item : items) {
            item.setContractId(contract.getId());
            contractProductMapper.insert(item);
        }
        // (原:折扣低于 8.5 折时自动写 crm_approval 待审。phase8 commit 1 拆掉 crm_approval 表,
        //  保留 contract.status=0 留痕,审批流转人工在合同详情页操作)
        log.info("创建合同: id={}, num={}, total={}, status={}", contract.getId(), contract.getContractNum(), total, status);
        return contract.getId();
    }

    @Transactional
    public void update(ContractUpdateRequest req) {
        CrmContract contract = contractMapper.selectById(req.getId());
        if (contract == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "合同不存在");
        }
        if (contract.getStatus() != null && (contract.getStatus() == 2 || contract.getStatus() == 3)) {
            throw new BusinessException(ResultCode.CONTRACT_CANNOT_UPDATE, "已结束/已作废的合同不能修改");
        }
        if (StringUtils.hasText(req.getContractName())) contract.setContractName(req.getContractName());
        if (req.getStartDate() != null) contract.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) contract.setEndDate(req.getEndDate());
        contract.setUpdateBy(UserContext.currentUsername());
        contractMapper.updateById(contract);
    }

    @Transactional
    public void delete(Long id) {
        CrmContract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "合同不存在");
        }
        // @TableLogic 自动转 UPDATE is_deleted=1
        contractMapper.deleteById(id);
        log.info("逻辑删除合同: id={}", id);
    }

    // ---------- helpers ----------

    /** 生成合同编号 HT-YYYYMMDD-XXXXXX (6位时间戳后缀,单机足以唯一) */
    private String generateContractNum() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("HT-%s-%06d", date, System.currentTimeMillis() % 1000000);
    }

    private ContractVO toVO(CrmContract c, NameMap nameMap) {
        ContractVO vo = new ContractVO();
        BeanUtils.copyProperties(c, vo);
        vo.setStatusText(statusText(c.getStatus()));
        if (nameMap != null) {
            if (c.getCustomerId() != null) {
                vo.setCustomerName(nameMap.customerNames.get(c.getCustomerId()));
            }
            if (c.getOwnerUserId() != null) {
                vo.setOwnerName(nameMap.ownerNames.get(c.getOwnerUserId()));
            }
        }
        return vo;
    }

    private List<ContractItemVO> toItemVOs(List<CrmContractProduct> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        // 批量查产品名(快照)用于展示
        Set<Long> pids = items.stream().map(CrmContractProduct::getProductId).collect(Collectors.toSet());
        Map<Long, CrmProduct> productMap = productMapper.selectBatchIds(pids).stream()
                .collect(Collectors.toMap(CrmProduct::getId, p -> p));
        return items.stream().map(i -> {
            ContractItemVO vo = new ContractItemVO();
            BeanUtils.copyProperties(i, vo);
            CrmProduct p = productMap.get(i.getProductId());
            if (p != null) {
                vo.setProductCode(p.getProductCode());
                vo.setProductName(p.getProductName());
                vo.setSpec(p.getSpec());
                vo.setUnit(p.getUnit());
            }
            vo.setSubtotal(i.getSalesPrice().multiply(new BigDecimal(i.getCount()))
                    .setScale(2, RoundingMode.HALF_UP));
            return vo;
        }).collect(Collectors.toList());
    }

    private String statusText(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 0 -> "审批中";
            case 1 -> "执行中";
            case 2 -> "已结束";
            case 3 -> "已作废";
            default -> "-";
        };
    }
}

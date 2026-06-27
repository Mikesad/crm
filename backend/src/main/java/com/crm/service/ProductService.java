package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.ProductCreateRequest;
import com.crm.dto.ProductQueryRequest;
import com.crm.dto.ProductUpdateRequest;
import com.crm.entity.CrmProduct;
import com.crm.mapper.CrmProductMapper;
import com.crm.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 产品服务
 *
 * <p>产品为公共资源，无 owner_user_id，不受数据权限拦截。所有角色可读，
 * 仅 {@code crm:product:edit} 权限可写。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final CrmProductMapper productMapper;

    public IPage<ProductVO> page(ProductQueryRequest query) {
        Page<CrmProduct> page = new Page<>(query.normalizeCurrent(), query.normalizeSize());
        LambdaQueryWrapper<CrmProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(CrmProduct::getProductCode, query.getKeyword())
                    .or().like(CrmProduct::getProductName, query.getKeyword()));
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(CrmProduct::getCategoryId, query.getCategoryId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(CrmProduct::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(CrmProduct::getCreateTime);
        IPage<CrmProduct> result = productMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    public ProductVO detail(Long id) {
        CrmProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "产品不存在");
        }
        return toVO(product);
    }

    @Transactional
    public Long create(ProductCreateRequest req) {
        // 编码唯一性校验（DB 也有唯一索引，这里先预校验以给出友好提示）
        Long count = productMapper.selectCount(new LambdaQueryWrapper<CrmProduct>()
                .eq(CrmProduct::getProductCode, req.getProductCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "产品编码已存在");
        }
        CrmProduct product = new CrmProduct();
        BeanUtils.copyProperties(req, product);
        product.setCreateBy(UserContext.currentUsername());
        productMapper.insert(product);
        log.info("创建产品: id={}, code={}", product.getId(), product.getProductCode());
        return product.getId();
    }

    @Transactional
    public void update(ProductUpdateRequest req) {
        CrmProduct product = productMapper.selectById(req.getId());
        if (product == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "产品不存在");
        }
        if (req.getCategoryId() != null) product.setCategoryId(req.getCategoryId());
        if (StringUtils.hasText(req.getProductCode())) {
            // 编码变更时校验唯一
            if (!req.getProductCode().equals(product.getProductCode())) {
                Long count = productMapper.selectCount(new LambdaQueryWrapper<CrmProduct>()
                        .eq(CrmProduct::getProductCode, req.getProductCode())
                        .ne(CrmProduct::getId, req.getId()));
                if (count > 0) {
                    throw new BusinessException(ResultCode.DATA_EXISTS, "产品编码已存在");
                }
            }
            product.setProductCode(req.getProductCode());
        }
        if (StringUtils.hasText(req.getProductName())) product.setProductName(req.getProductName());
        if (StringUtils.hasText(req.getSpec())) product.setSpec(req.getSpec());
        if (req.getPrice() != null) product.setPrice(req.getPrice());
        if (StringUtils.hasText(req.getUnit())) product.setUnit(req.getUnit());
        if (req.getStatus() != null) product.setStatus(req.getStatus());
        productMapper.updateById(product);
        log.info("更新产品: id={}", product.getId());
    }

    @Transactional
    public void delete(Long id) {
        CrmProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "产品不存在");
        }
        // @TableLogic 自动转 UPDATE is_deleted=1
        productMapper.deleteById(id);
        log.info("逻辑删除产品: id={}", id);
    }

    private ProductVO toVO(CrmProduct product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        vo.setStatusText(product.getStatus() != null && product.getStatus() == 1 ? "上架" : "下架");
        return vo;
    }
}

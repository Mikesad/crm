package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.ProductCategoryCreateRequest;
import com.crm.dto.ProductCategoryQueryRequest;
import com.crm.dto.ProductCategoryUpdateRequest;
import com.crm.entity.CrmProductCategory;
import com.crm.mapper.CrmProductCategoryMapper;
import com.crm.mapper.CrmProductMapper;
import com.crm.vo.ProductCategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品分类服务
 *
 * <p>阶段六 commit 2 新增。分类为公共资源,无 owner_user_id 字段,所有角色可访问。
 * 删除前需校验 {@code crm_product.category_id} 引用数,引用数 > 0 不允许删除。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final CrmProductCategoryMapper productCategoryMapper;
    private final CrmProductMapper productMapper;

    /** 分页查询(关联产品数填充) */
    public IPage<ProductCategoryVO> page(ProductCategoryQueryRequest query) {
        Page<CrmProductCategory> page = new Page<>(query.normalizeCurrent(), query.normalizeSize());
        LambdaQueryWrapper<CrmProductCategory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(CrmProductCategory::getCategoryName, query.getKeyword());
        }
        wrapper.orderByAsc(CrmProductCategory::getId);
        IPage<CrmProductCategory> result = productCategoryMapper.selectPage(page, wrapper);
        IPage<ProductCategoryVO> voPage = result.convert(this::toVO);
        // 批量填充关联产品数
        for (ProductCategoryVO vo : voPage.getRecords()) {
            vo.setProductCount(productMapper.countByCategoryId(vo.getId()));
        }
        return voPage;
    }

    /** 全量查询(产品表单下拉用,不填 productCount) */
    public List<ProductCategoryVO> all() {
        List<CrmProductCategory> list = productCategoryMapper.selectList(
            new LambdaQueryWrapper<CrmProductCategory>()
                .orderByAsc(CrmProductCategory::getId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional
    public Long create(ProductCategoryCreateRequest req) {
        // 同级分类名重名校验
        Long count = productCategoryMapper.selectCount(new LambdaQueryWrapper<CrmProductCategory>()
            .eq(CrmProductCategory::getParentId, req.getParentId() == null ? 0L : req.getParentId())
            .eq(CrmProductCategory::getCategoryName, req.getCategoryName()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "同级分类下已存在同名分类");
        }
        CrmProductCategory category = new CrmProductCategory();
        BeanUtils.copyProperties(req, category);
        if (category.getParentId() == null) category.setParentId(0L);
        category.setCreateBy(UserContext.currentUsername());
        productCategoryMapper.insert(category);
        log.info("创建产品分类: id={}, name={}", category.getId(), category.getCategoryName());
        return category.getId();
    }

    @Transactional
    public void update(ProductCategoryUpdateRequest req) {
        CrmProductCategory category = productCategoryMapper.selectById(req.getId());
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "产品分类不存在");
        }
        // 改名时校验同级重名
        if (StringUtils.hasText(req.getCategoryName())
            && !req.getCategoryName().equals(category.getCategoryName())) {
            Long parentId = req.getParentId() != null ? req.getParentId() : category.getParentId();
            Long count = productCategoryMapper.selectCount(new LambdaQueryWrapper<CrmProductCategory>()
                .eq(CrmProductCategory::getParentId, parentId)
                .eq(CrmProductCategory::getCategoryName, req.getCategoryName())
                .ne(CrmProductCategory::getId, req.getId()));
            if (count > 0) {
                throw new BusinessException(ResultCode.DATA_EXISTS, "同级分类下已存在同名分类");
            }
            category.setCategoryName(req.getCategoryName());
        }
        if (req.getParentId() != null) category.setParentId(req.getParentId());
        productCategoryMapper.updateById(category);
        log.info("更新产品分类: id={}", category.getId());
    }

    @Transactional
    public void delete(Long id) {
        CrmProductCategory category = productCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "产品分类不存在");
        }
        // 引用校验:被产品引用则不允许删除
        Long productCount = productMapper.countByCategoryId(id);
        if (productCount != null && productCount > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS,
                "该分类下还有 " + productCount + " 个产品,无法删除;请先迁移产品到其他分类");
        }
        productCategoryMapper.deleteById(id);
        log.info("逻辑删除产品分类: id={}", id);
    }

    private ProductCategoryVO toVO(CrmProductCategory category) {
        ProductCategoryVO vo = new ProductCategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}

package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.RecordCreateRequest;
import com.crm.dto.RecordQueryRequest;
import com.crm.entity.CrmRecord;
import com.crm.mapper.CrmRecordMapper;
import com.crm.vo.RecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 跟进记录服务
 *
 * <p>记录只能新增（无 updateBy/updateTime/isDeleted 字段），append-only 模式符合 CRM 行业惯例。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {

    /** 允许的 relatedType 白名单 */
    private static final Set<String> ALLOWED_TYPES = Set.of("lead", "customer", "business");

    private final CrmRecordMapper recordMapper;

    public List<RecordVO> timeline(RecordQueryRequest query) {
        if (!ALLOWED_TYPES.contains(query.getRelatedType())) {
            throw new BusinessException("relatedType 仅支持 lead / customer / business");
        }
        LambdaQueryWrapper<CrmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmRecord::getRelatedType, query.getRelatedType());
        wrapper.eq(CrmRecord::getRelatedId, query.getRelatedId());
        wrapper.orderByDesc(CrmRecord::getCreateTime);
        return recordMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Transactional
    public Long append(RecordCreateRequest req) {
        if (!ALLOWED_TYPES.contains(req.getRelatedType())) {
            throw new BusinessException("relatedType 仅支持 lead / customer / business");
        }
        CrmRecord r = new CrmRecord();
        BeanUtils.copyProperties(req, r);
        r.setCreateBy(UserContext.currentNickname() != null
                ? UserContext.currentNickname() : UserContext.currentUsername());
        r.setCreateTime(LocalDateTime.now());
        recordMapper.insert(r);
        log.info("新增跟进记录: id={}, related={}:{}, by={}",
                r.getId(), r.getRelatedType(), r.getRelatedId(), r.getCreateBy());
        return r.getId();
    }

    private RecordVO toVO(CrmRecord r) {
        RecordVO vo = new RecordVO();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }
}

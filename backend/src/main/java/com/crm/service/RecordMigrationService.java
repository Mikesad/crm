package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.entity.CrmRecord;
import com.crm.entity.CrmRecordMigrationLog;
import com.crm.mapper.CrmRecordMapper;
import com.crm.mapper.CrmRecordMigrationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 跟进记录迁移服务（阶段五新增）
 *
 * <p>当线索转客户时，把该线索下所有 {@code relatedType='lead'} 的 crm_record 物理迁移为
 * {@code relatedType='customer', relatedId=customerId}，同步写迁移日志。</p>
 *
 * <p><b>事务边界</b>：本方法不加 {@code @Transactional}，靠调用方（{@code LeadService.convertToCustomer}）
 * 的事务回滚。如果调用方事务回滚，UPDATE crm_record 与 INSERT migration_log 都将回滚。</p>
 *
 * <p><b>模式 A 物理迁移</b>（v0.2 决策）：不复制追加，不保持独立。客户详情时间轴从首次接触一气呵成，
 * 聚合查询无需 DISTINCT。如需"反向追溯"或"撤销转客户"，通过 {@code crm_record_migration_log} 关联查询。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordMigrationService {

    private final CrmRecordMapper recordMapper;
    private final CrmRecordMigrationLogMapper migrationLogMapper;

    /**
     * 把指定 lead 下所有 related_type='lead' 的跟进记录迁移为 'customer'，并写迁移日志。
     *
     * @param leadId      原线索 ID
     * @param customerId  新客户 ID
     * @param operator    操作人 username
     * @return 迁移的记录数
     */
    public int migrate(Long leadId, Long customerId, String operator) {
        // 1) 查出该线索下所有跟进记录（用于写迁移日志）
        LambdaQueryWrapper<CrmRecord> query = new LambdaQueryWrapper<>();
        query.eq(CrmRecord::getRelatedType, "lead");
        query.eq(CrmRecord::getRelatedId, leadId);
        List<CrmRecord> records = recordMapper.selectList(query);
        if (records.isEmpty()) {
            log.info("线索 {} 无跟进记录可迁移,直接转客户 customerId={}", leadId, customerId);
            return 0;
        }

        // 2) 写迁移日志（必须在 UPDATE crm_record 之前完成,否则后续 JOIN 会找不到旧记录）
        LocalDateTime now = LocalDateTime.now();
        for (CrmRecord r : records) {
            CrmRecordMigrationLog logEntry = new CrmRecordMigrationLog();
            logEntry.setRecordId(r.getId());
            logEntry.setFromType("lead");
            logEntry.setFromId(leadId);
            logEntry.setToType("customer");
            logEntry.setToId(customerId);
            logEntry.setOperator(operator != null ? operator : "");
            logEntry.setMigrateTime(now);
            migrationLogMapper.insert(logEntry);
        }

        // 3) 物理迁移 crm_record.related_type / related_id
        for (CrmRecord r : records) {
            r.setRelatedType("customer");
            r.setRelatedId(customerId);
            recordMapper.updateById(r);
        }

        log.info("线索 {} → 客户 {} 完成 {} 条跟进记录迁移,操作人 {}",
                leadId, customerId, records.size(), operator);
        return records.size();
    }
}
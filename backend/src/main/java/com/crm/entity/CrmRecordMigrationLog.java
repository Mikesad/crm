package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 跟进记录迁移日志（阶段五新增）
 *
 * <p>当线索转客户时，原 {@code crm_record} 中 {@code relatedType='lead'} 的记录
 * 会按模式 A（物理迁移）改为 {@code relatedType='customer'}，每次迁移写入一条日志，</p>
 *
 * <p>用于审计与反向追溯（万一未来需要"撤销转客户"或数据修复）。</p>
 *
 * <p><b>字段说明：</b></p>
 * <ul>
 *   <li>{@code operator} 用 VARCHAR username 而非 BIGINT user_id：用户删除后仍可追溯</li>
 *   <li>无审计四件套（create_by/update_by/update_time）——append-only 表</li>
 * </ul>
 *
 * <p>字段说明见 {@code sql/crm_full.sql} 中 {@code crm_record_migration_log} 表。</p>
 */
@Data
@TableName("crm_record_migration_log")
public class CrmRecordMigrationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联跟进记录 ID（crm_record.id） */
    private Long recordId;

    /** 原主体类型（lead） */
    private String fromType;

    /** 原主体 ID */
    private Long fromId;

    /** 新主体类型（customer） */
    private String toType;

    /** 新主体 ID */
    private Long toId;

    /** 操作人 username（不用 user_id 便于用户删除后追溯） */
    private String operator;

    /** 迁移时间 */
    private LocalDateTime migrateTime;
}
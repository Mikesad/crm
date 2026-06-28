-- =====================================================================
--  阶段五(commit 1) · 跟进中心 Schema 增量迁移
--
--  适用：已按 crm_full.sql + phase2-menu-update.sql +
--        phase3-approval-and-plan-soft-delete.sql +
--        phase4-customer-share-and-public-pool.sql 初始化过数据库,
--        需要补齐阶段五跟进中心功能的环境。
--  时间：2026-06-28
--
--  变更：
--    1. crm_lead 表新增 2 列(死线索规则):
--         dead_reason VARCHAR(500)  死因备注(可选)
--         dead_time   DATETIME      标记时间
--    2. 新建 crm_record_migration_log 表(线索-客户跟进迁移日志)
--    3. 为 crm_record 表新增 2 个二级索引(优化跟进中心 todo 查询性能):
--         idx_next_follow (next_follow_time)
--         idx_create_by   (create_by)
--    4. 新增 2 个菜单权限码:
--         crm:record:center   跟进中心(侧边栏入口,菜单类型 C)
--         crm:lead:markDead   标为死线索按钮(菜单类型 F,按钮级)
--    5. 重新绑定 sys_role_menu,把这 2 个新菜单按规则挂上
--       (admin/director/lead/sales 4 个角色;finance 不参与)
--
--  幂等：菜单按 perms NOT EXISTS 保护;角色绑定用 DELETE + INSERT 模式;
--        可重复执行。
--  MySQL 版本要求：>= 5.5（使用 INFORMATION_SCHEMA + 动态 SQL 模式）
-- =====================================================================

-- ---------- 0) 工具存储过程：按列名幂等加列 ----------
DROP PROCEDURE IF EXISTS phase5_add_col_if_missing;
DELIMITER //
CREATE PROCEDURE phase5_add_col_if_missing(
  IN  p_tbl   VARCHAR(64),
  IN  p_col   VARCHAR(64),
  IN  p_def   TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = p_tbl
       AND COLUMN_NAME  = p_col
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', p_tbl, ' ADD COLUMN ', p_col, ' ', p_def);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;

-- 工具存储过程：按索引名幂等加索引
DROP PROCEDURE IF EXISTS phase5_add_idx_if_missing;
DELIMITER //
CREATE PROCEDURE phase5_add_idx_if_missing(
  IN  p_tbl   VARCHAR(64),
  IN  p_idx   VARCHAR(64),
  IN  p_def   TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = p_tbl
       AND INDEX_NAME   = p_idx
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', p_tbl, ' ADD INDEX ', p_idx, ' ', p_def);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;

-- ---------- 1) crm_lead 加 2 列(死线索规则) ----------
CALL phase5_add_col_if_missing('crm_lead', 'dead_reason', 'VARCHAR(500) DEFAULT NULL COMMENT ''死线索原因(可选)''');
CALL phase5_add_col_if_missing('crm_lead', 'dead_time',   'DATETIME DEFAULT NULL COMMENT ''死线索标记时间''');

-- ---------- 2) crm_record 加 2 个二级索引(优化 todo 查询) ----------
CALL phase5_add_idx_if_missing('crm_record', 'idx_next_follow', '(`next_follow_time`)');
CALL phase5_add_idx_if_missing('crm_record', 'idx_create_by',   '(`create_by`)');

-- ---------- 3) 兜底：若老库 crm_record_migration_log 表不存在 ----------
CREATE TABLE IF NOT EXISTS `crm_record_migration_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT                COMMENT '主键',
  `record_id`    BIGINT       NOT NULL                               COMMENT '关联跟进记录ID(crm_record.id)',
  `from_type`    VARCHAR(20)  NOT NULL                               COMMENT '原主体类型(lead)',
  `from_id`      BIGINT       NOT NULL                               COMMENT '原主体ID',
  `to_type`      VARCHAR(20)  NOT NULL                               COMMENT '新主体类型(customer)',
  `to_id`        BIGINT       NOT NULL                               COMMENT '新主体ID',
  `operator`     VARCHAR(64)  DEFAULT ''                             COMMENT '操作人(username)',
  `migrate_time` DATETIME     DEFAULT CURRENT_TIMESTAMP               COMMENT '迁移时间',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_from` (`from_type`, `from_id`),
  KEY `idx_to` (`to_type`, `to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跟进记录主体迁移日志(阶段五新增)';

-- ---------- 4) 2 个新菜单（按 perms 幂等，id 由 AUTO_INCREMENT 自动分配） ----------

-- 4.1 跟进中心(侧边栏菜单入口,菜单类型 C)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '跟进中心', 0, 25, 'record/center', 'record/center', 'C', 'crm:record:center', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:record:center');

-- 4.2 标为死线索按钮(按钮级权限,菜单类型 F,不显示)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '标为死线索', 0, 26, '', NULL, 'F', 'crm:lead:markDead', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:lead:markDead');

-- ---------- 5) 重新绑定角色-菜单(只对包含新权限的 4 个角色; finance 不参与) ----------
-- 注意:此处不能 DELETE 全表,会破坏阶段一/二/三/四的角色绑定。
--       采用"按角色清掉旧 + 按角色插新"的方式,保留其他角色的原有权限。

-- 5.1) admin (1) — 全部
DELETE FROM sys_role_menu WHERE role_id = 1
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN ('crm:record:center','crm:lead:markDead'));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 1, id FROM sys_menu WHERE perms IN ('crm:record:center','crm:lead:markDead');

-- 5.2) sales_director (2) — 跟进中心 + 标死线索
DELETE FROM sys_role_menu WHERE role_id = 2
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN ('crm:record:center','crm:lead:markDead'));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 2, id FROM sys_menu WHERE perms IN ('crm:record:center','crm:lead:markDead');

-- 5.3) sales_lead (3) — 跟进中心 + 标死线索
DELETE FROM sys_role_menu WHERE role_id = 3
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN ('crm:record:center','crm:lead:markDead'));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 3, id FROM sys_menu WHERE perms IN ('crm:record:center','crm:lead:markDead');

-- 5.4) sales (4) — 跟进中心 + 标死线索
DELETE FROM sys_role_menu WHERE role_id = 4
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN ('crm:record:center','crm:lead:markDead'));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 4, id FROM sys_menu WHERE perms IN ('crm:record:center','crm:lead:markDead');

-- 5.5) finance (5) — 财务不参与跟进中心/死线索,此处不动

-- ---------- 6) 清理：删除工具存储过程 ----------
DROP PROCEDURE phase5_add_col_if_missing;
DROP PROCEDURE phase5_add_idx_if_missing;

-- =====================================================================
--  验证（执行后跑一下确认）：
--  SHOW COLUMNS FROM crm_lead LIKE 'dead%';
--  SHOW INDEX FROM crm_record WHERE Key_name IN ('idx_next_follow','idx_create_by');
--  SHOW TABLES LIKE 'crm_record_migration_log';
--  SELECT id, perms, menu_name, menu_type, path FROM sys_menu
--   WHERE perms IN ('crm:record:center','crm:lead:markDead') ORDER BY id;
--  SELECT r.role_key, m.perms
--    FROM sys_role r
--    JOIN sys_role_menu rm ON r.id = rm.role_id
--    JOIN sys_menu m       ON rm.menu_id = m.id
--   WHERE m.perms IN ('crm:record:center','crm:lead:markDead')
--   ORDER BY r.id, m.perms;
-- =====================================================================
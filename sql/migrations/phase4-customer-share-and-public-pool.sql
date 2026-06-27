-- =====================================================================
--  阶段四 Schema 增量迁移
--
--  适用：已按 crm_full.sql + phase2-menu-update.sql +
--        phase3-approval-and-plan-soft-delete.sql 初始化过数据库,
--        需要补齐阶段四菜单/权限的环境。
--  时间：2026-06-27
--
--  变更：
--    1. crm_customer_share 表已在 crm_full.sql 中创建,本脚本不重复 DDL
--       (crm_full.sql 第 152 行,含 customer_id/user_id/auth_type/create_time 字段)
--    2. 新增 2 个菜单权限码:
--         crm:customer:share         客户共享(主销售发起/撤销;只读/读写两类)
--         crm:customer:public_pool   公海池(查看/认领,以及 admin/director 的手动回收)
--    3. 重新绑定 sys_role_menu,把 2 个新菜单按规则挂上
--    4. (可选)兜底:为老库可能缺失 crm_customer_share 表的极端情况补 DDL
--
--  幂等：菜单按 perms NOT EXISTS 保护;角色绑定用 DELETE + INSERT 模式,
--        可重复执行。
--  MySQL 版本要求：>= 5.5（使用 INFORMATION_SCHEMA + 动态 SQL 模式）
-- =====================================================================

-- ---------- 0) 工具存储过程：按列名幂等加列(沿用阶段三命名空间) ----------
DROP PROCEDURE IF EXISTS phase4_add_col_if_missing;
DELIMITER //
CREATE PROCEDURE phase4_add_col_if_missing(
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

-- ---------- 1) 兜底：若老库 crm_customer_share 表不存在(理论上 crm_full.sql 已建) ----------
CREATE TABLE IF NOT EXISTS `crm_customer_share` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键ID',
  `customer_id` BIGINT       NOT NULL                         COMMENT '客户ID(逻辑关联crm_customer.id)',
  `user_id`     BIGINT       NOT NULL                         COMMENT '被共享人ID(逻辑关联sys_user.id)',
  `auth_type`   TINYINT      DEFAULT 1                        COMMENT '权限类型(1只读 2读写)',
  `create_by`   VARCHAR(64)  DEFAULT ''                       COMMENT '发起人(主销售username)',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP        COMMENT '共享时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cust_user` (`customer_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户团队协同共享表';

-- 兜底加列:老库可能没有 create_by
CALL phase4_add_col_if_missing('crm_customer_share', 'create_by', 'VARCHAR(64) DEFAULT ''''');

-- ---------- 2) 2 个新菜单（按 perms 幂等，id 由 AUTO_INCREMENT 自动分配） ----------
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '客户共享', 0, 19, '', NULL, 'F', 'crm:customer:share', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:customer:share');
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '公海池',   0, 20, '', NULL, 'F', 'crm:customer:public_pool', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:customer:public_pool');

-- ---------- 3) 重新绑定角色-菜单(只对包含新权限的 4 个角色; finance 不参与) ----------
-- 注意:此处不能 DELETE 全表,会破坏阶段一/二/三的角色绑定。
--       采用"按角色清掉旧 + 按角色插新"的方式,保留其他角色的原有权限。
--       兜底:对于 admin/sales_director/sales_lead/sales,先清掉这两个 perms 的旧绑定,
--             再按新规则 INSERT。

-- 3.1) admin (1) — 全部
DELETE FROM sys_role_menu WHERE role_id = 1
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN ('crm:customer:share','crm:customer:public_pool'));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 1, id FROM sys_menu WHERE perms IN ('crm:customer:share','crm:customer:public_pool');

-- 3.2) sales_director (2) — 共享 + 公海
DELETE FROM sys_role_menu WHERE role_id = 2
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN ('crm:customer:share','crm:customer:public_pool'));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 2, id FROM sys_menu WHERE perms IN ('crm:customer:share','crm:customer:public_pool');

-- 3.3) sales_lead (3) — 共享 + 公海
DELETE FROM sys_role_menu WHERE role_id = 3
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN ('crm:customer:share','crm:customer:public_pool'));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 3, id FROM sys_menu WHERE perms IN ('crm:customer:share','crm:customer:public_pool');

-- 3.4) sales (4) — 共享 + 公海
DELETE FROM sys_role_menu WHERE role_id = 4
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN ('crm:customer:share','crm:customer:public_pool'));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 4, id FROM sys_menu WHERE perms IN ('crm:customer:share','crm:customer:public_pool');

-- 3.5) finance (5) — 财务不参与客户/共享/公海,此处不动

-- ---------- 4) 清理：删除工具存储过程 ----------
DROP PROCEDURE phase4_add_col_if_missing;

-- =====================================================================
--  验证（执行后跑一下确认）：
--  SHOW TABLES LIKE 'crm_customer_share';
--  SHOW COLUMNS FROM crm_customer_share;
--  SELECT id, perms, menu_name FROM sys_menu WHERE perms LIKE 'crm:customer:%' ORDER BY id;
--  SELECT r.role_key, m.perms
--    FROM sys_role r
--    JOIN sys_role_menu rm ON r.id = rm.role_id
--    JOIN sys_menu m       ON rm.menu_id = m.id
--   WHERE m.perms IN ('crm:customer:share','crm:customer:public_pool')
--   ORDER BY r.id, m.perms;
-- =====================================================================

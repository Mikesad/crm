-- =====================================================================
--  阶段三 Schema 增量迁移(phase8 commit1 已剔除 crm_approval 段)
--
--  适用：已按 crm_full.sql + phase2-menu-update.sql 初始化过数据库,
--        需要补齐阶段三表/字段/权限的环境。
--  时间：2026-06-27(phase8 commit1 修订:2026-06-30 移除 crm_approval)
--
--  变更：
--    1. crm_receivable_plan 补齐 is_deleted / create_by / create_time /
--       update_by / update_time 5 个字段（与 CLAUDE.md "客户/联系人/商机/
--       合同表均含 is_deleted" 对齐 + MyBatis-Plus @TableLogic 需要）
--    2. crm_product 补齐 create_by / update_by / update_time 3 个字段
--       (MyBatis-Plus 实体需要)
--    3. 新增 4 个菜单权限码:
--         crm:receivable:edit       回款编辑（财务录入）
--         crm:receivable_plan:edit  回款计划编辑（销售录入）
--         crm:product:list          产品列表
--         crm:product:edit          产品编辑
--    4. 重新绑定 sys_role_menu，覆盖阶段二的"销售无合同"漏配
--    (phase8 commit1 移除:crm:contract:approve / crm_approval 表)
--
--  幂等：每条 ALTER/INSERT 用存储过程 + WHERE NOT EXISTS 保护，
--        可重复执行。
--  MySQL 版本要求：>= 5.5（使用 INFORMATION_SCHEMA + 动态 SQL 模式）
-- =====================================================================

-- ---------- 0) 工具存储过程：按列名幂等加列 ----------
DROP PROCEDURE IF EXISTS phase3_add_col_if_missing;
DELIMITER //
CREATE PROCEDURE phase3_add_col_if_missing(
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

-- ---------- 1) crm_receivable_plan 补齐 5 个字段 ----------
-- 注：列 COMMENT 在 crm_full.sql 全新安装时已带;此处为已运行老版本 SQL 的迁移,
--     简化定义为无 COMMENT,不影响 MyBatis-Plus 实体映射
CALL phase3_add_col_if_missing('crm_receivable_plan', 'is_deleted',  'TINYINT DEFAULT 0');
CALL phase3_add_col_if_missing('crm_receivable_plan', 'create_by',   'VARCHAR(64) DEFAULT NULL');
CALL phase3_add_col_if_missing('crm_receivable_plan', 'create_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP');
CALL phase3_add_col_if_missing('crm_receivable_plan', 'update_by',   'VARCHAR(64) DEFAULT NULL');
CALL phase3_add_col_if_missing('crm_receivable_plan', 'update_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- ---------- 1.1) crm_product 补齐 3 个字段 (阶段三实体需要 create_by/update_by/update_time) ----------
CALL phase3_add_col_if_missing('crm_product', 'create_by',   'VARCHAR(64) DEFAULT NULL');
CALL phase3_add_col_if_missing('crm_product', 'update_by',   'VARCHAR(64) DEFAULT NULL');
CALL phase3_add_col_if_missing('crm_product', 'update_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- (phase8 commit1 移除 crm_approval 表创建段)

-- ---------- 3) 4 个新菜单（按 perms 幂等，id 由 AUTO_INCREMENT 自动分配） ----------
-- phase8 commit1 移除:crm:contract:approve (合同审批)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '回款编辑',     0, 15, '', NULL, 'F', 'crm:receivable:edit',     1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:receivable:edit');
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '回款计划编辑', 0, 16, '', NULL, 'F', 'crm:receivable_plan:edit', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:receivable_plan:edit');
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '产品列表',     0, 17, '', NULL, 'F', 'crm:product:list',        1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:product:list');
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '产品编辑',     0, 18, '', NULL, 'F', 'crm:product:edit',        1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:product:edit');

-- ---------- 4) 重新绑定角色-菜单 ----------
DELETE FROM sys_role_menu;

-- admin (id=1): 全部
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 1, id FROM sys_menu WHERE status = 1;

-- sales_director (id=2): 业务+合同+审批+计划+产品, 无任何 crm:receivable:*
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 2, id FROM sys_menu
   WHERE status = 1
     AND perms NOT LIKE 'crm:receivable:%';

-- sales_lead (id=3): 业务+合同(仅列表)+回款(仅列表)+计划编辑+产品(仅列表)
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 3, id FROM sys_menu
   WHERE status = 1
     AND perms IN (
       'crm:customer:list', 'crm:lead:list', 'crm:business:list',
       'crm:contact:list', 'crm:record:list', 'crm:contract:list',
       'crm:receivable:list', 'crm:receivable_plan:edit', 'crm:product:list'
     );

-- sales (id=4): 业务+合同(含编辑)+计划编辑+产品全, 无回款
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 4, id FROM sys_menu
   WHERE status = 1
     AND perms IN (
       'crm:customer:list', 'crm:customer:edit',
       'crm:lead:list',     'crm:lead:edit',
       'crm:business:list', 'crm:business:edit',
       'crm:contact:list',  'crm:contact:edit',
       'crm:record:list',   'crm:record:add',
       'crm:contract:list', 'crm:contract:edit',
       'crm:receivable_plan:edit',
       'crm:product:list',  'crm:product:edit'
     );

-- finance (id=5): 合同(仅列表)+回款(含编辑)+产品(仅列表)
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 5, id FROM sys_menu
   WHERE status = 1
     AND perms IN (
       'crm:contract:list', 'crm:receivable:list',
       'crm:receivable:edit', 'crm:product:list'
     );

-- ---------- 5) 清理：删除工具存储过程 ----------
DROP PROCEDURE phase3_add_col_if_missing;

-- =====================================================================
--  验证（执行后跑一下确认）：
-- (phase8 commit1 移除 crm_approval 验证行)
--  SHOW COLUMNS FROM crm_receivable_plan;       -- 应包含 is_deleted / create_*
--  SHOW TABLES LIKE 'crm_approval';        -- phase8 commit1 移除:crm_approval 已不存在
--  SELECT perms, COUNT(*) AS role_count FROM sys_role_menu rm
--    JOIN sys_menu m ON rm.menu_id = m.id
--   GROUP BY perms ORDER BY perms;
--  SELECT r.role_key, COUNT(rm.menu_id) AS perm_count
--    FROM sys_role r LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
--   GROUP BY r.id, r.role_key;
-- =====================================================================

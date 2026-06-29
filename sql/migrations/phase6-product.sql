-- =====================================================================
--  阶段六(commit 2) · 产品管理 Schema 增量迁移
--  v0.7 撤回:D4 "中度 SaaS 升级"撤销,移除 productLine + billingCycle 2 字段
--
--  适用:已按 crm_full.sql + phase2-menu-update.sql +
--        phase3-approval-and-plan-soft-delete.sql +
--        phase4-customer-share-and-public-pool.sql +
--        phase5-record.sql + phase5-report.sql +
--        phase6-system.sql 初始化过数据库,
--        需要补齐阶段六产品管理功能的环境。
--  时间:2026-06-29(v0.7 修订:移除 product_line/billing_cycle 字段)
--
--  变更:
--    1. crm_product_category 表补 5 审计字段(原建表漏了):
--         create_by / create_time / update_by / update_time / is_deleted
--    2. (v0.7 撤销)crm_product 加 2 字段——已撤回,产品库回到阶段三原版
--    3. 新增 2 个 sys_menu 记录(产品分类,F 按钮级,parent_id=0 顶级):
--         F  crm:product:category:list   产品分类(查看)
--         F  crm:product:category:edit   产品分类(新建/编辑/删除)
--    4. sys_role_menu 绑定 **5 角色全员**(D7 v0.4 产品/分类全员可见):
--         admin(1) / sales_director(2) / sales_lead(3) / sales(4) / finance(5)
--
--  幂等:列加用 phase6_product_add_col_if_missing 存储过程;
--        菜单按 perms NOT EXISTS 保护;角色绑定用 DELETE + INSERT 模式。
--        可重复执行。
--  MySQL 版本要求:>= 5.5
-- =====================================================================

-- ---------- 0) 加列工具(按列名幂等加列) ----------
DROP PROCEDURE IF EXISTS phase6_product_add_col_if_missing;
DELIMITER //
CREATE PROCEDURE phase6_product_add_col_if_missing(
  IN  p_tbl VARCHAR(64),
  IN  p_col VARCHAR(64),
  IN  p_def TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = p_tbl
       AND COLUMN_NAME  = p_col
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', p_tbl, ' ADD COLUMN ', p_col, ' ', p_def);
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;

-- ---------- 1) crm_product_category 补 5 审计字段 ----------
CALL phase6_product_add_col_if_missing('crm_product_category', 'create_by',
  'VARCHAR(64) DEFAULT '''' COMMENT ''创建者''');

CALL phase6_product_add_col_if_missing('crm_product_category', 'create_time',
  'DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''');

CALL phase6_product_add_col_if_missing('crm_product_category', 'update_by',
  'VARCHAR(64) DEFAULT '''' COMMENT ''更新者''');

CALL phase6_product_add_col_if_missing('crm_product_category', 'update_time',
  'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');

CALL phase6_product_add_col_if_missing('crm_product_category', 'is_deleted',
  'TINYINT DEFAULT 0 COMMENT ''是否删除(逻辑删除)''');

-- 清理加列工具
DROP PROCEDURE phase6_product_add_col_if_missing;

-- ---------- 2) 2 个新菜单(按 perms 幂等,id 由 AUTO_INCREMENT 自动分配) ----------

-- 2.1 产品分类(查看,菜单类型 F)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '产品分类', 0, 24, '', NULL, 'F', 'crm:product:category:list', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:product:category:list');

-- 2.2 产品分类(编辑,菜单类型 F)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '产品分类编辑', 0, 25, '', NULL, 'F', 'crm:product:category:edit', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:product:category:edit');

-- ---------- 3) 重新绑定角色-菜单(5 角色全员绑产品分类 2 条;不动其他菜单) --
-- 注意:此处不能 DELETE 全表,会破坏阶段一/二/三/四/五的角色绑定。
--       采用"按角色清掉旧 + 按角色插新"的方式,保留其他角色的原有权限。

-- 2 条 crm:product:category:* 权限码
-- crm:product:category:list / crm:product:category:edit

-- 3.1) admin (1) — 2 条全绑
DELETE FROM sys_role_menu WHERE role_id = 1
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN (
    'crm:product:category:list','crm:product:category:edit'
  ));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 1, id FROM sys_menu WHERE perms IN (
    'crm:product:category:list','crm:product:category:edit'
  );

-- 3.2) sales_director (2) — 2 条全绑
DELETE FROM sys_role_menu WHERE role_id = 2
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN (
    'crm:product:category:list','crm:product:category:edit'
  ));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 2, id FROM sys_menu WHERE perms IN (
    'crm:product:category:list','crm:product:category:edit'
  );

-- 3.3) sales_lead (3) — 2 条全绑(D7 v0.4:产品/分类全员)
DELETE FROM sys_role_menu WHERE role_id = 3
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN (
    'crm:product:category:list','crm:product:category:edit'
  ));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 3, id FROM sys_menu WHERE perms IN (
    'crm:product:category:list','crm:product:category:edit'
  );

-- 3.4) sales (4) — 2 条全绑
DELETE FROM sys_role_menu WHERE role_id = 4
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN (
    'crm:product:category:list','crm:product:category:edit'
  ));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 4, id FROM sys_menu WHERE perms IN (
    'crm:product:category:list','crm:product:category:edit'
  );

-- 3.5) finance (5) — 2 条全绑(财务选产品时要看分类)
DELETE FROM sys_role_menu WHERE role_id = 5
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN (
    'crm:product:category:list','crm:product:category:edit'
  ));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 5, id FROM sys_menu WHERE perms IN (
    'crm:product:category:list','crm:product:category:edit'
  );

-- =====================================================================
--  验证(执行后跑一下确认):
--  DESCRIBE crm_product_category;
--  -- 应有 5 审计字段(create_by/create_time/update_by/update_time/is_deleted)
--
--  SELECT m.id, m.menu_name, m.menu_type, m.perms, m.order_num
--    FROM sys_menu m
--   WHERE m.perms LIKE 'crm:product:category:%'
--   ORDER BY m.id;
--
--  SELECT r.role_key, m.perms
--    FROM sys_role r
--    JOIN sys_role_menu rm ON r.id = rm.role_id
--    JOIN sys_menu m       ON rm.menu_id = m.id
--   WHERE m.perms LIKE 'crm:product:category:%'
--   ORDER BY r.id, m.id;
--  -- 5 个角色都应看到 crm:product:category:list / edit 2 条
-- =====================================================================
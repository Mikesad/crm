-- =====================================================================
--  阶段五(commit 2) · 报表中心 Schema 增量迁移
--
--  适用：已按 crm_full.sql + phase2-menu-update.sql +
--        phase3-approval-and-plan-soft-delete.sql +
--        phase4-customer-share-and-public-pool.sql +
--        phase5-record.sql 初始化过数据库,
--        需要补齐阶段五报表中心功能的环境。
--  时间：2026-06-28
--
--  变更：
--    1. 7 个二级索引(优化报表聚合 SQL 性能):
--         crm_contract   idx_start_date   (start_date)
--         crm_receivable idx_return_date  (return_date)
--         crm_business   idx_stage        (stage)
--         crm_business   idx_expected_deal(expected_deal_date)
--         crm_record     idx_create_time  (create_time)
--         crm_record     idx_related      (related_type, related_id)
--         crm_customer   idx_industry     (industry)
--         crm_customer   idx_last_follow  (last_follow_time, is_deleted)
--    2. 新增 1 个菜单权限码:
--         crm:report:view   报表中心(侧边栏入口,菜单类型 C,4 Tab 共用)
--    3. 重新绑定 sys_role_menu,5 个角色全部挂上(决策 B:
--       报表中心所有登录用户可见,finance 也参与,看全量数据)
--
--  幂等：索引走 phase5_add_idx_if_missing 存储过程(若已删则重建);
--        菜单按 perms NOT EXISTS 保护;角色绑定用 DELETE + INSERT 模式;
--        可重复执行。
--  MySQL 版本要求：>= 5.5（使用 INFORMATION_SCHEMA + 动态 SQL 模式）
-- =====================================================================

-- ---------- 0) 工具存储过程：按索引名幂等加索引(若已被 commit 1 删除则重建) ----------
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

-- ---------- 1) 8 个二级索引(报表聚合 SQL 性能) ----------

-- 1.1 合同表:按开始日期聚合(销售业绩/合同趋势;表无 sign_date 字段)
CALL phase5_add_idx_if_missing('crm_contract',   'idx_start_date',   '(`start_date`)');
-- 1.2 回款表:按实收日期聚合(回款趋势;表无 actual_time 字段,实际为 return_date)
CALL phase5_add_idx_if_missing('crm_receivable', 'idx_return_date',  '(`return_date`)');
-- 1.3 商机表:按阶段分组(销售漏斗;stage 是 varchar 存中文)
CALL phase5_add_idx_if_missing('crm_business',   'idx_stage',        '(`stage`)');
-- 1.4 商机表:按预计成交日期(转化率分析)
CALL phase5_add_idx_if_missing('crm_business',   'idx_expected_deal','(`expected_deal_date`)');
-- 1.5 跟进表:按创建时间(跟进趋势/统计)
CALL phase5_add_idx_if_missing('crm_record',     'idx_create_time',  '(`create_time`)');
-- 1.6 跟进表:按 related_type+related_id 复合(转化漏斗关联)
CALL phase5_add_idx_if_missing('crm_record',     'idx_related',      '(`related_type`, `related_id`)');
-- 1.7 客户表:按行业(行业分布)
CALL phase5_add_idx_if_missing('crm_customer',   'idx_industry',     '(`industry`)');
-- 1.8 客户表:按最后跟进时间+逻辑删除(活跃/沉睡分布)
CALL phase5_add_idx_if_missing('crm_customer',   'idx_last_follow',  '(`last_follow_time`, `is_deleted`)');

-- ---------- 2) 1 个新菜单(报表中心,侧边栏入口,菜单类型 C) ----------
-- order_num=30 跟在 crm:lead:markDead(26) 之后,留出 27-29 给后续 commit
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '报表中心', 0, 30, 'report', 'report/index', 'C', 'crm:report:view', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:report:view');

-- ---------- 3) 重新绑定角色-菜单(全部 5 个角色:admin/director/lead/sales/finance) ----------
-- 决策 B:报表中心所有登录用户可见,无数据权限分层。
-- 注意:此处不能 DELETE 全表,会破坏阶段一/二/三/四/五-1 的角色绑定。
--       采用"按角色清掉旧 + 按角色插新"的方式,保留其他角色的原有权限。

-- 3.1) admin (1) — 全部
DELETE FROM sys_role_menu WHERE role_id = 1
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms = 'crm:report:view');
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 1, id FROM sys_menu WHERE perms = 'crm:report:view';

-- 3.2) sales_director (2) — 报表中心
DELETE FROM sys_role_menu WHERE role_id = 2
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms = 'crm:report:view');
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 2, id FROM sys_menu WHERE perms = 'crm:report:view';

-- 3.3) sales_lead (3) — 报表中心
DELETE FROM sys_role_menu WHERE role_id = 3
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms = 'crm:report:view');
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 3, id FROM sys_menu WHERE perms = 'crm:report:view';

-- 3.4) sales (4) — 报表中心
DELETE FROM sys_role_menu WHERE role_id = 4
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms = 'crm:report:view');
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 4, id FROM sys_menu WHERE perms = 'crm:report:view';

-- 3.5) finance (5) — 报表中心(决策 B:财务也参与,看全量数据)
DELETE FROM sys_role_menu WHERE role_id = 5
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms = 'crm:report:view');
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 5, id FROM sys_menu WHERE perms = 'crm:report:view';

-- ---------- 4) 清理：删除工具存储过程(与 commit 1 保持一致) ----------
DROP PROCEDURE phase5_add_idx_if_missing;

-- =====================================================================
--  验证（执行后跑一下确认）：
--  SHOW INDEX FROM crm_contract   WHERE Key_name = 'idx_start_date';
--  SHOW INDEX FROM crm_receivable WHERE Key_name = 'idx_return_date';
--  SHOW INDEX FROM crm_business   WHERE Key_name IN ('idx_stage','idx_expected_deal');
--  SHOW INDEX FROM crm_record     WHERE Key_name IN ('idx_create_time','idx_related');
--  SHOW INDEX FROM crm_customer   WHERE Key_name IN ('idx_industry','idx_last_follow');
--
--  SELECT id, perms, menu_name, menu_type, path, order_num
--    FROM sys_menu WHERE perms = 'crm:report:view';
--
--  SELECT r.id, r.role_key, m.perms
--    FROM sys_role r
--    JOIN sys_role_menu rm ON r.id = rm.role_id
--    JOIN sys_menu m       ON rm.menu_id = m.id
--   WHERE m.perms = 'crm:report:view'
--   ORDER BY r.id;
-- =====================================================================

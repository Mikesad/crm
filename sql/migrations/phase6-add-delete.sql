-- =====================================================================
--  阶段六 commit 1 v0.5 · 加 6 个独立的 crm:*:delete 权限码
--
--  适用:已跑 phase6-system.sql + phase6-add-perms.sql 的环境
--  时间:2026-06-29
--
--  背景:
--    阶段六 detail.vue 权限矩阵里有"删除"checkbox,但 code=null,所以点不动。
--    仿照 crm:*:add 的做法,补全 crm:*:delete 独立 permCode。
--
--  变更:
--    新增 6 个 F-type sys_menu:
--      crm:lead:delete
--      crm:customer:delete
--      crm:business:delete
--      crm:contract:delete
--      crm:receivable:delete
--      crm:product:delete
--
--    角色绑定(与 crm:*:add 一致:admin + sales_director):
--      6 条新删 perms → role_id 1 (admin) + 2 (sales_director)
--
--  幂等:sys_menu INSERT 走 WHERE NOT EXISTS;sys_role_menu 走 DELETE + INSERT 模式。
-- =====================================================================

-- ---------- 0) 6 个新菜单 ----------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '线索删除', 0, 99, '', NULL, 'F', 'crm:lead:delete', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:lead:delete');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '客户删除', 0, 99, '', NULL, 'F', 'crm:customer:delete', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:customer:delete');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '商机删除', 0, 99, '', NULL, 'F', 'crm:business:delete', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:business:delete');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '合同删除', 0, 99, '', NULL, 'F', 'crm:contract:delete', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:contract:delete');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '回款删除', 0, 99, '', NULL, 'F', 'crm:receivable:delete', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:receivable:delete');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '产品删除', 0, 99, '', NULL, 'F', 'crm:product:delete', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:product:delete');

-- ---------- 1) 角色绑定 ----------
DELETE FROM sys_role_menu
 WHERE menu_id IN (SELECT id FROM sys_menu
                   WHERE perms IN ('crm:lead:delete','crm:customer:delete','crm:business:delete',
                                   'crm:contract:delete','crm:receivable:delete','crm:product:delete'));

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.perms IN ('crm:lead:delete','crm:customer:delete','crm:business:delete',
                                   'crm:contract:delete','crm:receivable:delete','crm:product:delete')
 WHERE r.id IN (1, 2);

-- =====================================================================
--  验证:
--  SELECT perms, menu_name FROM sys_menu
--   WHERE perms LIKE 'crm:%:delete' ORDER BY id;
--  SELECT r.role_key, m.perms
--    FROM sys_role r
--    JOIN sys_role_menu rm ON r.id = rm.role_id
--    JOIN sys_menu m ON rm.menu_id = m.id
--   WHERE m.perms LIKE 'crm:%:delete'
--   ORDER BY r.id, m.perms;
-- =====================================================================

-- =====================================================================
--  阶段六 commit 1 收尾 · 加 5 个独立的 crm:*:add 权限码
--
--  适用:已跑 phase6-system.sql(10 条 sys_menu)的环境
--  时间:2026-06-29
--
--  背景:
--    原 sys_menu 只有 crm:lead:edit / crm:customer:edit / crm:business:edit /
--    crm:contract:edit / crm:product:edit(添加/编辑共用 1 个 permCode),
--    导致 detail.vue 权限矩阵里"添加"和"编辑"两个 checkbox 共享同一个 code,
--    点任一个另一个视觉跟着翻,UX 看起来像 bug。
--
--  变更:
--    新增 5 个 F-type sys_menu 记录(独立 permCode):
--      crm:lead:add
--      crm:customer:add
--      crm:business:add
--      crm:contract:add
--      crm:product:add
--
--    角色绑定(与 crm:*:edit 镜像 + 产品单独 admin/director/sales):
--      crm:lead:add      → roles 1, 2, 3, 4 (admin/director/lead/sales)
--      crm:customer:add  → roles 1, 2, 3, 4
--      crm:business:add  → roles 1, 2, 3, 4
--      crm:contract:add  → roles 1, 2, 3, 4
--      crm:product:add   → roles 1, 2, 4    (admin/director/sales)
--
--  幂等:
--    1. INSERT IGNORE INTO sys_menu(perms 唯一+WHERE NOT EXISTS 等价)
--    2. DELETE + INSERT 模式重绑 sys_role_menu(老绑定先清,后绑)
--    可重复执行。
-- =====================================================================

-- ---------- 0) 5 个新菜单(WHERE NOT EXISTS 幂等) ----------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '线索添加', 0, 99, '', NULL, 'F', 'crm:lead:add', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:lead:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '客户添加', 0, 99, '', NULL, 'F', 'crm:customer:add', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:customer:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '商机添加', 0, 99, '', NULL, 'F', 'crm:business:add', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:business:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '合同添加', 0, 99, '', NULL, 'F', 'crm:contract:add', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:contract:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '产品添加', 0, 99, '', NULL, 'F', 'crm:product:add', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:product:add');

-- ---------- 1) 角色绑定(DELETE + INSERT 幂等模式) ----------

-- 1.1) 清掉 5 个新菜单的所有老绑定
DELETE FROM sys_role_menu
 WHERE menu_id IN (SELECT id FROM sys_menu
                   WHERE perms IN ('crm:lead:add','crm:customer:add','crm:business:add','crm:contract:add','crm:product:add'));

-- 1.2) 4 角色绑前 4 个 add(lead/customer/business/contract)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.perms IN ('crm:lead:add','crm:customer:add','crm:business:add','crm:contract:add')
 WHERE r.id IN (1, 2, 3, 4);

-- 1.3) 3 角色绑 product:add(与 product:edit 镜像:admin/director/sales)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.perms = 'crm:product:add'
 WHERE r.id IN (1, 2, 4);

-- =====================================================================
--  验证(执行后跑一下确认):
--  SELECT perms, menu_name FROM sys_menu
--   WHERE perms IN ('crm:lead:add','crm:customer:add','crm:business:add',
--                   'crm:contract:add','crm:product:add') ORDER BY id;
--
--  SELECT r.role_key, m.perms
--    FROM sys_role r
--    JOIN sys_role_menu rm ON r.id = rm.role_id
--    JOIN sys_menu m       ON rm.menu_id = m.id
--   WHERE m.perms LIKE 'crm:%:add'
--   ORDER BY r.id, m.perms;
-- =====================================================================

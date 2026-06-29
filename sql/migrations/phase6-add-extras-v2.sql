-- =====================================================================
--  阶段六 commit 1 v0.6 · 拆 编辑共享:加 3 个独立 permCode
--
--  适用:已跑 phase6-system.sql + phase6-add-perms.sql + phase6-add-delete.sql
--  时间:2026-06-29
--
--  背景:
--    用户管理"添加/编辑"和回款"添加/编辑"在同一行共享同一个 permCode(sys:user:edit / crm:receivable:edit),
--    导致 detail.vue 矩阵里勾一个另一个视觉同时翻。仿造 crm:*:add 的做法拆开:
--
--  新增 3 个 permCode:
--    sys:user:add            用户新建(原 create 共用 sys:user:edit)
--    sys:user:delete         用户删除(此前根本没这个 permCode,删除=null)
--    crm:receivable:add      回款新建(原 create 共用 crm:receivable:edit)
--
--  注:crm:receivable:delete 阶段六 add-delete 早已加入,这里不动。
--
--  角色绑定(admin + sales_director):
--    这 3 个 sys_menu F-type 绑 role 1 + 2 即可
--
--  幂等:sys_menu INSERT 走 WHERE NOT EXISTS;sys_role_menu 走 DELETE + INSERT。
-- =====================================================================

-- ---------- 0) 3 个新菜单 ----------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '用户新建', 0, 99, '', NULL, 'F', 'sys:user:add', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:user:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '用户删除', 0, 99, '', NULL, 'F', 'sys:user:delete', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:user:delete');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, status)
SELECT '回款新建', 0, 99, '', NULL, 'F', 'crm:receivable:add', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'crm:receivable:add');

-- ---------- 1) 角色绑定 ----------
DELETE FROM sys_role_menu
 WHERE menu_id IN (SELECT id FROM sys_menu
                   WHERE perms IN ('sys:user:add','sys:user:delete','crm:receivable:add'));

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.perms IN ('sys:user:add','sys:user:delete','crm:receivable:add')
 WHERE r.id IN (1, 2);

-- =====================================================================
--  验证:
--  SELECT perms, menu_name FROM sys_menu
--   WHERE perms IN ('sys:user:add','sys:user:delete','crm:receivable:add') ORDER BY id;
--
--  SELECT r.role_key, m.perms
--    FROM sys_role r
--    JOIN sys_role_menu rm ON r.id = rm.role_id
--    JOIN sys_menu m ON rm.menu_id = m.id
--   WHERE m.perms IN ('sys:user:add','sys:user:delete','crm:receivable:add')
--   ORDER BY r.id, m.perms;
-- =====================================================================

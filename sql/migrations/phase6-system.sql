-- =====================================================================
--  阶段六(commit 1) · 系统设置 Schema 增量迁移
--  v0.3 更新:撤销部门管理模块,从 12 条 sys_menu 减为 10 条
--
--  适用:已按 crm_full.sql + phase2-menu-update.sql +
--        phase3-approval-and-plan-soft-delete.sql +
--        phase4-customer-share-and-public-pool.sql +
--        phase5-record.sql + phase5-report.sql 初始化过数据库,
--        需要补齐阶段六系统设置功能的环境。
--  时间:2026-06-29(v0.3 修订:去掉 sys:dept:*)
--
--  变更:
--    1. (无)本 commit 1 不动业务表结构
--    2. 新增 10 个 sys_menu 记录(v0.3:删除部门管理 2 条 sys:dept:*):
--         M  sys:system:view       管理组入口
--         C  sys:user:list          用户管理
--         F  sys:user:edit          新建/编辑/删除/启停用
--         F  sys:user:reset_pwd     重置密码
--         F  sys:user:assign_role   分配角色
--         C  sys:role:list          角色管理
--         F  sys:role:edit          新建/编辑/删除
--         F  sys:role:assign_menu   分配菜单
--         C  sys:menu:list          菜单权限(矩阵展示用,CRUD UI 暂不做)
--         F  sys:menu:edit          同上
--    3. sys_role_menu 绑定 admin(role_id=1) + sales_director(role_id=2)
--       共 2 个角色,10 条菜单全绑;其他 3 角色不动
--    4. v0.3 老菜单清理(若老库已通过 v0.2 阶段跑过包含部门的脚本):
--         按 id 范围 34-35 兜底清理
--
--  幂等:菜单按 perms NOT EXISTS 保护;角色绑定用 DELETE + INSERT 模式;
--        v0.3 老菜单清理也按 id 范围幂等(若不存在则 DELETE 无影响)。
--        可重复执行。
--  MySQL 版本要求:>= 5.5
-- =====================================================================

-- ---------- 0) (本 commit 1 不需要加列工具,保留空) ----------

-- ---------- 1) 10 个新菜单(按 perms 幂等,id 由 AUTO_INCREMENT 自动分配) ----------

-- 1.1 管理组入口(M 目录,作为"管理"组 sidebar 入口,菜单类型 M)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '管理', 0, 35, '/system', 'layout', 'M', 'sys:system:view', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:system:view');

-- 1.2 用户管理(子菜单入口,菜单类型 C)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '用户管理', (SELECT id FROM sys_menu WHERE perms = 'sys:system:view'), 1,
       'system/user', 'system/user', 'C', 'sys:user:list', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:user:list');

-- 1.3 用户编辑(按钮级,菜单类型 F)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '用户编辑', (SELECT id FROM sys_menu WHERE perms = 'sys:system:view'), 2,
       '', NULL, 'F', 'sys:user:edit', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:user:edit');

-- 1.4 重置密码
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '重置密码', (SELECT id FROM sys_menu WHERE perms = 'sys:system:view'), 3,
       '', NULL, 'F', 'sys:user:reset_pwd', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:user:reset_pwd');

-- 1.5 分配角色
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '分配角色', (SELECT id FROM sys_menu WHERE perms = 'sys:system:view'), 4,
       '', NULL, 'F', 'sys:user:assign_role', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:user:assign_role');

-- 1.6 角色管理(C)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '角色管理', (SELECT id FROM sys_menu WHERE perms = 'sys:system:view'), 5,
       'system/role', 'system/role', 'C', 'sys:role:list', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:role:list');

-- 1.7 角色编辑(F)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '角色编辑', (SELECT id FROM sys_menu WHERE perms = 'sys:system:view'), 6,
       '', NULL, 'F', 'sys:role:edit', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:role:edit');

-- 1.8 分配菜单
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '分配菜单', (SELECT id FROM sys_menu WHERE perms = 'sys:system:view'), 7,
       '', NULL, 'F', 'sys:role:assign_menu', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:role:assign_menu');

-- 1.9 菜单权限(C,只读用于权限矩阵展示,CRUD UI v0.3 不做)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '菜单权限', (SELECT id FROM sys_menu WHERE perms = 'sys:system:view'), 8,
       'system/menu', 'system/menu', 'C', 'sys:menu:list', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:menu:list');

-- 1.10 菜单编辑(F)
INSERT INTO sys_menu (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT '菜单编辑', (SELECT id FROM sys_menu WHERE perms = 'sys:system:view'), 9,
       '', NULL, 'F', 'sys:menu:edit', 1
  FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'sys:menu:edit');

-- 1.11 v0.3 老菜单清理(若老库已通过 v0.2 阶段跑过包含部门的脚本):
--       按 id 范围 34-35 兜底清理(id 范围匹配 sys:dept:* 菜单的 AUTO_INCREMENT 区间)
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 34 AND 35
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms LIKE 'sys:dept:%');
DELETE FROM sys_menu WHERE id BETWEEN 34 AND 35
  AND perms LIKE 'sys:dept:%';

-- ---------- 2) 重新绑定角色-菜单(admin + 销售总监 共 2 角色;其他 3 角色不动) --
-- 注意:此处不能 DELETE 全表,会破坏阶段一/二/三/四/五的角色绑定。
--       采用"按角色清掉旧 + 按角色插新"的方式,保留其他角色的原有权限。

-- 10 条 sys:* 权限码(v0.3:删 sys:dept:* 2 条,部门管理模块整体撤回)
-- sys:system:view / sys:user:list / sys:user:edit / sys:user:reset_pwd / sys:user:assign_role
-- sys:role:list / sys:role:edit / sys:role:assign_menu
-- sys:menu:list / sys:menu:edit

-- 2.1) admin (1) — 全部 10 条全绑
DELETE FROM sys_role_menu WHERE role_id = 1
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN (
    'sys:system:view',
    'sys:user:list','sys:user:edit','sys:user:reset_pwd','sys:user:assign_role',
    'sys:role:list','sys:role:edit','sys:role:assign_menu',
    'sys:menu:list','sys:menu:edit'
  ));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 1, id FROM sys_menu WHERE perms IN (
    'sys:system:view',
    'sys:user:list','sys:user:edit','sys:user:reset_pwd','sys:user:assign_role',
    'sys:role:list','sys:role:edit','sys:role:assign_menu',
    'sys:menu:list','sys:menu:edit'
  );

-- 2.2) sales_director (2) — 10 条全绑(与 admin 同等)
DELETE FROM sys_role_menu WHERE role_id = 2
  AND menu_id IN (SELECT id FROM sys_menu WHERE perms IN (
    'sys:system:view',
    'sys:user:list','sys:user:edit','sys:user:reset_pwd','sys:user:assign_role',
    'sys:role:list','sys:role:edit','sys:role:assign_menu',
    'sys:menu:list','sys:menu:edit'
  ));
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 2, id FROM sys_menu WHERE perms IN (
    'sys:system:view',
    'sys:user:list','sys:user:edit','sys:user:reset_pwd','sys:user:assign_role',
    'sys:role:list','sys:role:edit','sys:role:assign_menu',
    'sys:menu:list','sys:menu:edit'
  );

-- 2.3) sales_lead (3) — 不绑(主管不接触系统设置)
-- 2.4) sales (4) — 不绑
-- 2.5) finance (5) — 不绑

-- =====================================================================
--  验证(执行后跑一下确认):
--  SELECT m.id, m.menu_name, m.menu_type, m.perms, m.path
--    FROM sys_menu m WHERE m.perms LIKE 'sys:%' ORDER BY m.id;
--  SELECT r.role_key, COUNT(rm.menu_id) AS sys_perm_count
--    FROM sys_role r
--    LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
--    LEFT JOIN sys_menu m       ON rm.menu_id = m.id AND m.perms LIKE 'sys:%'
--   GROUP BY r.id, r.role_key
--   ORDER BY r.id;
-- =====================================================================

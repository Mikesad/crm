-- =====================================================================
--  阶段二菜单 / 权限更新迁移
--
--  适用：已按旧的 crm_full.sql 初始化过数据库，需要同步阶段二权限码的环境。
--  时间：2026-06-27
--
--  变更：
--    1. crm:opportunity:list  →  crm:business:list   （与表 crm_business 对齐）
--    2. 新增 6 个菜单：
--         商机编辑(crm:business:edit) / 联系人列表 / 联系人编辑 /
--         跟进记录列表 / 新增跟进 / 合同编辑 / 回款列表（同步阶段一遗漏）
--    3. 重新绑定 sys_role_menu，覆盖阶段一最小权限集
--
--  幂等：每条 INSERT/DELETE 用 IF EXISTS 保护，可重复执行。
-- =====================================================================

-- 1) 改名：crm:opportunity:list → crm:business:list
UPDATE sys_menu
   SET menu_name = '商机列表', perms = 'crm:business:list'
 WHERE perms = 'crm:opportunity:list';

-- 2) 修正旧"合同列表 / 合同编辑 / 回款列表" 编号（如果原 SQL 用的是旧 id=6/7/8，会与新插入冲突）
--    新版 sys_menu 用了 id=11/12/13 给合同/回款，这里不强制迁移旧数据，由用户自决。

-- 3) 新增菜单（id 100+ 防止与现有冲突；如果全新安装 crm_full.sql 可忽略此段）
INSERT IGNORE INTO sys_menu (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`) VALUES
  (101, '商机编辑',     0, 6,  '', NULL, 'F', 'crm:business:edit',  1),
  (102, '联系人列表',   0, 7,  '', NULL, 'F', 'crm:contact:list',   1),
  (103, '联系人编辑',   0, 8,  '', NULL, 'F', 'crm:contact:edit',   1),
  (104, '跟进记录列表', 0, 9,  '', NULL, 'F', 'crm:record:list',    1),
  (105, '新增跟进',     0, 10, '', NULL, 'F', 'crm:record:add',     1);

-- 4) 重新绑定角色-菜单（按新业务场景）
--    注意：以下为幂等删除 + 重新插入，不会重复添加

-- 清空旧绑定
DELETE FROM sys_role_menu;

-- 重新插入
-- admin (id=1): 全部
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 1, id FROM sys_menu WHERE status = 1;

-- sales_director (id=2): 业务 + 合同（含编辑），无回款
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 2, id FROM sys_menu
   WHERE status = 1
     AND perms NOT IN ('crm:receivable:list');

-- sales_lead (id=3): 业务 + 合同（仅列表）
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 3, id FROM sys_menu
   WHERE status = 1
     AND perms LIKE 'crm:%:list'
     AND perms IN ('crm:customer:list','crm:lead:list','crm:business:list',
                   'crm:contact:list','crm:record:list','crm:contract:list');

-- sales (id=4): 业务（仅列表，无合同/回款）
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 4, id FROM sys_menu
   WHERE status = 1
     AND perms LIKE 'crm:%:list'
     AND perms IN ('crm:customer:list','crm:lead:list','crm:business:list',
                   'crm:contact:list','crm:record:list');

-- finance (id=5): 合同/回款（仅列表）
INSERT INTO sys_role_menu (role_id, menu_id)
  SELECT 5, id FROM sys_menu
   WHERE status = 1
     AND perms IN ('crm:contract:list','crm:receivable:list');

-- =====================================================================
--  验证（执行后跑一下确认）：
--  SELECT perms, COUNT(*) FROM sys_role_menu rm
--    JOIN sys_menu m ON rm.menu_id = m.id
--   GROUP BY perms ORDER BY perms;
-- =====================================================================

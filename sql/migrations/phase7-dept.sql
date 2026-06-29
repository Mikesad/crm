-- =============================================================
-- 阶段七 commit · 部门管理增量迁移
-- 范围:仅插入 sys_menu 2 条(id 52-53)+ sys_role_menu 4 条(admin/sales_director 绑)
-- 无字段变更(sys_dept 表结构已在阶段一就绪),所以不走 phase<N>_add_col_if_missing 存储过程
-- 幂等策略:WHERE NOT EXISTS 兜底
-- 验证:mysql ... < sql/migrations/phase7-dept.sql  # 第一次 + 第二次(应无错)
-- =============================================================

-- 1. 部门管理 2 条菜单
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT 52, '部门管理', 24, 11, 'system/dept', 'system/dept/index', 'C', 'sys:dept:list', 1
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 52);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`)
SELECT 53, '部门编辑', 24, 12, '', NULL, 'F', 'sys:dept:edit', 1
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 53);

-- 2. 角色绑定:admin (1) + sales_director (2) 共 2 角色 × 2 菜单 = 4 条
-- 用 LEFT JOIN ... WHERE ... IS NULL 模式实现幂等
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 52
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 52);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 53
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 53);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, 52
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 2 AND menu_id = 52);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, 53
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 2 AND menu_id = 53);

-- =============================================================
-- 验证:阶段七 commit 完成后 admin 应有 24-33 + 47-48 + 50-51 + 52-53 全部权限码
--        sales_director 同上(财务/共享/公海除外)
-- SELECT role_id, COUNT(*) FROM sys_role_menu WHERE menu_id IN (52, 53) GROUP BY role_id;
-- 期望:role_id=1 → 2  role_id=2 → 2
-- =============================================================

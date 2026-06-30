-- =============================================================
-- 阶段八 commit 2 · 报表中心 部门业绩 + Filter 真树 增量迁移
-- 范围:仅 Java 代码变更,无 schema 变更(无新表/无新字段/无新菜单/无新权限码)
-- 因此本次无 phase<N>_add_col_if_missing / 无 INSERT sys_menu / 无 sys_role_menu
--
-- 改动列表(供运维/文档参考):
--   C2-D1 部门名接 sys_dept.dept_name     (替代"部门 + id"假数据)
--   C2-D2 部门业绩接前端 range              (替代写死近 10 年)
--   C2-D3 部门业绩接 ownerIds               (sales 看不到别人部门)
--   C2-D4 拆 2 口径 chip tab               (合同业绩 / 实际回款)
--   C2-D5 合同 status IN (1,2)             (排除审批中/已作废)
--   C2-D6 resolveOwnerIds 认子部门          (走 ancestors 后代)
--   C2-4  FilterBar depts 接 sys_dept 真树  (替代 mock 4 部门)
--   C2-5  FilterBar users 接 sys_user 真表  (替代空集合)
--
-- 验证:mysql ... < sql/migrations/phase8-report.sql  # 第一次 + 第二次(应无错)
-- =============================================================

SELECT 'phase8-report.sql applied: 部门业绩 6 改造 + Filter 真树' AS info;
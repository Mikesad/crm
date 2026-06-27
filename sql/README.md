# SQL 脚本目录

> 集中存放项目所有 SQL 脚本（**阶段三起**），与 `docs/db-migrations/` 旧布局兼容路径已废弃。

## 目录结构

```
sql/
├── crm.sql                                       # 原始脚本（历史归档,不要用于初始化）
├── crm_full.sql                                  # ⭐ 全新安装脚本（含建表 + 种子数据）
└── migrations/                                   # 增量迁移脚本（已运行老版本时用）
    ├── phase2-menu-update.sql                    # 阶段二：菜单权限码修正
    ├── phase2-test-data.sql                      # 阶段二：测试数据 seed
    └── phase3-approval-and-plan-soft-delete.sql  # 阶段三：crm_approval 新表 + crm_receivable_plan 补字段
```

## 何时用什么

| 场景 | 命令 |
| :--- | :--- |
| **全新安装** | `mysql -u root -p123456 crm_db < sql/crm_full.sql` |
| **已运行阶段一,需升级到阶段二** | `mysql -u root -p123456 crm_db < sql/migrations/phase2-menu-update.sql` |
| **已运行阶段二,需升级到阶段三** | `mysql -u root -p123456 crm_db < sql/migrations/phase3-approval-and-plan-soft-delete.sql` |
| **加载阶段二测试数据** | `mysql -u root -p123456 crm_db < sql/migrations/phase2-test-data.sql` |

## 维护规范

见 `CLAUDE.md` 的 **"数据库 Schema 维护约定（强制）"** 章节。每次新增/修改表结构时：

1. 同步更新 `sql/crm_full.sql`（全新安装用到）
2. 新建 `sql/migrations/phase<N>-<功能>.sql`（增量升级用到）
3. 测试两边都跑通

## 历史归档

`crm.sql` 是项目最初的脚本，已被 `crm_full.sql` 取代。**仅供考古用，不要用于初始化**。

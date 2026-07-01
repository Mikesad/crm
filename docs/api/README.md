# ZenCRM 接口文档索引

> 按 `CLAUDE.md` 「接口文档维护约定」要求，所有后端 HTTP 接口在此维护一份 Markdown 镜像，作为前端对接的权威参考。
> Knife4j 自动生成的 OpenAPI 文档与本文档同步；如出现差异以本文档为准。

## 文档列表

| 模块 | 文件 | 接口数 | 状态 | 最近更新 |
| :--- | :--- | :--- | :--- | :--- |
| 01 登录鉴权 | [auth.md](./auth.md) | 3 | stable | 2026-06-27 |
| 02 线索管理 | [lead.md](./lead.md) | 7 | stable | 2026-06-28 |
| 03 客户管理 | [customer.md](./customer.md) | 8 | stable | 2026-06-28 |
| 03b 客户共享 | [customer-share.md](./customer-share.md) | 3 | stable | 2026-06-27 |
| 04 联系人 | [contact.md](./contact.md) | 4 | stable | 2026-06-27 |
| 05 商机管理 | [business.md](./business.md) | 6 | stable | 2026-06-28 |
| 06 跟进记录 | [record.md](./record.md) | 5 | stable | 2026-06-28 |
| 07 产品管理 | [product.md](./product.md) | 5 | stable | 2026-06-29 |
| 07b 产品分类 | [product-category.md](./product-category.md) | 5 | stable | 2026-06-29 |
| 08 合同管理 | [contract.md](./contract.md) | 5 | stable | 2026-06-27 |
| 10 回款计划 | [receivable-plan.md](./receivable-plan.md) | 5 | stable | 2026-06-27 |
| 11 回款管理 | [receivable.md](./receivable.md) | 3 | stable | 2026-06-27 |
| 12 报表中心 | [report.md](./report.md) | 11 | stable | 2026-06-30 |
| 13 系统 - 用户 | [sys-user.md](./sys-user.md) | 8 | stable | 2026-06-29 |
| 14 系统 - 角色 | [sys-role.md](./sys-role.md) | 6 | stable | 2026-06-29 |
| 15 系统 - 菜单 | [sys-menu.md](./sys-menu.md) | 5 | stable | 2026-06-29 |
| 16 系统 - 部门 | [sys-dept.md](./sys-dept.md) | 7 | stable | 2026-06-29 |
| 数据字典 | dict.md | 待补 | planned | - |

## 公共约定

### 统一响应结构

所有接口返回 `Result<T>`：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

- `code`：`ResultCode` 枚举值，成功固定 200
- `message`：人类可读的错误描述
- `data`：业务数据，可空（无返回值接口）

### 鉴权约定

除登录 / 登出 / 公开接口外，所有接口需在请求头携带：

```
Authorization: <Sa-Token token>
```

前端通过 `src/utils/request.js` 自动附加，无需手动处理。401 由全局拦截器跳登录页。

### 业务码（ResultCode）

| 区间 | 含义 |
| :--- | :--- |
| 200 | 成功 |
| 401 | 未登录 / token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 系统异常 |
| 1001-1999 | 参数校验 |
| 2001-2999 | 用户 / 鉴权相关 |
| 3001-3999 | 业务异常 |

各模块具体业务码见各模块文档。

### 数据权限约定

业务模块（`crm_customer` / `crm_lead` / `crm_business` / `crm_contact` / `crm_record`）会自动按当前用户 `data_scope` 过滤数据，调用方无需手动拼 WHERE 条件。详见 `backend/src/main/java/com/crm/config/CrmDataPermissionHandler.java`。

> **阶段二对齐**：拦截器 MANAGED_TABLES 已对齐实际表名（`crm_opportunity` → `crm_business`，`crm_follow` → `crm_record`），修复了阶段一留下的命名不一致 bug。

### 字段命名

- 后端：Java 驼峰
- 数据库：snake_case（MyBatis-Plus `map-underscore-to-camel-case: true`）
- 前端：与后端一致（驼峰），不需要转换

### 时间格式

- `LocalDateTime` → `yyyy-MM-ddTHH:mm:ss`（ISO 8601，无时区后缀；服务端东八区生成）
- `LocalDate` → `yyyy-MM-dd`

### 金额字段

所有金额统一 `BigDecimal`，前端传字符串避免浮点精度丢失（如 `"2400000.00"`）。

## 阶段二新增权限码

阶段二在 `sql/crm_full.sql` 同步新增 / 重命名了以下权限码：

| 旧 | 新 | 说明 |
|:---|:---|:---|
| `crm:opportunity:list` | `crm:business:list` | 重命名以匹配 `crm_business` 表 |
| - | `crm:business:edit` | 商机新增/编辑/删除/阶段变更 |
| - | `crm:contact:list` / `crm:contact:edit` | 联系人模块（全新） |
| - | `crm:record:list` / `crm:record:add` | 跟进记录模块（全新） |

**已初始化数据库的迁移**：见 `sql/migrations/phase2-menu-update.sql`（在阶段二交付时同步生成）。

## 阶段三新增权限码

| 权限码 | 角色绑定 | 说明 |
|:---|:---|:---|
| `crm:product:list` | admin / director / lead / sales / finance | 产品列表,公共资源 |
| `crm:product:edit` | admin / director / sales | 产品编辑 |
| `crm:receivable:edit` | admin / finance | 回款编辑 (财务录入) |
| `crm:receivable_plan:edit` | admin / director / lead / sales | 回款计划编辑 (销售录入) |

**已初始化数据库的迁移**：见 `sql/migrations/phase3-approval-and-plan-soft-delete.sql`（crm_receivable_plan 补 5 字段 + crm_product 补 3 字段 + 4 菜单幂等插入 + 角色权限重绑;phase8 commit1 已剔除 crm_approval）。

## 阶段四新增权限码

| 权限码 | 角色绑定 | 说明 |
|:---|:---|:---|
| `crm:customer:share` | admin / director / lead / sales | 客户共享（主销售发起/撤销；只读/读写两类） |
| `crm:customer:public_pool` | admin / director / lead / sales | 公海池（查看/认领；手动回收接口额外校验 admin/director） |

**已初始化数据库的迁移**：见 `sql/migrations/phase4-customer-share-and-public-pool.sql`（2 个菜单幂等插入 + 4 个角色按需清/插；crm_customer_share 表兜底建表）。

## 阶段五 commit 1 新增权限码

| 权限码 | 角色绑定 | 说明 |
|:---|:---|:---|
| `crm:record:center` | admin / director / lead / sales | 跟进中心（顶部铃铛 + `/record/center` 三 Tab：今日 / 本周 / 我的历史） |
| `crm:lead:markDead` | admin / director / lead / sales | 标为死线索（仅线索 owner 可调，独立权限码便于不可逆业务审计） |

**已初始化数据库的迁移**：见 `sql/migrations/phase5-record.sql`（crm_record_migration_log 兜底建表 + crm_lead 兜底加 2 列 + crm_record 加 2 索引 + 2 菜单幂等插入 + 4 角色按需清/插）。

### 阶段五 commit 1 重点：跟进迁移模式 A + 死线索规则

* **跟进迁移**：线索转客户后，原线索下全部 `crm_record` 按模式 A（物理迁移）改为 `related_type='customer', related_id=新客户ID`，迁移日志写入 `crm_record_migration_log`。前端客户详情时间轴一气呵成看到从首次接触到当前的全部记录。
* **死线索规则**：`crm_lead.status=4` 后，详情页顶部红色 el-alert 警示 + 写跟进按钮灰化 + 后端 `RecordService.append()` 校验拒绝新增。死因字段 `dead_reason` 可选。
* **转客户软提示**：转客户接口不强制要求已有跟进记录；前端线索详情"转客户"按钮旁显示"⚠ 你还未跟进过，建议先写一条跟进"警示。

### 阶段五 commit 2 重点：商机详情页 + 跟进时间轴组件升级

* **商机详情页** `/business/:id`（阶段五 v2 上线）：参照 `frontend-design/phase5-business-detail.html` 原型，落地顶部 meta + 阶段流水线 + Tab + 跟进时间轴 + 跟进摘要侧栏。`GET /api/crm/business/{id}` 返回 `BusinessVO`，前端按 stage 启发式推赢率（需求分析 30 / 方案报价 50 / 商务谈判 70 / 赢单 100 / 输单 0）。
* **跟进时间轴升级 `RecordTimeline.vue`**：组件视觉对齐原型（垂直 rail + dot + 五色编码：电话-蓝 / 微信-绿 / 上门-橙 / 邮件-紫 / 阶段变更-森绿 / 系统-灰）；自动识别 `followType=系统` + `content` 含"阶段从…推进到…"渲染为阶段变更胶囊卡片；下次跟进时间高亮 + 逾期打标。客户 / 线索 / 商机 / 合同详情统一使用本组件（`relatedType` 切换）。
* **Tab 命名统一**：客户详情原"跟进时间轴" Tab 在阶段五 v2 改名为"跟进记录"，与线索 / 商机对齐。
* **本批前端改动无新增后端 HTTP 接口**：`BusinessController`、`RecordController` 端点不变，仅 UI 升级。Knife4j `@Operation` 已对齐 `business.md` / `record.md` 既有描述。

### 阶段五 commit 2 重点：报表中心

* **入口** `/report`（侧边栏"可视化"分组下，菜单类型 C），4 Tab 切换：销售漏斗+业绩 / 客户分布 / 跟进与转化率 / 回款/财务。
* **13 个接口** 全部走 `crm:report:view` 权限码，5 角色均绑定。详见 `report.md`。
* **不叠加数据权限拦截**（决策 B）：所有角色看全量；部门/人员筛选由 query 参数 `deptId` / `userId` 控制，Mapper `@InterceptorIgnore(dataPermission="true")` 接管。
* **5 分钟内存缓存**：`ReportCacheService`（TTL-based `ConcurrentHashMap`），不依赖 Caffeine/Spring Cache，hit/miss 日志在 `[ReportCache]` 前缀。
* **8 个聚合二级索引**：`crm_contract.idx_sign_date` / `crm_receivable.idx_actual_time` / `crm_business.idx_stage` + `idx_expected` / `crm_record.idx_create_time` + `idx_related` / `crm_customer.idx_industry` + `idx_last_follow`，全部走 `phase5_add_idx_if_missing` 存储过程幂等。
* **前端 UI** 走"侧边栏 + Cockpit 驾驶舱密度"混合版（参考 `frontend-design/phase5-report-variant-b-cockpit.html` v2），不用顶栏横向导航（与 Dashboard / 跟进中心保持一致）。
* **V1 简化**：KPI 同比字符串走 mock、地区分布用 industry 替代、团队 vs 全公司同值、应收 TopN 不分 series — 全部列入阶段六 TODO。

## 阶段六 commit 1 新增权限码（角色管理）

v0.3:仅"角色管理"模块（含用户/权限 2 个 tab），部门管理撤回。

| 权限码 | 角色绑定 | 说明 |
|:---|:---|:---|
| `sys:system:view` | admin / sales_director | 管理组入口（M 目录） |
| `sys:user:list` | admin / sales_director | 用户管理 Tab（C 菜单） |
| `sys:user:edit` | admin / sales_director | 新建/编辑/删除/启停用 |
| `sys:user:reset_pwd` | admin / sales_director | 重置密码 |
| `sys:user:assign_role` | admin / sales_director | 分配角色 |
| `sys:role:list` | admin / sales_director | 权限管理 Tab（角色 CRUD + 权限矩阵） |
| `sys:role:edit` | admin / sales_director | 新建/编辑/删除角色 |
| `sys:role:assign_menu` | admin / sales_director | 分配菜单（全量重绑 + 踢所有用户下线） |
| `sys:menu:list` | admin / sales_director | 菜单权限（矩阵展示用只读） |
| `sys:menu:edit` | admin / sales_director | （CRUD UI v0.3 暂不做） |

**已初始化数据库的迁移**：见 `sql/migrations/phase6-system.sql`（10 个菜单幂等插入 + admin + sales_director 共 2 角色绑定 + 老 v0.2 阶段留下的 id 34/35 部门菜单兜底清理）。

## 阶段六 commit 2 新增权限码（产品管理）

D7 v0.4 修订:产品/产品分类进 sidebar "系统设置"组;产品/产品分类 **5 角色全员可见**。

| 权限码 | 角色绑定 | 说明 |
|:---|:---|:---|
| `crm:product:category:list` | admin / director / lead / sales / finance | 产品分类(查看) |
| `crm:product:category:edit` | admin / director / lead / sales / finance | 产品分类(新建/编辑/删除) |

**D4 产品升级中度（commit 2 同批)**:`crm_product` 加 2 字段 `product_line`（套餐线:基础版/专业版/旗舰版） + `billing_cycle`（计费周期:月/季/年/一次性,仅展示）。产品本身权限码 `crm:product:list` / `crm:product:edit` 沿用阶段三,不变。

**已初始化数据库的迁移**：见 `sql/migrations/phase6-product.sql`（`phase6_product_add_col_if_missing` 存储过程幂等加 2 列 + 2 菜单幂等插入 + 5 角色全绑 + 兜底补 sales_lead 缺的产品 list 权限）。

### 阶段六 commit 2 重点

- **产品分类管理** `/product/category`：平铺表格（V1 不做树形），4 个种子分类（核心产品 / 功能模块 / 服务类 / 增值包）。VO 含 `productCount` 关联产品数（批量填充）；删除前引用校验，>0 不允许删。
- **产品库升级** `/product/list`：加 `分类` / `套餐线` / `计费周期` 3 列展示，表单加 3 字段；分类下拉走 `GET /api/crm/product/category/all` 全量接口。`productLine` 套餐线 3 档 radio-pills（基础版灰 / 专业版蓝 / 旗舰版森绿），`billingCycle` 计费周期 4 档。
- **Sidebar "系统设置"组 4 → 6 项**（D7 v0.4 修订）：用户/角色/菜单/部门 4 项 admin+总监；产品/产品分类 2 项 5 角色全员。前端 `SidebarMenu.vue` 改为 per-item `requiresRole` 过滤，整组 label 始终显示、空子项组自动隐藏。
- **本批 SQL 同步**：`crm_product_category` 表 `crm_full.sql` 全新安装路径补全 5 审计字段（id/parent_id/category_name + create_by/create_time/update_by/update_time/is_deleted）；`crm_product` 加 2 字段；`sys_menu` 新增 2 条 id 36/37；`sys_role_menu` 5 角色全绑产品分类 2 条 + 兜底补 `crm:product:list/edit` 漏绑。详见 `sql/migrations/phase6-product.sql` 注释。

### 阶段六 完整权限清单（v0.10 汇总）

阶段六累计新增 **26 条权限码**（commit 1 + commit 2 + 4 个 add 系列 + 6 个 delete 系列 + 3 个 extras 系列 + 2 个产品分类）：

| 权限码 | 菜单名 | admin | sales_director | sales_lead | sales | finance | 来源 |
|:---|:---|:---:|:---:|:---:|:---:|:---:|:---|
| `sys:system:view` | 系统设置入口 | ✅ | ✅ | | | | phase6-system.sql |
| `sys:user:list` | 用户管理 | ✅ | ✅ | | | | phase6-system.sql |
| `sys:user:edit` | 用户编辑 | ✅ | ✅ | | | | phase6-system.sql |
| `sys:user:reset_pwd` | 重置密码 | ✅ | ✅ | | | | phase6-system.sql |
| `sys:user:assign_role` | 分配角色 | ✅ | ✅ | | | | phase6-system.sql |
| `sys:role:list` | 角色管理 | ✅ | ✅ | | | | phase6-system.sql |
| `sys:role:edit` | 角色编辑 | ✅ | ✅ | | | | phase6-system.sql |
| `sys:role:assign_menu` | 分配菜单 | ✅ | ✅ | | | | phase6-system.sql |
| `sys:menu:list` | 菜单权限 | ✅ | ✅ | | | | phase6-system.sql |
| `sys:menu:edit` | 菜单编辑 | ✅ | ✅ | | | | phase6-system.sql |
| `sys:user:add` | 用户新建 | ✅ | ✅ | | | | phase6-add-extras-v2.sql |
| `sys:user:delete` | 用户删除 | ✅ | ✅ | | | | phase6-add-extras-v2.sql |
| `crm:lead:add` | 线索添加 | ✅ | ✅ | ✅ | ✅ | | phase6-add-perms.sql |
| `crm:customer:add` | 客户添加 | ✅ | ✅ | ✅ | ✅ | | phase6-add-perms.sql |
| `crm:business:add` | 商机添加 | ✅ | ✅ | ✅ | ✅ | | phase6-add-perms.sql |
| `crm:contract:add` | 合同添加 | ✅ | ✅ | ✅ | ✅ | | phase6-add-perms.sql |
| `crm:product:add` | 产品添加 | ✅ | ✅ | | ✅ | | phase6-add-perms.sql |
| `crm:receivable:add` | 回款新建 | ✅ | ✅ | | | | phase6-add-extras-v2.sql |
| `crm:lead:delete` | 线索删除 | ✅ | ✅ | | | | phase6-add-delete.sql |
| `crm:customer:delete` | 客户删除 | ✅ | ✅ | | | | phase6-add-delete.sql |
| `crm:business:delete` | 商机删除 | ✅ | ✅ | | | | phase6-add-delete.sql |
| `crm:contract:delete` | 合同删除 | ✅ | ✅ | | | | phase6-add-delete.sql |
| `crm:receivable:delete` | 回款删除 | ✅ | ✅ | | | | phase6-add-delete.sql |
| `crm:product:delete` | 产品删除 | ✅ | ✅ | | | | phase6-add-delete.sql |
| `crm:product:category:list` | 产品分类 | ✅ | ✅ | ✅ | ✅ | ✅ | phase6-product.sql |
| `crm:product:category:edit` | 产品分类编辑 | ✅ | ✅ | ✅ | ✅ | ✅ | phase6-product.sql |

**绑定规则摘要**：
- **admin + sales_director**：阶段六新增全部权限（系统设置 + add/delete + extras + 产品分类）
- **sales_lead**：只加业务 add(4 个，不含 product/add 与 delete)+ 产品分类查看/编辑
- **sales**：业务 add 全 5 个 + 产品分类查看/编辑（无 delete）
- **finance**：仅产品分类查看/编辑（其他全部没有）

**SQL 同步铁律**：所有 26 条均同步至 `crm_full.sql` 全新安装路径（id 24-51 含 commit 1/2 + add/delete/extras/category），与 3 个增量迁移脚本一致。新装 DB 直接跑 `crm_full.sql` 即得完整权限树。

### 阶段六 commit 1 v0.6/v0.7 重点（add/delete 拆分）

`phase6-add-perms.sql` / `phase6-add-delete.sql` / `phase6-add-extras-v2.sql` 三个补丁在 commit 1 完成后追加，把原本与 `*:edit` 共用 permCode 的 "添加/删除" 拆成独立 permCode：
- 原因：`role/detail.vue` 权限矩阵里 "添加" 与 "编辑" 共享一个 code 时，勾任一 checkbox 另一个视觉同步翻，UX 像 bug
- 解决：拆 `crm:*:add`（5 条）+ `crm:*:delete`（6 条）+ `sys:user:add/delete` + `crm:receivable:add`（共 14 条）
- 绑定规则：见上表（add 系列给业务操作角色，delete/extras 仅 admin + director）

### 阶段四重点：数据权限拦截器升级

`CrmDataPermissionHandler` 阶段四起对 `dataScope=5`（仅本人）的 `crm_customer` 表查询生成如下 WHERE：

```sql
owner_user_id = 当前用户
   OR id IN (SELECT customer_id FROM crm_customer_share WHERE user_id = 当前用户)
   OR is_public = 1
```

即：除"自己拥有的"外，被共享给自己的客户、公海客户也一并放行。`dataScope=1/3/4` 维持阶段三原逻辑不变。详见 `customer-share.md` 业务规则备注。

## 阶段八 commit 2 重点：报表中心 部门业绩 + Filter 真树

* **范围**：仅 Java 代码改动 + 1 个前端 Vue 改造 + 2 份文档 + 1 个 SQL 占位迁移。**0 schema 变更**。
* **8 项改造**（详见 `report.md` changelog）：
  - **C2-D1** 部门业绩 `deptName` 接 `sys_dept.dept_name` 真名
  - **C2-D2** 部门业绩接前端 `range`（去写死近 10 年）
  - **C2-D3** 部门业绩接 `ownerIds`（sales 不看别人部门）
  - **C2-D4** 拆 2 口径：`amount`/`percent`（合同业绩）+ `receivedAmount`/`receivedPercent`（实际回款），前端 chip tab 切换
  - **C2-D5** `crm_contract.status IN (1,2)` 过滤（排除审批中 / 已作废）
  - **C2-D6** `resolveOwnerIds(deptId)` 认子部门（走 `sys_dept.ancestors` 前缀匹配），影响全部 13 个接口
  - **C2-4/5** `/filter/depts` 接 `sys_dept` 真表，`/filter/users` 接 `sys_user` 真表（自动含子部门）
* **关键决策**：
  - **D-A**：拆 2 口径（合同业绩 + 实际回款）— 业务逻辑上"签合同 ≠ 钱到账"
  - **D-B**：chip tab 来回切换（沿用阶段五 4 Tab 风格）
  - **D-C**：合同 `status IN (1,2)` — 只算执行中 + 已结束
* **sql**：`sql/migrations/phase8-report.sql` 占位脚本，跑两遍幂等
* **关键 Mapper 改动**：
  - `CrmContractMapper.sumByDeptIds(...)`：crm_contract JOIN sys_user 按 dept_id 分组 SUM(total_amount)（合同业绩）
  - `CrmReceivableMapper.sumActualByDeptIds(...)`：crm_receivable JOIN crm_contract JOIN sys_user 按 dept_id 分组 SUM(actual_amount)（实际回款）
  - `SysDeptMapper.selectDescendantIds(...)`：ancestors LIKE 前缀匹配，自动含子部门
* **不做**（留给后续）：Excel 导出 / 同比实算 / monthlyStacked 拆 series / cache/clear 限 admin / 自定义时间范围 / 报表 dataScope 分层

## 更新流程

每次新增 / 修改接口，按以下步骤同步本文档：

1. 新建或更新对应模块的 `<module>.md`
2. 在本索引表追加 / 更新对应行（接口数、状态、最近更新时间）
3. 在控制器方法上加 Knife4j `@Tag` / `@Operation` 注解，与文档保持一致

# ZenCRM 接口文档索引

> 按 `CLAUDE.md` 「接口文档维护约定」要求，所有后端 HTTP 接口在此维护一份 Markdown 镜像，作为前端对接的权威参考。
> Knife4j 自动生成的 OpenAPI 文档与本文档同步；如出现差异以本文档为准。

## 文档列表

| 模块 | 文件 | 接口数 | 状态 | 最近更新 |
| :--- | :--- | :--- | :--- | :--- |
| 01 登录鉴权 | [auth.md](./auth.md) | 3 | stable | 2026-06-27 |
| 02 线索管理 | [lead.md](./lead.md) | 6 | stable | 2026-06-27 |
| 03 客户管理 | [customer.md](./customer.md) | 5 | stable | 2026-06-27 |
| 04 联系人 | [contact.md](./contact.md) | 4 | stable | 2026-06-27 |
| 05 商机管理 | [business.md](./business.md) | 6 | stable | 2026-06-27 |
| 06 跟进记录 | [record.md](./record.md) | 2 | stable | 2026-06-27 |
| 07 产品管理 | [product.md](./product.md) | 5 | stable | 2026-06-27 |
| 08 合同管理 | [contract.md](./contract.md) | 5 | stable | 2026-06-27 |
| 09 合同审批 | [approval.md](./approval.md) | 3 | stable | 2026-06-27 |
| 10 回款计划 | [receivable-plan.md](./receivable-plan.md) | 5 | stable | 2026-06-27 |
| 11 回款管理 | [receivable.md](./receivable.md) | 3 | stable | 2026-06-27 |
| 公海池 | pool.md | 待补 | planned | 阶段四 |
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
| `crm:contract:approve` | admin / director | 合同审批 (销售总监专用) |
| `crm:receivable:edit` | admin / finance | 回款编辑 (财务录入) |
| `crm:receivable_plan:edit` | admin / director / lead / sales | 回款计划编辑 (销售录入) |

**已初始化数据库的迁移**：见 `sql/migrations/phase3-approval-and-plan-soft-delete.sql`（含 crm_approval 新表 + crm_receivable_plan 补 5 字段 + 5 菜单幂等插入 + 角色权限重绑）。

## 更新流程

每次新增 / 修改接口，按以下步骤同步本文档：

1. 新建或更新对应模块的 `<module>.md`
2. 在本索引表追加 / 更新对应行（接口数、状态、最近更新时间）
3. 在控制器方法上加 Knife4j `@Tag` / `@Operation` 注解，与文档保持一致

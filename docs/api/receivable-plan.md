# 10. 回款计划接口

入口前缀：`/api/crm/receivable-plan`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:contract:list`（查询）/ `crm:receivable_plan:edit`（创建/更新/删除）
- **数据权限**：V1 `crm_receivable_plan` 无 `owner_user_id`，未入 `CrmDataPermissionHandler.MANAGED_TABLES`，靠 `@SaCheckPermission` 兜底
- **业务约束**：仅 `contract.status = 1`（执行中）的合同可录入回款计划
- **自动联动**：状态 `0 未到期` → `2 已回款` 由 `ReceivableEventListener` 监听回款事件自动维护
- **V1 用 list 替代 page**：单合同计划数通常 3~10，列表接口不分页

---

## 1.1 按合同查询回款计划

**基本信息**
- 方法：GET
- 路径：`/api/crm/receivable-plan/list`
- 权限：`crm:contract:list`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| contractId | long | **是** | 合同 ID | 1 |
| status | int | 否 | 0 未到期 / 1 催款中 / 2 已回款 | 0 |

**响应**

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "contractId": 1,
      "period": 1,
      "expectedAmount": 154400.00,
      "expectedDate": "2026-07-15",
      "status": 2,
      "statusText": "已回款",
      "remark": "首款 40%",
      "receivedAmount": 154400.00,
      "createTime": "2026-06-25T10:00:00"
    },
    {
      "id": 2,
      "contractId": 1,
      "period": 2,
      "expectedAmount": 193000.00,
      "expectedDate": "2026-09-30",
      "status": 1,
      "statusText": "催款中",
      "remark": "中期款 50%",
      "receivedAmount": 0,
      "createTime": "2026-06-25T10:00:00"
    }
  ]
}
```

> **排序**：默认按 `period ASC`。
> **receivedAmount**：该计划累计实收（由 `ReceivableEventListener` 累加写入 `crm_receivable`）。

**curl 示例**

```bash
curl -X GET 'http://localhost:8080/api/crm/receivable-plan/list?contractId=1' \
  -H 'Authorization: <token>'
```

---

## 1.2 回款计划详情

**基本信息**
- 方法：GET
- 路径：`/api/crm/receivable-plan/{id}`
- 权限：`crm:contract:list`

**请求参数（path）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 回款计划 ID |

**响应**：同 1.1 单条结构。

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1003 | 计划不存在 |
| 403 | 无 `crm:contract:list` 权限 |

---

## 1.3 批量创建回款计划

**基本信息**
- 方法：POST
- 路径：`/api/crm/receivable-plan`
- 权限：`crm:receivable_plan:edit`
- **业务约束**：合同必须 `status=1`（执行中），期数（period）不能与已有重复

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| contractId | long | 是 | 合同 ID |
| plans | array | 是 | 回款计划明细数组,至少 1 期 |

**plans 子项**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| period | int | 是 | 期数, ≥ 1 |
| expectedAmount | decimal(12,2) | 是 | 预计金额, > 0 |
| expectedDate | date | 是 | 预计回款日期 |
| remark | string | 否 | 备注,如 "首款 40%" |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**curl 示例**

```bash
curl -X POST 'http://localhost:8080/api/crm/receivable-plan' \
  -H 'Authorization: <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "contractId": 1,
    "plans": [
      {"period": 1, "expectedAmount": 154400.00, "expectedDate": "2026-07-15", "remark": "首款 40%"},
      {"period": 2, "expectedAmount": 193000.00, "expectedDate": "2026-09-30", "remark": "中期款 50%"},
      {"period": 3, "expectedAmount": 38600.00,  "expectedDate": "2027-01-30", "remark": "质保金 10%"}
    ]
  }'
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1001 | 期数重复 / 参数校验失败 |
| 1003 | 合同不存在 |
| 3001 | 合同状态非执行中,不能录入计划 |
| 403 | 无 `crm:receivable_plan:edit` 权限 |

---

## 1.4 更新回款计划

**基本信息**
- 方法：PUT
- 路径：`/api/crm/receivable-plan`
- 权限：`crm:receivable_plan:edit`
- **约束**：已回款（`status=2`）的计划不能修改

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 计划 ID |
| expectedAmount | decimal(12,2) | 否 | 预计金额 |
| expectedDate | date | 否 | 预计日期 |
| status | int | 否 | 0 未到期 / 1 催款中 / 2 已回款 |
| remark | string | 否 | 备注 |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1003 | 计划不存在 |
| 3001 | 已回款的计划不能修改 |
| 403 | 无 `crm:receivable_plan:edit` 权限 |

---

## 1.5 删除回款计划

**基本信息**
- 方法：DELETE
- 路径：`/api/crm/receivable-plan/{id}`
- 权限：`crm:receivable_plan:edit`
- 逻辑删除 `is_deleted=1`；已回款不能删

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

---

## 变更记录

| 时间 | 变更 |
|:---|:---|
| 2026-06-27 | 阶段三首次发布：5 个接口,list 替代 page(单合同计划数小) |

# 08. 合同接口

入口前缀：`/api/crm/contract`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:contract:list`（查询）/ `crm:contract:edit`（创建/更新/删除）
- **数据权限**：受 `dataScope` 拦截（`CrmDataPermissionHandler.MANAGED_TABLES` 已含 `crm_contract`）
- **状态机**：`0 审批中` → `1 执行中` → `2 已结束`；`0` → `3 已作废`（驳回）
- **核心业务**：`create()` 按明细 `sales_price = standard_price × discount / 10` 反推，校验与前端 `totalAmount` 误差 ≤ 0.01；最低折扣 < 8.5 折时 status=0 + 写 `crm_approval` 自动进入审批

---

## 1.1 分页查询合同

**基本信息**
- 方法：GET
- 路径：`/api/crm/contract/page`
- 权限：`crm:contract:list`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| pageNum / pageSize | int | 否 | 分页（最大 200） | 1 / 10 |
| keyword | string | 否 | 模糊匹配 contractNum 或 contractName | HT-2026 |
| customerId | long | 否 | 客户 ID 精确过滤 | 1 |
| status | int | 否 | 0 审批中 / 1 执行中 / 2 已结束 / 3 已作废 | 1 |

**响应**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "contractNum": "HT-20260627-123456",
        "contractName": "华东医药 CRM 一期",
        "customerId": 1,
        "customerName": "华东医药集团",
        "businessId": 22,
        "totalAmount": 386000.00,
        "startDate": "2026-07-01",
        "endDate": "2027-06-30",
        "status": 1,
        "statusText": "执行中",
        "ownerUserId": 4,
        "ownerName": "李销售",
        "createTime": "2026-06-25T14:30:00",
        "updateTime": "2026-06-27T10:00:00",
        "items": null
      }
    ],
    "total": 12,
    "current": 1,
    "size": 10
  }
}
```

> **注**：列表接口 `items` 字段为 `null`；明细仅在详情接口返回。

**curl 示例**

```bash
curl -X GET 'http://localhost:8080/api/crm/contract/page?status=1&pageNum=1&pageSize=10' \
  -H 'Authorization: <token>'
```

---

## 1.2 合同详情

**基本信息**
- 方法：GET
- 路径：`/api/crm/contract/{id}`
- 权限：`crm:contract:list`

**请求参数（path）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 合同 ID |

**响应**

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "contractNum": "HT-20260627-123456",
    "contractName": "华东医药 CRM 一期",
    "customerId": 1,
    "customerName": "华东医药集团",
    "businessId": 22,
    "totalAmount": 386000.00,
    "startDate": "2026-07-01",
    "endDate": "2027-06-30",
    "status": 1,
    "statusText": "执行中",
    "ownerUserId": 4,
    "ownerName": "李销售",
    "createTime": "2026-06-25T14:30:00",
    "updateTime": "2026-06-27T10:00:00",
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productCode": "P-CRM-001",
        "productName": "ZenCRM 企业版 (50 席位)",
        "spec": "50 用户 / 1 年",
        "unit": "套",
        "count": 1,
        "standardPrice": 128000.00,
        "salesPrice": 121600.00,
        "discount": 9.50,
        "subtotal": 121600.00
      },
      {
        "id": 2,
        "productId": 3,
        "productCode": "P-IMPL-001",
        "productName": "实施服务",
        "spec": "人天",
        "unit": "人天",
        "count": 60,
        "standardPrice": 3000.00,
        "salesPrice": 2700.00,
        "discount": 9.00,
        "subtotal": 162000.00
      }
    ]
  }
}
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 401 | 未登录 |
| 403 | 无 `crm:contract:list` 权限 |
| 1003 | 合同不存在 |

---

## 1.3 创建合同 ⭐ 核心业务

**基本信息**
- 方法：POST
- 路径：`/api/crm/contract`
- 权限：`crm:contract:edit`
- **金额防篡改**：`totalAmount` 必须等于后端按明细重算结果，误差 ≤ 0.01
- **折扣自动审批**：明细中最低折扣 < 8.5 折时 `status=0` 并自动创建 `crm_approval` 待总监审批

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| contractName | string | 否 | 合同名称，最长 100 字符 |
| customerId | long | 是 | 客户 ID |
| businessId | long | 否 | 商机 ID（从商机赢单跳转时填入） |
| totalAmount | decimal(12,2) | 是 | 合同总金额，**后端会重算校验** |
| startDate | date | 否 | 合同开始日期 |
| endDate | date | 否 | 合同结束日期 |
| items | array | 是 | 合同明细数组,至少 1 项 |

**items 子项**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| productId | long | 是 | 产品 ID（必须是 `crm_product.status=1` 上架产品） |
| count | int | 是 | 数量,≥ 1 |
| discount | decimal(4,2) | 是 | 折扣,如 9.5 折传 `9.50`,范围 0.01 ~ 10.00 |

**响应**

```json
{ "code": 200, "msg": "操作成功", "data": 1 }
```

**curl 示例（含折扣 9.5 折）**

```bash
curl -X POST 'http://localhost:8080/api/crm/contract' \
  -H 'Authorization: <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "contractName": "蓝海科技 200 席位旗舰版",
    "customerId": 2,
    "businessId": 18,
    "totalAmount": 121600.00,
    "startDate": "2026-07-01",
    "endDate": "2027-06-30",
    "items": [
      {"productId": 1, "count": 1, "discount": 9.50}
    ]
  }'
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1001 | 参数校验失败（必填/范围） |
| 1003 | 产品不存在或已下架 |
| 3001 | 合同金额与明细不符（误差 > 0.01,视为前端篡改） |
| 403 | 无 `crm:contract:edit` 权限 |

**特殊行为：折扣 < 8.5 折时**

- `crm_contract.status = 0`（审批中）
- 自动创建 `crm_approval`（status=0 待审，trigger_reason = "折扣 X.XX 折,低于 8.50 折审批线"）
- 响应 `data` 仍为合同 ID，业务无感知

---

## 1.4 更新合同

**基本信息**
- 方法：PUT
- 路径：`/api/crm/contract`
- 权限：`crm:contract:edit`
- **V1 限制**：仅允许修改 `contractName` / `startDate` / `endDate`；明细不允许修改（防止金额绕过重算）
- 状态流转由 `ApprovalService`（通过/驳回）和 `ReceivableEventListener`（全部回款完成）触发

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 合同 ID |
| contractName | string | 否 | 合同名称 |
| startDate | date | 否 | 开始日期 |
| endDate | date | 否 | 结束日期 |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1003 | 合同不存在 |
| 3001 | 已结束/已作废的合同不能修改 |
| 403 | 无 `crm:contract:edit` 权限 |

---

## 1.5 删除合同

**基本信息**
- 方法：DELETE
- 路径：`/api/crm/contract/{id}`
- 权限：`crm:contract:edit`
- **逻辑删除**：`is_deleted` 置 1，关联 `crm_contract_product` / `crm_approval` 不级联（append-only）

**请求参数（path）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 合同 ID |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1003 | 合同不存在 |
| 403 | 无 `crm:contract:edit` 权限 |

---

## 变更记录

| 时间 | 变更 |
|:---|:---|
| 2026-06-27 | 阶段三首次发布：5 个标准 CRUD + 金额重算 + 折扣审批触发 |

# 05. 商机接口

入口前缀：`/api/crm/business`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:business:list`（查询）/ `crm:business:edit`（创建/更新/删除/阶段变更）
- **数据权限**：受 `dataScope` 拦截
- **金额字段**：使用 `BigDecimal`，禁止浮点

## 1.1 分页查询商机

**基本信息**
- 方法：GET
- 路径：`/api/crm/business/page`
- 权限：`crm:business:list`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| pageNum / pageSize | int | 否 | 分页 | 1 / 10 |
| keyword | string | 否 | 模糊匹配 businessName | 部署 |
| customerId | long | 否 | 关联客户 ID | 100 |
| stage | string | 否 | 商机阶段 | 商务谈判 |

**响应**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 2001,
        "customerId": 100,
        "customerName": "华为集团",
        "businessName": "全集团 CRM 部署",
        "expectedAmount": 2400000.00,
        "expectedDealDate": "2026-08-30",
        "stage": "商务谈判",
        "ownerUserId": 4,
        "ownerName": "李明",
        "createTime": "2026-04-10T10:00:00",
        "updateTime": "2026-06-25T15:18:00"
      }
    ],
    "total": 42
  }
}
```

---

## 1.2 商机详情

**基本信息**
- 方法：GET
- 路径：`/api/crm/business/{id}`

---

## 1.3 创建商机

**基本信息**
- 方法：POST
- 路径：`/api/crm/business`
- 权限：`crm:business:edit`

**请求体（body）**

```json
{
  "customerId": 100,
  "businessName": "全集团 CRM 部署",
  "expectedAmount": 2400000.00,
  "expectedDealDate": "2026-08-30"
}
```

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| customerId | long | 是 | 客户必须存在 |
| businessName | string | 是 | 最长 100 字符 |
| expectedAmount | decimal(12,2) | 否 | 非负数，0 允许 |
| expectedDealDate | date | 否 | 预计结单日期 |

创建后默认 `stage=需求分析`、`ownerUserId=当前用户`。

---

## 1.4 更新商机

**基本信息**
- 方法：PUT
- 路径：`/api/crm/business`
- 权限：`crm:business:edit`

> **阶段字段不在此处修改**，必须走 `/stage` 端点。

---

## 1.5 删除商机

**基本信息**
- 方法：DELETE
- 路径：`/api/crm/business/{id}`
- 权限：`crm:business:edit`
- 行为：逻辑删除

---

## 1.6 商机阶段变更（核心业务）

**基本信息**
- 方法：PUT
- 路径：`/api/crm/business/{id}/stage`
- 权限：`crm:business:edit`
- **核心业务**：`@Transactional` 内两件事：
  1. 严格单向校验（见下方规则）
  2. 追加 `crm_record` 时间轴

**请求体（body）**

```json
{
  "stage": "方案报价",
  "followContent": "已发送报价单 v2，等待客户反馈"
}
```

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| stage | string | 是 | 目标阶段，见下方枚举 |
| followContent | string | 否 | 跟进内容（会写入时间轴） |

**阶段枚举**
- 需求分析
- 方案报价
- 商务谈判
- 赢单（终态）
- 输单（终态）

**严格单向规则**

| 当前阶段 | 允许的目标阶段 | 禁止的目标阶段 |
|:---|:---|:---|
| 需求分析 | 方案报价、输单 | 商务谈判、赢单（跳级） |
| 方案报价 | 商务谈判、输单 | 需求分析（回退）、赢单（跳级） |
| 商务谈判 | 赢单、输单 | 需求分析、方案报价（回退） |
| 赢单 | （终态） | 任何其他阶段 |
| 输单 | （终态） | 任何其他阶段 |

**业务码**
- 3001：阶段不合法 / 跳级 / 回退 / 终态再变更
- 1003：商机不存在

**调用示例**

```bash
curl -X PUT "http://localhost:8080/api/crm/business/2001/stage" \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"stage":"方案报价","followContent":"已发送报价单"}'
```

---

## 关联接口

- 商机时间轴：`GET /api/crm/record/timeline?relatedType=business&relatedId={id}`
- 商机关联客户：`GET /api/crm/customer/{customerId}`

## 业务规则备注

- **赢单后**：阶段三"合同与产品金额防篡改"会用商机 ID 自动跳转到"新建合同"页（前端需在响应后处理跳转逻辑）。
- **金额字段**：`expectedAmount` 仅是销售预计值，最终合同金额以 `crm_contract.total_amount` 为准。

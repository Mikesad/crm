# 11. 回款管理接口

入口前缀：`/api/crm/receivable`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:receivable:list`（查询）/ `crm:receivable:edit`（录入）
- **数据权限**：V1 暂未走 `CrmDataPermissionHandler.MANAGED_TABLES`（receivable 无 owner_user_id,TODO V2 扩展）
- **核心业务**：财务录入回款 → `ReceivableService.create()` 写库 → 同步发 `ReceivableRecordedEvent` → `@TransactionalEventListener(AFTER_COMMIT)` 异步更新 `plan.status` / `contract.status`
- **计划外回款**：`planId` 传 null 即可，不会触发 plan 状态联动

---

## 1.1 分页查询回款

**基本信息**
- 方法：GET
- 路径：`/api/crm/receivable/page`
- 权限：`crm:receivable:list`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| pageNum / pageSize | int | 否 | 分页（最大 200） | 1 / 10 |
| contractId | long | 否 | 合同 ID 过滤 | 1 |
| planId | long | 否 | 计划 ID 过滤 | 1 |
| paymentMethod | string | 否 | 支付方式过滤 | 银行转账 |
| returnDateStart | date | 否 | 回款起始日期 | 2026-07-01 |
| returnDateEnd | date | 否 | 回款结束日期 | 2026-07-31 |

**响应**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "receivableNum": "SK-20260715-123456",
        "contractId": 1,
        "contractNum": "HT-20260627-123456",
        "contractName": "华东医药 CRM 一期",
        "planId": 1,
        "planPeriod": 1,
        "planExtra": false,
        "actualAmount": 154400.00,
        "returnDate": "2026-07-15",
        "paymentMethod": "银行转账",
        "createBy": "赵财务",
        "createTime": "2026-07-15T10:30:00"
      },
      {
        "id": 2,
        "receivableNum": "SK-20260720-234567",
        "contractId": 4,
        "contractId": 4,
        "contractName": "中科云创 二期扩容",
        "planId": null,
        "planPeriod": null,
        "planExtra": true,
        "actualAmount": 10000.00,
        "returnDate": "2026-06-25",
        "paymentMethod": "银行转账",
        "createBy": "赵财务",
        "createTime": "2026-06-25T14:00:00"
      }
    ],
    "total": 2,
    "current": 1,
    "size": 10
  }
}
```

> `planExtra = true` 表示计划外回款,`planId` / `planPeriod` 为 null。

---

## 1.2 回款详情

**基本信息**
- 方法：GET
- 路径：`/api/crm/receivable/{id}`
- 权限：`crm:receivable:list`

**响应**：同 1.1 单条结构。

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1003 | 回款记录不存在 |
| 403 | 无 `crm:receivable:list` 权限 |

---

## 1.3 录入回款 ⭐ 触发 Spring Event

**基本信息**
- 方法：POST
- 路径：`/api/crm/receivable`
- 权限：`crm:receivable:edit`
- **触发事件**：提交后 `@TransactionalEventListener(AFTER_COMMIT)` 异步处理 plan/contract 状态联动

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| contractId | long | 是 | 合同 ID(必须 status=1 执行中) |
| planId | long | 否 | 对应回款计划 ID,可空(计划外回款) |
| actualAmount | decimal(12,2) | 是 | 回款金额, > 0 |
| returnDate | date | 是 | 回款日期 |
| paymentMethod | string | 否 | 银行转账/微信/支付宝/现金,默认 银行转账 |

**响应**

```json
{ "code": 200, "msg": "操作成功", "data": 1 }
```

**curl 示例（关联计划）**

```bash
curl -X POST 'http://localhost:8080/api/crm/receivable' \
  -H 'Authorization: <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "contractId": 1,
    "planId": 1,
    "actualAmount": 154400.00,
    "returnDate": "2026-07-15",
    "paymentMethod": "银行转账"
  }'
```

**curl 示例（计划外回款）**

```bash
curl -X POST 'http://localhost:8080/api/crm/receivable' \
  -H 'Authorization: <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "contractId": 4,
    "planId": null,
    "actualAmount": 10000.00,
    "returnDate": "2026-06-25",
    "paymentMethod": "银行转账"
  }'
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1001 | 参数校验失败 |
| 1003 | 合同/回款计划不存在 |
| 3001 | 合同状态非执行中 / 计划不属于该合同 |
| 403 | 无 `crm:receivable:edit` 权限 |

---

## Spring Event 联动机制

### 事件流

```
财务 POST /api/crm/receivable
  ↓
ReceivableService.create() @Transactional
  ├─ 校验合同 status=1
  ├─ insert crm_receivable
  └─ eventPublisher.publishEvent(ReceivableRecordedEvent)
                                ↓
                    [Transaction commits]
                                ↓
        @TransactionalEventListener(AFTER_COMMIT)
        ReceivableEventListener.onReceivableRecorded()
          ├─ if planId != null: SUM(plan_id 下的实收) >= 预计 → plan.status=2
          └─ if 合同下所有 plan.status=2 → contract.status=2
```

### 容错

监听器所有异常 `try-catch` 兜底,仅 `log.error`,不抛。
- 即使 plan/contract 状态更新失败,receivable 记录已落库,财务数据不丢
- V2 阶段考虑加对账 Job 修复丢的事件

### 计划外回款

`planId = null` 时：
- receivable 正常落库
- 监听器跳过 plan 联动
- 不影响 plan / contract 状态机

---

## 变更记录

| 时间 | 变更 |
|:---|:---|
| 2026-06-27 | 阶段三首次发布：3 个接口 + Spring Event 联动 |

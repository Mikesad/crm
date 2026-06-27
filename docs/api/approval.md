# 09. 合同审批接口

入口前缀：`/api/crm/approval`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:contract:approve`（仅销售总监 / admin 持有该权限）
- **数据权限**：V1 `crm_approval` 无 `owner_user_id` 字段，未入 `CrmDataPermissionHandler.MANAGED_TABLES`，完全靠 `@SaCheckPermission` 兜底
- **业务流**：`ContractService.create()` 检测折扣 < 8.5 折 → 自动写 `crm_approval(status=0)` → 总监 `approve/reject` → 联动 `crm_contract.status`

---

## 1.1 分页查询审批单

**基本信息**
- 方法：GET
- 路径：`/api/crm/approval/page`
- 权限：`crm:contract:approve`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| pageNum / pageSize | int | 否 | 分页（最大 200） | 1 / 10 |
| status | int | 否 | 0 待审 / 1 通过 / 2 驳回 / 3 撤回;null 全部 | 0 |
| contractId | long | 否 | 合同 ID | 1 |
| applicantId | long | 否 | 申请人 ID(销售) | 4 |
| approverId | long | 否 | 审批人 ID(总监) | 2 |

**排序**：默认 `status ASC, create_time DESC`（待审排前面）。

**响应**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "contractId": 2,
        "contractNum": "HT-20260627-789012",
        "contractName": "蓝海科技 200 席位旗舰版",
        "applicantId": 5,
        "applicantName": "陈销售",
        "approverId": null,
        "approverName": null,
        "status": 0,
        "statusText": "待审",
        "triggerReason": "折扣 7.50 折,低于 8.50 折审批线",
        "comment": null,
        "createTime": "2026-06-26T09:15:00",
        "finishTime": null,
        "contractTotalAmount": 960000.00,
        "minDiscount": 7.50
      }
    ],
    "total": 3,
    "current": 1,
    "size": 10
  }
}
```

**curl 示例**

```bash
curl -X GET 'http://localhost:8080/api/crm/approval/page?status=0&pageNum=1&pageSize=10' \
  -H 'Authorization: <token>'
```

---

## 1.2 审批通过

**基本信息**
- 方法：POST
- 路径：`/api/crm/approval/approve`
- 权限：`crm:contract:approve`

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 审批单 ID |
| comment | string | 否 | 审批意见,选填,最长 500 字符 |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1001 | 参数校验失败 |
| 1003 | 审批单不存在 / 关联合同不存在 |
| 3001 | 该审批单已处理,不能重复操作 |
| 403 | 无 `crm:contract:approve` 权限 |

**副作用**
- `crm_approval.status = 1`（通过）
- `crm_approval.approver_id = 当前用户`
- `crm_approval.finish_time = now`
- `crm_contract.status = 1`（执行中）

---

## 1.3 审批驳回

**基本信息**
- 方法：POST
- 路径：`/api/crm/approval/reject`
- 权限：`crm:contract:approve`
- **驳回原因必填**

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 审批单 ID |
| comment | string | **是** | 驳回原因,最长 500 字符 |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1001 | 参数校验失败（驳回原因不能为空） |
| 1003 | 审批单不存在 / 关联合同不存在 |
| 3001 | 该审批单已处理,不能重复操作 |
| 403 | 无 `crm:contract:approve` 权限 |

**副作用**
- `crm_approval.status = 2`（驳回）
- `crm_approval.approver_id = 当前用户`
- `crm_approval.finish_time = now`
- `crm_contract.status = 3`（已作废）

---

## 变更记录

| 时间 | 变更 |
|:---|:---|
| 2026-06-27 | 阶段三首次发布：3 个接口(分页/通过/驳回),完整审批流 V1 |

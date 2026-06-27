# 03b. 客户共享接口

入口前缀：`/api/customer/share`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:customer:share`
- **业务约束**：
  - **发起人**：必须是 customer 的 `owner_user_id`(主销售),私海客户才能被共享,公海/他人私海不能共享
  - **被共享人**：不能是 customer 的 owner 本身(自己无需共享自己)
  - **权限类型**：`auth_type=1 只读 / 2 读写`
    - 只读共享人:能看 customer 详情、时间轴、联系人/商机列表;**不能**改 customer 字段、不能新增跟进记录
    - 读写共享人:除只读权限外,可调用 `crm:customer:edit` 与 `crm:record:add`(由 Service 层做 `auth_type=2` 二次校验)
  - **数据权限**:dataScope=5 用户查询客户时,`CrmDataPermissionHandler` 自动把 `crm_customer_share` 子查询 OR 入 WHERE 条件

---

## 2.1 发起共享

**基本信息**
- 方法:POST
- 路径:`/api/customer/share`
- 权限:`crm:customer:share`
- 行为:`INSERT INTO crm_customer_share`;若已存在相同 (customer_id, user_id) 记录,更新 `auth_type` 与 `create_time`(重新共享)

**请求体(body)**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| customerId | long | 是 | 客户 ID,必须是当前用户的私海客户 | 1001 |
| userId | long | 是 | 被共享人用户 ID | 5 |
| authType | int | 是 | 1 只读 / 2 读写 | 2 |

**响应**

```json
{
  "code": 200,
  "data": {
    "id": 17,
    "customerId": 1001,
    "userId": 5,
    "authType": 2,
    "createBy": "sales_li",
    "createTime": "2026-06-27T10:00:00"
  }
}
```

**业务码**

| code | 含义 |
|:---|:---|
| 200 | 成功 |
| 1003 | 客户不存在 |
| 1005 | 当前用户不是该客户的 owner,无权共享 |
| 1006 | 客户在公海池,不能共享(请先认领) |

---

## 2.2 撤销共享

**基本信息**
- 方法:DELETE
- 路径:`/api/customer/share/{id}`
- 权限:`crm:customer:share`
- 行为:仅 customer 的 owner 可撤销;物理删除该条 `crm_customer_share` 记录

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | crm_customer_share 主键 ID |

**响应**

```json
{ "code": 200, "data": null, "message": "撤销成功" }
```

**业务码**

| code | 含义 |
|:---|:---|
| 200 | 成功 |
| 1003 | 共享记录不存在 |
| 1005 | 当前用户不是该客户的 owner,无权撤销 |

---

## 2.3 查看某客户的共享名单

**基本信息**
- 方法:GET
- 路径:`/api/customer/share/list`
- 权限:`crm:customer:share`
- 行为:返回该客户的所有共享记录(含被共享人昵称/部门)

**请求参数(query)**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| customerId | long | 是 | 客户 ID | 1001 |

**响应**

```json
{
  "code": 200,
  "data": [
    {
      "id": 17,
      "customerId": 1001,
      "userId": 5,
      "userNickname": "陈销售",
      "userDeptName": "华南销售部",
      "authType": 2,
      "authTypeText": "读写",
      "createBy": "sales_li",
      "createTime": "2026-06-27T10:00:00"
    }
  ]
}
```

**业务码**

| code | 含义 |
|:---|:---|
| 200 | 成功 |
| 1003 | 客户不存在 |
| 1005 | 当前用户不是该客户的 owner,无权查看 |

---

## 业务规则备注

- **共享 ≠ 转交**:共享不改变 `owner_user_id`,客户始终归属主销售;只有【公海认领】会改变 owner。
- **撤销共享后**:被共享人立即无法再查到该客户(下次查询时拦截器子查询不命中)。该过程无需重启服务,也不需要 token 失效。
- **只读 vs 读写**:`authType=1` 的用户调用 `PUT /api/crm/customer` 或 `POST /api/crm/record` 时,Service 层会返回 `403 - 仅可读,不能编辑`。注意拦截在 Service 层,不在 `@SaCheckPermission`(因为读和写共用 `crm:customer:share` 权限码)。
- **删除/转交场景**:若主销售把客户删了(逻辑删除),共享记录仍存在但被拦截器屏蔽;若主销售把客户转入公海(走 `POST /api/customer/public-pool/transfer`,阶段四暂不实现),共享记录应同步清掉(V2 扩展)。

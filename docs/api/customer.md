# 03. 客户接口

入口前缀：`/api/crm/customer`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:customer:list`（查询）/ `crm:customer:edit`（创建/更新/删除）
- **数据权限**：受 `dataScope` 拦截
- **公海池**：`isPublic=1` 时附加 `owner_user_id IS NULL` 条件

## 1.1 分页查询客户

**基本信息**
- 方法：GET
- 路径：`/api/crm/customer/page`
- 权限：`crm:customer:list`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| pageNum / pageSize | int | 否 | 分页 | 1 / 10 |
| keyword | string | 否 | 模糊匹配 customerName | 华为 |
| level | string | 否 | A / B / C | A |
| industry | string | 否 | 行业精确匹配 | 通信 |
| isPublic | int | 否 | 0/null 私海 / 1 公海 | 1 |

**响应**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 100,
        "customerName": "华为集团",
        "industry": "通信",
        "level": "A",
        "levelText": "重要客户",
        "ownerUserId": 4,
        "ownerName": "李明",
        "isPublic": 0,
        "lastFollowTime": "2026-06-22T14:23:00",
        "createTime": "2026-03-15T10:00:00"
      }
    ],
    "total": 156,
    "current": 1,
    "size": 10
  }
}
```

---

## 1.2 客户详情

**基本信息**
- 方法：GET
- 路径：`/api/crm/customer/{id}`
- 权限：`crm:customer:list`

---

## 1.3 创建客户

**基本信息**
- 方法：POST
- 路径：`/api/crm/customer`
- 权限：`crm:customer:edit`

**请求体（body）**

```json
{
  "customerName": "小米科技",
  "industry": "智能硬件",
  "level": "B"
}
```

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| customerName | string | 是 | - | 最长 100 字符 |
| industry | string | 否 | - | 最长 50 字符 |
| level | string | 否 | C | A 重要 / B 普通 / C 意向 |

创建后 `ownerUserId=当前用户`、`isPublic=0`。

---

## 1.4 更新客户

**基本信息**
- 方法：PUT
- 路径：`/api/crm/customer`
- 权限：`crm:customer:edit`

**请求体（body）**

```json
{ "id": 100, "customerName": "华为集团 (更名)", "level": "A" }
```

---

## 1.5 删除客户

**基本信息**
- 方法：DELETE
- 路径：`/api/crm/customer/{id}`
- 权限：`crm:customer:edit`
- 行为：逻辑删除

> **注意**：删除客户不会级联删除联系人/商机/跟进记录，由数据权限拦截器屏蔽其访问即可。

---

## 关联接口

- 客户下的联系人列表：`GET /api/crm/contact/list?customerId={id}`
- 客户下的商机列表：`GET /api/crm/business/page?customerId={id}`
- 客户跟进时间轴：`GET /api/crm/record/timeline?relatedType=customer&relatedId={id}`

## 业务规则备注

- **公海池**：见下方 1.6~1.8 节。自动回收由 `@Scheduled` 每天凌晨 2 点触发；手动触发支持秒级阈值参数（开发期联调用）。
- **公海认领**：从公海池把客户"捞"到自己名下，强制写一条 `crm_record` 跟进记录 + 更新 `last_follow_time = now`。
- **数据权限**：阶段四起，dataScope=5 用户的客户列表同时放行 crm_customer_share 命中与公海客户；具体见 [customer-share.md](./customer-share.md) 与 `CrmDataPermissionHandler` 源码。

---

## 1.6 手动触发公海回收

> 该接口与凌晨 2 点的 `@Scheduled` 任务共用同一个 Service 方法，规则一致；区别是支持秒级参数（联调友好），并立即返回扫描/回收明细。

**基本信息**
- 方法：POST
- 路径：`/api/customer/public-pool/recycle`
- 权限：`crm:customer:public_pool` + 仅限 `admin` / `sales_director` 角色（Service 层强制二次校验）
- 用途：开发/演示期手动跑回收；运维期可用于临时重跑

**请求体（body，可空）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| thresholdSeconds | long | 否 | 读 yml `crm.customer.public-pool-days × 86400` | 阈值秒数,范围 1~7,776,000(1秒~90天) |
| limit | int | 否 | 1000 | 本次最多回收 N 条,防误伤 |
| dryRun | boolean | 否 | false | true 时只统计不真回收 |

**响应**

```json
{
  "code": 200,
  "data": {
    "thresholdSeconds": 10,
    "limit": 1000,
    "dryRun": false,
    "scanned": 5,
    "recycled": 3,
    "durationMs": 42,
    "details": [
      { "customerId": 1001, "customerName": "ACME", "ownerUserId": 12, "lastFollowTime": "2026-06-10T08:00:00" }
    ]
  }
}
```

**业务码**

| code | 含义 |
|:---|:---|
| 200 | 成功 |
| 401 | 未登录 |
| 403 | 角色不在 admin/director 范围内,或权限码缺失 |

**curl 示例**

```bash
curl -X POST http://localhost:8080/api/customer/public-pool/recycle \
  -H "Authorization: ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"thresholdSeconds": 10, "limit": 100, "dryRun": false}'
```

---

## 1.7 公海认领

> 把公海池里的客户"捞"到自己名下。需要 customer 当前是 `is_public=1` 且 `owner_user_id IS NULL`；非公海客户(私海)不能被认领。

**基本信息**
- 方法：POST
- 路径：`/api/customer/public-pool/claim/{id}`
- 权限：`crm:customer:public_pool`
- 副作用：① `owner_user_id = 当前用户`；② `is_public = 0`；③ 强制追加一条 `crm_record` 跟进记录（内容："从公海池认领客户"）；④ `last_follow_time = now`

**响应**

```json
{ "code": 200, "data": null, "message": "认领成功" }
```

**业务码**

| code | 含义 |
|:---|:---|
| 200 | 成功 |
| 1003 | 客户不存在 |
| 1004 | 该客户不在公海池(已是私海/已被认领) |
| 500 | 系统异常 |

**curl 示例**

```bash
curl -X POST http://localhost:8080/api/customer/public-pool/claim/1001 \
  -H "Authorization: ${TOKEN}"
```

---

## 1.8 公海池分页查询（前端 PublicPool 页面使用）

> 与 1.1 同一接口,前端传 `isPublic=1` 即可,见 1.1 节。
>
> 阶段四起,后台拦截器在 dataScope=5 用户调用时,会同时放行"owner 是自己 OR 共享表命中 OR 公海"的所有客户,所以前端无需额外区分。

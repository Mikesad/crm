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

- **公海池**（阶段四实现）：阶段二暂不实现自动回收，仅支持手动查询公海。定时任务在阶段四加入 `@EnableScheduling` 调度。
- **公海认领**：阶段二不实现，阶段四在客户共享表上扩展。

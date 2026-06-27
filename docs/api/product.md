# 06. 产品接口

入口前缀：`/api/crm/product`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:product:list`（查询）/ `crm:product:edit`（创建/更新/删除）
- **数据权限**：**无**（产品为公共资源，无 `owner_user_id`，不进入 `CrmDataPermissionHandler.MANAGED_TABLES`）
- **逻辑删除**：删除即更新 `is_deleted=1`

---

## 1.1 分页查询产品

**基本信息**
- 方法：GET
- 路径：`/api/crm/product/page`
- 权限：`crm:product:list`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| pageNum / pageSize | int | 否 | 分页（最大 200） | 1 / 10 |
| keyword | string | 否 | 模糊匹配 productCode 或 productName | CRM |
| categoryId | long | 否 | 产品分类 ID 精确匹配 | 1 |
| status | int | 否 | 0 下架 / 1 上架 | 1 |

**响应**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "categoryId": 1,
        "productCode": "P-CRM-001",
        "productName": "ZenCRM 企业版 (50 席位)",
        "spec": "50 用户 / 1 年",
        "price": 128000.00,
        "unit": "套",
        "status": 1,
        "statusText": "上架",
        "createTime": "2026-06-25T10:00:00"
      }
    ],
    "total": 5,
    "current": 1,
    "size": 10
  }
}
```

**curl 示例**

```bash
curl -X GET 'http://localhost:8080/api/crm/product/page?keyword=CRM&status=1&pageNum=1&pageSize=10' \
  -H 'Authorization: <token>'
```

---

## 1.2 产品详情

**基本信息**
- 方法：GET
- 路径：`/api/crm/product/{id}`
- 权限：`crm:product:list`

**请求参数（path）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| id | long | 是 | 产品 ID | 1 |

**响应**

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "categoryId": 1,
    "productCode": "P-CRM-001",
    "productName": "ZenCRM 企业版 (50 席位)",
    "spec": "50 用户 / 1 年",
    "price": 128000.00,
    "unit": "套",
    "status": 1,
    "statusText": "上架",
    "createTime": "2026-06-25T10:00:00"
  }
}
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 401 | 未登录 |
| 403 | 无 `crm:product:list` 权限 |
| 1003 | 产品不存在 |

---

## 1.3 创建产品

**基本信息**
- 方法：POST
- 路径：`/api/crm/product`
- 权限：`crm:product:edit`

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| categoryId | long | 否 | 产品分类 ID | 1 |
| productCode | string | 是 | 产品编码,全局唯一,最长 50 字符 | P-CRM-001 |
| productName | string | 是 | 产品名称,最长 100 字符 | ZenCRM 企业版 (50 席位) |
| spec | string | 否 | 规格型号,最长 100 字符 | 50 用户 / 1 年 |
| price | decimal(12,2) | 是 | 标准售价, ≥ 0 | 128000.00 |
| unit | string | 否 | 单位,默认 "个" | 套 |
| status | int | 否 | 0 下架 / 1 上架,默认 1 | 1 |

**响应**

```json
{ "code": 200, "msg": "操作成功", "data": 1 }
```

**curl 示例**

```bash
curl -X POST 'http://localhost:8080/api/crm/product' \
  -H 'Authorization: <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "categoryId": 1,
    "productCode": "P-CRM-001",
    "productName": "ZenCRM 企业版 (50 席位)",
    "spec": "50 用户 / 1 年",
    "price": 128000.00,
    "unit": "套",
    "status": 1
  }'
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1001 | 参数校验失败 (code 名称/价格 等校验) |
| 1002 | 产品编码已存在 |
| 403 | 无 `crm:product:edit` 权限 |

---

## 1.4 更新产品

**基本信息**
- 方法：PUT
- 路径：`/api/crm/product`
- 权限：`crm:product:edit`

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 产品 ID |
| 其他字段 | - | 否 | 任意子集,只更新非空字段 |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1001 | 参数校验失败 |
| 1002 | 编码变更为已存在的编码 |
| 1003 | 产品不存在 |
| 403 | 无 `crm:product:edit` 权限 |

---

## 1.5 删除产品

**基本信息**
- 方法：DELETE
- 路径：`/api/crm/product/{id}`
- 权限：`crm:product:edit`
- **逻辑删除**：`is_deleted` 置 1,DB 行保留

**请求参数（path）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 产品 ID |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1003 | 产品不存在 |
| 403 | 无 `crm:product:edit` 权限 |

---

## 变更记录

| 时间 | 变更 |
|:---|:---|
| 2026-06-27 | 阶段三首次发布,5 个标准 CRUD 接口 |

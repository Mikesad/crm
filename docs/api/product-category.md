# 06. 产品分类接口

入口前缀：`/api/crm/product/category`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:product:category:list`（查询）/ `crm:product:category:edit`（创建/更新/删除）
- **数据权限**：**无**（产品分类为公共资源，无 `owner_user_id`，不进入 `CrmDataPermissionHandler.MANAGED_TABLES`）
- **逻辑删除**：删除即更新 `is_deleted=1`
- **可见角色**（D7 v0.4）：**5 角色全员**（admin / sales_director / sales_lead / sales / finance）
- **D5 决策**：V1 仅一级分类，`parentId=0` 即顶级；不做树形

> 阶段六 commit 2 新增（2026-06-29）。补齐原有 `crm_product_category` 表的 entity/Service/Controller 链路。

---

## 2.1 分页查询产品分类

**基本信息**
- 方法：GET
- 路径：`/api/crm/product/category/page`
- 权限：`crm:product:category:list`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| pageNum / pageSize | int | 否 | 分页（最大 200） | 1 / 10 |
| keyword | string | 否 | 分类名称模糊匹配 | 核心 |

**响应**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "parentId": 0,
        "categoryName": "核心产品",
        "productCount": 3,
        "createBy": "admin",
        "createTime": "2026-01-15T09:00:00",
        "updateBy": "admin",
        "updateTime": "2026-06-29T10:00:00"
      }
    ],
    "total": 4,
    "current": 1,
    "size": 10
  }
}
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 401 | 未登录 |
| 403 | 无 `crm:product:category:list` 权限 |

---

## 2.2 全量查询产品分类

**基本信息**
- 方法：GET
- 路径：`/api/crm/product/category/all`
- 权限：`crm:product:category:list`
- **用途**：产品表单下拉 / 合同新建时产品归类选择

**请求参数**：无

**响应**

```json
{
  "code": 200,
  "data": [
    { "id": 1, "parentId": 0, "categoryName": "核心产品", "createTime": "2026-01-15T09:00:00" },
    { "id": 2, "parentId": 0, "categoryName": "功能模块", "createTime": "2026-01-15T09:05:00" },
    { "id": 3, "parentId": 0, "categoryName": "服务类",   "createTime": "2026-01-15T09:10:00" },
    { "id": 4, "parentId": 0, "categoryName": "增值包",   "createTime": "2026-01-15T09:15:00" }
  ]
}
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 401 | 未登录 |
| 403 | 无 `crm:product:category:list` 权限 |

---

## 2.3 创建产品分类

**基本信息**
- 方法：POST
- 路径：`/api/crm/product/category`
- 权限：`crm:product:category:edit`

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| parentId | long | 否 | 父分类 ID（V1 固定 0,即顶级）,默认 0 | 0 |
| categoryName | string | 是 | 分类名称,2-50 字符,同级不可重复 | 高级支持服务 |

**响应**

```json
{ "code": 200, "msg": "操作成功", "data": 5 }
```

**curl 示例**

```bash
curl -X POST 'http://localhost:8080/api/crm/product/category' \
  -H 'Authorization: <token>' \
  -H 'Content-Type: application/json' \
  -d '{ "parentId": 0, "categoryName": "高级支持服务" }'
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1001 | 参数校验失败(名称为空/长度不合规) |
| 1002 | 同级分类下已存在同名分类 |
| 403 | 无 `crm:product:category:edit` 权限 |

---

## 2.4 更新产品分类

**基本信息**
- 方法：PUT
- 路径：`/api/crm/product/category`
- 权限：`crm:product:category:edit`

**请求参数（body）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 分类 ID |
| parentId | long | 否 | 父分类 ID（V1 固定 0） |
| categoryName | string | 否 | 分类名称,变更时校验同级重名 |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1001 | 参数校验失败 |
| 1002 | 名称变更为已存在的同级分类名 |
| 1003 | 产品分类不存在 |
| 403 | 无 `crm:product:category:edit` 权限 |

---

## 2.5 删除产品分类

**基本信息**
- 方法：DELETE
- 路径：`/api/crm/product/category/{id}`
- 权限：`crm:product:category:edit`
- **逻辑删除**：`is_deleted` 置 1,DB 行保留
- **引用校验**：若 `crm_product.category_id` 引用数 > 0，返回 1002 DATA_EXISTS，提示先迁移产品

**请求参数（path）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 分类 ID |

**响应**

```json
{ "code": 200, "msg": "操作成功" }
```

**业务码**

| 码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 1002 | 该分类下还有 N 个产品,无法删除 |
| 1003 | 产品分类不存在 |
| 403 | 无 `crm:product:category:edit` 权限 |

---

## 变更记录

| 时间 | 变更 |
|:---|:---|
| 2026-06-29 | 阶段六 commit 2 首次发布,5 个标准 CRUD 接口,补齐 entity/Service/Controller 链路 |

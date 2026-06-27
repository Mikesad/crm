# 04. 联系人接口

入口前缀：`/api/crm/contact`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:contact:list`（查询）/ `crm:contact:edit`（创建/更新/删除）
- **数据权限**：无（按 `customerId` 关联后由客户级 dataScope 间接约束）

## 1.1 按客户查询联系人列表

**基本信息**
- 方法：GET
- 路径：`/api/crm/contact/list`
- 权限：`crm:contact:list`
- 注：联系人查询不分页，依赖 `customerId` 强约束

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| customerId | long | 是 | 客户 ID |
| keyword | string | 否 | 模糊匹配 contactName / phone |

**响应**

```json
{
  "code": 200,
  "data": [
    {
      "id": 501,
      "customerId": 100,
      "contactName": "张磊",
      "post": "采购总监",
      "phone": "13800138000",
      "isMaster": 1,
      "decisionWeight": 1,
      "decisionWeightText": "核心决策者",
      "createTime": "2026-03-15T10:00:00"
    }
  ]
}
```

主联系人排前，按创建时间倒序。

---

## 1.2 创建联系人

**基本信息**
- 方法：POST
- 路径：`/api/crm/contact`
- 权限：`crm:contact:edit`

**请求体（body）**

```json
{
  "customerId": 100,
  "contactName": "王芳",
  "post": "CTO",
  "phone": "13900139000",
  "isMaster": 0,
  "decisionWeight": 1
}
```

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| customerId | long | 是 | - | 客户必须存在 |
| contactName | string | 是 | - | 最长 30 字符 |
| post | string | 否 | - | 最长 50 字符 |
| phone | string | 否 | - | 最长 20 字符 |
| isMaster | int | 否 | 0 | 0 否 / 1 是 |
| decisionWeight | int | 否 | 3 | 1 核心 / 2 弱影响 / 3 普通 |

---

## 1.3 更新联系人

**基本信息**
- 方法：PUT
- 路径：`/api/crm/contact`
- 权限：`crm:contact:edit`

**请求体（body）**

```json
{ "id": 501, "post": "副总裁", "decisionWeight": 1 }
```

---

## 1.4 删除联系人

**基本信息**
- 方法：DELETE
- 路径：`/api/crm/contact/{id}`
- 权限：`crm:contact:edit`
- 行为：逻辑删除

## 业务规则备注

- **主联系人**：`isMaster=1` 标识客户的主对接人。一个客户允许多个主联系人，但前端通常约定为 1 个。
- **决策权重**：用于商机阶段推进时识别关键决策者，不影响流程。

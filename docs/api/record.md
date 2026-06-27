# 06. 跟进记录接口

入口前缀：`/api/crm/record`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:record:list`（查询）/ `crm:record:add`（新增）
- **数据权限**：无（按 `relatedId` 关联后由主体级 dataScope 间接约束）
- **特殊说明**：跟进记录是 append-only，**无 update / delete 接口**，无 `is_deleted` 字段，符合 CRM 行业惯例（审计追溯）

## 1.1 拉取时间轴

**基本信息**
- 方法：GET
- 路径：`/api/crm/record/timeline`
- 权限：`crm:record:list`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| relatedType | string | 是 | lead / customer / business |
| relatedId | long | 是 | 关联主体 ID |

**响应**

```json
{
  "code": 200,
  "data": [
    {
      "id": 9001,
      "relatedType": "customer",
      "relatedId": 100,
      "content": "电话沟通 25 分钟，客户对部署周期有疑问",
      "followType": "电话",
      "nextFollowTime": "2026-07-01T10:00:00",
      "createBy": "李明",
      "createTime": "2026-06-22T14:23:00"
    },
    {
      "id": 9002,
      "relatedType": "customer",
      "relatedId": 100,
      "content": "线索已转化为客户「北京星辰科技有限公司」",
      "followType": "系统",
      "createBy": "李明",
      "createTime": "2026-06-22T14:25:00"
    }
  ]
}
```

按 `createTime` 倒序。系统自动埋点（followType=系统）也会出现在时间轴中。

---

## 1.2 新增跟进

**基本信息**
- 方法：POST
- 路径：`/api/crm/record`
- 权限：`crm:record:add`

**请求体（body）**

```json
{
  "relatedType": "customer",
  "relatedId": 100,
  "content": "客户确认方案可行，预约 7/1 上门签约",
  "followType": "电话",
  "nextFollowTime": "2026-07-01T10:00:00"
}
```

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| relatedType | string | 是 | - | lead / customer / business |
| relatedId | long | 是 | - | 关联主体 ID |
| content | string | 是 | - | 跟进内容 |
| followType | string | 否 | 电话 | 电话 / 微信 / 上门拜访 / 邮件 |
| nextFollowTime | datetime | 否 | - | 下次跟进时间 |

**业务码**
- 3001：relatedType 非法

---

## 系统自动埋点

下列业务变更会自动追加 `crm_record`（`followType=系统`），无需手动调新增接口：

| 触发动作 | 接口 | relatedType | relatedId |
|:---|:---|:---|:---|
| 线索转客户 | `POST /api/crm/lead/{id}/convert` | lead | 线索 ID |
| 商机阶段变更 | `PUT /api/crm/business/{id}/stage` | business | 商机 ID |

**示例：线索转客户写入的时间轴记录**

```json
{
  "id": 9999,
  "relatedType": "lead",
  "relatedId": 12345,
  "content": "线索已转化为客户「北京星辰科技有限公司」",
  "followType": "系统",
  "createBy": "李明",
  "createTime": "2026-06-27T14:25:00"
}
```

---

## 业务规则备注

- **不可修改 / 不可删除**：CRM 行业惯例，保留全部审计痕迹。如需"删除跟进"，应在 UI 上做软删除标记（增加字段），不在后端做硬删除。
- **跟进人来源**：`createBy` 优先取 `tokenSession.NICKNAME`（如"李明"），回退到 `USERNAME`。

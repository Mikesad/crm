# 06. 跟进记录接口

入口前缀：`/api/crm/record`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:record:list`（查询）/ `crm:record:add`（新增）/ `crm:record:center`（跟进中心）
- **数据权限**：无（按 `relatedId` 关联后由主体级 dataScope 间接约束）
- **特殊说明**：跟进记录是 append-only，**无 update / delete 接口**，无 `is_deleted` 字段，符合 CRM 行业惯例（审计追溯）
- **阶段五升级**：`relatedType` 枚举从 `lead/customer/business` 扩展为 `lead/customer/business/contract`

## 1.1 拉取时间轴

**基本信息**
- 方法：GET
- 路径：`/api/crm/record/timeline`
- 权限：`crm:record:list`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| relatedType | string | 是 | lead / customer / business / contract |
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
| relatedType | string | 是 | - | lead / customer / business / contract |
| relatedId | long | 是 | - | 关联主体 ID |
| content | string | 是 | - | 跟进内容 |
| followType | string | 否 | 电话 | 电话 / 微信 / 上门拜访 / 邮件 |
| nextFollowTime | datetime | 否 | - | 下次跟进时间 |

**业务码**
- 3001：relatedType 非法
- 3001：线索不存在
- 3001：已死线索不可继续跟进（status=4）

---

## 1.3 待办数量统计（顶部铃铛用）

**基本信息**
- 方法：GET
- 路径：`/api/crm/record/todo/count`
- 权限：`crm:record:center`
- **说明**：按 `next_follow_time` 范围统计 6 个字段，**不过滤 owner**（铃铛展示全员数字）；前端 store 内 60 秒节流 + 5 分钟轮询

**请求参数（query）**：无

**响应**

```json
{
  "code": 200,
  "data": {
    "today": 12,
    "week": 35,
    "overdue": 6,
    "total": 48,
    "monthWritten": 28,
    "byType": {
      "lead": 8,
      "customer": 14,
      "business": 11,
      "contract": 2
    }
  }
}
```

| 字段 | 含义 |
|:---|:---|
| today | next_follow_time 在今日 00:00 ~ 23:59 内的条数 |
| week | next_follow_time 在今日 ~ 未来 7 天内的条数（含今日） |
| overdue | next_follow_time < NOW() 的条数（已过期） |
| total | next_follow_time 不为空的总条数 |
| monthWritten | 当前用户本月（自月初 00:00 起）已写入的跟进条数 |
| byType | **嵌套 Map**：`{ today: {lead, customer, business, contract}, week: {lead, customer, business, contract} }`。前端根据当前 Tab 切片取对应范围的分组计数，用于实体类型过滤栏 chip 数字（无需切换 Tab 时重新请求） |

---

## 1.4 待办列表（跟进中心"今日"/"本周" Tab）

**基本信息**
- 方法：GET
- 路径：`/api/crm/record/todo/list`
- 权限：`crm:record:center`

**请求参数（query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| range | string | 否 | today | today / week |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 20 | 每页条数 |

**响应**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "recordId": 9001,
        "relatedType": "business",
        "relatedId": 200,
        "subjectName": "蓝海科技 · 智能仓储 SaaS 采购",
        "subjectStatusText": "方案报价",
        "subjectAmount": "328000.00",
        "content": "客户认可 9 折方案",
        "followType": "上门拜访",
        "nextFollowTime": "2026-06-29T10:00:00",
        "createBy": "李雪",
        "createTime": "2026-06-26T10:42:00",
        "daysUntilNext": 1,
        "overdue": false,
        "leadStatus": null
      }
    ],
    "total": 35
  }
}
```

**排序约定（后端硬编码）**：逾期优先 → 按 `next_follow_time` 升序。前端如需切换排序（升降序、按创建时间）目前在客户端内存中处理（`filteredRows` computed 内 sort），无需重新请求。

**卡片视觉约定**：
- `overdue = true` → 卡片左侧红边 + 顶右"逾期"标签（仅此一种标红状态）
- `leadStatus = 4` → 卡片灰化（死线索）
- 其他情况保持普通边框

**前端根据 `overdue` 红边卡片、`leadStatus=4` 灰化卡片。

---

## 1.5 我的历史（跟进中心"我的历史" Tab）

**基本信息**
- 方法：GET
- 路径：`/api/crm/record/mine`
- 权限：`crm:record:center`

**请求参数（query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 20 | 每页条数 |

**响应**：同 1.4，`data.records` 内字段一致。

按 `create_by = 当前用户名` 过滤，按 `create_time` 倒序。本阶段不支持 `relatedType` 过滤。

---

## 业务规则备注（新增）

- **线索转客户的跟进迁移**：调用 `POST /api/crm/lead/{id}/convert` 后，原线索下全部跟进记录按**模式 A 物理迁移**为 `relatedType=customer, relatedId=新客户ID`，迁移日志写入 `crm_record_migration_log` 表，可通过该表反向追溯。
- **死线索禁止写跟进**：`POST /api/crm/record` 当 `relatedType=lead` 且 `crm_lead.status=4` 时，后端抛 `BusinessException("已死线索不可继续跟进")`。
- **本期不做**：跟进记录编辑/删除（业务 append-only）；跟进统计报表（在 commit 2 报表模块）。

---

## 系统自动埋点

下列业务变更会自动追加 `crm_record`（`followType=系统`），无需手动调新增接口：

| 触发动作 | 接口 | relatedType | relatedId | 阶段 |
|:---|:---|:---|:---|:---|
| 线索转客户 | `POST /api/crm/lead/{id}/convert` | lead | 线索 ID（迁移到 customer） | 二 |
| 商机阶段变更 | `PUT /api/crm/business/{id}/stage` | business | 商机 ID | 三 |
| 标记死线索 | `POST /api/crm/lead/{id}/markDead` | lead | 线索 ID | 五 |

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

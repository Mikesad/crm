# 02. 线索接口

入口前缀：`/api/crm/lead`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:lead:list`（查询）/ `crm:lead:edit`（创建/更新/删除/转客户）
- **数据权限**：受 `dataScope` 拦截，普通销售（scope=5）仅看本人 owner 的线索

## 1.1 分页查询线索

**基本信息**
- 方法：GET
- 路径：`/api/crm/lead/page`
- 权限：`crm:lead:list`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|:---|:---|:---|:---|:---|
| pageNum | int | 否 | 页码，默认 1 | 1 |
| pageSize | int | 否 | 每页条数，默认 10，最大 200 | 10 |
| keyword | string | 否 | 模糊匹配 leadName / contactName / phone | 华为 |
| status | int | 否 | 1 未跟进 / 2 跟进中 / 3 已转客户 / 4 已死线索 | 2 |
| source | string | 否 | 线索来源 | 广告 |

**响应**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "leadName": "北京星辰科技",
        "contactName": "王晓东",
        "phone": "13800138000",
        "source": "线上留单",
        "status": 2,
        "statusText": "跟进中",
        "ownerUserId": 4,
        "ownerName": "李明",
        "remark": "客户对价格敏感",
        "createTime": "2026-06-20T10:23:00",
        "updateTime": "2026-06-25T15:18:00"
      }
    ],
    "total": 28,
    "current": 1,
    "size": 10
  }
}
```

**业务码**
- 200 / 401 / 403 / 1001

**调用示例**

```bash
curl -X GET "http://localhost:8080/api/crm/lead/page?keyword=华为&status=2&pageNum=1&pageSize=10" \
  -H "Authorization: <token>"
```

---

## 1.2 线索详情

**基本信息**
- 方法：GET
- 路径：`/api/crm/lead/{id}`
- 权限：`crm:lead:list`

**路径参数**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 线索 ID |

**业务码**
- 200 / 1003（数据不存在）

---

## 1.3 创建线索

**基本信息**
- 方法：POST
- 路径：`/api/crm/lead`
- 权限：`crm:lead:edit`

**请求体（body）**

```json
{
  "leadName": "北京星辰科技",
  "contactName": "王晓东",
  "phone": "13800138000",
  "source": "线上留单",
  "remark": "客户对价格敏感"
}
```

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| leadName | string | 是 | 最长 100 字符 |
| contactName | string | 是 | 最长 30 字符 |
| phone | string | 否 | 最长 20 字符 |
| source | string | 否 | 最长 50 字符 |
| remark | string | 否 | 最长 500 字符 |

**响应**

```json
{ "code": 200, "data": 12345 }
```

`data` 为新建线索 ID。创建后默认 `status=1（未跟进）`、`ownerUserId=当前用户`。

---

## 1.4 更新线索

**基本信息**
- 方法：PUT
- 路径：`/api/crm/lead`
- 权限：`crm:lead:edit`

**请求体（body）**

```json
{
  "id": 12345,
  "leadName": "北京星辰科技 (改)",
  "status": 2,
  "remark": "已发送方案"
}
```

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| id | long | 是 | 线索 ID |
| leadName / contactName / phone / source | string | 否 | 修改字段 |
| status | int | 否 | 1/2/4，禁止通过此接口设 3（已转客户） |
| remark | string | 否 | |

**业务码**
- 3001：已转客户（status=3）的线索不可修改
- 3001：禁止通过此接口把 status 设为 3

---

## 1.5 删除线索

**基本信息**
- 方法：DELETE
- 路径：`/api/crm/lead/{id}`
- 权限：`crm:lead:edit`
- 行为：逻辑删除（`is_deleted` 置 1），数据保留可恢复

---

## 1.6 线索转客户

**基本信息**
- 方法：POST
- 路径：`/api/crm/lead/{id}/convert`
- 权限：`crm:lead:edit`
- **核心业务**：`@Transactional` 内三步：
  1. 校验线索 `status != 3`（不可重复转）
  2. 写入 `crm_customer`（`ownerUserId=当前用户`，`isPublic=0`）
  3. 写入 `crm_contact`（`isMaster=1`，沿用线索联系人）
  4. 标记线索 `status=3`
  5. 追加 `crm_record` 时间轴

**请求体（body）**

```json
{
  "customerName": "北京星辰科技有限公司",
  "industry": "智能硬件",
  "level": "B",
  "post": "采购经理",
  "phone": "13800138000",
  "decisionWeight": 1
}
```

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| customerName | string | 是 | - | 客户主体名称，最长 100 字符 |
| industry | string | 否 | - | 所属行业 |
| level | string | 否 | C | A 重要 / B 普通 / C 意向 |
| post | string | 否 | - | 主联系人职务 |
| phone | string | 否 | 沿用线索电话 | 主联系人手机 |
| decisionWeight | int | 否 | 1 | 1 核心 / 2 弱影响 / 3 普通 |

**响应**

```json
{ "code": 200, "data": 67890 }
```

`data` 为新建客户 ID。

**业务码**
- 1002：该线索已转客户，不可重复转化
- 1003：线索不存在

---

## 1.7 标记为死线索（阶段五新增）

**基本信息**
- 方法：POST
- 路径：`/api/crm/lead/{id}/markDead`
- 权限：`crm:lead:markDead`（独立权限码，仅 owner 可调）

**请求体（body）**

```json
{
  "deadReason": "客户长期不回复，3 个月未接通电话"
}
```

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| deadReason | string | 否 | null | 死因备注，可空（前端表单显示"建议填写"），最长 500 字符 |

**业务校验**
- 调用者必须是该线索的 owner（`owner_user_id = StpUtil.getLoginId()`），否则 3001 "仅线索负责人可标记为死线索"
- `status` 必须 ∈ {1, 2}（已转客户/已死线索 拒绝），否则 3001 "仅 1-未跟进 / 2-跟进中 可标记为死线索"

**响应**

```json
{ "code": 200, "data": null, "msg": "操作成功" }
```

**副作用**
- UPDATE `crm_lead SET status=4, dead_time=NOW(), dead_reason=?`
- INSERT `crm_record(relatedType='lead', followType='系统', content='线索已标记为死线索，原因：xxx')`

**业务码**
- 3001：线索不存在 / 非 owner / 状态机非法
- 1003：线索不存在

---

## 关联接口

- 拉取线索跟进时间轴：`GET /api/crm/record/timeline?relatedType=lead&relatedId={id}`
- 转入后产生的客户详情：`GET /api/crm/customer/{data}`

## 业务规则备注

- **不可逆**：转客户后线索的 `status=3` 永久不可改回。
- **唯一转化路径**：除本接口外，没有任何其他途径可以把 `status` 改成 3。
- **死线索不可逆**：阶段五新增约束，`status=4` 后只能查看历史跟进，不可再写新跟进（后端 `RecordService.append` 校验）。
- **跟进迁移**：线索转客户后，原跟进记录按模式 A 物理迁移到客户时间轴，迁移日志写入 `crm_record_migration_log`。
- **删除**：已转化（status=3）的线索理论上仍可删除，但推荐保留作为审计。

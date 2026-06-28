# 12. 报表中心接口

入口前缀：`/api/crm/report`

## 通用信息

- **是否需要登录**：是
- **所需权限**：`crm:report:view`（所有 13 个接口共用,所有登录用户可见）
- **数据权限**：**不叠加** `CrmDataPermissionHandler`（决策 B）。所有报表聚合方法走 `@InterceptorIgnore(dataPermission="true")`,管理员/财务默认看全量；个人/部门筛选由 `deptId` / `userId` 参数控制。
- **缓存策略**：5 分钟内存缓存,`ConcurrentHashMap` + TTL,key = `reportKey + ":" + range + ":d<deptId>:u<userId>" + ":dim=<dim>"`。`/cache/clear` 手动清。
- **特殊说明**：
  - 4 个 Tab 各有 1 个"主接口"（funnel / customer / conversion / finance）一次性返回该 Tab 全部数据,前端默认按主接口渲染。
  - 每个 Tab 还有少量"局部刷新"接口（trend / performer / distribution / aging / funnel）,支持单独刷新某个 widget 而不重算整个 Tab。
  - 部门/人员下拉 `/filter/depts` / `/filter/users` 在 V1 简化为静态 mock,V2 接 `sys_dept` / `sys_user` 表。
- **阶段五 commit 2 新增**：本批 13 个接口 + 1 个权限码 `crm:report:view` + 8 个聚合索引 + 1 个新菜单(报表中心)。

## 通用请求参数

4 个 Tab 主接口 + 局部刷新接口共用以下 query 参数(均为可空,有默认值):

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| range | string | 否 | `month` | `today` / `week` / `month` / `quarter` / `year` / `custom`,`custom` 时 `startDate` + `endDate` 必填 |
| startDate | string (yyyy-MM-dd) | 否 | - | range=custom 时必填 |
| endDate | string (yyyy-MM-dd) | 否 | - | range=custom 时必填 |
| deptId | long | 否 | null | 部门 ID(走 sys_user.dept_id → ownerIds),null=全部 |
| userId | long | 否 | null | 人员 ID,优先级高于 deptId |
| topN | int | 否 | 5 | 仅 performer 类接口,最大 50 |
| dim | string | 否 | `industry` | 仅 customer 分布接口,`industry` / `level` / `source` |

## 通用响应

所有接口返回 `Result<T>`,结构见 README 公共约定。失败时 `code` 不为 200,`data` 为 null。

---

# Tab ① 销售漏斗 + 业绩

## 1.1 主接口（funnel）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/funnel`
- 权限：`crm:report:view`
- 缓存：5 分钟

**请求参数（query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| range | string | 否 | month | 见通用参数 |
| startDate | string | 否 | - | custom 模式必填 |
| endDate | string | 否 | - | custom 模式必填 |
| deptId | long | 否 | - | 部门筛选 |
| userId | long | 否 | - | 人员筛选 |
| topN | int | 否 | 5 | 销售榜 TOP N,1-50 |

**响应 data 结构（ReportFunnelVO）**

```json
{
  "kpis": [
    { "key": "totalAmount",   "label": "销售总额",   "value": "2468000", "unit": "¥",   "delta": "↑ 18.6%",  "deltaDir": "up",   "footnote": "vs 上月" },
    { "key": "contractCount", "label": "新签合同",   "value": "11",       "unit": "单",  "delta": "↑ 2 单",    "deltaDir": "up",   "footnote": "vs 上月" },
    { "key": "avgPrice",      "label": "客单价",     "value": "224364",   "unit": "¥",   "delta": "↓ 3.2%",   "deltaDir": "down", "footnote": "vs 上月" },
    { "key": "winRate",       "label": "赢单率",     "value": "47.8",     "unit": "%",   "delta": null,        "deltaDir": null,   "footnote": null },
    { "key": "convRate",      "label": "线索转化率", "value": "17.8",     "unit": "%",   "delta": null,        "deltaDir": null,   "footnote": null },
    { "key": "received",      "label": "回款金额",   "value": "1860000",  "unit": "¥",   "delta": "↑ 24.1%",  "deltaDir": "up",   "footnote": "vs 上月" }
  ],
  "funnel": [
    { "stage": "stage1", "stageName": "新建线索", "count": 62, "amount": "0",       "convRate": "100%" },
    { "stage": "stage2", "stageName": "需求分析", "count": 48, "amount": "2150000", "convRate": "77.4%" },
    { "stage": "stage3", "stageName": "方案报价", "count": 30, "amount": "1820000", "convRate": "48.4%" },
    { "stage": "stage4", "stageName": "商务谈判", "count": 23, "amount": "1680000", "convRate": "37.1%" },
    { "stage": "stage5", "stageName": "赢单",     "count": 11, "amount": "2468000", "convRate": "17.7%" }
  ],
  "trend": [
    { "date": "2026-01", "value": "1420000", "seriesKey": null },
    { "date": "2026-02", "value": "1680000", "seriesKey": null },
    { "date": "2026-03", "value": "1820000", "seriesKey": null },
    { "date": "2026-04", "value": "1750000", "seriesKey": null },
    { "date": "2026-05", "value": "2081000", "seriesKey": null },
    { "date": "2026-06", "value": "2468000", "seriesKey": null }
  ],
  "departmentPerformers": [
    { "deptId": 1, "deptName": "部门 1", "amount": "1160000", "percent": "47.0%" },
    { "deptId": 2, "deptName": "部门 2", "amount": "720000",  "percent": "29.1%" },
    { "deptId": 3, "deptName": "部门 3", "amount": "408000",  "percent": "16.5%" },
    { "deptId": 4, "deptName": "部门 4", "amount": "180000",  "percent": "7.3%" }
  ],
  "sourceDistribution": [
    { "key": "官网咨询", "count": 142, "amount": null, "percent": "37.5%" },
    { "key": "电话拜访", "count": 98,  "amount": null, "percent": "25.9%" },
    { "key": "老客户介绍", "count": 76, "amount": null, "percent": "20.1%" },
    { "key": "展会推广", "count": 48, "amount": null, "percent": "12.7%" },
    { "key": "其他",   "count": 22,  "amount": null, "percent": "5.8%" }
  ],
  "topPerformers": [
    { "rank": 1, "name": "李销售", "subtitle": null, "count": 4, "amount": "982000", "convRate": null, "overdueAmount": null },
    { "rank": 2, "name": "陈销售", "subtitle": null, "count": 3, "amount": "738000", "convRate": null, "overdueAmount": null },
    { "rank": 3, "name": "王主管", "subtitle": null, "count": 2, "amount": "696000", "convRate": null, "overdueAmount": null }
  ]
}
```

**字段说明**

- `kpis[].value` 是字符串形式,BigDecimal → toPlainString(防科学计数法)
- `funnel[].convRate` 是相对第一阶段"新建线索"的转化率,首阶段固定 "100%"
- `trend[].seriesKey` 留空表示单 series,阶段六扩展多 series 时填 "contract" / "received" / "predicted"
- `sourceDistribution[].amount` 此处为 null(线索来源不绑定金额),如需金额请走 customer 接口
- `topPerformers[].name` 由 sys_user.nickname 补全,owner 离职/匿名时显示 "用户<ID>"

**业务码**

| 码 | 含义 |
|:---|:---|
| 200 | 成功 |
| 401 | 未登录 |
| 403 | 无 `crm:report:view` 权限 |
| 1001 | range=custom 时 startDate/endDate 缺失或格式错 |

**curl 示例**

```bash
curl -X GET 'http://localhost:8080/api/crm/report/funnel?range=month&topN=5' \
  -H 'Authorization: <Sa-Token token>'
```

---

## 1.2 趋势（funnel/trend）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/funnel/trend`
- 权限：`crm:report:view`
- 缓存：5 分钟（共用 funnel 主接口 key）

**请求参数（query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| range | string | 否 | `year` | 趋势接口默认 year(覆盖通用默认 month) |
| startDate / endDate | string | 否 | - | custom 模式必填 |
| deptId | long | 否 | - | 部门筛选 |
| userId | long | 否 | - | 人员筛选 |

**响应**：`Result<List<ReportTrendPointVO>>`,见 1.1 `trend` 字段。

**业务码**：同 1.1

**curl 示例**

```bash
curl -X GET 'http://localhost:8080/api/crm/report/funnel/trend?range=year&deptId=2' \
  -H 'Authorization: <Sa-Token token>'
```

---

## 1.3 销售个人榜（funnel/performer）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/funnel/performer`
- 权限：`crm:report:view`

**请求参数（query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| range | string | 否 | month | - |
| deptId | long | 否 | - | - |
| userId | long | 否 | - | - |
| topN | int | 否 | 5 | 1-50 |

**响应**：`Result<List<ReportPerformerVO>>`,见 1.1 `topPerformers` 字段。

**业务码**：同 1.1

---

# Tab ② 客户分布

## 2.1 主接口（customer）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/customer`
- 权限：`crm:report:view`
- 缓存：5 分钟

**请求参数（query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| range | string | 否 | month | 影响趋势/活跃度时间窗 |
| deptId / userId | long | 否 | - | 部门/人员筛选 |
| dim | string | 否 | `industry` | `industry` / `level` / `source`(V1 暂为 industry / level,source 走 crm_lead 转介后统计) |

**响应 data 结构（ReportCustomerVO）**

```json
{
  "kpis": [
    { "key": "total",          "label": "客户总数",     "value": "386",   "unit": null, "delta": null, "deltaDir": null, "footnote": null },
    { "key": "industryCount",  "label": "行业数",       "value": "12",    "unit": null, "delta": null, "deltaDir": null, "footnote": null },
    { "key": "publicPct",      "label": "公海占比",     "value": "14.0",  "unit": "%",   "delta": null, "deltaDir": null, "footnote": null },
    { "key": "dormantCount",   "label": "沉睡客户数",   "value": "76",    "unit": null, "delta": null, "deltaDir": null, "footnote": "19.7% 客户超 30 天未跟进" }
  ],
  "distribution": [
    { "key": "企业服务", "count": 142, "amount": null, "percent": "36.8%" },
    { "key": "制造",     "count": 98,  "amount": null, "percent": "25.4%" },
    { "key": "广告",     "count": 76,  "amount": null, "percent": "19.7%" },
    { "key": "教育",     "count": 48,  "amount": null, "percent": "12.4%" },
    { "key": "金融",     "count": 22,  "amount": null, "percent": "5.7%" }
  ],
  "activity": {
    "total": 386,
    "active": 256,
    "dormant": 76,
    "publicPool": 54,
    "activePercent": "66.3%",
    "dormantPercent": "19.7%",
    "publicPercent": "14.0%"
  },
  "levelDistribution": [
    { "key": "A", "count": 86, "amount": null, "percent": "22.3%" },
    { "key": "B", "count": 220, "amount": null, "percent": "57.0%" },
    { "key": "C", "count": 80, "amount": null, "percent": "20.7%" }
  ],
  "regionDistribution": [
    { "key": "企业服务", "count": 142, "amount": null, "percent": "36.8%" }
  ],
  "activityTrend": [
    { "date": "2026-01", "value": "230", "seriesKey": "active" },
    { "date": "2026-02", "value": "245", "seriesKey": "active" },
    { "date": "2026-03", "value": "256", "seriesKey": "active" },
    { "date": "2026-04", "value": "262", "seriesKey": "active" },
    { "date": "2026-05", "value": "258", "seriesKey": "active" },
    { "date": "2026-06", "value": "256", "seriesKey": "active" }
  ]
}
```

**字段说明**

- `activity` 中 `active` = last_follow_time 距今 ≤30 天;`dormant` = >30 天 或 NULL;`publicPool` = is_public=1(不受 ownerIds 过滤)
- `levelDistribution` 是常显 widget(等级 A/B/C),不随 dim 切换变化
- `regionDistribution` V1 暂用 industry 替代(crm_customer 无 region 字段),V2 增强
- `activityTrend` V1 简化为"近 30 天有跟进的客户数"按月快照(无历史回溯能力),V2 增强

**业务码**：同 1.1

**curl 示例**

```bash
curl -X GET 'http://localhost:8080/api/crm/report/customer?range=month&dim=industry&deptId=2' \
  -H 'Authorization: <Sa-Token token>'
```

---

## 2.2 维度分布（customer/distribution）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/customer/distribution`
- 权限：`crm:report:view`

同 2.1,只是 `dim` 必传且 `range` 默认为 month(不影响 distribution 计算,只影响趋势)。

**业务码**：同 1.1

---

# Tab ③ 跟进与转化率

## 3.1 主接口（conversion）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/conversion`
- 权限：`crm:report:view`
- 缓存：5 分钟

**请求参数（query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| range | string | 否 | month | - |
| deptId / userId | long | 否 | - | - |
| topN | int | 否 | 5 | 高频跟进人榜 TOP N |

**响应 data 结构（ReportConversionVO）**

```json
{
  "kpis": [
    { "key": "totalRecord", "label": "跟进总数",     "value": "428",   "unit": "条", "delta": null, "deltaDir": null, "footnote": null },
    { "key": "convRate",    "label": "平均转化率",   "value": "47.8",  "unit": "%",  "delta": null, "deltaDir": null, "footnote": null },
    { "key": "dailyAvg",    "label": "日均跟进",     "value": "14.3",  "unit": "条", "delta": null, "deltaDir": null, "footnote": null },
    { "key": "performers",  "label": "活跃跟进人数", "value": "6",     "unit": null, "delta": null, "deltaDir": null, "footnote": null }
  ],
  "stageFunnel": [
    { "stage": "stage1", "stageName": "新建线索", "count": 62, "amount": null, "convRate": "100%" },
    { "stage": "stage2", "stageName": "需求分析", "count": 48, "amount": null, "convRate": "77.4%" },
    { "stage": "stage3", "stageName": "方案报价", "count": 30, "amount": null, "convRate": "48.4%" },
    { "stage": "stage4", "stageName": "商务谈判", "count": 23, "amount": null, "convRate": "37.1%" },
    { "stage": "stage5", "stageName": "赢单",     "count": 11, "amount": null, "convRate": "17.7%" }
  ],
  "followTypeDist": [
    { "key": "电话",     "count": 162, "amount": null, "percent": "37.9%" },
    { "key": "微信",     "count": 118, "amount": null, "percent": "27.6%" },
    { "key": "上门拜访", "count": 86,  "amount": null, "percent": "20.1%" },
    { "key": "邮件",     "count": 42,  "amount": null, "percent": "9.8%" },
    { "key": "其他",     "count": 20,  "amount": null, "percent": "4.6%" }
  ],
  "topPerformers": [
    { "rank": 1, "name": "李销售",   "subtitle": null, "count": 86, "amount": null, "convRate": null, "overdueAmount": null },
    { "rank": 2, "name": "王主管",   "subtitle": null, "count": 78, "amount": null, "convRate": null, "overdueAmount": null },
    { "rank": 3, "name": "陈销售",   "subtitle": null, "count": 65, "amount": null, "convRate": null, "overdueAmount": null }
  ],
  "teamVsCompany": [
    { "group": "team",    "stage1Lead": "100%", "stage2Analysis": "77.4%", "stage3Quote": "48.4%", "stage4Negotiate": "37.1%", "stage5Win": "17.7%" },
    { "group": "company", "stage1Lead": "100%", "stage2Analysis": "77.4%", "stage3Quote": "48.4%", "stage4Negotiate": "37.1%", "stage5Win": "17.7%" }
  ],
  "trend": [
    { "date": "2026-01", "value": "62",  "seriesKey": null },
    { "date": "2026-02", "value": "68",  "seriesKey": null },
    { "date": "2026-03", "value": "75",  "seriesKey": null },
    { "date": "2026-04", "value": "70",  "seriesKey": null },
    { "date": "2026-05", "value": "78",  "seriesKey": null },
    { "date": "2026-06", "value": "75",  "seriesKey": null }
  ]
}
```

**字段说明**

- `stageFunnel[].amount` 留空(同 Tab ① 已用 funnel 接口返回)
- `topPerformers[].name` = crm_record.create_by(销售昵称/账号,无 sys_user JOIN)
- `teamVsCompany` V1 团队与全公司同值(deptId 未选时),V2 增强为真对比

**业务码**：同 1.1

**curl 示例**

```bash
curl -X GET 'http://localhost:8080/api/crm/report/conversion?range=month&topN=10' \
  -H 'Authorization: <Sa-Token token>'
```

---

## 3.2 阶段转化漏斗（conversion/funnel）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/conversion/funnel`
- 权限：`crm:report:view`

**请求参数（query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| range | string | 否 | month | - |
| deptId / userId | long | 否 | - | - |

**响应**：`Result<ReportConversionVO>`,字段同 3.1。

**业务码**：同 1.1

---

# Tab ④ 回款 / 财务

## 4.1 主接口（finance）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/finance`
- 权限：`crm:report:view`
- 缓存：5 分钟

**请求参数（query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| range | string | 否 | month | 影响已回款/趋势范围 |
| deptId / userId | long | 否 | - | - |
| topN | int | 否 | 5 | 应收 TopN |

**响应 data 结构（ReportFinanceVO）**

```json
{
  "kpis": [
    { "key": "contractTotal", "label": "合同总额",   "value": "8200000",  "unit": "¥", "delta": null, "deltaDir": null, "footnote": null },
    { "key": "received",      "label": "已回款",     "value": "1860000",  "unit": "¥", "delta": null, "deltaDir": null, "footnote": null },
    { "key": "unreceived",    "label": "未回款",     "value": "4560000",  "unit": "¥", "delta": null, "deltaDir": null, "footnote": null },
    { "key": "overdueRate",   "label": "逾期率",     "value": "4.3",      "unit": "%", "delta": null, "deltaDir": null, "footnote": null }
  ],
  "trend": [
    { "date": "2026-01", "value": "1200000", "seriesKey": "contract" },
    { "date": "2026-01", "value": "980000",  "seriesKey": "received" },
    { "date": "2026-02", "value": "1480000", "seriesKey": "contract" },
    { "date": "2026-02", "value": "1180000", "seriesKey": "received" }
  ],
  "monthlyStacked": [
    { "date": "2026-01", "value": "1200000", "seriesKey": "contract" }
  ],
  "agingBuckets": [
    { "key": "0-30",  "label": "0-30 天",  "count": 12, "amount": "1200000", "percent": "60.0%" },
    { "key": "31-60", "label": "31-60 天", "count": 5,  "amount": "560000",  "percent": "25.0%" },
    { "key": "61-90", "label": "61-90 天", "count": 2,  "amount": "180000",  "percent": "10.0%" },
    { "key": "90+",   "label": "90+ 天",   "count": 1,  "amount": "60000",   "percent": "5.0%" }
  ],
  "receivableMethod": [
    { "key": "银行转账", "count": 18, "amount": "1480000", "percent": "60.0%" },
    { "key": "微信",     "count": 8,  "amount": "320000",  "percent": "26.7%" },
    { "key": "支付宝",   "count": 3,  "amount": "60000",   "percent": "10.0%" },
    { "key": "现金",     "count": 1,  "amount": "20000",   "percent": "3.3%" }
  ],
  "topDebtors": [
    { "rank": 1, "name": "杭州xx科技", "subtitle": null, "count": 3, "amount": "1420000", "convRate": null, "overdueAmount": null },
    { "rank": 2, "name": "深圳yy电子", "subtitle": null, "count": 2, "amount": "980000",  "convRate": null, "overdueAmount": null }
  ]
}
```

**字段说明**

- `kpis[].contractTotal` 是累计合同总额(全量,不受 range 影响)
- `kpis[].received` 受 range 影响
- `kpis[].unreceived` 来自 crm_receivable_plan.status != 2 的 expected_amount 累加
- `kpis[].overdueRate` 分子=expected_date < today 的 plan 数,分母=全部未回款 plan 数
- `agingBuckets` 按 DATEDIFF(today, expected_date) 分桶,V1 全量拉 plan 在 Service 端 Java Stream 分桶
- `monthlyStacked` V1 复用 trend 数据,V2 拆 已回款/计划未回款
- `topDebtors[].name` 由 crm_contract JOIN crm_customer 补全

**业务码**：同 1.1

**curl 示例**

```bash
curl -X GET 'http://localhost:8080/api/crm/report/finance?range=month&topN=10' \
  -H 'Authorization: <Sa-Token token>'
```

---

## 4.2 账龄分布（finance/aging）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/finance/aging`
- 权限：`crm:report:view`

**请求参数（query）**：无（range 固定 year,dept/user 暂不参与分桶计算）

**响应**：`Result<List<ReportAgingBucketVO>>`,见 4.1 `agingBuckets` 字段。

**业务码**：同 1.1

---

## 4.3 应收 TopN（finance/performer）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/finance/performer`
- 权限：`crm:report:view`

**请求参数（query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|:---|:---|:---|:---|:---|
| topN | int | 否 | 5 | 1-50 |

**响应**：`Result<List<ReportPerformerVO>>`,见 4.1 `topDebtors` 字段。

**业务码**：同 1.1

---

# 通用:筛选下拉

## 5.1 部门下拉（filter/depts）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/filter/depts`
- 权限：`crm:report:view`

**请求参数（query）**：无

**响应**

```json
{
  "code": 200,
  "data": [
    { "id": 1, "name": "销售一部" },
    { "id": 2, "name": "销售二部" },
    { "id": 3, "name": "销售三部" },
    { "id": 4, "name": "销售四部" }
  ]
}
```

**V1 说明**：返回静态 mock 4 部门,V2 接 `sys_dept` 表后自动同步。

**业务码**：同 1.1

---

## 5.2 人员下拉（filter/users）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/filter/users`
- 权限：`crm:report:view`

**请求参数（query）**

| 字段 | 类型 | 必填 | 说明 |
|:---|:---|:---|:---|
| deptId | long | 否 | 部门筛选,null=全部 |

**响应**

```json
{
  "code": 200,
  "data": []
}
```

**V1 说明**：返回空数组,前端顶部"销售"下拉用"全部销售"兜底。V2 接 `sys_user` 表后按 deptId/status 过滤。

**业务码**：同 1.1

---

## 5.3 清空缓存（cache/clear）

**基本信息**
- 方法：GET
- 路径：`/api/crm/report/cache/clear`
- 权限：`crm:report:view`（V1 暂用统一权限码,V2 改成 `crm:report:manage` 仅 admin）

**请求参数（query）**：无

**响应**

```json
{
  "code": 200,
  "data": 12
}
```

- `data`：被清掉的缓存条目数(整数)

**业务码**

| 码 | 含义 |
|:---|:---|
| 200 | 成功 |
| 403 | V2 后将仅 admin 可调,普通用户 403 |

**curl 示例**

```bash
curl -X GET 'http://localhost:8080/api/crm/report/cache/clear' \
  -H 'Authorization: <Sa-Token token>'
```

---

## 业务码汇总

| 码 | 含义 | 触发场景 |
|:---|:---|:---|
| 200 | 成功 | 正常返回 |
| 401 | 未登录 | token 缺失/过期 |
| 403 | 无权限 | 缺 `crm:report:view` |
| 1001 | 参数校验失败 | range=custom 缺 startDate/endDate 或格式错 |

**阶段六增强**:
- 1002: range 枚举值非法
- 2003: 同比计算时除零(本期/上期值)
- 3001-3010: 报表聚合 SQL 异常分类(超时/超 500 LIMIT/字段缺失)

---

## 更新日志

- **2026-06-28 (commit 2)**:
  - 新增 13 个接口(funnel / funnel/trend / funnel/performer / customer / customer/distribution / conversion / conversion/funnel / finance / finance/aging / finance/performer / filter/depts / filter/users / cache/clear)
  - 新增 1 个权限码 `crm:report:view`(全 5 角色绑定)
  - 新增 8 个聚合二级索引(7 张业务表)
  - 新增 1 个菜单"报表中心"(`/report` 路由,菜单类型 C)
  - 5 分钟内存缓存(TTL-based ConcurrentHashMap,不依赖 Caffeine)
  - 决策 B:报表不叠加数据权限拦截,Mapper `@InterceptorIgnore(dataPermission="true")`
  - 决策 B:本期不做 Excel 导出、邮件推送、报表订阅、数据权限分层

## V1 简化与阶段六 TODO

- [ ] `filter/depts` 改接 `sys_dept` 表(V1 mock 4 部门)
- [ ] `filter/users` 改接 `sys_user` 表(V1 空)
- [ ] Tab ② `regionDistribution` 真正按地区(V1 用 industry 替代)
- [ ] Tab ② `activityTrend` 支持历史回溯(V1 单点快照)
- [ ] Tab ③ `teamVsCompany` 真正分别算(V1 同值)
- [ ] Tab ④ `monthlyStacked` 拆 已回款/计划未回款 2 series
- [ ] Tab ① KPI 同比对比期计算(V1 mock 字符串)
- [ ] Excel/CSV 导出
- [ ] 报表订阅 + 邮件推送
- [ ] 报表数据权限分层(管理员/财务默认全量,sales 仅本人 + 部门,V2 用更细 data_scope)

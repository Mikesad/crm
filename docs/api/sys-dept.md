# 15. 系统 - 部门管理

> 阶段七 commit：0→1 完整建。
> 访问角色：admin + sales_director。
> 3 类删除保护：顶级不可删 / 有子部门拒绝 / 有启用用户拒绝。
> 父变更事务内刷后代 ancestors。

## 接口列表

### 1. 部门全量列表（树形 / 平铺共用）

`GET /api/sys/dept/all`

**响应**：`List<SysDeptVO>`，字段见下表。

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | Long | 部门 ID |
| parentId | Long | 上级部门 ID,0 表示顶级 |
| parentName | String | 上级部门名称(批量填) |
| ancestors | String | 祖级链,如 `0,1`,逗号分隔 |
| deptName | String | 部门名称 |
| orderNum | Integer | 同级显示顺序 |
| status | Integer | 0 停用 / 1 正常 |
| statusText | String | 显示文本 |
| childCount | Integer | 直接子部门数 |
| userCount | Integer | 启用用户数 |
| createTime / updateTime | LocalDateTime | 审计时间 |

**权限码**：`sys:dept:list`

### 2. 部门分页（搜索）

`GET /api/sys/dept/page?keyword=&status=&parentId=&pageNum=&pageSize=`

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| keyword | String | 否 | deptName 模糊匹配 |
| status | Integer | 否 | 0/1 |
| parentId | Long | 否 | 按上级筛选 |
| pageNum / pageSize | int | 否 | 默认 1 / 10 |

**权限码**：`sys:dept:list`

### 3. 部门详情

`GET /api/sys/dept/{id}`

**响应**：`SysDeptVO`（含 `parentName / childCount / userCount`）。

**权限码**：`sys:dept:list`

### 4. 新建部门

`POST /api/sys/dept`

**请求**：

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| deptName | String | 是 | 同级唯一 |
| parentId | Long | 是 | V1 暂不允许新建顶级(parentId=0 拒绝) |
| orderNum | Integer | 是 | 同级从小到大 |
| status | Integer | 否 | 默认 1 |

**副作用**：后端自动计算 `ancestors = parent.ancestors + "," + parent.id`。

**权限码**：`sys:dept:edit`

### 5. 更新部门

`PUT /api/sys/dept`

**请求**：

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| id | Long | 是 | |
| deptName | String | 否 | 改时同级唯一校验 |
| parentId | Long | 否 | **变更会触发祖先链重建** |
| orderNum | Integer | 否 | |
| status | Integer | 否 | |

**父变更细节**：
1. 同事务内先 updateById 更新自身 ancestors 为新父链
2. 再 `selectDescendantsByAncestors(oldPrefix)` 拉所有后代
3. 对每个后代 `replaceFirst(oldPrefix, newPrefix)` 重写 ancestors 字段
4. 后代 skip 自身(selfId 已在 updateById 处理)

**权限码**：`sys:dept:edit`

### 6. 删除部门

`DELETE /api/sys/dept/{id}`

3 类保护(任一命中抛 `BusinessException`)：
1. 顶级(parent_id=0) → "顶级部门不可删除"
2. 有子部门 → "部门 X 下存在子部门,请先删除子部门"
3. 有启用用户 → "部门 X 下存在 N 名启用用户,请先转移用户"

**权限码**：`sys:dept:edit`

### 7. 启停用

`PUT /api/sys/dept/{id}/status?status=0|1`

顶级部门不可停用(管理上保持顶级永远在岗)。

不踢下线(部门不在 Sa-Token session 缓存,与登录态解耦)。

**权限码**：`sys:dept:edit`

## ancestors 字段维护规则

| 场景 | 公式 |
| :--- | :--- |
| 顶级(parentId=0) | `ancestors='0'` |
| 新建 dept (parentId=P) | `ancestors = parent.ancestors + "," + parent.id` |
| 父变更(P_old → P_new) | self 先按上式重算;后代 ancestors 字符串中"旧 ancestors"前缀替换为"新 ancestors"前缀 |

### 父变更示例

```
self.id=2, old ancestors='0,1', new ancestors='0,5'
├─ direct child d=8   old='0,1,2'   new='0,5,2'
└─ 2nd-level  d=9     old='0,1,2,8' new='0,5,2,8'
```

## 业务规则备注

- **V1 限制**：暂不允许新建顶级部门(parentId=0 时 `addDept` 直接拒绝)
- **级联检查(el-cascader 父选择控件)**：后端 `updateDept` 中校验 parentId 不能是自己或自己的后代,防死循环
- **删除顺序**：用户 → 子部门 → 父部门(自下而上)

## 调用示例

```bash
# 1. 列出全量部门(树形 / 平铺)
curl -H "Authorization: <token>" "http://localhost:8080/api/sys/dept/all"

# 2. 新建子部门
curl -X POST "http://localhost:8080/api/sys/dept" \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"deptName":"华北销售部","parentId":1,"orderNum":5,"status":1}'

# 3. 父变更触发祖先链重建
curl -X PUT "http://localhost:8080/api/sys/dept" \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"id":2,"parentId":5}'

# 4. 删除(被业务规则保护)
curl -X DELETE "http://localhost:8080/api/sys/dept/2" \
  -H "Authorization: <token>"
```

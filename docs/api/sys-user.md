# 13. 系统 - 用户管理

> 阶段六 commit 1：完整 CRUD + 重置密码 + 角色分配。
> 访问角色：admin + sales_director。

## 公共约定

- 鉴权：除 `/list`（轻量版，被共享人下拉用）外，所有接口需 Sa-Token 鉴权
- 业务码：复用 `ResultCode.PARAM_ERROR` (1001) / `DATA_NOT_FOUND` (1003) / `DATA_EXISTS` (1002) / `FORBIDDEN` (403) / `BUSINESS_ERROR` (3001)
- 核心约束：admin 自保护（不能操作自己）/ admin 至少 1 人 / username 不可改

## 接口列表

### 1. 用户分页

`GET /api/sys/user/page`

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| keyword | string | 否 | 模糊匹配 username / nickname |
| deptId | long | 否 | 按部门过滤 |
| status | 0\|1 | 否 | 0 停用 / 1 正常 |
| pageNum | int | 否 | 默认 1 |
| pageSize | int | 否 | 默认 10 |

**响应**：`IPage<SysUserVO>`，每条含 `id / username / nickname / deptId / deptName / roleIds / roleNames / status / statusText / createTime / updateTime`

**权限码**：`sys:user:list`

### 2. 用户详情

`GET /api/sys/user/{id}`

**响应**：`SysUserVO`（含 `roleIds` 列表）

**权限码**：`sys:user:list`

### 3. 新建用户

`POST /api/sys/user`

**请求**：`SysUserCreateRequest`（username / nickname / deptId / password(可空,默认123456) / phone / email / sex / status / roleIds）

**响应**：`{ id: Long }`

**权限码**：`sys:user:edit`

### 4. 更新用户

`PUT /api/sys/user`

**请求**：`SysUserUpdateRequest`（id 必填；username 不可改；传 roleIds 时全量重绑）

**权限码**：`sys:user:edit`

### 5. 删除用户

`DELETE /api/sys/user/{id}`

逻辑删除 + 踢下线。admin 自保护 + 至少 1 人校验。

**权限码**：`sys:user:edit`

### 6. 重置密码

`POST /api/sys/user/{id}/resetPassword?newPassword=xxx`

`newPassword` 留空则重置为 `123456`。重置后踢下线，迫使用户重新登录。

**权限码**：`sys:user:reset_pwd`

### 7. 分配角色

`PUT /api/sys/user/{id}/roles`

**请求**：`[roleId1, roleId2, ...]`（全量重绑）

**权限码**：`sys:user:assign_role`

### 8. 启停用

`PUT /api/sys/user/{id}/status?status=0|1`

status=0 时踢下线。

**权限码**：`sys:user:edit`

### 9. 轻量用户列表（既有）

`GET /api/sys/user/list?keyword=&deptId=`

被共享人下拉用，max 200 条，已登录即可（`@SaIgnore`）。

## 调用示例

```bash
# 分页查询
curl -X GET "http://localhost:8080/api/sys/user/page?keyword=admin&pageNum=1&pageSize=10" \
  -H "Authorization: <token>"

# 新建用户
curl -X POST "http://localhost:8080/api/sys/user" \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "sales_zhao",
    "nickname": "赵销售",
    "deptId": 2,
    "phone": "13800000010",
    "email": "zhao@zencrm.local",
    "roleIds": [4]
  }'
```

## 业务规则备注

- **admin 自保护**：所有写接口拒绝 `targetUserId == StpUtil.getLoginId()`，避免误把自己禁用
- **admin 至少 1 人**：DELETE / 停用 / 剥离 admin 角色时实时校验
- **踢下线**：`StpUtil.logout(userId)`，使该用户 Sa-Token session 失效，下次请求会触发 401

# 14. 系统 - 角色管理

> 阶段六 commit 1：完整 CRUD + 菜单绑定。
> 访问角色：admin + sales_director。
> 内置 5 角色（admin / sales_director / sales_lead / sales / finance）不可删。

## 接口列表

### 1. 角色分页

`GET /api/sys/role/page?keyword=&pageNum=&pageSize=`

**响应**：`IPage<SysRoleVO>`，每条含 `id / roleName / roleKey / dataScope / dataScopeText / status / statusText / userCount / createTime`

**权限码**：`sys:role:list`

### 2. 全部启用角色下拉

`GET /api/sys/role/all`

返回 status=1 的全量角色，供用户编辑/分配时使用。

**权限码**：`sys:role:list`

### 3. 角色详情

`GET /api/sys/role/{id}`

**响应**：`SysRoleVO`（含 `menuIds: List<Long>` / `userCount`）

**权限码**：`sys:role:list`

### 4. 新建角色

`POST /api/sys/role`

**请求**：`SysRoleCreateRequest`（roleName / roleKey / dataScope / status / menuIds）

**权限码**：`sys:role:edit`

### 5. 更新角色

`PUT /api/sys/role`

**请求**：`SysRoleUpdateRequest`（id 必填；roleKey 不可改；传 menuIds 时全量重绑 + 踢所有绑定用户下线）

**权限码**：`sys:role:edit`

### 6. 删除角色

`DELETE /api/sys/role/{id}`

内置 5 角色拒绝 + 存在用户绑定时拒绝。

**权限码**：`sys:role:edit`

### 7. 分配菜单

`PUT /api/sys/role/{id}/menus`

**请求**：`[menuId1, menuId2, ...]`（全量重绑 sys_role_menu）

**执行效果**：
1. 同事务内 `DELETE FROM sys_role_menu WHERE role_id=?` + 批量 `INSERT`
2. 踢所有绑定此角色的用户下线（Sa-Token session 失效）

**权限码**：`sys:role:assign_menu`

### 8. 添加成员（v0.3 收尾新增）

`POST /api/sys/role/{id}/members`

**请求**：`[userId1, userId2, ...]`

**执行效果**：
1. 逐个 userId 用 `INSERT IGNORE` 插入 `sys_user_role`（重复绑定走 IGNORE 跳过，无异常）
2. 受影响的所有 userId 踢下线（Sa-Token logout）

**响应**：`{ data: 实际新增数量 (Integer) }`

**权限码**：`sys:role:assign_menu`

### 9. 移除成员（v0.3 收尾新增）

`DELETE /api/sys/role/{id}/members/{userId}`

**执行效果**：
1. admin 角色兜底：若本 admin 角色绑定的活跃用户数 ≤ 1,抛 `BusinessException` 拒绝
2. `DELETE FROM sys_user_role WHERE user_id=? AND role_id=?`
3. 该 userId 踢下线

**权限码**：`sys:role:assign_menu`

### 10. 成员列表（v0.3 收尾新增）

`GET /api/sys/role/{id}/members?pageNum=&pageSize=`

**响应**：`IPage<SysUserVO>`，每条含 `id / username / nickname / deptName / roleIds / roleNames / status / statusText / createTime`

**字段特性**：
- 返回的 roleIds / roleNames **故意不含当前 roleId**，避免在"其他角色"列里重复显示

**权限码**：`sys:role:list`

## dataScope 取值

| 取值 | 含义 | 拼接效果 |
| :--- | :--- | :--- |
| 1 | 全部 | 不拼 |
| 2 | 自定义 | V1 暂未启用 |
| 3 | 本部门组 | `owner_user_id IN (我的部门 OR 同 parent_id 兄弟部门的所有用户)` |
| 4 | ~~本部门及以下~~ | phase8 commit1 已拆档；历史值兜底为仅本人 |
| 5 | 仅本人 | `owner_user_id = 当前用户` |

## 调用示例

```bash
# 分配菜单
curl -X PUT "http://localhost:8080/api/sys/role/3/menus" \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '[1, 3, 5, 7, 9, 11, 13]'
```

## 业务规则备注

- **内置 5 角色保护**：`role_key IN ('admin','sales_director','sales_lead','sales','finance')` 不可删
- **菜单绑定踢下线**：所有绑定此角色的用户 Sa-Token session 失效，下一次请求 401 跳登录页
- **角色权限分层**（v0.1 决策）：仅 `dataScope=1`（admin）能管所有用户，5 个内置角色由 admin 维护

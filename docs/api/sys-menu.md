# 15. 系统 - 菜单权限

> 阶段六 commit 1：菜单元数据 CRUD（区别于"角色 ↔ 菜单绑定"在 sys-role.md）。
> 访问角色：admin + sales_director。
> 关键能力菜单（perms LIKE `sys:system:view` 等）不可删。

## 接口列表

### 1. 全量菜单平铺

`GET /api/sys/menu/all`

返回 status=1 的全量菜单平铺列表，前端自行组装树。

**权限码**：`sys:menu:list`

### 2. 菜单树

`GET /api/sys/menu/tree`

后端组装好 children 字段的树形结构（用于 Dialog 父菜单下拉）。

**权限码**：`sys:menu:list`

### 3. 菜单详情

`GET /api/sys/menu/{id}`

**权限码**：`sys:menu:list`

### 4. 新建菜单

`POST /api/sys/menu`

**请求**：`SysMenuCreateRequest`（menuName / parentId / orderNum / path / component / menuType / perms / status）

**菜单类型校验**：
- M（目录）：perms 必空
- C（菜单）：path + component 必填
- F（按钮）：perms 必填

**权限码**：`sys:menu:edit`

### 5. 更新菜单

`PUT /api/sys/menu`

**请求**：`SysMenuUpdateRequest`（id 必填）

**权限码**：`sys:menu:edit`

### 6. 删除菜单

`DELETE /api/sys/menu/{id}`

**校验**：
- 关键能力菜单（`sys:system:view` / `sys:user:list` / `sys:role:list` / `sys:menu:list` / `sys:dept:list`）不可删
- 存在子菜单时拒绝
- 存在角色绑定时拒绝

sys_menu 表**无 is_deleted 字段**，走物理删除。

**权限码**：`sys:menu:edit`

## menuType 取值

| 取值 | 含义 | path | component | perms |
| :--- | :--- | :--- | :--- | :--- |
| M | 目录 | 必填 | 可空 | 必空 |
| C | 菜单 | 必填 | 必填 | 可空 |
| F | 按钮 | 可空 | 可空 | 必填 |

## 业务规则备注

- **关键能力菜单保护**：阶段六新增的 5 个核心能力菜单（管理入口 + 4 个子模块入口）不可删，否则破坏系统引导
- **sys_menu 无 is_deleted**：与 sys_user / sys_role / sys_dept 不同，`sys_menu` 表结构不包含 `is_deleted` 字段（阶段一定义如此），删除走物理删
- **perms 唯一**：新增菜单的 perms 字段在 sys_menu 表内应当唯一（DB 无 UNIQUE 索引，依赖后端业务校验）

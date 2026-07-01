# 登录鉴权接口

> 控制器：`com.crm.controller.AuthController`
> 路径前缀：`/api/auth`（`application.yml` 的 `context-path: /api`）
> 鉴权：`/login` 与 `/logout` 公开；`/me` 需登录

## 接口清单

| 路径 | 方法 | 鉴权 | 说明 |
| :--- | :--- | :--- | :--- |
| `/api/auth/login` | POST | 公开 | 账号密码登录 |
| `/api/auth/logout` | POST | 公开（幂等） | 注销当前 token |
| `/api/auth/me` | GET | 需登录 | 读取当前用户信息（来自 tokenSession，0 DB 命中） |

---

## 1. 登录

### 基本信息

- **路径**：`POST /api/auth/login`
- **鉴权**：公开（`@SaIgnore`，已加入 `SaTokenConfig` 排除名单）
- **权限码**：无

### 请求参数（body）

| 字段 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :--- | :--- | :--- |
| `username` | string | 是 | 账号，2-30 字符 | `admin` |
| `password` | string | 是 | 密码，6-64 字符（明文，传输走 HTTPS） | `123456` |
| `remember` | boolean | 否 | 记住我，开启后 token 有效期延长到 90 天，默认 30 天 | `true` |

### 响应参数（data）

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `token` | string | Sa-Token 字符串，前端存入 `localStorage`，请求时放入 `Authorization` header |
| `userId` | number | 用户 ID |
| `username` | string | 账号 |
| `nickname` | string | 昵称（来自 `sys_user.nickname`） |
| `deptId` | number | 部门 ID，可空 |
| `roleKeys` | string[] | 角色 key 列表，如 `["admin"]` |
| `dataScope` | number | 有效数据范围：1 全部 / 3 本部门组 / 5 仅本人（phase8 commit1 拆档，4 已废弃） |
| `permissions` | string[] | 功能权限码列表，如 `["crm:customer:list", "crm:customer:edit"]` |

### 业务码

| code | 含义 | 处理建议 |
| :--- | :--- | :--- |
| 200 | 登录成功 | 取 `data.token` 存 localStorage，跳首页 |
| 2002 | 用户名或密码错误 | 提示文案统一，避免账号枚举 |
| 2001 | 账号已停用 | 联系管理员 |
| 403 | 账号未分配角色 | 联系管理员 |
| 1001 | 参数校验失败（账号或密码为空 / 长度不符） | 校验前端表单 |

### 调用示例

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456",
    "remember": true
  }'
```

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "5f8e9d2c-1234-5678-90ab-cdef01234567",
    "userId": 1,
    "username": "admin",
    "nickname": "超级管理员",
    "deptId": 1,
    "roleKeys": ["admin"],
    "dataScope": 1,
    "permissions": [
      "crm:customer:list",
      "crm:customer:edit",
      "crm:lead:list",
      "crm:opportunity:list",
      "crm:contract:list"
    ]
  }
}
```

### 登录流程说明

1. 按 `username` 查 `sys_user`（仅 status=1 且未逻辑删除）
2. `BCrypt.matches(rawPwd, user.password)` 验密
3. 校验 `status != 0`
4. 一次性查该用户的角色（`sys_role` JOIN `sys_user_role`）+ 功能权限码（`sys_menu.perms` 去重）
5. 计算有效 `dataScope`（多角色取最宽，数值最小）
6. `StpUtil.login(userId, timeout)` 写入 Sa-Token
7. 把 `userId / username / nickname / deptId / roleKeys / dataScope / perms` 一次性写入 `tokenSession`
8. 返回 `token`

后续每次请求 `StpInterfaceImpl#getPermissionList` 与 `CrmDataPermissionHandler` 都直接从 `tokenSession` 读，**0 次 DB 命中**。

### 失效策略

默认不主动失效。管理员改了角色后，受影响用户**下次重新登录**才生效。靠 token 过期（默认 30 天 / rememberMe 90 天）自然重登。如未来需要立即生效再加 `sys_user.role_version` 字段做版本比对。

---

## 2. 登出

### 基本信息

- **路径**：`POST /api/auth/logout`
- **鉴权**：公开（幂等，未登录调用不会报错）
- **权限码**：无

### 请求参数

无。

### 响应参数

无 data。返回 `code: 200, message: "操作成功"`。

### 业务码

| code | 含义 | 处理建议 |
| :--- | :--- | :--- |
| 200 | 登出成功 | 清空 localStorage 中的 token，跳登录页 |

### 调用示例

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: 5f8e9d2c-1234-5678-90ab-cdef01234567"
```

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

## 3. 当前用户信息

### 基本信息

- **路径**：`GET /api/auth/me`
- **鉴权**：需登录（`Sa-Token` 拦截器自动校验 token）
- **权限码**：无（任何登录用户都可调用）
- **数据来源**：`tokenSession`，**0 DB 命中**

### 请求参数

无。

### 响应参数（data）

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `userId` | number | 用户 ID |
| `username` | string | 账号 |
| `nickname` | string | 昵称 |
| `deptId` | number | 部门 ID |
| `roleKeys` | string[] | 角色 key 列表 |
| `dataScope` | number | 有效数据范围 |
| `permissions` | string[] | 功能权限码列表 |

> 与登录响应 `data` 结构基本一致，但**不含 token 字段**（token 仅在登录时返回一次）。

### 业务码

| code | 含义 | 处理建议 |
| :--- | :--- | :--- |
| 200 | 成功 | 用返回数据恢复前端 store 状态 |
| 401 | 未登录或 token 已过期 | 跳登录页 |

### 调用示例

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: 5f8e9d2c-1234-5678-90ab-cdef01234567"
```

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "超级管理员",
    "deptId": 1,
    "roleKeys": ["admin"],
    "dataScope": 1,
    "permissions": [
      "crm:customer:list",
      "crm:customer:edit"
    ]
  }
}
```

### 典型使用场景

- 页面刷新后用 `/me` 恢复 Pinia 中的用户信息，无需让用户重新登录
- 前端路由守卫校验登录态：未登录跳登录页，已登录但 store 为空则调 `/me` 填充
- 拿到 `permissions` 列表后做按钮级权限控制（前端组件按权限码显隐）

---

## 前端调用样例（Vue 3 + Axios）

```js
import axios from '@/utils/request'

// 登录
export const login = (data) => axios.post('/api/auth/login', data)

// 登出
export const logout = () => axios.post('/api/auth/logout')

// 当前用户
export const me = () => axios.get('/api/auth/me')
```

Pinia store 用法示例：

```js
// stores/auth.js
import { defineStore } from 'pinia'
import { login as apiLogin, me as apiMe, logout as apiLogout } from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null,
    permissions: [],
    roleKeys: [],
    dataScope: 5
  }),
  actions: {
    async login(payload) {
      const { data } = await apiLogin(payload)
      this.token = data.token
      this.user = { userId: data.userId, username: data.username, nickname: data.nickname, deptId: data.deptId }
      this.roleKeys = data.roleKeys
      this.permissions = data.permissions
      this.dataScope = data.dataScope
      localStorage.setItem('token', data.token)
    },
    async fetchMe() {
      const { data } = await apiMe()
      this.user = { userId: data.userId, username: data.username, nickname: data.nickname, deptId: data.deptId }
      this.roleKeys = data.roleKeys
      this.permissions = data.permissions
      this.dataScope = data.dataScope
    },
    async logout() {
      await apiLogout()
      this.reset()
    },
    reset() {
      this.token = ''
      this.user = null
      this.permissions = []
      this.roleKeys = []
      this.dataScope = 5
      localStorage.removeItem('token')
    }
  }
})
```

## 相关文件

- 控制器：`backend/src/main/java/com/crm/controller/AuthController.java`
- 服务：`backend/src/main/java/com/crm/service/AuthService.java`
- 用户上下文：`backend/src/main/java/com/crm/common/UserContext.java`
- Sa-Token 鉴权接口实现：`backend/src/main/java/com/crm/service/StpInterfaceImpl.java`
- 数据权限拦截器：`backend/src/main/java/com/crm/config/CrmDataPermissionHandler.java`
- 会话键常量：`backend/src/main/java/com/crm/common/SessionKeys.java`
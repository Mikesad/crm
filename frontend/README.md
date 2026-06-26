# 智能企业 CRM 系统 - 前端

## 技术栈

- **Vue 3.4** + **Vite 5** + **Composition API** + `<script setup>`
- **Element Plus 2.7** - 桌面端 UI 框架
- **Pinia 2** - 状态管理（持久化）
- **Vue Router 4** - 路由
- **Axios** - HTTP 客户端（已封装 Sa-Token 拦截器）
- **ECharts 5** - 图表（销售漏斗、业绩趋势）
- **Sass** - 样式预编译

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务（默认 http://localhost:5173）
npm run dev

# 打包生产
npm run build
```

## 目录结构

```
src/
├── api/             # 接口封装（按业务模块拆分）
├── layout/          # 全局布局（侧边栏 / 顶栏）
├── router/          # 路由配置
├── store/           # Pinia 状态（user / app）
├── styles/          # 全局样式（变量 / reset / 进度条）
├── utils/           # 工具类（request.js 等）
├── views/           # 页面（按业务模块组织）
│   ├── login/       # 登录页
│   ├── dashboard/   # 首页数据看板
│   ├── lead/        # 线索管理
│   ├── customer/    # 客户管理（私海 + 公海）
│   ├── business/    # 商机管理（漏斗）
│   ├── contract/    # 合同与回款
│   ├── system/      # 系统管理（用户/角色/菜单/部门）
│   └── error/       # 错误页 404
├── App.vue
└── main.js
```

## 默认账号

> 等待后端就绪后即可登录

| 角色 | 账号 | 密码 |
| :--- | :--- | :--- |
| 系统管理员 | admin | 123456 |

## 后端代理

开发模式下 Vite 已配置代理：

- 前端请求 `/api/**` → `http://localhost:8080`
- 可通过根目录 `.env.development` 中的 `VITE_API_BASE` 调整

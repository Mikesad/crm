# 🌲 ZenCRM — 智能企业 CRM（B2B）

**线索 → 客户 → 商机 → 合同 → 回款** 全生命周期的轻量级企业级 CRM 系统。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.7-409EFF?logo=element&logoColor=white)](https://element-plus.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/license-MIT-166534)](#许可证)


</div>

## ✨ 项目简介

**ZenCRM** 是一套面向 B2B 业务场景的智能企业 CRM 系统，从最前端的**线索录入**到最终的**回款核销**形成完整业务闭环。系统采用前后端分离架构：后端基于 Spring Boot 3 + Sa-Token + MyBatis-Plus，前端基于 Vue 3 + Element Plus + ECharts，开箱即用、单体部署友好，适合作为企业销售管理数字化基座，或作为 Java 全栈学习项目参考。

> 整套系统已经过 8 个开发阶段、累计 18 个 Controller + 31 个前端视图 + 13 份 SQL 迁移脚本的端到端验证。

项目截图
<img width="1920" height="906" alt="image" src="https://github.com/user-attachments/assets/04576996-78cb-48fd-9c35-151da0524b8b" />


## 🎯 核心亮点

| 🧩 能力         | 📌 说明                                                       |
| -------------- | ------------------------------------------------------------ |
| 🔐 细粒度权限   | 基于 Sa-Token + `@SaCheckPermission`，支持**功能权限**（菜单/按钮）与**数据权限**（全部/本部门/本人） |
| 🧮 金额零误差   | 全部使用 `BigDecimal` 存储，后端用合同明细反推重算  |
| ⏰ 公海自动回收 | `@Scheduled` 定时扫描，`last_follow_time` 超过 15 天自动回收到公海池，零人工介入 |
| 📊 报表中心     | 4 大 Tab（客户/商机/业绩/财务）+ N 张 ECharts 图 |
| 🪪 跟进中心     | 跨实体（客户/商机/合同/线索）跟进记录聚合，支持全部/我的/逾期/搜索多维度 |
| 🔄 业务闭环联动 | Spring `ApplicationEventPublisher` 解耦，回款录 → 合同实收/计划自动更新 |

## 🧱 功能矩阵

<details>
<summary><b>📥 线索 / 客户 / 联系人</b></summary>


- 线索录入 → 查重 → 一键转客户（双写事务，原线索 `status=3`）
- 客户详情时间轴：基本信息 + 联系人 + 跟进 + 商机 + 合同 + 回款六合一
- 客户公海池：自动回收 + 主动领取/释放 + 批量转移
- 客户共享：基于 `crm_customer_share` 的多对多协作，权限穿透
- 联系人独立维护，与客户多对一

</details>

<details>
<summary><b>💼 商机 / 合同 / 回款</b></summary>


- 商机阶段强校验：需求分析 → 方案报价 → 商务谈判 → 赢单/输单，**单向流转**
- 合同金额重算：明细行汇总与 `total_amount` 校对，差额 > 0.01 直接拒绝
- 折扣阈值：< 8.5 折触发审批流（`crm_approval`），财务总监可在审批中心处理
- 回款计划：合同下手动添加期次，支持部分核销
- 回款核销：财务录入 → Spring 事件 → 合同实收累加 + 计划状态机自动推进

</details>

<details>
<summary><b>📊 跟进中心 / 报表中心</b></summary>


- **跟进中心**：跨 4 实体聚合，Tab 切换全部 / 我的 / 逾期 / 搜索
- **报表中心**：
  - 客户分析（活跃度、跟进分布）
  - 商机分析（漏斗、阶段停留）
  - 业绩分析（销售排行、回款达成率）
  - 财务分析（应收/逾期/坏账预警）

</details>

<details>
<summary><b>⚙️ 系统设置 / 产品管理 / 部门管理</b></summary>


- 用户 / 角色 / 菜单 三位一体 RBAC
- 部门管理：Variant B 完整树形 + 级联选择 + 删除保护
- 产品管理：基础信息 + 版本 + 计费周期（中度 SaaS 化）
- 产品分类：树形维护，支撑合同明细下拉

</details>
## 🛠 技术栈

**后端**

| 技术         | 版本  | 用途                                   |
| ------------ | ----- | -------------------------------------- |
| Spring Boot  | 3.2.5 | Web 框架 / 自动装配                    |
| JDK          | 17    | LTS 运行时                             |
| Sa-Token     | 1.37  | 鉴权（**本地内存模式**，不依赖 Redis） |
| MyBatis-Plus | 3.5.5 | ORM / 分页 / 逻辑删除 / 乐观锁         |
| MySQL        | 8.0   | 主存储                                 |
| Knife4j      | 4.5   | OpenAPI 文档（`/api/doc.html`）        |
| Lombok       | -     | 简化样板代码                           |

**前端**

| 技术         | 版本 | 用途                                    |
| ------------ | ---- | --------------------------------------- |
| Vue          | 3.4  | `<script setup>` Composition API        |
| Vite         | 5    | 构建 / HMR                              |
| Element Plus | 2.7  | UI 组件库（按需自动引入）               |
| Pinia        | 2    | 状态管理（持久化插件）                  |
| Vue Router   | 4    | 路由 + 权限元信息                       |
| ECharts      | 5    | 报表图表                                |
| Axios        | -    | HTTP 客户端（统一 `/utils/request.js`） |
| dayjs        | -    | 日期处理                                |


## 🚀 快速部署

> 整套系统**完全本地化**：Sa-Token 走内存模式，无需 Redis / Nacos / ES 等中间件。  
> 一台能跑 MySQL + JDK 17 + Node 18 的电脑即可拉起全栈。

### 1️⃣ 环境要求

| 组件    | 最低版本 | 备注                                      |
| ------- | -------- | ----------------------------------------- |
| MySQL   | 8.0+     | 默认账号 `root / 123456`，数据库 `crm_db` |
| JDK     | 17+      | 推荐 Eclipse Temurin / OpenJDK 17         |
| Maven   | 3.8+     | 仅后端需要                                |
| Node.js | 18+      | 推荐 20 LTS，仅前端需要                   |

### 1️⃣ 环境要求

| 组件    | 最低版本 | 备注                                      |
| ------- | -------- | ----------------------------------------- |
| MySQL   | 8.0+     | 默认账号 `root / 123456`，数据库 `crm_db` |
| JDK     | 17+      | 推荐 Eclipse Temurin / OpenJDK 17         |
| Maven   | 3.8+     | 仅后端需要                                |
| Node.js | 18+      | 推荐 20 LTS，仅前端需要                   |

### 2️⃣ 初始化数据库

```bash
# 全新安装（含建表 + 种子数据）
mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS crm_db DEFAULT CHARSET utf8mb4;"
mysql -u root -p123456 crm_db < sql/crm_full.sql

# （可选）导入演示数据：60+ 客户、46 合同、600 跟进、13 共享记录
mysql -u root -p123456 crm_db < sql/demo-data-full.sql
```

> ⚠️ 默认密码 `123456` 与 `application.yml` 对齐。生产环境请通过环境变量覆盖。

### 3️⃣ 启动后端

```bash
cd backend

# 方式 A：开发模式（推荐，热加载）
mvn spring-boot:run

# 方式 B：打包后运行
mvn clean package -DskipTests
java -jar target/crm-backend.jar
```

启动成功后会看到：

```
Tomcat started on port(s): 8080 (http) with context path '/api'
Knife4j 文档: http://localhost:8080/api/doc.html
```

### 4️⃣ 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 **http://localhost:5173** 即可。开发服务器已配置 `/api` 代理到 `:8080`。

5️⃣ 生产打包
# 后端产物：backend/target/crm-backend.jar
mvn clean package -DskipTests

# 前端产物：frontend/dist/（Nginx 静态托管即可）
npm run build

## 👤 默认账号

| 账号                      | 角色       | 密码     | 数据范围        |
| ------------------------- | ---------- | -------- | --------------- |
| `admin`                   | 系统管理员 | `123456` | 全部            |
| `director`                | 销售总监   | `123456` | 本部门          |
| `lead_wang`               | 销售主管   | `123456` | 本部门          |
| `sales_li` / `sales_chen` | 普通销售   | `123456` | 仅本人          |
| `finance`                 | 财务人员   | `123456` | 合同 / 财务全部 |

> 密码为 BCrypt 加密值，所有种子账号均可用明文 `123456` 登录。  
> ⚠️ **生产环境请第一时间修改默认密码。**
>
> ## 🤝 贡献指南

欢迎 PR / Issue！提交前请：

1. 阅读 [`CLAUDE.md`](./CLAUDE.md) 中的接口文档维护约定与 SQL Schema 维护约定
2. 新增 / 修改后端接口时同步更新 [`docs/api/`](./docs/api) 对应 Markdown
3. 新增 / 修改业务表时同步更新 `sql/crm_full.sql` + `sql/migrations/`
4. 提交信息遵循 `feat(phaseN): xxx` / `fix(phaseN): xxx` 规范

```bash
# 拉代码
git clone https://github.com/<your-org>/ZenCRM.git

# 创建分支
git checkout -b feat/phase9-wechat-integration

# 提交
git commit -m "feat(phase9): 接入企业微信回调"

# 推送并发起 PR
git push origin feat/phase9-wechat-integration
```

---

## 📄 许可证

本项目基于 **MIT License** 开源，可自由用于商业 / 学习 / 二次开发，但请保留原作者署名。

---

## 🙏 致谢

- [Sa-Token](https://sa-token.cc/) — 简洁易用的国产鉴权框架
- [MyBatis-Plus](https://baomidou.com/) — 让 MyBatis 增删改查如丝般顺滑
- [Element Plus](https://element-plus.org/) — 颜值在线的 Vue 3 组件库
- [ECharts](https://echarts.apache.org/) — 百度出品的数据可视化库

---

<div align="center">


如果这个项目对你有帮助，欢迎 ⭐ Star 支持一下！  
有问题或建议请提 [Issue](../../issues) 或 [PR](../../pulls)。

<sub>Built with ❤️ by ZenCRM Team · 2026</sub>

</div>

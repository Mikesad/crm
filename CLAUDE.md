# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

智能企业 CRM 系统（B2B 方向），覆盖线索 → 客户 → 商机 → 合同 → 回款全生命周期。
需求细节见 `PRD.md`，开发计划与阶段划分见 `开发计划.md`，。原始脚本 `sql/crm.sql` ，初始化请使用 `sql/crm_full.sql`。

## 设计资源

- **原型 UI 图片**：`frontend-design/UI/`（设计参考,实现前端时按此对齐视觉风格）
  - `理想效果.png` — 整体理想效果参考
  - `登录界面.png` — 登录页视觉参考
  - `颜色效果.png` — 品牌色板参考
- **HTML 交互原型**：`frontend-design/` 根目录下的 `.html` 文件（如 `phase3-preview.html` / `dashboard-variant-*.html`），可直接浏览器打开看交互与布局

## 技术栈

**后端**（`backend/`）
- Spring Boot 3.2.5 + JDK 17 + Maven
- Sa-Token 1.37（**本地内存模式**，单机部署，不依赖 Redis）
- MyBatis-Plus 3.5.5（自带分页/乐观锁/防全表更新插件）
- MySQL 8.0
- Knife4j 4.5（OpenAPI 文档，访问 `http://localhost:8080/api/doc.html`）

**前端**（`frontend/`）
- Vue 3.4 + Vite 5 + `<script setup>` + Pinia 2（持久化）
- Element Plus 2.7 + Vue Router 4 + Axios + ECharts 5

## 常用命令

### 后端
```bash
cd backend
# 编译
mvn clean compile
# 运行（监听 8080）
mvn spring-boot:run
# 打包
mvn clean package -DskipTests
# 运行 jar
java -jar target/crm-backend.jar
```

### 前端
```bash
cd frontend
npm install            # 安装依赖
npm run dev            # 开发服务（http://localhost:5173，已代理 /api → :8080）
npm run build          # 打包到 dist/
```

### 数据库
```bash
mysql -u root -p < sql/crm_full.sql
```

## 架构要点

### 包结构
```
backend/src/main/java/com/crm/
├── CrmApplication.java          # 启动类，启用 @EnableScheduling
├── common/
│   ├── result/                  # 统一返回 Result / ResultCode
│   └── exception/               # BusinessException + GlobalExceptionHandler
└── config/                      # Sa-Token、MyBatis-Plus、Knife4j 配置
```

后续业务包应按此规约组织：`controller / service / mapper / entity / dto / vo`。

### 核心设计约束（来自 PRD §5）

1. **逻辑删除**：客户、联系人、商机、合同表均含 `is_deleted` 字段，MyBatis-Plus 已配置全局逻辑删除（删除值=1，正常值=0），删除即更新，前台所有"删除"按钮实际走 UPDATE。
2. **金额精度**：所有金额字段使用 `BigDecimal`，禁止浮点。前端传来的 `total_amount` 必须后端用明细重算校验。
3. **数据权限**：MyBatis-Plus 自定义 `DataPermissionHandler`（**待实现**），通过 `StpUtil.getLoginId()` 取当前用户，拼接 `WHERE owner_user_id = ...`（data_scope=5 仅本人）/`IN (本部门用户)`（=3）等。
4. **功能权限**：Sa-Token 的 `@SaCheckPermission("crm:customer:list")` 注解鉴权，需实现 `StpInterface#getPermissionList`，从 `sys_user_role` + `sys_role_menu` 加载。

### 角色与数据范围（PRD §4.1.2）

| 角色 | role_key | data_scope |
| :--- | :--- | :--- |
| 系统管理员 | `admin` | 1 全部 |
| 销售总监 | `sales_director` | 4 本部门及以下 |
| 销售主管 | `sales_lead` | 3 本部门 |
| 普通销售 | `sales` | 5 仅本人 |
| 财务人员 | `finance` | 1 全部（仅合同/财务） |

### 业务流程关键点

- **线索转客户**（开发计划 §阶段二）：`@Transactional` 内双写 `crm_customer` + `crm_contact`，原线索 `status=3`（已转客户）。
- **公海池回收**：启动类已开启 `@EnableScheduling`，需编写 `@Scheduled(cron="0 0 2 * * ?")`，扫描 `last_follow_time` 超过 15 天的客户置 `is_public=1`、`owner_user_id=NULL`。
- **商机赢单**：返回特定标识给前端，引导跳转新建合同页（商机阶段需强校验"需求分析 → 方案报价 → 商务谈判 → 赢单/输单"单向流转）。
- **回款核销**：财务录入 `crm_receivable` 后通过 Spring `ApplicationEventPublisher` 发事件，合同服务监听后累加实收并更新计划 / 合同状态。
- **客户共享**：`crm_customer_share` 表，权限穿透要回头扩展阶段一的数据权限拦截器（`OR id IN (SELECT customer_id FROM crm_customer_share WHERE user_id=?)`）。

### 前端约定

- 所有金额/日期展示用 `dayjs`；图表统一用 `vue-echarts`。
- HTTP 请求统一走 `src/utils/request.js`，已自动附加 `Authorization`（Sa-Token token），401 自动跳登录。
- Pinia store 默认开启 `pinia-plugin-persistedstate`（token 等存 localStorage）。
- 路由 `meta.permissions` / `meta.roles` 字段供后续指令式权限校验使用，目前路由未做硬拦截（依赖后端注解）。

## 接口文档维护约定（强制）

> **每次新增 / 修改任何后端 HTTP 接口时，必须同步维护接口文档，供前端对接使用，不得遗漏。**

- **文档位置**：`docs/api/<模块名>.md`，按业务模块划分文件（如 `auth.md`、`customer.md`、`contract.md`）。
- **索引文件**：`docs/api/README.md` 汇总所有接口的入口路径、负责人、最近更新时间。
- **每个接口必须包含**：
  1. **基本信息**：接口名称、请求方式（GET/POST/PUT/DELETE）、路径、是否需要登录（Sa-Token）、所需权限码（如 `crm:customer:list`）。
  2. **请求参数**：以表格列出字段名、类型、是否必填、说明、示例值；包含 `query` / `path` / `body` 三类来源；如为分页接口需列出分页参数（`pageNum`/`pageSize`）。
  3. **响应参数**：以表格列出字段名、类型、说明；明确统一响应结构 `Result<T>` 的 `code` / `msg` / `data` 含义。
  4. **业务码说明**：列出该接口可能返回的业务异常码（来自 `ResultCode`）。
  5. **调用示例**：提供 `curl` 命令与一份最小化的请求 / 响应 JSON 样例。
- **更新触发点**（满足任一即更新文档）：
  - 新增 Controller 里的任何 `@GetMapping` / `@PostMapping` 等注解方法。
  - 修改现有接口的入参、出参、权限注解或业务逻辑导致响应结构变化。
  - 新增 / 修改 `dto` 或 `vo` 类字段。
- **自动化辅助**：每个 Controller 类使用 Knife4j `@Tag` / `@Operation` 注解，文档作为 Knife4j 输出的 Markdown 镜像；如有差异以 `docs/api/` 下的 Markdown 为准。

## 数据库 Schema 维护约定（强制）

> **每次新增 / 修改任何业务表时，必须同时更新两份 SQL 脚本 + 一份字段约定，确保新装与升级路径都不破。**

- **两份 SQL 脚本（必须同步）**：
  1. **`sql/crm_full.sql`**（项目根 `sql/` 目录）：全新安装用的完整脚本，含建表 + 种子数据。任何表结构变更（新建表 / 加字段 / 改字段类型）必须同步更新，否则新装的 DB 会缺字段。
  2. **`sql/migrations/phase<N>-<功能>.sql`**：增量迁移脚本，给已按老版本初始化过 DB 的环境用。
- **MyBatis-Plus 实体字段规范（避免再被坑）**：
  - 所有业务表（除 `crm_record` / `crm_receivable` 这类 append-only 表）必须包含以下 4 个审计字段：
    ```sql
    create_by   VARCHAR(64) DEFAULT ''        -- 创建者(username)
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP
    update_by   VARCHAR(64) DEFAULT ''
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ```
  - 凡是涉及逻辑删除的表（客户/联系人/商机/合同/产品/回款计划）必须含 `is_deleted TINYINT DEFAULT 0`。
  - **阶段三踩过的坑**：`crm_product` 表原 SQL 漏写 `create_by/update_by/update_time`，跑接口时报 `Unknown column 'create_by'`，必须补。
- **迁移脚本写法（兼容 MySQL 5.5+，避免 `ADD COLUMN IF NOT EXISTS` 8.0.29+ 限制）**：
  ```sql
  -- 工具存储过程：按列名幂等加列
  DROP PROCEDURE IF EXISTS phase<N>_add_col_if_missing;
  DELIMITER //
  CREATE PROCEDURE phase<N>_add_col_if_missing(
    IN  p_tbl VARCHAR(64),
    IN  p_col VARCHAR(64),
    IN  p_def TEXT
  )
  BEGIN
    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME   = p_tbl
         AND COLUMN_NAME  = p_col
    ) THEN
      SET @ddl = CONCAT('ALTER TABLE ', p_tbl, ' ADD COLUMN ', p_col, ' ', p_def);
      PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
  END //
  DELIMITER ;

  CALL phase<N>_add_col_if_missing('table_name', 'col_name', 'TYPE DEFAULT ...');
  -- 最后 DROP PROCEDURE phase<N>_add_col_if_missing;
  ```
- **新表 / 新菜单 / 新权限**：均要在 `sql/crm_full.sql` + `sql/migrations/phase<N>-*.sql` 里**同时**加，且 `sys_role_menu` 绑定也要在两处同步（迁移用 `DELETE + INSERT` 模式 + `WHERE perms IN (...)` 过滤器实现幂等）。
- **验证流程**：每次改 SQL 后必须实测两遍：
  ```bash
  # 1) 全新安装
  mysql -u root -p123456 -e "DROP DATABASE IF EXISTS crm_db; CREATE DATABASE crm_db;"
  mysql -u root -p123456 crm_db < sql/crm_full.sql

  # 2) 增量迁移(模拟已运行老版本)
  # 先在 crm_db 里手工跑旧版 SQL,再跑 phase<N>-*.sql,跑两遍验证幂等
  mysql -u root -p123456 crm_db < sql/migrations/phase<N>-*.sql  # 第一次
  mysql -u root -p123456 crm_db < sql/migrations/phase<N>-*.sql  # 第二次(应无错)
  ```

## 默认账号（来自 sql/crm_full.sql 种子数据）

| 账号 | 角色 | 密码 |
| :--- | :--- | :--- |
| `admin` | 系统管理员 | `123456` |
| `director` | 销售总监 | `123456` |
| `lead_wang` | 销售主管 | `123456` |
| `sales_li` / `sales_chen` | 普通销售 | `123456` |
| `finance` | 财务人员 | `123456` |

> 密码为 BCrypt 加密值，启动后用明文 `123456` 登录即可。

## 关键配置文件

- `backend/src/main/resources/application.yml` — 数据源、Sa-Token、MyBatis-Plus、Knife4j、`crm.customer.public-pool-days=15`、`crm.contract.discount-threshold=8.5`（折扣 < 8.5 折需总监审批）。
- `frontend/vite.config.js` — `/api` 代理到后端；按需引入 Element Plus（`unplugin-auto-import` + `unplugin-vue-components`）。
- `frontend/.env.development` — `VITE_API_PREFIX=/api`。

## 开发顺序参考

严格按 `开发计划.md` 阶段执行：
1. 阶段一：Sa-Token 登录 + StpInterface、MyBatis-Plus DataPermissionHandler 登录功能（**最优先**，业务代码前必须完成）
2. 阶段二：线索/客户/商机 CRUD + 跟进时间轴
3. 阶段三：合同金额校验 + 回款核销（Spring Event）
4. 阶段四：公海回收 Job + 客户共享 SQL 注入

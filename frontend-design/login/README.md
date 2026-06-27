# ZenCRM 登录页设计稿

> 阶段一交付物之一。三个独立的 HTML 静态稿，用于在 Vue 工程落地前对齐视觉方向与交互细节。
> 双击任一文件即可在浏览器中预览。

## 目录

- [三种风格对照](#三种风格对照)
- [设计 token 总览](#设计-token-总览)
- [公共规则](#公共规则)
- [移植到 Vue 的注意事项](#移植到-vue-的注意事项)
- [自检清单](#自检清单)
- [后续动作](#后续动作)

---

## 三种风格对照

| 维度 | `login-split.html` | `login-center.html` | `login-terminal.html` |
| :--- | :--- | :--- | :--- |
| **气质** | 分屏叙事，专业 B2B 旗舰感 | 极简卡片，效率优先 | 终端 / 命令行，硬核数据感 |
| **布局** | 50 / 50 双栏（左品牌 / 右表单） | 居中卡片，dot-grid 底 | 居中"终端窗口"，macOS chrome |
| **主色** | 深森林绿 `#166534` | 电光蓝 `#0066ff` | 琥珀 `#f59e0b` |
| **字体** | Inter + JetBrains Mono（数字） | Inter + JetBrains Mono | JetBrains Mono 全栈 |
| **圆角** | 6 px | 6 px / 卡片 8 px | 4 px |
| **数据装饰** | 左栏 `47,832 单 / ¥2.4B` 数据条 | 底部状态行 `api.zencrm.local 在线` | 启动序列 `$ zencrm auth init` |
| **快捷登录** | 三枚胶囊按钮 | 三枚胶囊按钮 | 三行 CLI 历史记录（带角色 tag） |
| **VARIANCE** | 5 | 4 | 6 |
| **MOTION** | 3 | 3 | 2 |
| **DENSITY** | 3 | 2 | 4 |
| **目标用户** | 决策层（销售总监 / 主管） | 全员默认入口 | 财务 / 数据重度用户 |

## 推荐场景

- **默认上线**：`login-center.html`。信息密度最低、首屏聚焦在表单、跨设备表现稳定，最符合"销售每日开机"的使用频率。
- **品牌官网带登录入口**：`login-split.html`。左侧叙事能强化"我们是谁 / 为什么选我们"，适合把登录页作为官网子页直接复用。
- **内网 / 财务 / 数据后台**：`login-terminal.html`。与 ECS / SSH 控制台气质同源，对重度用户更友好；不建议作为首次接触产品的访客入口。

> 三种风格互不冲突。线上建议主用 center，营销带登录的页面复用 split，财务模块登录走 terminal。

---

## 设计 token 总览

| Token | split | center | terminal |
| :--- | :--- | :--- | :--- |
| 背景 | `#fafaf9` / 左栏 `#f4f4f0` | `#fafafa` + dot-grid | `#0a0e14` + 1px scanline |
| 卡片 / 表面 | `#ffffff` | `#ffffff` | `#131720` / 输入 `#0e131c` |
| 主文字 | `#18181b` | `#18181b` | `#e5e7eb` |
| 次文字 | `#3f3f46` | `#3f3f46` | `#9ca3af` |
| 弱文字 | `#71717a` | `#71717a` | `#6b7280` |
| 描边 | `#e4e4e7` | `#e4e4e7` | `#1f2937` |
| 主色 | `#166534` | `#0066ff` | `#f59e0b` |
| 焦点环 | `rgba(24,24,27,.08)` | `rgba(0,102,255,.14)` | `rgba(245,158,11,.18)` |
| 圆角 | 6 px | 6 / 8 px | 4 px |
| 主字体 | Inter | Inter | JetBrains Mono |
| 等宽字体 | JetBrains Mono（数字） | JetBrains Mono（数字） | JetBrains Mono（全栈） |

> **形状一致性锁**：单一项目锁定一套圆角。三个 variant 用不同圆角仅因为它们是并列方案；正式接入时只取其中一套，**禁止 mix-and-match**（按钮圆角与卡片圆角不一致 = broken design）。

---

## 公共规则

所有 variant 共同遵守的硬性约束。任何后续迭代与 Vue 实现必须保持一致。

### 排版
- **禁止** em-dash（U+2014）与 en-dash（U+2013）（标题 / 标签 / 正文 / 引用 / 按钮 / alt）。全部用半角连字符 `-` 或句号替代。
- **禁止** Inter 默认独占；如必须用 Inter，需配合 JetBrains Mono 做数字点缀。
- **禁止** Fraunces / Instrument Serif 等典型 AI 衬线。
- **数字一律** `font-variant-numeric: tabular-nums`，防止位宽抖动。

### 色彩
- **禁止** 紫 / 蓝紫渐变。
- **禁止** `box-shadow: 0 0 30px rgba(蓝紫)` 之类的外发光。
- **每个 variant 只有一个 accent**，跨章节不允许切换。
- 所有正文文字与背景对比度 ≥ WCAG AA（4.5:1）。当前 token 已校验。

### 形状 / 圆角
- 单页面只允许一套圆角尺度（建议 center 的 6 px 作为统一标准）。
- 禁止卡片圆角 > 16 px。

### 交互
- **禁止** "Welcome back" / "Hello there" 类模板问候语。
- **禁止** emoji 充当图标。当前唯一 inline SVG 是"→"登录箭头（带 `aria-hidden`）。
- **禁止** div 拼接假截图 / 假仪表板做装饰。
- **禁止** 滚动提示文案（"Scroll to explore" 等）。
- 字段标签永远在上方；**禁止** placeholder-as-label。

### 文案
- 标题不超过两行。
- 引言正文 ≤ 3 行 + 明确署名（`<cite>` 用名，`role` 用公司职位）。
- 中文文案优先；只有 demo 账号 / 状态行保留英文，符合"工具感"。

---

## 移植到 Vue 的注意事项

阶段一落地时建议：

1. **目录**
   ```
   frontend/src/views/auth/
   ├── LoginSplitView.vue
   ├── LoginCenterView.vue     // 默认
   └── LoginTerminalView.vue
   ```
   路由 `/login` 默认指向 center，`?v=split` / `?v=terminal` 用于 A/B 切换。

2. **状态**
   - 表单值用 `reactive({ username, password, remember })`。
   - 提交动作走 Pinia `useAuthStore().login()`，调用 `POST /api/auth/login`（见 CLAUDE.md 接口文档约定，登录接口文档写在 `docs/api/auth.md`）。
   - 401 走全局拦截器（`src/utils/request.js` 已具备），自动跳回当前页并保留用户名。

3. **种子账号 quick-fill**
   - 当前 mock 里硬编码 `123456`。Vue 版应放在 `src/config/demo-accounts.ts` 数组，仅当 `import.meta.env.DEV` 时挂载，避免泄露到生产。
   - 推荐数据结构：`{ username, password, role, label }`。

4. **字体加载**
   - 不要再用 Google Fonts `<link>`。改用 `@font-face` 自托管 + `font-display: swap`。
   - Inter 与 JetBrains Mono 都可在 [rsms.me/inter](https://rsms.me/inter/) 与 [jetbrains.com/mono](https://www.jetbrains.com/lp/mono/) 取得可商用 woff2。

5. **图标**
   - 用 `@phosphor-icons/vue` 或 `@tabler/icons-vue` 替换当前的内联 SVG（仅"→"箭头一处）。
   - 一个项目只选一个图标库，**禁止** 混用。

6. **a11y**
   - 当前三个 variant 的 form 都已用 `<label for>` 关联，符合 WCAG AA。
   - 移植时给 `<input>` 加 `aria-invalid` + `aria-describedby`，绑定到字段级错误信息。
   - terminal variant 的闪烁光标已包 `prefers-reduced-motion: reduce`，保留。

7. **暗色 / 亮色切换**
   - split 与 center 当前仅亮色；如未来接入主题系统，先做 token 抽离（CSS variables 已经是 token 化的，迁移成本极低）。
   - terminal 已经是固定暗色，不参与主题切换。

---

## 自检清单

任一 variant 落地到 Vue 前，逐项确认：

- [ ] 全 HTML 中**无** em-dash（U+2014）/ en-dash（U+2013）
- [ ] 无 emoji 充当 UI 图标
- [ ] 无紫 / 蓝紫渐变与外发光
- [ ] 圆角 ≤ 8 px（terminal 4 / split·center 6）
- [ ] 表单字段标签在上方
- [ ] focus 状态有可见环（`box-shadow` 或 outline）
- [ ] 占位符不充当标签
- [ ] 数字 `font-variant-numeric: tabular-nums`
- [ ] 1440 px 桌面与 375 px 手机下均无横向滚动
- [ ] quick-fill 仅在 dev 环境挂载
- [ ] 提交按钮文案 ≤ 2 字 + 1 个动作图标
- [ ] 暗色版（如启用）通过 `prefers-color-scheme: dark` 与手动 toggle 双通道测试
- [ ] `prefers-reduced-motion: reduce` 时闪烁 / 自动滚动停止

---

## 后续动作

1. 评审三套稿的视觉方向（建议周三前）。
2. 确定 default 选型 → 在 `frontend/src/views/auth/` 落 Vue 模板。
3. 后端 `POST /api/auth/login` 接口完成后（阶段一），补一份 `docs/api/auth.md`，包含请求 / 响应字段、业务码、curl 示例。
4. 与设计 token 同步更新 `frontend/src/styles/tokens.scss` 或新增 `tokens.css`，避免组件里再写裸 hex。
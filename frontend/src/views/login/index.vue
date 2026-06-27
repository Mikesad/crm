<!--
  LoginSplitView.vue
  阶段一登录页 — split 分屏风格（与 frontend-design/login/login-split.html 视觉对齐）。

  设计 token 锁定（与 login.scss 中的 :root 变量对应）：
  - 背景 #fafaf9 / 左栏 #f4f4f0
  - 主文字 #18181b
  - 主色 深森林绿 #166534（NOT 紫渐变）
  - 圆角 6px
  - 字体 Inter + JetBrains Mono（数字）
  - 零 em-dash

  关键实现要点：
  - 表单使用原生 HTML（input / button / checkbox），不依赖 Element Plus，
    避免 :deep() + scoped :root 变量在 EP 组件中失效导致的样式不渲染。
  - 提交走 Pinia userStore.login(form)，与现有 auth.js 协议保持一致。
  - quick-fill 仅在 import.meta.env.DEV 挂载，避免泄露到生产。
-->
<template>
  <div class="crm-login-root">
    <main class="login-split">
      <!-- LEFT: 品牌叙事 -->
      <aside class="brand-panel" aria-label="品牌介绍">
        <a href="/" class="wordmark" aria-label="ZenCRM 首页">
          <span class="wordmark-glyph" aria-hidden="true" />
          <span>ZenCRM</span>
        </a>

        <div class="brand-content">
          <h1>
            把时间还给<span class="accent">客户</span>，<br />而不是表格。
          </h1>
          <p class="brand-subtitle">
            为认真做 ToB 的销售团队设计。从线索到回款，一个工作台走完。
          </p>

          <dl class="data-strip">
            <div>
              <dt>本季度成交</dt>
              <dd><span class="num">47,832</span><span class="unit">单</span></dd>
            </div>
            <div>
              <dt>管理中管线</dt>
              <dd><span class="num">¥2.4B</span><span class="unit">/ 1,284 客户</span></dd>
            </div>
          </dl>

          <blockquote class="quote">
            <p>"上线三个月，我们少开了四次周会，多签了六单。"</p>
            <p class="quote-attribution">
              <cite>李伟</cite><span class="role">, 销售总监, 瀚海工业</span>
            </p>
          </blockquote>
        </div>

        <p class="brand-footer">© 2026 ZenCRM · v1.0</p>
      </aside>

      <!-- RIGHT: 登录表单 -->
      <section class="form-panel" aria-label="登录表单">
        <div class="form-wrap">
          <header class="form-header">
            <span class="form-eyebrow">Team workspace</span>
            <h2>登录工作台</h2>
            <p>使用 crm_full.sql 种子账号即可体验。</p>
          </header>

          <form
            class="login-form"
            autocomplete="on"
            novalidate
            @submit.prevent="submit"
          >
            <div class="field">
              <label for="username" class="field-label">账号</label>
              <input
                id="username"
                v-model="form.username"
                type="text"
                name="username"
                autocomplete="username"
                placeholder="admin"
                required
              />
            </div>

            <div class="field">
              <label for="password" class="field-label">密码</label>
              <input
                id="password"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                name="password"
                autocomplete="current-password"
                placeholder="••••••"
                required
                @keyup.enter="submit"
              />
              <button
                type="button"
                class="toggle-eye"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <svg
                  v-if="!showPassword"
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"
                >
                  <path d="M1 8s2.5-5 7-5 7 5 7 5-2.5 5-7 5-7-5-7-5z" />
                  <circle cx="8" cy="8" r="2.5" />
                </svg>
                <svg
                  v-else
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"
                >
                  <path d="M2 2l12 12" />
                  <path d="M3.5 5.5C1.9 7 1 8 1 8s2.5 5 7 5c1.4 0 2.6-.4 3.6-1" />
                  <path d="M6.5 4.4C7 4.1 7.5 4 8 4c4.5 0 7 4 7 4s-.6.9-1.6 2" />
                </svg>
              </button>
            </div>

            <div v-if="errorMsg" class="form-error" role="alert">
              {{ errorMsg }}
            </div>

            <div class="field-row">
              <label class="checkbox">
                <input v-model="form.remember" type="checkbox" name="remember" />
                <span>记住我</span>
              </label>
              <a href="#" class="forgot">忘记密码？</a>
            </div>

            <button
              type="submit"
              class="btn-primary"
              :disabled="loading"
            >
              <span>{{ loading ? '登录中…' : '登录' }}</span>
              <svg
                v-if="!loading"
                class="arrow"
                width="14"
                height="14"
                viewBox="0 0 14 14"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M2 7h10M8 3l4 4-4 4" />
              </svg>
            </button>
          </form>

          <!-- quick-fill 仅 dev 环境挂载 -->
          <div v-if="showQuickFill" class="quick-fill">
            <span class="quick-fill-label">快速试用</span>
            <div class="quick-fill-pills">
              <button
                v-for="acc in demoAccounts"
                :key="acc.username"
                type="button"
                class="pill"
                @click="fillAccount(acc)"
              >
                {{ acc.username }}<span class="role-tag">{{ acc.roleLabel }}</span>
              </button>
            </div>
          </div>

          <p class="form-footer">还没有账号？<a href="#">联系商务</a></p>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { demoAccounts } from '@/config/demo-accounts'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const showPassword = ref(false)
const errorMsg = ref('')
const form = reactive({
  username: '',
  password: '',
  remember: false
})

// 仅 dev 环境显示 quick-fill，避免泄露到生产
const showQuickFill = computed(() => import.meta.env.DEV)

const fillAccount = (acc) => {
  form.username = acc.username
  form.password = acc.password
}

const submit = async () => {
  errorMsg.value = ''
  if (!form.username.trim()) {
    errorMsg.value = '请输入账号'
    return
  }
  if (!form.password) {
    errorMsg.value = '请输入密码'
    return
  }
  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
      remember: form.remember
    })
    // 优先跳到路由守卫携带的 redirect，其次 /dashboard
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect)
  } catch (e) {
    errorMsg.value = e?.message || '登录失败，请检查账号密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* === 布局：50/50 分屏 === */
.login-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100dvh;
}

/* === LEFT: Brand panel === */
.brand-panel {
  background: var(--crm-login-bg-warm);
  padding: 40px 56px;
  border-right: 1px solid var(--crm-login-hairline);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.wordmark {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: -0.01em;
  color: var(--crm-login-ink);
  text-decoration: none;
}
.wordmark-glyph {
  width: 14px;
  height: 14px;
  background: var(--crm-login-accent);
  border-radius: 2px;
  display: inline-block;
}

.brand-content {
  margin: auto 0;
  max-width: 480px;
}
.brand-content h1 {
  font-size: clamp(32px, 3.6vw, 44px);
  font-weight: 600;
  line-height: 1.08;
  letter-spacing: -0.025em;
  color: var(--crm-login-ink);
  margin: 0;
}
.brand-content h1 .accent { color: var(--crm-login-accent); }

.brand-subtitle {
  margin-top: 16px;
  font-size: 15px;
  line-height: 1.55;
  color: var(--crm-login-ink-soft);
  max-width: 36ch;
}

.data-strip {
  margin: 40px 0 0;
  padding-top: 24px;
  border-top: 1px solid var(--crm-login-hairline);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}
.data-strip dt {
  font-family: var(--crm-login-font-mono);
  font-size: 10.5px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--crm-login-muted);
  margin-bottom: 8px;
}
.data-strip dd {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 0;
}
.data-strip .num {
  font-family: var(--crm-login-font-mono);
  font-size: 24px;
  font-weight: 600;
  color: var(--crm-login-ink);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}
.data-strip .unit {
  font-size: 12px;
  color: var(--crm-login-muted);
}

.quote {
  margin: 40px 0 0;
  padding-top: 24px;
  border-top: 1px solid var(--crm-login-hairline);
}
.quote p {
  font-size: 14.5px;
  font-style: italic;
  line-height: 1.55;
  color: var(--crm-login-ink-soft);
  max-width: 38ch;
  margin: 0;
}
.quote cite {
  font-style: normal;
  color: var(--crm-login-ink);
  font-weight: 500;
}
.quote .role {
  color: var(--crm-login-muted);
  font-weight: 400;
}
.quote-attribution {
  margin-top: 6px !important;
}

.brand-footer {
  font-family: var(--crm-login-font-mono);
  font-size: 11px;
  color: var(--crm-login-muted);
  letter-spacing: 0.02em;
  margin: 0;
}

/* === RIGHT: Form panel === */
.form-panel {
  padding: 40px 56px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.form-wrap {
  width: 100%;
  max-width: var(--crm-login-max-form);
}

.form-eyebrow {
  font-family: var(--crm-login-font-mono);
  font-size: 10.5px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--crm-login-muted);
  display: block;
}
.form-header h2 {
  margin: 8px 0 0;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--crm-login-ink);
}
.form-header p {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--crm-login-muted);
}

.login-form {
  margin-top: 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  position: relative;
}
.field-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--crm-login-ink);
  margin-bottom: 6px;
}
.field input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  background: var(--crm-login-surface);
  border: 1px solid var(--crm-login-hairline);
  border-radius: var(--crm-login-radius);
  font-family: inherit;
  font-size: 14px;
  color: var(--crm-login-ink);
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.field input::placeholder { color: var(--crm-login-placeholder); }
.field input:hover { border-color: var(--crm-login-hairline-strong); }
.field input:focus {
  border-color: var(--crm-login-ink);
  box-shadow: 0 0 0 3px var(--crm-login-focus-ring);
}

/* 密码框右侧的眼睛按钮 */
.field .toggle-eye {
  position: absolute;
  right: 6px;
  top: 26px;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  color: var(--crm-login-muted);
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.12s ease, background 0.12s ease;
}
.field .toggle-eye:hover {
  color: var(--crm-login-ink);
  background: var(--crm-login-bg-warm);
}

.form-error {
  margin-top: -4px;
  font-size: 13px;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: var(--crm-login-radius);
  padding: 8px 12px;
}

.field-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  margin-top: -4px;
}
.checkbox {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--crm-login-ink-soft);
  cursor: pointer;
  user-select: none;
}
.checkbox input {
  width: 14px;
  height: 14px;
  accent-color: var(--crm-login-ink);
  cursor: pointer;
  margin: 0;
}
.forgot {
  color: var(--crm-login-ink-soft);
  text-decoration: none;
}
.forgot:hover {
  color: var(--crm-login-ink);
  text-decoration: underline;
}

.btn-primary {
  margin-top: 8px;
  width: 100%;
  height: 44px;
  padding: 0 18px;
  background: var(--crm-login-ink);
  color: var(--crm-login-surface);
  border: none;
  border-radius: var(--crm-login-radius);
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.08s ease, opacity 0.15s ease;
}
.btn-primary:hover:not(:disabled) { background: #000; }
.btn-primary:active:not(:disabled) { transform: translateY(1px); }
.btn-primary:disabled { opacity: 0.7; cursor: not-allowed; }
.btn-primary .arrow { transition: transform 0.15s ease; }
.btn-primary:hover:not(:disabled) .arrow { transform: translateX(2px); }

/* quick-fill 胶囊 */
.quick-fill {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--crm-login-hairline);
}
.quick-fill-label {
  display: block;
  font-family: var(--crm-login-font-mono);
  font-size: 10.5px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--crm-login-muted);
  margin-bottom: 10px;
}
.quick-fill-pills {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.pill {
  font-family: var(--crm-login-font-mono);
  font-size: 12px;
  padding: 6px 10px;
  background: transparent;
  border: 1px solid var(--crm-login-hairline);
  border-radius: var(--crm-login-radius);
  color: var(--crm-login-ink-soft);
  cursor: pointer;
  transition: all 0.12s ease;
}
.pill:hover {
  border-color: var(--crm-login-ink);
  color: var(--crm-login-ink);
  background: var(--crm-login-surface);
}
.pill .role-tag {
  color: var(--crm-login-muted);
  margin-left: 4px;
}
.pill:hover .role-tag { color: var(--crm-login-ink-soft); }

.form-footer {
  margin-top: 24px;
  font-size: 13px;
  color: var(--crm-login-muted);
  text-align: center;
}
.form-footer a {
  color: var(--crm-login-ink);
  text-decoration: none;
  font-weight: 500;
}
.form-footer a:hover { text-decoration: underline; }

/* === Mobile collapse === */
@media (max-width: 900px) {
  .login-split {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }
  .brand-panel {
    padding: 32px 24px;
    border-right: none;
    border-bottom: 1px solid var(--crm-login-hairline);
  }
  .brand-content { margin: 24px 0; }
  .brand-content h1 { font-size: 26px; }
  .data-strip {
    grid-template-columns: 1fr 1fr;
    gap: 20px;
    margin-top: 28px;
    padding-top: 20px;
  }
  .data-strip .num { font-size: 20px; }
  .quote { display: none; }
  .brand-footer { display: none; }
  .form-panel {
    padding: 32px 24px;
    align-items: flex-start;
  }
}
@media (max-width: 480px) {
  .data-strip { grid-template-columns: 1fr; }
}
</style>

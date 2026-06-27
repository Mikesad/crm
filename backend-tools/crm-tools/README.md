# crm-tools

CRM 项目本地工具集。**不进生产 jar**，仅供开发期本地使用。

## GeneratePasswordHash

BCrypt 密码哈希生成器，用于：
- 生成 `sql/crm_full.sql` 中种子账号的密码 hash
- 新增测试账号时快速生成 hash

### 用法

```bash
cd backend-tools/crm-tools
mvn exec:java -Dexec.args="123456"
# 输出：$2a$10$...  （每次 salt 不同，但都能 verify 通过）
```

多个密码一次性生成：

```bash
mvn exec:java -Dexec.args="123456 admin888 demo123"
# 输出 3 行，每行一个 hash
```

### 强度

默认 10，与后端 `PasswordEncoderConfig` 中 `new BCryptPasswordEncoder()` 一致。
强度由 BCrypt 内部决定（10 = 2^10 次迭代），无需配置。

### 关于 salt

BCrypt 每次 `encode()` 都随机生成 salt，所以同一明文每次输出不同 hash。
但 `matches(rawPassword, hash)` 总能正确验证。

这意味着：
- ✅ 种子 SQL 中 hash 是哪个版本无所谓，能 verify `123456` 就行
- ✅ 新增用户时随时 `encode("新密码")` 即可，不需要记住旧 salt
- ❌ 不要用「hash 比对」判断两个账号是否用了同一密码（永远不相等）

### SQL 中使用

```sql
INSERT INTO sys_user (username, password, ...) VALUES
  ('admin', '$2a$10$abcd...', ...);
```

字段长度 100 字符足够（BCrypt hash 通常 60 字符）。

---

## VerifyPasswordHash

验证一个 BCrypt hash 与指定明文是否匹配。

### 用法

```bash
# 验证任意 hash
mvn compile exec:java \
  -Dexec.mainClass=com.crm.tools.VerifyPasswordHash \
  -Dexec.args='$2a$10$... 123456'
# 最后一行为 OK 或 FAIL

# 自检 sql/crm_full.sql 中嵌入的种子 hash 是否能 verify "123456"
mvn compile exec:java \
  -Dexec.mainClass=com.crm.tools.VerifyPasswordHash \
  -Dexec.args='--all'
```

> 注：`-Dexec.mainClass=` 会覆盖 pom 中默认的 `GeneratePasswordHash`。
> 需要先 `mvn compile` 才能用 `exec:java`，否则会找不到新增的 class。

### 自检输出

```
$ mvn compile exec:java -Dexec.mainClass=com.crm.tools.VerifyPasswordHash -Dexec.args='--all'
...
seed hash matches "123456": OK
```

---

## 通用说明

两个工具都用 `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`，
与后端 `PasswordEncoderConfig` 中的 Bean 是同一个类。所以 SQL 里嵌入的 hash
一定能被后端 `matches()` 验证通过，无需担心版本不一致。
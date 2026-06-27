#!/usr/bin/env python3
"""
阶段四 联调冒烟脚本 — Python 版(规避 Windows bash GBK 编码问题)
"""
import json
import sys
import io
import urllib.request
import urllib.error
import subprocess

# Windows Python 默认 GBK,强制 UTF-8 输出
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

BASE = "http://localhost:8080/api"
results = []

def req(method, path, token=None, body=None):
    url = BASE + path
    data = None
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = token
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
    r = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(r, timeout=10) as resp:
            code = resp.getcode()
            text = resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        code = e.code
        text = e.read().decode("utf-8", errors="replace")
    try:
        return code, json.loads(text)
    except Exception:
        return code, text

def hr(s):
    print()
    print("-" * 60)
    print(">>", s)
    print("-" * 60)

def ok(label, cond, extra=""):
    mark = "[OK]" if cond else "[FAIL]"
    print(f"  {mark} {label}" + (f" -- {extra}" if extra else ""))
    results.append((label, cond, extra))

# 1) 登录 4 个角色
hr("1) 登录 admin / sales_li / sales_chen / finance")
TOK = {}
for u in ["admin", "sales_li", "sales_chen", "finance"]:
    code, d = req("POST", "/auth/login", body={"username": u, "password": "123456"})
    if code != 200 or "token" not in (d.get("data") or {}):
        print(f"  ✗ 登录 {u} 失败: code={code} resp={d}")
        sys.exit(1)
    TOK[u] = d["data"]["token"]
    print(f"  ✓ {u}: token 前 8 位 = {TOK[u][:8]}...")

# 2) sales_li 创建一个私海客户
hr("2) sales_li 新建客户")
code, d = req("POST", "/crm/customer", TOK["sales_li"],
              {"customerName": "测试共享客户 ACME", "industry": "IT", "level": "A"})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("新建客户成功", code == 200 and d.get("data"))
CID = d["data"]

# 2.1) 给客户写一条跟进
hr("2.1) sales_li 给客户写一条跟进")
code, d = req("POST", "/crm/record", TOK["sales_li"],
              {"relatedType": "customer", "relatedId": CID, "content": "首次接触,客户对SaaS方案感兴趣", "followType": "电话"})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("写入跟进成功", code == 200)

# 3) sales_li 把客户共享给 sales_chen (auth=2 读写)
hr("3) sales_li 共享给 sales_chen (读写 authType=2)")
code, d = req("POST", "/customer/share", TOK["sales_li"],
              {"customerId": CID, "userId": 5, "authType": 2})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("发起共享成功", code == 200 and d.get("data"))
SHARE_ID = d["data"]

# 4) sales_chen 查私海 — 应能看到
hr("4) sales_chen 查私海客户 — 应包含刚共享的")
code, d = req("GET", "/crm/customer/page?isPublic=0&pageSize=20", TOK["sales_chen"])
print(f"  HTTP={code}, total={d.get('data',{}).get('total')}")
seen = [r["id"] for r in d.get("data", {}).get("records", [])]
print(f"  records ids: {seen}")
ok(f"sales_chen 私海列表包含客户 {CID}", CID in seen, f"列表={seen}")

# 4.1) sales_chen 查"被共享给我的"
hr("4.1) sales_chen 查 '被共享给我的' Tab")
code, d = req("GET", "/crm/customer/page?sharedToMeOnly=1&pageSize=20", TOK["sales_chen"])
print(f"  HTTP={code}, total={d.get('data',{}).get('total')}")
seen = [r["id"] for r in d.get("data", {}).get("records", [])]
print(f"  records ids: {seen}")
ok(f"被共享列表包含客户 {CID}", CID in seen, f"列表={seen}")

# 5) sales_chen 改客户(读写应成功)
hr("5) sales_chen 修改客户(读写共享,应成功)")
code, d = req("PUT", "/crm/customer", TOK["sales_chen"],
              {"id": CID, "customerName": "ACME 已改(chen)", "level": "A"})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("读写共享可改客户", code == 200)

# 5.1) sales_chen 加一条跟进(读写应成功)
hr("5.1) sales_chen 加跟进(读写共享,应成功)")
code, d = req("POST", "/crm/record", TOK["sales_chen"],
              {"relatedType": "customer", "relatedId": CID, "content": "chen 跟进了", "followType": "微信"})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("读写共享可加跟进", code == 200)

# 6) sales_li 改共享为只读
hr("6) sales_li 把共享改为只读 (authType=1)")
code, d = req("POST", "/customer/share", TOK["sales_li"],
              {"customerId": CID, "userId": 5, "authType": 1})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("切换只读成功", code == 200)

# 7) sales_chen 再改(应被 1008 拒绝)
hr("7) sales_chen 再尝试改客户(只读共享,应被 1008 拒绝)")
code, d = req("PUT", "/crm/customer", TOK["sales_chen"],
              {"id": CID, "customerName": "chen 强改(应失败)"})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("只读共享改客户被拦截", code == 200 and d.get("code") == 1008,
   f"code={code} bizCode={d.get('code')} msg={d.get('message','')[:60]}")

# 7.1) sales_chen 再加跟进(也应被 1008 拒绝)
hr("7.1) sales_chen 再加跟进(只读共享,应被 1008 拒绝)")
code, d = req("POST", "/crm/record", TOK["sales_chen"],
              {"relatedType": "customer", "relatedId": CID, "content": "chen 强加", "followType": "电话"})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("只读共享加跟进也被拦截", code == 200 and d.get("code") == 1008,
   f"bizCode={d.get('code')}")

# 8) sales_li 撤销共享
hr("8) sales_li 撤销共享")
code, d = req("DELETE", f"/customer/share/{SHARE_ID}", TOK["sales_li"])
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("撤销成功", code == 200)

# 8.1) 撤销后 sales_chen 查不到了
hr("8.1) 撤销后 sales_chen 查被共享给我 — 应不包含")
code, d = req("GET", "/crm/customer/page?sharedToMeOnly=1&pageSize=20", TOK["sales_chen"])
seen = [r["id"] for r in d.get("data", {}).get("records", [])]
print(f"  records ids: {seen}")
ok(f"撤销后共享列表不包含 {CID}", CID not in seen)

# 9) 准备公海客户
hr("9) 改客户 lastFollowTime 为 30 天前 + admin dryRun 回收 (thresholdSeconds=10)")
sql = f"UPDATE crm_customer SET last_follow_time = DATE_SUB(NOW(), INTERVAL 30 DAY) WHERE id = {CID}"
subprocess.run(["mysql", "-u", "root", "-p123456", "crm_db", "-e", sql], capture_output=True)

code, d = req("POST", "/customer/public-pool/recycle", TOK["admin"],
              {"thresholdSeconds": 10, "limit": 100, "dryRun": True})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:300]}")
ok("admin dryRun 成功", code == 200 and d.get("data", {}).get("dryRun") == True,
   f"scanned={d.get('data',{}).get('scanned')} recycled={d.get('data',{}).get('recycled')}")

# 9.1) 真回收
hr("9.1) admin 真回收 (dryRun=false, thresholdSeconds=10)")
code, d = req("POST", "/customer/public-pool/recycle", TOK["admin"],
              {"thresholdSeconds": 10, "limit": 100, "dryRun": False})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:400]}")
recycled = d.get("data", {}).get("recycled", 0)
ok(f"真回收成功(回收 {recycled} 条)", code == 200 and recycled >= 1,
   f"scanned={d.get('data',{}).get('scanned')} recycled={recycled}")

# 9.2) 验证 DB
hr("9.2) 验证 DB:客户已被回收至公海")
out = subprocess.run(["mysql", "-u", "root", "-p123456", "crm_db", "-N", "-B", "-e",
                      f"SELECT id, customer_name, owner_user_id, is_public FROM crm_customer WHERE id = {CID}"],
                     capture_output=True, text=True)
print(f"  {out.stdout.strip()}")
parts = out.stdout.strip().split("\t")
ok("客户 is_public=1 且 owner=NULL", len(parts) >= 4 and parts[2] == "NULL" and parts[3] == "1",
   f"owner={parts[2] if len(parts)>=3 else '?'} isPublic={parts[3] if len(parts)>=4 else '?'}")

# 9.3) 验证系统记录
hr("9.3) 验证 DB:crm_record 写入了系统回收记录")
out = subprocess.run(
    f'mysql -u root -p123456 crm_db -N -B -e "SELECT id, related_id, follow_type, create_by, LEFT(content, 50) FROM crm_record WHERE related_type=\'customer\' AND related_id={CID} AND follow_type=\'\\xCE\\xC4\\xBC\\xFE\' ORDER BY id DESC LIMIT 1"',
    shell=True, capture_output=True, text=True
)
# 直接用 Python 中文,改用 pymysql 兜底
try:
    import pymysql
    conn = pymysql.connect(host="localhost", user="root", password="123456", database="crm_db", charset="utf8mb4")
    cur = conn.cursor()
    cur.execute("SELECT id, related_id, follow_type, create_by, LEFT(content, 50) FROM crm_record WHERE related_type='customer' AND related_id=%s AND follow_type='系统' ORDER BY id DESC LIMIT 1", (CID,))
    row = cur.fetchone()
    print(f"  (pymysql) row = {row}")
    ok("crm_record 系统记录已写", row is not None and "系统" in (row[2] or "") and "回收" in (row[4] or ""), f"row={row}")
    conn.close()
except Exception as e:
    print(f"  [pymysql 失败: {e}]")
    ok("crm_record 系统记录已写", False)

# 10) sales_li 认领
hr("10) sales_li 认领公海客户")
code, d = req("POST", f"/crm/customer/public-pool/claim/{CID}", TOK["sales_li"])
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("认领成功", code == 200)

# 10.1) 验证 owner 改为 sales_li
hr("10.1) 验证 DB:客户 owner=sales_li(4), isPublic=0")
out = subprocess.run(["mysql", "-u", "root", "-p123456", "crm_db", "-N", "-B", "-e",
                      f"SELECT id, customer_name, owner_user_id, is_public, last_follow_time FROM crm_customer WHERE id = {CID}"],
                     capture_output=True, text=True)
print(f"  {out.stdout.strip()}")
parts = out.stdout.strip().split("\t")
ok("认领后 owner=4 isPublic=0", len(parts) >= 4 and parts[2] == "4" and parts[3] == "0",
   f"owner={parts[2] if len(parts)>=3 else '?'} isPublic={parts[3] if len(parts)>=4 else '?'}")

# 10.2) 验证认领记录
hr("10.2) 验证 DB:crm_record 写入了认领记录")
try:
    import pymysql
    conn = pymysql.connect(host="localhost", user="root", password="123456", database="crm_db", charset="utf8mb4")
    cur = conn.cursor()
    # pymysql 的 %s 占位符与 LIKE '%xxx%' 中的 % 冲突,需把 LIKE 里的 % 转义为 %%
    cur.execute("SELECT id, follow_type, create_by, content FROM crm_record WHERE related_type='customer' AND related_id=%s AND content LIKE '%%认领%%' ORDER BY id DESC LIMIT 1", (CID,))
    row = cur.fetchone()
    print(f"  (pymysql) row = {row}")
    ok("认领记录已写", row is not None and "认领" in (row[3] or ""), f"row={row}")
    conn.close()
except Exception as e:
    print(f"  [pymysql 失败: {e}]")
    ok("认领记录已写", False)

# 11) 角色校验:finance 不能回收
hr("11) finance 调手动回收(应被 403 拒绝 — Sa-Token 无权限码拦截)")
code, d = req("POST", "/customer/public-pool/recycle", TOK["finance"], {"thresholdSeconds": 10})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("finance 角色校验拒绝", code == 403 or d.get("code") == 403,
   f"HTTP={code} bizCode={d.get('code')} msg={(d.get('message') or '')[:60]}")

# 12) 参数校验:超范围 threshold
hr("12) thresholdSeconds=0 (超出下限,应被 1001 拒绝)")
code, d = req("POST", "/customer/public-pool/recycle", TOK["admin"], {"thresholdSeconds": 0})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("threshold 下限校验生效", code == 200 and d.get("code") == 1001,
   f"bizCode={d.get('code')}")

# 13) owner 不能共享给自己
hr("13) owner 共享给自己(应被 1007 拒绝)")
code, d = req("POST", "/customer/share", TOK["sales_li"],
              {"customerId": CID, "userId": 4, "authType": 2})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("共享给 owner 自身被拦截", code == 200 and d.get("code") == 1007,
   f"bizCode={d.get('code')}")

# 14) finance 也不能共享(无权限码)
hr("14) finance 调共享(无 crm:customer:share 权限码,应 403)")
code, d = req("POST", "/customer/share", TOK["finance"],
              {"customerId": CID, "userId": 5, "authType": 1})
print(f"  HTTP={code}, resp={json.dumps(d, ensure_ascii=False)[:200]}")
ok("finance 无共享权限被拒", code != 200 or d.get("code") in (403, 1001),
   f"HTTP={code} bizCode={d.get('code')}")

# 15) SysUserController
hr("15) admin 查用户列表")
code, d = req("GET", "/sys/user/list", TOK["admin"])
print(f"  HTTP={code}, data sample={json.dumps(d, ensure_ascii=False)[:200]}")
ok("SysUserController 工作", code == 200 and isinstance(d.get("data"), list) and len(d["data"]) >= 5)

# 汇总
hr("=== 冒烟结果汇总 ===")
total = len(results)
passed = sum(1 for _, c, _ in results if c)
print(f"通过 {passed} / {total}")
for label, c, extra in results:
    print(f"  {'✓' if c else '✗'} {label}")
print()
sys.exit(0 if passed == total else 1)

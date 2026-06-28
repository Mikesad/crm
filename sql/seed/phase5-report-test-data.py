"""
阶段五 commit 2 报表测试数据生成器 + 直接插入 MySQL

目的:在全新安装的 crm_db 上插入 7 张业务表的测试数据,
让 4 个 Tab 报表能跑出非空结果。

数据规模(对齐报表需求):
- crm_lead: 60 条(状态分布 25/20/10/5)
- crm_customer: 100 条(前 10 来自 lead 转客户,后 90 新增)
- crm_business: 30 条(4 stage + 输单)
- crm_contract: 15 条(2-3 状态,total_amount 5-30 万)
- crm_receivable_plan: 25 条(2-3 期/合同)
- crm_receivable: 30 条(append-only,4 支付方式)
- crm_record: 80 条(4 实体 + 4 跟进方式)

执行方式:python 直接连 MySQL 插入(绕过 Windows mysql client 的中文编码问题)
"""

import random
import datetime
import sys
import io
import pymysql

# Windows console fix
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

random.seed(42)  # 确定性 seed,方便复现

# ============== 配置 ==============
NOW = datetime.datetime(2026, 6, 28, 18, 0, 0)
START_OF_MONTH = datetime.datetime(2026, 6, 1, 0, 0, 0)
START_OF_QUARTER = datetime.datetime(2026, 4, 1, 0, 0, 0)
START_OF_YEAR = datetime.datetime(2026, 1, 1, 0, 0, 0)
RECENT_30D = NOW - datetime.timedelta(days=30)
RECENT_60D = NOW - datetime.timedelta(days=60)
RECENT_90D = NOW - datetime.timedelta(days=90)

# 销售/owner 分配
SALES_IDS = [3, 4, 5]  # lead_wang / sales_li / sales_chen
SALES_DEPT = {3: 2, 4: 2, 5: 3}

INDUSTRIES = ['企业服务', '制造', '广告', '教育', '金融', '医疗', '互联网', '零售', '物流', '建筑']
LEVELS = ['A', 'B', 'C']
SOURCES = ['官网咨询', '电话拜访', '老客户介绍', '展会推广', '广告投放', '社交媒体', '搜索引擎']
REGIONS = ['北京', '上海', '广州', '深圳', '杭州', '成都', '南京', '武汉', '苏州', '西安']
STAGES = ['需求分析', '方案报价', '商务谈判', '赢单', '输单']
FOLLOW_TYPES = ['电话', '微信', '上门拜访', '邮件']
PAYMENT_METHODS = ['银行转账', '微信', '支付宝', '现金']

# ============== 辅助函数 ==============
def fmt_dt(dt):
    return dt.strftime('%Y-%m-%d %H:%M:%S')

def fmt_date(dt):
    return dt.strftime('%Y-%m-%d')

def random_dt(start, end):
    delta = (end - start).total_seconds()
    offset = random.randint(0, int(delta))
    return start + datetime.timedelta(seconds=offset)

def random_date(start, end):
    delta = (end - start).days
    return start + datetime.timedelta(days=random.randint(0, delta))

# ============== 生成 SQL ==============
out = []
out.append("-- ================================================================")
out.append("-- 阶段五 commit 2 报表测试数据(种子)")
out.append("-- 适用:全新安装的 crm_db 跑完 crm_full.sql 之后,本脚本生成")
out.append("--      报表 4 Tab 能跑出非空结果的演示数据")
out.append("-- 时间:2026-06-28")
out.append("-- 规模:crm_lead 60 / crm_customer 100 / crm_business 30 /")
out.append("--      crm_contract 15 / crm_receivable_plan 25 / crm_receivable 30 /")
out.append("--      crm_record 80")
out.append("-- ================================================================\n")
out.append("SET FOREIGN_KEY_CHECKS = 0;")

# 1. crm_lead: 60 条
out.append("\n-- ---------- 1) crm_lead 60 条(状态 25/20/10/5) ----------")
leads = []
for i in range(1, 61):
    if i <= 25:
        status, owner = 1, random.choice(SALES_IDS)  # 未跟进
    elif i <= 45:
        status, owner = 2, random.choice(SALES_IDS)  # 跟进中
    elif i <= 55:
        status, owner = 3, random.choice(SALES_IDS)  # 已转客户
    else:
        status, owner = 4, random.choice(SALES_IDS)  # 已死
    lead_name = f"线索公司-{i:03d}"
    contact = f"联系人{i}"
    phone = f"138{i:08d}"
    source = random.choice(SOURCES)
    remark = f"备注-{i}" if i % 3 == 0 else None
    create_time = random_dt(START_OF_YEAR, NOW)
    remark_sql = "NULL" if remark is None else f"'{remark}'"
    if status == 4:
        dead_time_sql = f"'{fmt_dt(NOW - datetime.timedelta(days=random.randint(1, 20)))}'"
        dead_reason_sql = f"'已死原因-{i:03d}'"
    else:
        dead_time_sql = "NULL"
        dead_reason_sql = "NULL"
    leads.append(f"({i}, '{lead_name}', '{contact}', '{phone}', '{source}', {status}, {owner}, "
                 f"{remark_sql}, {dead_reason_sql}, {dead_time_sql}, "
                 f"'系统', '{fmt_dt(create_time)}', '', '{fmt_dt(create_time)}', 0)")
out.append("INSERT INTO crm_lead (id, lead_name, contact_name, phone, source, status, owner_user_id, remark, dead_reason, dead_time, create_by, create_time, update_by, update_time, is_deleted) VALUES")
out.append(",\n".join(leads) + ";")

# 2. crm_customer: 100 条(前 10 来自 lead 46-55 转客户,后 90 新增)
out.append("\n-- ---------- 2) crm_customer 100 条(10 来自 lead 转客户,90 新增) ----------")
customers = []
for i in range(1, 101):
    if i <= 10:
        # 来自 lead 45-54(已转客户),继承 owner
        lead_owner = 45 + i  # 简化,实际是 leads[45+i-1].owner
        lead_owner = random.choice(SALES_IDS)
        name = f"线索公司-{45+i:03d}(已转)"
        is_public = 0
    elif i <= 20:
        # 公海客户(没owner,is_public=1)
        name = f"公海客户-{i:03d}"
        lead_owner = None
        is_public = 1
    else:
        # 普通客户
        name = f"正式客户-{i:03d}"
        lead_owner = random.choice(SALES_IDS)
        is_public = 0
    industry = random.choice(INDUSTRIES)
    level = random.choice(LEVELS)
    create_time = random_dt(START_OF_YEAR, NOW)
    last_follow = random_dt(RECENT_30D, NOW) if i % 4 != 0 else (random_dt(RECENT_60D, RECENT_30D) if i % 7 == 0 else None)
    owner_sql = "NULL" if lead_owner is None else str(lead_owner)
    last_follow_sql = "NULL" if last_follow is None else f"'{fmt_dt(last_follow)}'"
    customers.append(f"({i}, '{name}', '{industry}', '{level}', "
                     f"{owner_sql}, {is_public}, "
                     f"{last_follow_sql}, "
                     f"'系统', '{fmt_dt(create_time)}', '', '', 0)")
out.append("INSERT INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, last_follow_time, create_by, create_time, update_by, update_time, is_deleted) VALUES")
out.append(",\n".join(customers) + ";")

# 3. crm_business: 30 条(覆盖 4 stage + 输单)
out.append("\n-- ---------- 3) crm_business 30 条(4 stage + 输单) ----------")
businesses = []
# stage 分布:需求分析 12 / 方案报价 8 / 商务谈判 5 / 赢单 3 / 输单 2
stage_count = {'需求分析': 12, '方案报价': 8, '商务谈判': 5, '赢单': 3, '输单': 2}
biz_id = 1
for stage, count in stage_count.items():
    for _ in range(count):
        cust_id = random.randint(11, 100)  # 关联正式客户
        amount = round(random.uniform(50000, 500000), 2)
        expected_date = random_date(START_OF_MONTH, NOW + datetime.timedelta(days=90))
        owner = random.choice(SALES_IDS)
        create_time = random_dt(START_OF_QUARTER, NOW)
        businesses.append(f"({biz_id}, {cust_id}, '商机-{biz_id:03d}', {amount}, "
                          f"'{fmt_date(expected_date)}', '{stage}', {owner}, "
                          f"'系统', '{fmt_dt(create_time)}', '', '', 0)")
        biz_id += 1
out.append("INSERT INTO crm_business (id, customer_id, business_name, expected_amount, expected_deal_date, stage, owner_user_id, create_by, create_time, update_by, update_time, is_deleted) VALUES")
out.append(",\n".join(businesses) + ";")

# 4. crm_contract: 15 条
out.append("\n-- ---------- 4) crm_contract 15 条 ----------")
contracts = []
# 取 stage='赢单' 的 3 个商机赢单,转合同;再加 12 个直签合同
win_biz_ids = [i+1 for i, b in enumerate(businesses) if "'赢单'" in b][:3]  # 取赢单 ID
for i in range(1, 16):
    contract_num = f"HT-2026{i:04d}"
    if i <= 3:
        # 从赢单商机转
        biz_id = win_biz_ids[i-1]
        cust_id = random.randint(11, 100)
        amount = round(random.uniform(200000, 500000), 2)
        status = 1  # 执行中
    else:
        cust_id = random.randint(11, 100)
        amount = round(random.uniform(50000, 300000), 2)
        status = random.choice([0, 1, 1, 1, 2, 3])  # 审批中/执行中/已结束/已作废
    owner = random.choice(SALES_IDS)
    start_date = random_date(START_OF_YEAR, NOW)
    end_date = start_date + datetime.timedelta(days=random.randint(180, 720))
    create_time = random_dt(START_OF_QUARTER, NOW)
    biz_sql = "NULL" if i > 3 else str(biz_id)
    contracts.append(f"({i}, '{contract_num}', '合同-{i:03d}', {cust_id}, "
                     f"{biz_sql}, {amount}, "
                     f"'{fmt_date(start_date)}', '{fmt_date(end_date)}', {status}, {owner}, "
                     f"'系统', '{fmt_dt(create_time)}', '', '', 0)")
out.append("INSERT INTO crm_contract (id, contract_num, contract_name, customer_id, business_id, total_amount, start_date, end_date, status, owner_user_id, create_by, create_time, update_by, update_time, is_deleted) VALUES")
out.append(",\n".join(contracts) + ";")

# 5. crm_receivable_plan: 25 条(2-3 期/合同,15 个合同 -> 25 条)
out.append("\n-- ---------- 5) crm_receivable_plan 25 条 ----------")
plans = []
plan_id = 1
for contract_id in range(1, 16):
    # 跳过期作废合同
    if contracts[contract_id-1].find("', 3,") >= 0:  # status=3 作废
        continue
    periods = random.choice([1, 2, 2, 3])
    contract_total = float([c for c in contracts if c.startswith(f"({contract_id},")][0].split(",")[5])
    per_period = contract_total / periods
    for p in range(1, periods + 1):
        expected_date = random_date(START_OF_MONTH, NOW + datetime.timedelta(days=180))
        # 70% 已回款,20% 催款中,10% 未到期
        r = random.random()
        if r < 0.7:
            status = 2
        elif r < 0.9:
            status = 1
        else:
            status = 0
        plans.append(f"({plan_id}, {contract_id}, {p}, {round(per_period, 2)}, "
                     f"'{fmt_date(expected_date)}', {status}, "
                     f"'备注-{plan_id}', '系统', '{fmt_dt(NOW - datetime.timedelta(days=random.randint(30, 200)))}', '', '', 0)")
        plan_id += 1
out.append("INSERT INTO crm_receivable_plan (id, contract_id, period, expected_amount, expected_date, status, remark, create_by, create_time, update_by, update_time, is_deleted) VALUES")
out.append(",\n".join(plans) + ";")

# 6. crm_receivable: 30 条(已回款的 25 条 plan 全部生成 + 5 条计划外)
out.append("\n-- ---------- 6) crm_receivable 30 条 ----------")
receivables = []
recv_id = 1
# 已回款的 plan(plan.status=2)
paid_plans = [p for p in plans if p.endswith(", 2, '")]
for plan_str in paid_plans[:25]:
    plan_id = int(plan_str.split(",")[0].strip("("))
    contract_id = int(plan_str.split(",")[1].strip())
    amount = float(plan_str.split(",")[3].strip())
    return_date = random_date(START_OF_YEAR, NOW)
    method = random.choice(PAYMENT_METHODS)
    receivables.append(f"('SK-2026{recv_id:04d}', {contract_id}, {plan_id}, {round(amount, 2)}, "
                       f"'{fmt_date(return_date)}', '{method}', '赵财务', "
                       f"'{fmt_dt(random_dt(START_OF_YEAR, NOW))}')")
    recv_id += 1
# 5 条计划外(plan_id NULL)
for i in range(5):
    contract_id = random.randint(1, 15)
    amount = round(random.uniform(10000, 50000), 2)
    return_date = random_date(START_OF_QUARTER, NOW)
    method = random.choice(PAYMENT_METHODS)
    receivables.append(f"('SK-2026{recv_id:04d}', {contract_id}, NULL, {amount}, "
                       f"'{fmt_date(return_date)}', '{method}', '赵财务', "
                       f"'{fmt_dt(random_dt(START_OF_QUARTER, NOW))}')")
    recv_id += 1
out.append("INSERT INTO crm_receivable (receivable_num, contract_id, plan_id, actual_amount, return_date, payment_method, create_by, create_time) VALUES")
out.append(",\n".join(receivables) + ";")

# 7. crm_record: 80 条
out.append("\n-- ---------- 7) crm_record 80 条(4 实体 + 4 跟进方式) ----------")
records = []
for i in range(1, 81):
    related_type = random.choice(['lead', 'customer', 'business', 'contract'])
    if related_type == 'lead':
        related_id = random.randint(1, 60)
    elif related_type == 'customer':
        related_id = random.randint(1, 100)
    elif related_type == 'business':
        related_id = random.randint(1, 30)
    else:
        related_id = random.randint(1, 15)
    follow_type = random.choice(FOLLOW_TYPES)
    content = f"沟通纪要-{i}: 客户反馈积极,下周约面谈"
    create_time = random_dt(START_OF_QUARTER, NOW)
    # 50% 有 next_follow_time
    if random.random() < 0.5:
        next_follow = create_time + datetime.timedelta(days=random.randint(1, 14))
    else:
        next_follow = None
    creator = random.choice(['李销售', '陈销售', '王主管'])
    next_follow_sql = "NULL" if next_follow is None else f"'{fmt_dt(next_follow)}'"
    records.append(f"({i}, '{related_type}', {related_id}, '{content}', '{follow_type}', "
                   f"{next_follow_sql}, "
                   f"'{creator}', '{fmt_dt(create_time)}')")
out.append("INSERT INTO crm_record (id, related_type, related_id, content, follow_type, next_follow_time, create_by, create_time) VALUES")
out.append(",\n".join(records) + ";")

out.append("\nSET FOREIGN_KEY_CHECKS = 1;")
out.append("\n-- 验证查询:")
out.append("-- SELECT 'crm_lead' AS tbl, COUNT(*) AS cnt FROM crm_lead UNION ALL")
out.append("-- SELECT 'crm_customer', COUNT(*) FROM crm_customer UNION ALL")
out.append("-- SELECT 'crm_business', COUNT(*) FROM crm_business UNION ALL")
out.append("-- SELECT 'crm_contract', COUNT(*) FROM crm_contract UNION ALL")
out.append("-- SELECT 'crm_receivable_plan', COUNT(*) FROM crm_receivable_plan UNION ALL")
out.append("-- SELECT 'crm_receivable', COUNT(*) FROM crm_receivable UNION ALL")
out.append("-- SELECT 'crm_record', COUNT(*) FROM crm_record;")

# ============== 直接连 MySQL 插入(绕开 Windows 客户端中文编码) ==============
def insert_via_pymysql():
    conn = pymysql.connect(
        host='localhost', port=3306,
        user='root', password='123456',
        database='crm_db', charset='utf8mb4',
        autocommit=False
    )
    try:
        with conn.cursor() as cur:
            # 解析 INSERT INTO ... VALUES (...);
            import re
            for line in out:
                m = re.match(r'^(INSERT INTO \S+.*?VALUES)\s*(.*?);\s*$', line, re.DOTALL)
                if not m: continue
                prefix, body = m.group(1), m.group(2)
                # 拆分为每行(以 ),( 或 ),  结尾)
                rows = []
                buf = ''
                depth = 0
                in_str = False
                esc = False
                for ch in body:
                    if esc:
                        buf += ch; esc = False; continue
                    if ch == '\\':
                        buf += ch; esc = True; continue
                    if ch == "'":
                        in_str = not in_str; buf += ch; continue
                    if not in_str:
                        if ch == '(': depth += 1
                        elif ch == ')': depth -= 1
                    buf += ch
                    if depth == 0 and ch == ')':
                        rows.append(buf.strip().rstrip(',').strip())
                        buf = ''
                if not rows: continue
                sql = prefix + ' ' + ', '.join('(' + r.strip('() ') + ')' for r in rows)
                print(f"执行: {sql[:80]}... ({len(rows)} 行)")
                cur.execute(sql)
        conn.commit()
        print("\n✅ 全部插入成功,提交事务")
    except Exception as e:
        conn.rollback()
        print(f"\n❌ 失败: {e}")
        raise
    finally:
        conn.close()

if __name__ == '__main__':
    insert_via_pymysql()

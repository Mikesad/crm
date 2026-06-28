"""
阶段五 commit 2 报表测试数据 — 简版 pymysql 直插
"""

import random
import datetime
import sys
import io
import pymysql

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

random.seed(42)

NOW = datetime.datetime(2026, 6, 28, 18, 0, 0)
START_OF_YEAR = datetime.datetime(2026, 1, 1, 0, 0, 0)
START_OF_QUARTER = datetime.datetime(2026, 4, 1, 0, 0, 0)
RECENT_30D = NOW - datetime.timedelta(days=30)
RECENT_60D = NOW - datetime.timedelta(days=60)

SALES_IDS = [3, 4, 5]
INDUSTRIES = ['企业服务', '制造', '广告', '教育', '金融', '医疗', '互联网', '零售', '物流', '建筑']
LEVELS = ['A', 'B', 'C']
SOURCES = ['官网咨询', '电话拜访', '老客户介绍', '展会推广', '广告投放', '社交媒体', '搜索引擎']
STAGES = ['需求分析', '方案报价', '商务谈判', '赢单', '输单']
FOLLOW_TYPES = ['电话', '微信', '上门拜访', '邮件']
PAYMENT_METHODS = ['银行转账', '微信', '支付宝', '现金']

def fmt_dt(dt): return dt.strftime('%Y-%m-%d %H:%M:%S')
def fmt_date(dt): return dt.strftime('%Y-%m-%d')
def random_dt(start, end):
    delta = (end - start).total_seconds()
    return start + datetime.timedelta(seconds=random.randint(0, int(delta)))
def random_date(start, end):
    delta = (end - start).days
    return start + datetime.timedelta(days=random.randint(0, delta))

conn = pymysql.connect(
    host='localhost', port=3306,
    user='root', password='123456',
    database='crm_db', charset='utf8mb4',
    autocommit=False
)

try:
    with conn.cursor() as cur:
        # 1. crm_lead 60 条
        for i in range(1, 61):
            if i <= 25:    status, owner = 1, random.choice(SALES_IDS)
            elif i <= 45:  status, owner = 2, random.choice(SALES_IDS)
            elif i <= 55:  status, owner = 3, random.choice(SALES_IDS)
            else:           status, owner = 4, random.choice(SALES_IDS)
            dead_time = NOW - datetime.timedelta(days=random.randint(1, 20)) if status == 4 else None
            dead_reason = f'已死原因-{i:03d}' if status == 4 else None
            remark = f'备注-{i}' if i % 3 == 0 else None
            create_time = random_dt(START_OF_YEAR, NOW)
            cur.execute(
                "INSERT INTO crm_lead (id, lead_name, contact_name, phone, source, status, owner_user_id, remark, dead_reason, dead_time, create_by, create_time, update_by, update_time, is_deleted) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
                (i, f'线索公司-{i:03d}', f'联系人{i}', f'138{i:08d}', random.choice(SOURCES),
                 status, owner, remark, dead_reason, dead_time,
                 '系统', create_time, '系统', create_time, 0)
            )
        print(f'crm_lead: 60 条')

        # 2. crm_customer 100 条
        for i in range(1, 101):
            if i <= 10:
                name = f'正式客户-{i:03d}'
                owner = random.choice(SALES_IDS); is_public = 0
            elif i <= 20:
                name = f'公海客户-{i:03d}'
                owner = None; is_public = 1
            else:
                name = f'正式客户-{i:03d}'
                owner = random.choice(SALES_IDS); is_public = 0
            last_follow = random_dt(RECENT_30D, NOW) if i % 4 != 0 else (random_dt(RECENT_60D, RECENT_30D) if i % 7 == 0 else None)
            create_time = random_dt(START_OF_YEAR, NOW)
            cur.execute(
                "INSERT INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, last_follow_time, create_by, create_time, update_by, update_time, is_deleted) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
                (i, name, random.choice(INDUSTRIES), random.choice(LEVELS),
                 owner, is_public, last_follow,
                 '系统', create_time, '系统', create_time, 0)
            )
        print(f'crm_customer: 100 条')

        # 3. crm_business 30 条
        biz_id = 1
        stage_count = [('需求分析', 12), ('方案报价', 8), ('商务谈判', 5), ('赢单', 3), ('输单', 2)]
        for stage, count in stage_count:
            for _ in range(count):
                cust_id = random.randint(11, 100)
                amount = round(random.uniform(50000, 500000), 2)
                expected_date = random_date(START_OF_QUARTER, NOW + datetime.timedelta(days=90))
                create_time = random_dt(START_OF_QUARTER, NOW)
                cur.execute(
                    "INSERT INTO crm_business (id, customer_id, business_name, expected_amount, expected_deal_date, stage, owner_user_id, create_by, create_time, update_by, update_time, is_deleted) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
                    (biz_id, cust_id, f'商机-{biz_id:03d}', amount, expected_date, stage,
                     random.choice(SALES_IDS), '系统', create_time, '系统', create_time, 0)
                )
                biz_id += 1
        print(f'crm_business: 30 条')

        # 4. crm_contract 15 条
        win_biz_ids = [4, 14, 24]  # 赢单商机 ID(硬编码简化)
        for i in range(1, 16):
            contract_num = f'HT-2026{i:04d}'
            if i <= 3:
                cust_id = random.randint(11, 100)
                amount = round(random.uniform(200000, 500000), 2)
                status = 1
                biz_id_val = win_biz_ids[i-1]
            else:
                cust_id = random.randint(11, 100)
                amount = round(random.uniform(50000, 300000), 2)
                status = random.choice([0, 1, 1, 1, 2, 3])
                biz_id_val = None
            owner = random.choice(SALES_IDS)
            start_date = random_date(START_OF_YEAR, NOW)
            end_date = start_date + datetime.timedelta(days=random.randint(180, 720))
            create_time = random_dt(START_OF_QUARTER, NOW)
            cur.execute(
                "INSERT INTO crm_contract (id, contract_num, contract_name, customer_id, business_id, total_amount, start_date, end_date, status, owner_user_id, create_by, create_time, update_by, update_time, is_deleted) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
                (i, contract_num, f'合同-{i:03d}', cust_id, biz_id_val, amount, start_date, end_date,
                 status, owner, '系统', create_time, '系统', create_time, 0)
            )
        print(f'crm_contract: 15 条')

        # 5. crm_receivable_plan
        plan_id = 1
        for contract_id in range(1, 16):
            periods = random.choice([1, 2, 2, 3])
            # 取 contract total_amount
            cur.execute("SELECT total_amount FROM crm_contract WHERE id=%s", (contract_id,))
            row = cur.fetchone()
            if not row or row[0] is None: continue
            contract_total = float(row[0])
            per_period = round(contract_total / periods, 2)
            for p in range(1, periods + 1):
                expected_date = random_date(START_OF_QUARTER, NOW + datetime.timedelta(days=180))
                r = random.random()
                status = 2 if r < 0.7 else (1 if r < 0.9 else 0)
                cur.execute(
                    "INSERT INTO crm_receivable_plan (id, contract_id, period, expected_amount, expected_date, status, remark, create_by, create_time, update_by, update_time, is_deleted) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
                    (plan_id, contract_id, p, per_period, expected_date, status,
                     f'备注-{plan_id}', '系统', NOW - datetime.timedelta(days=random.randint(30, 200)),
                     '系统', NOW, 0)
                )
                plan_id += 1
        print(f'crm_receivable_plan: {plan_id-1} 条')

        # 6. crm_receivable
        recv_id = 1
        # 已回款 plan
        cur.execute("SELECT id, contract_id, expected_amount, expected_date FROM crm_receivable_plan WHERE status=2")
        paid_plans = cur.fetchall()
        for plan_id, contract_id, amount, expected_date in paid_plans:
            return_date = random_date(START_OF_QUARTER, NOW)
            cur.execute(
                "INSERT INTO crm_receivable (receivable_num, contract_id, plan_id, actual_amount, return_date, payment_method, create_by, create_time) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s, %s)",
                (f'SK-2026{recv_id:04d}', contract_id, plan_id, amount, return_date,
                 random.choice(PAYMENT_METHODS), '赵财务', random_dt(START_OF_QUARTER, NOW))
            )
            recv_id += 1
        # 5 条计划外
        for i in range(5):
            contract_id = random.randint(1, 15)
            amount = round(random.uniform(10000, 50000), 2)
            return_date = random_date(START_OF_QUARTER, NOW)
            cur.execute(
                "INSERT INTO crm_receivable (receivable_num, contract_id, plan_id, actual_amount, return_date, payment_method, create_by, create_time) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s, %s)",
                (f'SK-2026{recv_id:04d}', contract_id, None, amount, return_date,
                 random.choice(PAYMENT_METHODS), '赵财务', random_dt(START_OF_QUARTER, NOW))
            )
            recv_id += 1
        print(f'crm_receivable: {recv_id-1} 条')

        # 7. crm_record 80 条
        for i in range(1, 81):
            related_type = random.choice(['lead', 'customer', 'business', 'contract'])
            if related_type == 'lead':     related_id = random.randint(1, 60)
            elif related_type == 'customer': related_id = random.randint(1, 100)
            elif related_type == 'business': related_id = random.randint(1, 30)
            else:                           related_id = random.randint(1, 15)
            follow_type = random.choice(FOLLOW_TYPES)
            content = f'沟通纪要-{i}: 客户反馈积极,下周约面谈'
            create_time = random_dt(START_OF_QUARTER, NOW)
            next_follow = create_time + datetime.timedelta(days=random.randint(1, 14)) if random.random() < 0.5 else None
            creator = random.choice(['李销售', '陈销售', '王主管'])
            cur.execute(
                "INSERT INTO crm_record (id, related_type, related_id, content, follow_type, next_follow_time, create_by, create_time) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s, %s)",
                (i, related_type, related_id, content, follow_type, next_follow, creator, create_time)
            )
        print(f'crm_record: 80 条')

    conn.commit()
    print('\n✅ 全部插入成功,已提交')
except Exception as e:
    conn.rollback()
    print(f'\n❌ 失败: {e}')
    raise
finally:
    conn.close()

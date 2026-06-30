-- =============================================================
-- phase8 commit 2 业务验证数据
-- 用途:补全 crm_db,让报表中心 7 个改造点有数据可看
-- 范围:
--   1) sys_dept 加 3 个嵌套子部门(让 C2-D6 认子部门可验证)
--   2) sys_user 调整 + 新增 2 个(覆盖嵌套部门)
--   3) crm_product 4 条
--   4) crm_lead 10 条
--   5) crm_customer 8 条
--   6) crm_business 6 条
--   7) crm_contract 6 条(status 混合 0/1/2,跨月份)
--   8) crm_contract_product 10 条
--   9) crm_receivable_plan 12 条(每个合同 2 期)
--  10) crm_receivable 12 条(部分欠款验证 C2-D4 口径)
--
-- 验证流程:
--   mysql ... crm_db < sql/test-data-phase8.sql
--   # 跑两遍幂等(虽然本脚本不严格幂等,但 WHERE NOT EXISTS 保证不重复)
--
-- 重置流程(需要回到干净状态时):
--   DELETE FROM crm_receivable;
--   DELETE FROM crm_receivable_plan;
--   DELETE FROM crm_contract_product;
--   DELETE FROM crm_contract;
--   DELETE FROM crm_business;
--   DELETE FROM crm_customer;
--   DELETE FROM crm_lead;
--   DELETE FROM crm_product;
--   DELETE FROM sys_user WHERE id IN (7,8);
--   DELETE FROM sys_dept WHERE id IN (5,6,7);
--   UPDATE sys_user SET dept_id=2 WHERE id=4;
--   UPDATE sys_user SET dept_id=3 WHERE id=5;
-- =============================================================

-- ============ 1) sys_dept 加嵌套子部门 ============
-- 当前结构:总公司(1) → 华东销售部(2) / 华南销售部(3) / 财务部(4) 平铺
-- 加:华东一组(5) parent=2 / 华东二组(6) parent=2 / 华南一组(7) parent=3
INSERT IGNORE INTO sys_dept (id, parent_id, ancestors, dept_name, order_num, status, create_by, create_time)
VALUES
  (5, 2, '0,1,2', '华东一组', 1, 1, 'admin', NOW()),
  (6, 2, '0,1,2', '华东二组', 2, 1, 'admin', NOW()),
  (7, 3, '0,1,3', '华南一组', 1, 1, 'admin', NOW());

-- ============ 2) sys_user 调整 + 新增 ============
-- sales_li (id=4) → 调到华东一组 (dept=5)
UPDATE sys_user SET dept_id = 5 WHERE id = 4 AND dept_id = 2;
-- sales_chen (id=5) → 调到华南一组 (dept=7)
UPDATE sys_user SET dept_id = 7 WHERE id = 5 AND dept_id = 3;
-- 新增 2 个用户覆盖华东二组 + 华南一组(让 ownerIds 跨多个 dept 可验证)
INSERT IGNORE INTO sys_user (id, username, password, nickname, email, phone, dept_id, status, create_by, create_time)
VALUES
  (7, 'sales_zhao', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '赵销售', 'zhao@crm.local', '13800000007', 6, 1, 'admin', NOW()),
  (8, 'sales_qian', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '钱销售', 'qian@crm.local', '13800000008', 7, 1, 'admin', NOW());
-- 给新用户分配 sales 角色
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT 7, 4 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id=7 AND role_id=4);
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT 8, 4 WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id=8 AND role_id=4);

-- ============ 3) crm_product 4 条 ============
INSERT IGNORE INTO crm_product (id, category_id, product_code, product_name, spec, price, unit, status, create_by, create_time)
VALUES
  (1, 1, 'PRD-001', 'ZenCRM 企业版',   '旗舰版',  120000.00, '套',   1, 'admin', NOW()),
  (2, 1, 'PRD-002', 'ZenCRM 基础版',   '基础版',  36000.00,  '套',   1, 'admin', NOW()),
  (3, 2, 'PRD-003', '数据分析模块',    '功能模块', 28000.00,  '模块', 1, 'admin', NOW()),
  (4, 3, 'PRD-004', '实施服务包',      '服务类',   50000.00,  '次',   1, 'admin', NOW());

-- ============ 4) crm_lead 10 条(线索) ============
INSERT IGNORE INTO crm_lead (id, lead_name, contact_name, phone, source, status, owner_user_id, create_by, create_time)
VALUES
  (1, '北京智能科技',  '张总', '13900000001', '官网咨询', 3, 4, 'admin', '2025-12-15 10:00:00'),
  (2, '上海贸易公司',  '李经理','13900000002', '电话拜访', 2, 4, 'admin', '2026-01-10 10:00:00'),
  (3, '广州电商平台',  '王老板','13900000003', '老客户介绍', 2, 5, 'admin', '2026-02-05 10:00:00'),
  (4, '深圳制造企业',  '陈工', '13900000004', '展会推广', 2, 7, 'admin', '2026-02-20 10:00:00'),
  (5, '杭州互联网公司', '刘VP', '13900000005', '官网咨询', 2, 8, 'admin', '2026-03-01 10:00:00'),
  (6, '成都教育集团',  '杨校长','13900000006', '电话拜访', 1, 4, 'admin', '2026-03-15 10:00:00'),
  (7, '武汉医疗科技',  '吴主任','13900000007', '老客户介绍', 2, 7, 'admin', '2026-04-05 10:00:00'),
  (8, '南京物流公司',  '徐总', '13900000008', '官网咨询', 2, 8, 'admin', '2026-04-20 10:00:00'),
  (9, '西安建工集团',  '孙总', '13900000009', '展会推广', 1, 5, 'admin', '2026-05-10 10:00:00'),
  (10,'重庆文旅集团',  '周总', '13900000010', '电话拜访', 2, 4, 'admin', '2026-05-25 10:00:00');

-- ============ 5) crm_customer 8 条 ============
INSERT IGNORE INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, last_follow_time, create_by, create_time)
VALUES
  (1, '北京智能科技',   '科技', 'A', 4, 0, '2026-06-20 10:00:00', 'admin', '2025-12-15 10:00:00'),
  (2, '上海贸易公司',   '贸易', 'B', 4, 0, '2026-06-15 10:00:00', 'admin', '2026-01-10 10:00:00'),
  (3, '广州电商平台',   '电商', 'A', 5, 0, '2026-06-25 10:00:00', 'admin', '2026-02-05 10:00:00'),
  (4, '深圳制造企业',   '制造', 'A', 7, 0, '2026-06-22 10:00:00', 'admin', '2026-02-20 10:00:00'),
  (5, '杭州互联网公司', '科技', 'A', 8, 0, '2026-06-28 10:00:00', 'admin', '2026-03-01 10:00:00'),
  (6, '武汉医疗科技',   '医疗', 'B', 7, 0, '2026-06-18 10:00:00', 'admin', '2026-04-05 10:00:00'),
  (7, '南京物流公司',   '物流', 'C', 8, 0, '2026-06-10 10:00:00', 'admin', '2026-04-20 10:00:00'),
  (8, '重庆文旅集团',   '文旅', 'A', 4, 0, '2026-06-26 10:00:00', 'admin', '2026-05-25 10:00:00');

-- ============ 6) crm_business 6 条(商机,stage 各阶段) ============
INSERT IGNORE INTO crm_business (id, customer_id, business_name, expected_amount, expected_deal_date, stage, owner_user_id, create_by, create_time)
VALUES
  (1, 1, '智能科技 CRM 全套',  240000.00, '2026-04-30', '赢单',     4, 'admin', '2026-02-01 10:00:00'),
  (2, 2, '贸易公司基础版',      72000.00,  '2026-05-31', '商务谈判', 4, 'admin', '2026-03-15 10:00:00'),
  (3, 3, '电商平台旗舰版',     360000.00,  '2026-06-30', '方案报价', 5, 'admin', '2026-04-01 10:00:00'),
  (4, 4, '制造企业数据分析',    56000.00,  '2026-07-31', '需求分析', 7, 'admin', '2026-05-01 10:00:00'),
  (5, 5, '互联网公司增值包',    60000.00,  '2026-06-30', '赢单',     8, 'admin', '2026-05-10 10:00:00'),
  (6, 7, '物流公司基础版',      36000.00,  '2026-08-31', '方案报价', 8, 'admin', '2026-06-01 10:00:00');

-- ============ 7) crm_contract 6 条(status 0/1/2 各 2 条,跨月份) ============
INSERT IGNORE INTO crm_contract (id, contract_num, contract_name, customer_id, business_id, total_amount, start_date, end_date, status, owner_user_id, create_by, create_time)
VALUES
  -- status=0 审批中(C2-D5 验证:排除)
  (1, 'HT-20260101-0001', '智能科技企业版合同', 1, 1, 240000.00, '2026-01-15', '2026-12-31', 0, 4, 'admin', '2026-01-15 10:00:00'),
  (2, 'HT-20260201-0002', '贸易公司基础版合同', 2, 2, 72000.00,  '2026-02-20', '2026-12-31', 0, 4, 'admin', '2026-02-20 10:00:00'),
  -- status=1 执行中(纳入 C2-D5)
  (3, 'HT-20260301-0003', '电商平台旗舰版合同', 3, 3, 360000.00, '2026-03-10', '2026-12-31', 1, 5, 'admin', '2026-03-10 10:00:00'),
  (4, 'HT-20260401-0004', '互联网公司增值包',   5, 5, 60000.00,  '2026-04-15', '2026-12-31', 1, 8, 'admin', '2026-04-15 10:00:00'),
  -- status=2 已结束
  (5, 'HT-20260501-0005', '制造企业数据分析',   4, 4, 56000.00,  '2026-05-20', '2026-12-31', 2, 7, 'admin', '2026-05-20 10:00:00'),
  (6, 'HT-20260601-0006', '物流公司基础版',     7, 6, 36000.00,  '2026-06-25', '2026-12-31', 2, 8, 'admin', '2026-06-25 10:00:00');

-- ============ 8) crm_contract_product 8 条 ============
INSERT IGNORE INTO crm_contract_product (id, contract_id, product_id, count, standard_price, sales_price, discount)
VALUES
  (1, 1, 1, 1, 120000.00, 120000.00, 10.00),
  (2, 1, 3, 1, 28000.00,  28000.00,  10.00),
  (3, 1, 4, 2, 50000.00,  50000.00,  10.00),
  (4, 2, 2, 2, 36000.00,  36000.00,  10.00),
  (5, 3, 1, 3, 120000.00, 120000.00, 10.00),
  (6, 4, 3, 2, 28000.00,  28000.00,  10.00),
  (7, 4, 4, 1, 50000.00,  50000.00,  10.00),
  (8, 5, 3, 2, 28000.00,  28000.00,  10.00),
  (9, 6, 2, 1, 36000.00,  36000.00,  10.00),
  (10,6, 4, 1, 50000.00,  50000.00,  10.00);

-- ============ 9) crm_receivable_plan 12 条(每个合同 2 期) ============
INSERT IGNORE INTO crm_receivable_plan (id, contract_id, period, expected_amount, expected_date, status, remark, create_by, create_time)
VALUES
  (1,  1, 1, 120000.00, '2026-02-15', 0, '首付款', 'admin', '2026-01-15 10:00:00'),
  (2,  1, 2, 120000.00, '2026-06-15', 0, '尾款',   'admin', '2026-01-15 10:00:00'),
  (3,  2, 1, 36000.00,  '2026-03-20', 0, '首付款', 'admin', '2026-02-20 10:00:00'),
  (4,  2, 2, 36000.00,  '2026-07-20', 0, '尾款',   'admin', '2026-02-20 10:00:00'),
  (5,  3, 1, 180000.00, '2026-04-10', 0, '首付款', 'admin', '2026-03-10 10:00:00'),
  (6,  3, 2, 180000.00, '2026-08-10', 0, '尾款',   'admin', '2026-03-10 10:00:00'),
  (7,  4, 1, 30000.00,  '2026-05-15', 0, '首付款', 'admin', '2026-04-15 10:00:00'),
  (8,  4, 2, 30000.00,  '2026-09-15', 0, '尾款',   'admin', '2026-04-15 10:00:00'),
  (9,  5, 1, 28000.00,  '2026-06-20', 0, '首付款', 'admin', '2026-05-20 10:00:00'),
  (10, 5, 2, 28000.00,  '2026-10-20', 0, '尾款',   'admin', '2026-05-20 10:00:00'),
  (11, 6, 1, 18000.00,  '2026-07-25', 0, '首付款', 'admin', '2026-06-25 10:00:00'),
  (12, 6, 2, 18000.00,  '2026-11-25', 0, '尾款',   'admin', '2026-06-25 10:00:00');

-- ============ 10) crm_receivable 12 条(部分欠款·验证 C2-D4) ============
-- 设计:已回款总计 ~ 60 万,合同总业绩 ~ 82.4 万(只算 IN(1,2))
-- contract 1 (status=0 审批中):不入业绩,也不录入回款 → 全空(验证 C2-D5)
-- contract 2 (status=0 审批中):同上
-- contract 3 (status=1 执行中,360k):首付款 180k 已回,尾款 0 → 回款 180k
-- contract 4 (status=1 执行中,60k):首付款 30k 已回,尾款 0 → 回款 30k
-- contract 5 (status=2 已结束,56k):全部 56k 已回 → 回款 56k
-- contract 6 (status=2 已结束,36k):首付款 18k 已回 → 回款 18k
INSERT IGNORE INTO crm_receivable (id, receivable_num, contract_id, plan_id, actual_amount, return_date, payment_method, create_by, create_time)
VALUES
  (1,  'SK-20260415-0001', 3, 5,  180000.00, '2026-04-15', '银行转账', 'finance', '2026-04-15 10:00:00'),
  (2,  'SK-20260510-0002', 3, NULL, 20000.00, '2026-05-10', '银行转账', 'finance', '2026-05-10 10:00:00'),
  (3,  'SK-20260520-0003', 4, 7,  30000.00,  '2026-05-20', '微信',     'finance', '2026-05-20 10:00:00'),
  (4,  'SK-20260605-0004', 5, 9,  28000.00,  '2026-06-05', '银行转账', 'finance', '2026-06-05 10:00:00'),
  (5,  'SK-20260620-0005', 5, NULL, 28000.00, '2026-06-20', '银行转账', 'finance', '2026-06-20 10:00:00'),
  (6,  'SK-20260626-0006', 6, 11, 18000.00,  '2026-06-26', '支付宝',   'finance', '2026-06-26 10:00:00');

SELECT 'phase8 test-data inserted: dept+user+10 lead+8 customer+6 business+6 contract+10 contract_product+12 plan+6 receivable' AS info;
-- ============ 扩充:12 条合同(7-18),跨 1-6 月 ============
-- 设计:每月 2 条,owner_user_id 在 4/5/7/8 之间分散,status 混合
-- 注意:contract_id 7+ 没有对应的 customer_id(用现有 1-8),需要选 customer 跟 owner 不同部门
INSERT IGNORE INTO crm_contract (id, contract_num, contract_name, customer_id, business_id, total_amount, start_date, end_date, status, owner_user_id, create_by, create_time)
VALUES
  -- 1月(owner=4 sales_li / owner=7 sales_zhao)
  (7,  'HT-20260110-0007', '智能科技增值合同',   1, NULL, 80000.00,  '2026-01-10', '2026-12-31', 1, 4, 'admin', '2026-01-10 10:00:00'),
  (8,  'HT-20260120-0008', '深圳制造维护',       4, NULL, 36000.00,  '2026-01-20', '2026-12-31', 2, 7, 'admin', '2026-01-20 10:00:00'),
  -- 2月(owner=5 sales_chen / owner=8 sales_qian)
  (9,  'HT-20260210-0009', '广州电商增值包',     3, NULL, 50000.00,  '2026-02-10', '2026-12-31', 1, 5, 'admin', '2026-02-10 10:00:00'),
  (10, 'HT-20260220-0010', '南京物流系统集成',   7, NULL, 120000.00, '2026-02-20', '2026-12-31', 2, 8, 'admin', '2026-02-20 10:00:00'),
  -- 3月
  (11, 'HT-20260310-0011', '上海贸易高级版',     2, NULL, 96000.00,  '2026-03-10', '2026-12-31', 1, 4, 'admin', '2026-03-10 10:00:00'),
  (12, 'HT-20260320-0012', '杭州互联网维护',     5, NULL, 45000.00,  '2026-03-20', '2026-12-31', 2, 7, 'admin', '2026-03-20 10:00:00'),
  -- 4月
  (13, 'HT-20260410-0013', '广州电商二期',       3, NULL, 75000.00,  '2026-04-10', '2026-12-31', 1, 5, 'admin', '2026-04-10 10:00:00'),
  (14, 'HT-20260420-0014', '南京物流扩模块',     7, NULL, 60000.00,  '2026-04-20', '2026-12-31', 1, 8, 'admin', '2026-04-20 10:00:00'),
  -- 5月
  (15, 'HT-20260510-0015', '上海贸易升级',       2, NULL, 72000.00,  '2026-05-10', '2026-12-31', 1, 4, 'admin', '2026-05-10 10:00:00'),
  (16, 'HT-20260520-0016', '智能科技续约',       1, NULL, 108000.00, '2026-05-20', '2026-12-31', 1, 7, 'admin', '2026-05-20 10:00:00'),
  -- 6月
  (17, 'HT-20260610-0017', '杭州互联网二期',     5, NULL, 90000.00,  '2026-06-10', '2026-06-30', 1, 5, 'admin', '2026-06-10 10:00:00'),
  (18, 'HT-20260620-0018', '深圳制造新模块',     4, NULL, 48000.00,  '2026-06-20', '2026-12-31', 1, 8, 'admin', '2026-06-20 10:00:00');

-- ============ 扩充:14 条回款(7-20),覆盖 1-6 月 ============
-- 设计:大部分已回款(60-80%),部分欠款,验证 C2-D4 / P3 / P6
INSERT IGNORE INTO crm_receivable (id, receivable_num, contract_id, plan_id, actual_amount, return_date, payment_method, create_by, create_time)
VALUES
  -- 1月回款
  (7,  'SK-20260125-0007',  7, NULL, 80000.00,  '2026-01-25', '银行转账', 'finance', '2026-01-25 10:00:00'),
  (8,  'SK-20260128-0008',  8, NULL, 36000.00,  '2026-01-28', '银行转账', 'finance', '2026-01-28 10:00:00'),
  -- 2月回款
  (9,  'SK-20260225-0009',  9, NULL, 50000.00,  '2026-02-25', '银行转账', 'finance', '2026-02-25 10:00:00'),
  (10, 'SK-20260228-0010', 10, NULL, 80000.00,  '2026-02-28', '微信',     'finance', '2026-02-28 10:00:00'),
  -- 3月回款
  (11, 'SK-20260325-0011', 11, NULL, 96000.00,  '2026-03-25', '银行转账', 'finance', '2026-03-25 10:00:00'),
  (12, 'SK-20260328-0012', 12, NULL, 45000.00,  '2026-03-28', '银行转账', 'finance', '2026-03-28 10:00:00'),
  -- 4月回款
  (13, 'SK-20260415-0013', 13, NULL, 50000.00,  '2026-04-15', '银行转账', 'finance', '2026-04-15 10:00:00'),
  (14, 'SK-20260420-0014', 14, NULL, 30000.00,  '2026-04-20', '支付宝',   'finance', '2026-04-20 10:00:00'),
  -- 5月回款
  (15, 'SK-20260515-0015', 15, NULL, 40000.00,  '2026-05-15', '银行转账', 'finance', '2026-05-15 10:00:00'),
  (16, 'SK-20260525-0016', 16, NULL, 60000.00,  '2026-05-25', '银行转账', 'finance', '2026-05-25 10:00:00'),
  -- 6月回款
  (17, 'SK-20260605-0017', 17, NULL, 50000.00,  '2026-06-05', '银行转账', 'finance', '2026-06-05 10:00:00'),
  (18, 'SK-20260612-0018', 17, NULL, 20000.00,  '2026-06-12', '微信',     'finance', '2026-06-12 10:00:00'),
  (19, 'SK-20260618-0019', 18, NULL, 30000.00,  '2026-06-18', '银行转账', 'finance', '2026-06-18 10:00:00'),
  (20, 'SK-20260625-0020', 18, NULL, 18000.00,  '2026-06-25', '支付宝',   'finance', '2026-06-25 10:00:00');

SELECT 'phase8 extended data: 12 contracts + 14 receivables added' AS info;

-- ============ 扩充:240 条 crm_record 跟进记录(跨 1-6 月,4 销售均匀分布) ============
-- 设计:按用户名(create_by) 分组聚合,验证 Tab ③ "高频跟进人 · TOP N"
-- 数字辅助表生成 1..240,每月 40 条
INSERT IGNORE INTO crm_record (related_type, related_id, content, follow_type, create_by, create_time, next_follow_time)
SELECT
  -- related_type 轮转 customer/lead/business/contract
  ELT(1 + (n % 4), 'customer', 'lead', 'business', 'contract')                                                AS rt,
  -- related_id 映射到已有数据范围
  CASE ELT(1 + (n % 4), 'customer', 'lead', 'business', 'contract')
    WHEN 'customer' THEN 1 + (n % 8)
    WHEN 'lead'     THEN 1 + (n % 10)
    WHEN 'business' THEN 1 + (n % 6)
    ELSE                1 + (n % 18)
  END                                                                                                        AS rid,
  CONCAT('跟进记录 #', LPAD(n, 3, '0'), ' 客户沟通进展同步')                                                AS content,
  -- follow_type 轮转 电话/拜访/微信/邮件
  ELT(1 + (n % 4), '电话', '拜访', '微信', '邮件')                                                          AS ft,
  -- create_by 按 n%4 分配 4 个销售(sales_li/chen/zhao/qian)
  ELT(1 + (n % 4), 'sales_li', 'sales_chen', 'sales_zhao', 'sales_qian')                                    AS cb,
  -- create_time 跨 1-6 月,每月 40 条
  DATE_ADD('2026-01-01', INTERVAL FLOOR((n - 1) / 40) MONTH)
    + INTERVAL ((n - 1) % 40) * 30 MINUTE                                                                   AS ct,
  -- next_follow_time 比 create_time 晚 7-14 天
  DATE_ADD(DATE_ADD('2026-01-01', INTERVAL FLOOR((n - 1) / 40) MONTH)
    + INTERVAL ((n - 1) % 40) * 30 MINUTE, INTERVAL 7 + (n % 8) DAY)                                          AS nft
FROM (
  SELECT a.N + b.N * 10 + c.N * 100 AS n FROM
    (SELECT 1 AS N UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
     UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) a,
    (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
    (SELECT 0 AS N UNION ALL SELECT 1) c
) nums
WHERE n <= 240;

SELECT 'phase8 extended data: 240 follow records added' AS info;

-- ============ 扩充:补 40 条 6 月份跟进记录(n=201..240) ============
INSERT IGNORE INTO crm_record (related_type, related_id, content, follow_type, create_by, create_time, next_follow_time)
SELECT
  ELT(1 + (n % 4), 'customer', 'lead', 'business', 'contract')                                                AS rt,
  CASE ELT(1 + (n % 4), 'customer', 'lead', 'business', 'contract')
    WHEN 'customer' THEN 1 + (n % 8)
    WHEN 'lead'     THEN 1 + (n % 10)
    WHEN 'business' THEN 1 + (n % 6)
    ELSE                1 + (n % 18)
  END                                                                                                        AS rid,
  CONCAT('跟进记录 #', LPAD(n, 3, '0'), ' 6 月冲刺激活')                                                     AS content,
  ELT(1 + (n % 4), '电话', '拜访', '微信', '邮件')                                                          AS ft,
  ELT(1 + (n % 4), 'sales_li', 'sales_chen', 'sales_zhao', 'sales_qian')                                    AS cb,
  -- 6 月份 (n=201..240 → INTERVAL 5 MONTH = 2026-06-01)
  DATE_ADD('2026-01-01', INTERVAL 5 MONTH)
    + INTERVAL ((n - 201) % 40) * 30 MINUTE                                                                   AS ct,
  DATE_ADD(DATE_ADD('2026-01-01', INTERVAL 5 MONTH)
    + INTERVAL ((n - 201) % 40) * 30 MINUTE, INTERVAL 7 + (n % 8) DAY)                                         AS nft
FROM (
  SELECT a.N + b.N * 10 + c.N * 100 + 200 AS n FROM
    (SELECT 1 AS N UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) a,
    (SELECT 0 AS N) b,
    (SELECT 0 AS N) c
) nums
WHERE n >= 201 AND n <= 240;

SELECT 'phase8 extended data: +40 follow records (Jun)' AS info;

-- ============ 扩充:清空重插 240 条 crm_record 跟进记录(阶段八 P9) ============
-- 设计:跨 1-6 月均匀分布,4 个销售各 60 条,4 种 related_type / follow_type 轮转
DELETE FROM crm_record;

-- 1-5 月份 200 条
INSERT IGNORE INTO crm_record (related_type, related_id, content, follow_type, create_by, create_time, next_follow_time)
SELECT
  ELT(1 + (n % 4), 'customer', 'lead', 'business', 'contract') AS rt,
  CASE ELT(1 + (n % 4), 'customer', 'lead', 'business', 'contract')
    WHEN 'customer' THEN 1 + (n % 8)
    WHEN 'lead'     THEN 1 + (n % 10)
    WHEN 'business' THEN 1 + (n % 6)
    ELSE                1 + (n % 18)
  END AS rid,
  CONCAT('跟进记录 #', LPAD(n, 3, '0')) AS content,
  ELT(1 + (n % 4), '电话', '拜访', '微信', '邮件') AS ft,
  ELT(1 + (n % 4), 'sales_li', 'sales_chen', 'sales_zhao', 'sales_qian') AS cb,
  DATE_ADD('2026-01-01', INTERVAL FLOOR((n - 1) / 40) MONTH) + INTERVAL ((n - 1) % 40) * 30 MINUTE AS ct,
  DATE_ADD(DATE_ADD('2026-01-01', INTERVAL FLOOR((n - 1) / 40) MONTH) + INTERVAL ((n - 1) % 40) * 30 MINUTE, INTERVAL 7 + (n % 8) DAY) AS nft
FROM (
  SELECT a.N + b.N * 10 + c.N * 100 AS n FROM
    (SELECT 1 AS N UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
     UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) a,
    (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
    (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2) c
) nums
WHERE n >= 1 AND n <= 200;

-- 6 月份补 40 条
INSERT IGNORE INTO crm_record (related_type, related_id, content, follow_type, create_by, create_time, next_follow_time)
SELECT
  ELT(1 + (n % 4), 'customer', 'lead', 'business', 'contract') AS rt,
  CASE ELT(1 + (n % 4), 'customer', 'lead', 'business', 'contract')
    WHEN 'customer' THEN 1 + (n % 8)
    WHEN 'lead'     THEN 1 + (n % 10)
    WHEN 'business' THEN 1 + (n % 6)
    ELSE                1 + (n % 18)
  END AS rid,
  CONCAT('跟进记录 #', LPAD(n, 3, '0')) AS content,
  ELT(1 + (n % 4), '电话', '拜访', '微信', '邮件') AS ft,
  ELT(1 + (n % 4), 'sales_li', 'sales_chen', 'sales_zhao', 'sales_qian') AS cb,
  DATE_ADD('2026-06-01', INTERVAL (n - 201) * 30 MINUTE) AS ct,
  DATE_ADD(DATE_ADD('2026-06-01', INTERVAL (n - 201) * 30 MINUTE), INTERVAL 7 + (n % 8) DAY) AS nft
FROM (
  SELECT a.N + 200 AS n FROM
    (SELECT 1 AS N UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
     UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
     UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
     UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
     UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
     UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
     UNION ALL SELECT 31 UNION ALL SELECT 32 UNION ALL SELECT 33 UNION ALL SELECT 34 UNION ALL SELECT 35
     UNION ALL SELECT 36 UNION ALL SELECT 37 UNION ALL SELECT 38 UNION ALL SELECT 39 UNION ALL SELECT 40) a
) nums
WHERE n >= 201 AND n <= 240;

SELECT 'phase8 P9: 240 follow records (Jan-Jun, 4 sales evenly)' AS info;

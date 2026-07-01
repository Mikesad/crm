-- =====================================================================
--  Phase 8 数据权限测试数据集 (phase8 commit1)
--
--  目的:为 5 档 data_scope × 4 张受控表 提供可预测可见性的最小数据集
--  位置:sql/test-data/  (不进 migrations/, 仅本地/测试环境使用)
--  作者:claude, 2026-06-30
--
--  使用方法:
--    mysql -u root -p123456 crm_db < sql/test-data/phase8-data-permission-fixture.sql
--
--  重跑:每次开头 DELETE 测试数据 (id >= 900),保证幂等
--
--  =====================================================================
--  测试矩阵 (crm_customer 可见性, ✓=可见 / ✗=不可见)
--  ┌──────────┬──────────────────┬───────┬────────┬───────┬──────┬──────┬──────┬──────┐
--  │ 客户ID   │ owner/dept       │admin 1│dir 4 ds4│王 ds3 │李 ds5│赵 ds5│陈 ds5│钱 ds5│
--  ├──────────┼──────────────────┼───────┼────────┼───────┼──────┼──────┼──────┼──────┤
--  │ C901     │ 李销售 / 华东一组 │   ✓   │   ✓    │   ✗   │  ✓   │  ✗   │  ✗   │  ✗   │
--  │ C902     │ 赵销售 / 华东二组 │   ✓   │   ✓    │   ✗   │  ✗   │  ✓   │  ✗   │  ✗   │
--  │ C903     │ 王主管 / 华东销售部│   ✓  │   ✓    │   ✓   │  ✗   │  ✗   │  ✗   │  ✗   │
--  │ C904     │ 陈销售 / 华南一组 │   ✓   │   ✓    │   ✗   │  ✗   │  ✗   │  ✓   │  ✓   │
--  │ C905     │ 钱销售 / 华南一组 │   ✓   │   ✓    │   ✗   │  ✗   │  ✗   │  ✓   │  ✓   │
--  │ C906     │ 李销售 / 华东一组 │   ✓   │   ✓    │   ✗   │  ✓   │  ✓*  │  ✗   │  ✗   │
--  │          │ * 共享给赵销售 (crm_customer_share)                     │
--  │ C907     │ NULL / 公开 is_public=1│ ✓ │   ✓    │   ✓   │  ✓   │  ✓   │  ✓   │  ✓   │
--  │ C908     │ finance / 财务部  │   ✓   │   ✓    │   ✗   │  ✗   │  ✗   │  ✗   │  ✗   │
--  └──────────┴──────────────────┴───────┴────────┴───────┴──────┴──────┴──────┴──────┘
--  director 在 总公司(1) → scope=4 等同 scope=1 (所有子部门 ancestors 都含 1)
--  王主管 在 华东销售部(2) → scope=3 只看 owner_user_id IN (dept_id=2), 无其他人 → 仅看自己
--  =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 0. 清掉旧测试数据 (id >= 900) ----------
DELETE FROM crm_contact          WHERE customer_id >= 900;
DELETE FROM crm_contract_product WHERE contract_id >= 900;
DELETE FROM crm_receivable       WHERE contract_id >= 900;
DELETE FROM crm_receivable_plan  WHERE contract_id >= 900;
DELETE FROM crm_contract         WHERE id >= 900;
DELETE FROM crm_business         WHERE id >= 900;
DELETE FROM crm_customer         WHERE id >= 900;
DELETE FROM crm_lead             WHERE id >= 900;
DELETE FROM crm_customer_share   WHERE customer_id >= 900;
DELETE FROM crm_record           WHERE (related_type='customer' AND related_id >= 900)
                                  OR (related_type='lead'     AND related_id >= 900)
                                  OR (related_type='business' AND related_id >= 900)
                                  OR (related_type='contract' AND related_id >= 900);

SET FOREIGN_KEY_CHECKS = 1;

-- ---------- 1. 客户 (7 条, 覆盖 5 档 scope + 共享 + 公海) ----------
-- C901 李销售自有
INSERT INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, create_by, update_by)
VALUES (901, '测-T01 华东李自有客户', '制造业', 'B', 4, 0, 'admin', 'admin');
-- C902 赵销售自有
INSERT INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, create_by, update_by)
VALUES (902, '测-T02 华东赵自有客户', '服务业', 'A', 7, 0, 'admin', 'admin');
-- C903 王主管自有
INSERT INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, create_by, update_by)
VALUES (903, '测-T03 华东王主管自有', '金融业', 'A', 3, 0, 'admin', 'admin');
-- C904 陈销售自有
INSERT INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, create_by, update_by)
VALUES (904, '测-T04 华南陈自有客户', '教育业', 'B', 5, 0, 'admin', 'admin');
-- C905 钱销售自有
INSERT INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, create_by, update_by)
VALUES (905, '测-T05 华南钱自有客户', '医疗业', 'C', 8, 0, 'admin', 'admin');
-- C906 李销售自有, 但共享给赵销售 (ds=5 时, 赵销售通过 share 也能看到)
INSERT INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, create_by, update_by)
VALUES (906, '测-T06 共享客户(李→赵)', '互联网', 'A', 4, 0, 'admin', 'admin');
-- C907 公海池 (is_public=1, owner_user_id=NULL, 全员可见)
INSERT INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, create_by, update_by)
VALUES (907, '测-T07 公海池公开客户', '其他', 'C', NULL, 1, 'admin', 'admin');
-- C908 财务人员持有的客户 (极少,但验证 finance 也能是 owner)
INSERT INTO crm_customer (id, customer_name, industry, level, owner_user_id, is_public, create_by, update_by)
VALUES (908, '测-T08 财务持有客户',  '政府机构','B', 6, 0, 'admin', 'admin');

-- ---------- 2. 共享关系 (C906 李→赵) ----------
INSERT INTO crm_customer_share (customer_id, user_id, auth_type, create_by)
VALUES (906, 7, 1, 'admin');   -- 1=只读共享给赵销售
-- (待验证: 赵销售 ds=5 列表里能看到 C906, 但不能编辑)

-- ---------- 3. 联系人 (trust-parent 验证: 子表无 owner, 跟着父表走) ----------
INSERT INTO crm_contact (customer_id, contact_name, post, phone, is_master, decision_weight, create_by, update_by) VALUES
  (901, '李客户-主联系人', '采购总监', '13800000901', 1, 1, 'admin', 'admin'),
  (906, '李共享-主联系人', 'CTO',       '13800000906', 1, 1, 'admin', 'admin'),
  (907, '公海-主联系人',   '业务经理',   '13800000907', 1, 1, 'admin', 'admin');

-- ---------- 4. 线索 (4 条, 跨部门 + 转化后) ----------
-- L901 李销售自有 (未转化)
INSERT INTO crm_lead (id, lead_name, contact_name, phone, source, status, owner_user_id, create_by, update_by)
VALUES (901, '测-线索 李销售',  '李线索人', '13800001901', '广告', 2, 4, 'admin', 'admin');
-- L902 赵销售自有
INSERT INTO crm_lead (id, lead_name, contact_name, phone, source, status, owner_user_id, create_by, update_by)
VALUES (902, '测-线索 赵销售',  '赵线索人', '13800001902', '展会', 1, 7, 'admin', 'admin');
-- L903 陈销售自有
INSERT INTO crm_lead (id, lead_name, contact_name, phone, source, status, owner_user_id, create_by, update_by)
VALUES (903, '测-线索 陈销售',  '陈线索人', '13800001903', '线上留单', 2, 5, 'admin', 'admin');
-- L904 公海池线索 owner=NULL (ds=5 时, crm_lead 仍走 owner_user_id=me → 普通销售看不到,
--   即使 owner 是 NULL 也被 eq 过滤掉;admin/director 因 scope=1/4 仍可见)
INSERT INTO crm_lead (id, lead_name, contact_name, phone, source, status, owner_user_id, create_by, update_by)
VALUES (904, '测-线索 公海池', '公海线索人','13800001904', '电话', 2, NULL, 'admin', 'admin');

-- ---------- 5. 商机 (3 条, owner 跨部门) ----------
INSERT INTO crm_business (id, customer_id, business_name, expected_amount, expected_deal_date, stage, owner_user_id, create_by, update_by) VALUES
  (901, 901, '测-商机 李/华东客户', 500000.00, '2026-09-30', '方案报价', 4, 'admin', 'admin'),
  (902, 902, '测-商机 赵/华东客户', 300000.00, '2026-10-31', '商务谈判', 7, 'admin', 'admin'),
  (903, 903, '测-商机 王/华东主管', 800000.00, '2026-12-31', '需求分析', 3, 'admin', 'admin');

-- ---------- 6. 合同 (3 条, owner 跨部门) ----------
INSERT INTO crm_contract (id, contract_num, contract_name, customer_id, business_id, total_amount, start_date, end_date, status, owner_user_id, create_by, update_by) VALUES
  (901, 'TEST2026-C901', '测-合同 李销售', 901, 901, 480000.00, '2026-07-01', '2027-06-30', 1, 4, 'admin', 'admin'),
  (902, 'TEST2026-C902', '测-合同 赵销售', 902, 902, 290000.00, '2026-07-01', '2027-06-30', 1, 7, 'admin', 'admin'),
  (903, 'TEST2026-C903', '测-合同 王主管', 903, 903, 760000.00, '2026-07-01', '2027-06-30', 1, 3, 'admin', 'admin');

-- ---------- 7. 合同明细 (跟合同走, 不在受控表) ----------
INSERT INTO crm_contract_product (contract_id, product_id, count, standard_price, sales_price, discount) VALUES
  (901, 1, 10, 50000.00, 48000.00, 9.60),
  (902, 2,  5, 60000.00, 58000.00, 9.67),
  (903, 1, 15, 52000.00, 50666.67, 9.74);

-- ---------- 8. 跟进记录 (trust-parent: 无 owner, 跟父表 related_id) ----------
INSERT INTO crm_record (related_type, related_id, content, follow_type, create_by) VALUES
  ('customer', 901, '李客户首次拜访,对方采购总监感兴趣',    '上门拜访', 'sales_li'),
  ('customer', 906, '共享客户: 赵销售协助跟进',           '电话',     'sales_zhao'),
  ('customer', 907, '公海客户: 全员可见跟进',             '系统',     'admin'),
  ('lead',     901, '李销售线索: 已联系 3 次',             '电话',     'sales_li'),
  ('lead',     904, '公海线索: 已重新分给李销售',          '系统',     'admin'),
  ('business', 901, '李商机: 已发方案,等对方反馈',         '邮件',     'sales_li'),
  ('contract', 901, '李合同: 已签约,等回款',               '系统',     'sales_li');

-- =====================================================================
--  验证 SQL (执行后跑一下确认):
--  -- 切到不同账号登录查看客户列表数:
--  -- admin:     SELECT COUNT(*) FROM crm_customer WHERE id>=900;        -- 期望 8
--  -- director:  SELECT COUNT(*) FROM crm_customer WHERE id>=900;        -- 期望 8 (scope=4 在总公司等价于 scope=1)
--  -- 王主管:    SELECT COUNT(*) FROM crm_customer WHERE id>=900;        -- 期望 2 (C903 自有 + C907 公海)
--  -- 李销售:    SELECT COUNT(*) FROM crm_customer WHERE id>=900;        -- 期望 3 (C901/C906 自有 + C907 公海)
--  -- 赵销售:    SELECT COUNT(*) FROM crm_customer WHERE id>=900;        -- 期望 3 (C902 自有 + C906 共享 + C907 公海)
--  -- 陈销售:    SELECT COUNT(*) FROM crm_customer WHERE id>=900;        -- 期望 3 (C904/C905 同部门 + C907 公海)
--  -- 钱销售:    SELECT COUNT(*) FROM crm_customer WHERE id>=900;        -- 期望 3 (C904/C905 同部门 + C907 公海)
--  =====================================================================
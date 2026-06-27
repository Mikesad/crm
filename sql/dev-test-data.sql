-- =====================================================================
--  ZenCRM dev 测试数据
--  适用：dev 环境，在已初始化过 crm_full.sql 的数据库上跑一次
--  用法：mysql -u root -p crm_db < dev-test-data.sql
--
--  行为：TRUNCATE 5 张业务表后,用固定 ID(101+)重灌测试数据,
--        不动 sys_user / sys_role / sys_menu 等基础表。
--  依赖：crm_full.sql 已建表;sys_user 6 个种子账号 ID = 1..6
--        1 admin / 2 director / 3 lead_wang(华东)
--        4 sales_li(华东) / 5 sales_chen(华南) / 6 finance
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 0) 清表(保留结构,固定 ID 重置) ----------
TRUNCATE TABLE crm_lead;
TRUNCATE TABLE crm_customer;
TRUNCATE TABLE crm_contact;
TRUNCATE TABLE crm_business;
TRUNCATE TABLE crm_record;

-- ---------- 1) 线索 crm_lead(10 条) ----------
-- 状态机:1 未跟进 / 2 跟进中 / 3 已转客户 / 4 已死线索
INSERT INTO `crm_lead` (`id`, `lead_name`, `contact_name`, `phone`, `source`, `status`, `owner_user_id`, `remark`, `create_by`) VALUES
  (101, '上海蓝芯科技',     '王经理', '13900001001', '百度推广',    1, 4, '官网留资未联系',         'sales_li'),
  (102, '深圳极视智能',     '陈总',   '13900001002', '线下展会',    1, 5, '展会扫码,意向 ERP',       'sales_chen'),
  (103, '杭州云策信息',     '林主管', '13900001003', '客户介绍',    2, 4, '需求待确认,本周回访',     'sales_li'),
  (104, '广州拓远电子',     '吴总监', '13900001004', '线上留单',    2, 5, '价格敏感,需方案支持',     'sales_chen'),
  (105, '苏州瀚海数据',     '周总',   '13900001005', '广告投放',    3, 4, '已转客户,见 customer 101','sales_li'),
  (106, '宁波锐驰自动化',   '赵工',   '13900001006', '电话咨询',    3, 5, '已转客户,见 customer 102','sales_chen'),
  (107, '南京恒盛机械',     '钱总',   '13900001007', '陌拜',        4, 4, '对方无预算,放弃',         'sales_li'),
  (108, '武汉合纵智造',     '孙总',   '13900001008', '线上留单',    4, 5, '竞品已签,流失',           'sales_chen'),
  (109, '成都极星软件',     '李总',   '13900001009', '客户介绍',    2, 3, '王主管指派,跟进中',       'lead_wang'),
  (110, '厦门远帆科技',     '徐经理', '13900001010', '百度推广',    1, 3, '新线索待分配',             'lead_wang');

-- ---------- 2) 客户 crm_customer(8 条) ----------
-- level: A 重要 / B 普通 / C 意向;is_public:0 私海 / 1 公海
-- 105/106 号线索已转客户(对应 customer 101/102)
INSERT INTO `crm_customer` (`id`, `customer_name`, `industry`, `level`, `owner_user_id`, `is_public`, `last_follow_time`, `create_by`) VALUES
  (101, '上海瀚海数据股份有限公司', '数据服务',     'A', 4, 0, '2026-06-26 10:30:00', 'sales_li'),
  (102, '宁波锐驰自动化设备有限公司','智能制造',   'A', 5, 0, '2026-06-25 14:00:00', 'sales_chen'),
  (103, '杭州远帆供应链管理有限公司','物流',       'B', 4, 0, '2026-06-20 09:15:00', 'sales_li'),
  (104, '广州蓝芯跨境电商有限公司',  '跨境电商',   'B', 5, 0, '2026-06-18 16:20:00', 'sales_chen'),
  (105, '深圳极视智能终端有限公司',  '硬件',       'C', 5, 0, '2026-05-30 11:00:00', 'sales_chen'),
  (106, '苏州合纵精密制造有限公司',  '精密制造',   'A', 4, 0, '2026-06-22 15:45:00', 'sales_li'),
  (107, '杭州远帆贸易有限公司',      '贸易',       'B', NULL, 1, '2026-04-10 10:00:00', 'system'),
  (108, '成都极星软件有限公司',      '软件',       'C', NULL, 1, NULL,                  'system');

-- ---------- 3) 联系人 crm_contact(12 条,每个主客户配 1-2 个) ----------
-- decision_weight:1 核心决策者 / 2 弱影响者 / 3 普通职员
INSERT INTO `crm_contact` (`id`, `customer_id`, `contact_name`, `post`, `phone`, `is_master`, `decision_weight`, `create_by`) VALUES
  (201, 101, '周建国', '总经理',     '13900002001', 1, 1, 'sales_li'),
  (202, 101, '张丽华', '采购总监',   '13900002002', 0, 2, 'sales_li'),
  (203, 102, '赵振华', 'CTO',       '13900002003', 1, 1, 'sales_chen'),
  (204, 102, '孙文博', '技术经理',   '13900002004', 0, 3, 'sales_chen'),
  (205, 103, '林晓燕', '运营总监',   '13900002005', 1, 1, 'sales_li'),
  (206, 104, '吴佩珊', '采购经理',   '13900002006', 1, 2, 'sales_chen'),
  (207, 104, '郑伟豪', '技术总监',   '13900002007', 0, 2, 'sales_chen'),
  (208, 105, '黄子轩', '项目主管',   '13900002008', 1, 2, 'sales_chen'),
  (209, 106, '钱思琪', '总经理',     '13900002009', 1, 1, 'sales_li'),
  (210, 106, '李建斌', '生产总监',   '13900002010', 0, 2, 'sales_li'),
  (211, 107, '徐丽萍', '采购经理',   '13900002011', 1, 2, 'system'),
  (212, 108, '马俊杰', '产品负责人', '13900002012', 1, 1, 'system');

-- ---------- 4) 商机 crm_business(8 条,覆盖全阶段) ----------
-- stage:需求分析 / 方案报价 / 商务谈判 / 赢单 / 输单
INSERT INTO `crm_business` (`id`, `customer_id`, `business_name`, `expected_amount`, `expected_deal_date`, `stage`, `owner_user_id`, `create_by`) VALUES
  (301, 101, '瀚海数据 - 数据中台一期',     480000.00, '2026-08-30', '需求分析', 4, 'sales_li'),
  (302, 103, '远帆供应链 - WMS 升级',        260000.00, '2026-09-15', '需求分析', 4, 'sales_li'),
  (303, 102, '锐驰自动化 - 视觉检测设备',   860000.00, '2026-09-30', '方案报价', 5, 'sales_chen'),
  (304, 106, '合纵精密 - MES 系统',         620000.00, '2026-10-20', '方案报价', 4, 'sales_li'),
  (305, 101, '瀚海数据 - 报表 BI 项目',     180000.00, '2026-07-25', '商务谈判', 4, 'sales_li'),
  (306, 102, '锐驰自动化 - 备件管理 SaaS',  120000.00, '2026-08-10', '商务谈判', 5, 'sales_chen'),
  (307, 106, '合纵精密 - 二期扩容',         320000.00, '2026-07-30', '赢单',     4, 'sales_li'),
  (308, 104, '蓝芯电商 - 全渠道中台',       420000.00, '2026-07-15', '输单',     5, 'sales_chen');

-- ---------- 5) 跟进记录 crm_record(8 条,带 last_follow_time) ----------
-- related_type:lead-线索 / customer-客户 / business-商机
INSERT INTO `crm_record` (`id`, `related_type`, `related_id`, `content`, `follow_type`, `next_follow_time`, `create_by`) VALUES
  (401, 'lead',     103, '电话沟通 30 分钟,客户有 ERP 替换意向,约周四上门',     '电话',       '2026-06-29 10:00:00', 'sales_li'),
  (402, 'lead',     104, '初步报价已发送,客户反馈价格偏高,准备差异化方案',     '微信',       '2026-06-28 15:00:00', 'sales_chen'),
  (403, 'customer', 101, '周总确认走 BI 项目,商务谈判中,关注付款周期',          '上门拜访',   '2026-06-30 14:00:00', 'sales_li'),
  (404, 'customer', 102, '锐驰二期备件 SaaS 已谈定,等法务审核合同',              '电话',       '2026-06-29 11:00:00', 'sales_chen'),
  (405, 'customer', 106, 'MES 系统 POC 通过,客户提出二期扩容意向',               '上门拜访',   '2026-07-02 09:30:00', 'sales_li'),
  (406, 'business', 303, '方案 V2 提交,客户对硬件选型有疑问,安排技术会',         '邮件',       '2026-06-30 16:00:00', 'sales_chen'),
  (407, 'business', 305, '商务谈判关键点:付款比例 5/3/2 vs 客户要求 4/4/2',      '上门拜访',   '2026-06-29 14:00:00', 'sales_li'),
  (408, 'business', 308, '输单复盘:价格差距 12%,客户最终选了竞品,保持关系',       '电话',       NULL,                  'sales_chen');

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
--  验证(执行完跑一下):
--  SELECT status, COUNT(*) FROM crm_lead     GROUP BY status;
--  SELECT level,  COUNT(*) FROM crm_customer GROUP BY level;
--  SELECT stage,  COUNT(*) FROM crm_business GROUP BY stage;
--  SELECT related_type, COUNT(*) FROM crm_record GROUP BY related_type;
--  SELECT c.id, c.customer_name, COUNT(b.id) AS biz_count
--    FROM crm_customer c LEFT JOIN crm_business b ON b.customer_id = c.id
--   GROUP BY c.id, c.customer_name ORDER BY c.id;
-- =====================================================================

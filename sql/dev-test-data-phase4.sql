-- =====================================================================
--  ZenCRM 阶段四演示数据(公海池 + 客户共享)
--  适用：已跑过 dev-test-data.sql 的开发环境
--  用法：mysql -u root -p crm_db < dev-test-data-phase4.sql
--
--  行为(均幂等):
--    1. 清空 crm_customer_share(阶段四新表)
--    2. 追加 12 个新客户(201~212,含 5 个公海池)
--    3. 追加 14 个联系人 + 20 条跟进记录(让私海客户时间轴有内容)
--    4. 插入 5 条共享记录(覆盖读写/只读两种权限)
--    5. 更新部分老客户(101/102)加共享 + 1 个公海
--    6. 给公海客户插一条系统回收记录(可视化"已回收"事件)
--
--  依赖:
--    - crm_full.sql 已初始化(用户 ID 1~6,客户 ID 101~108)
--    - dev-test-data.sql 已跑过(基础线索/客户/商机)
--
--  最终效果:
--    - sales_li (id=4) 私海客户:101, 103, 106, 201, 202, 203
--    - sales_chen (id=5) 私海客户:102, 104, 105, 206, 207, 208
--    - lead_wang (id=3) 私海客户:209
--    - 公海池:107, 108, 210, 211, 212, 213, 214, 215(8 个)
--    - 共享表命中:
--        sales_chen 被共享:101(读写), 201(只读)
--        sales_li   被共享:102(读写), 206(只读), 209(只读)
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 0) 幂等清理(只清本脚本会写入的记录,不动 dev-test-data 的 101~108 数据) ----------
TRUNCATE TABLE crm_customer_share;                          -- 共享表全新
DELETE FROM crm_record       WHERE id BETWEEN 500 AND 999;   -- 本脚本插入的 20 条 record
DELETE FROM crm_business     WHERE id BETWEEN 400 AND 499;   -- 本脚本插入的 8 条 business
DELETE FROM crm_contact      WHERE id BETWEEN 300 AND 399;   -- 本脚本插入的 14 条 contact
DELETE FROM crm_customer     WHERE id BETWEEN 200 AND 299;   -- 本脚本插入的 14 条 customer
-- 注意:不删 dev-test-data.sql 插入的 101~108(那些是基础数据)

-- ---------- 2) 追加 12 个新客户(ID 201~212) ----------
-- 私海客户 lastFollowTime 从"今天"到"12 天前"分布,展示跟进时间色彩
-- 公海客户 lastFollowTime 30~60 天前,触发回收规则
INSERT INTO `crm_customer` (`id`, `customer_name`, `industry`, `level`, `owner_user_id`, `is_public`, `last_follow_time`, `create_by`) VALUES
  -- sales_li 名下
  (201, '大疆创新科技有限公司',       '智能硬件',     'A', 4, 0, '2026-06-24 10:30:00', 'sales_li'),
  (202, '蔚来汽车销售服务有限公司',   '新能源汽车',   'B', 4, 0, '2026-06-19 16:00:00', 'sales_li'),
  (203, '小红书科技有限公司',         '互联网',       'B', 4, 0, '2026-06-15 11:20:00', 'sales_li'),
  (204, '中通快递股份有限公司',       '物流',         'C', 4, 0, '2026-06-15 09:00:00', 'sales_li'),
  -- sales_chen 名下
  (205, '京东物流科技有限公司',       '物流',         'A', 5, 0, '2026-06-25 14:00:00', 'sales_chen'),
  (206, '比亚迪股份有限公司',         '新能源汽车',   'A', 5, 0, '2026-06-26 09:30:00', 'sales_chen'),
  (207, '理想汽车销售服务有限公司',   '新能源汽车',   'B', 5, 0, '2026-06-22 15:00:00', 'sales_chen'),
  (208, '美的集团股份有限公司',       '家电制造',     'A', 5, 0, '2026-06-22 10:45:00', 'sales_chen'),
  -- lead_wang 名下
  (209, '招商银行股份有限公司',       '金融',         'A', 3, 0, '2026-06-26 17:00:00', 'lead_wang'),
  -- 公海池(5 个,owner 清空,lastFollowTime 30~60 天前)
  (210, '深圳顺丰速运有限公司',       '物流',         'B', NULL, 1, '2026-05-25 10:00:00', 'system'),
  (211, '宁波均胜电子股份有限公司',   '汽车电子',     'C', NULL, 1, '2026-04-20 14:00:00', 'system'),
  (212, '青岛海尔智能家居有限公司',   '智能家居',     'A', NULL, 1, '2026-05-05 11:30:00', 'system'),
  (213, '广州网易计算机系统有限公司', '互联网',       'B', NULL, 1, '2026-04-15 16:20:00', 'system'),
  (214, '杭州蚂蚁集团股份有限公司',   '金融科技',     'A', NULL, 1, NULL,                  'system');

-- ---------- 3) 给新客户配联系人(ID 300+) ----------
INSERT INTO `crm_contact` (`id`, `customer_id`, `contact_name`, `post`, `phone`, `is_master`, `decision_weight`, `create_by`) VALUES
  (301, 201, '汪滔',     '创始人兼CEO', '13900003001', 1, 1, 'sales_li'),
  (302, 201, '罗镇威',   '采购总监',   '13900003002', 0, 2, 'sales_li'),
  (303, 202, '秦力洪',   '联合创始人', '13900003003', 1, 1, 'sales_li'),
  (304, 203, '毛文超',   '电商负责人', '13900003004', 1, 2, 'sales_li'),
  (305, 204, '赖建法',   '运营总监',   '13900003005', 1, 1, 'sales_li'),
  (306, 205, '王振辉',   'CTO',       '13900003006', 1, 1, 'sales_chen'),
  (307, 206, '王传福',   '董事长',     '13900003007', 1, 1, 'sales_chen'),
  (308, 206, '廉玉波',   '副总裁',     '13900003008', 0, 1, 'sales_chen'),
  (309, 207, '李想',     '董事长',     '13900003009', 1, 1, 'sales_chen'),
  (310, 208, '方洪波',   '董事长',     '13900003010', 1, 1, 'sales_chen'),
  (311, 209, '缪建民',   '行长',       '13900003011', 1, 1, 'lead_wang'),
  (312, 210, '王卫',     '创始人',     '13900003012', 1, 1, 'system'),
  (313, 211, '王剑峰',   '总经理',     '13900003013', 1, 1, 'system'),
  (314, 212, '梁海山',   '总裁',       '13900003014', 1, 1, 'system');

-- ---------- 4) 给新客户追加商机(ID 400+) ----------
INSERT INTO `crm_business` (`id`, `customer_id`, `business_name`, `expected_amount`, `expected_deal_date`, `stage`, `owner_user_id`, `create_by`) VALUES
  (401, 201, '大疆 - 行业无人机平台',    1200000.00, '2026-10-15', '需求分析', 4, 'sales_li'),
  (402, 201, '大疆 - 农业植保机 SaaS',    380000.00, '2026-09-10', '方案报价', 4, 'sales_li'),
  (403, 202, '蔚来 - 充电桩物联网',       580000.00, '2026-09-20', '方案报价', 4, 'sales_li'),
  (404, 205, '京东物流 - 智能仓储 WMS',  880000.00, '2026-10-30', '商务谈判', 5, 'sales_chen'),
  (405, 206, '比亚迪 - 工厂 IoT 升级',   1500000.00, '2026-11-15', '需求分析', 5, 'sales_chen'),
  (406, 208, '美的 - 智能制造 SaaS',      720000.00, '2026-09-25', '赢单',     5, 'sales_chen'),
  (407, 209, '招行 - 私有云迁移',        2200000.00, '2026-12-30', '需求分析', 3, 'lead_wang'),
  (408, 210, '顺丰 - 大数据可视化',       340000.00, '2026-09-15', '需求分析', NULL, 'system');

-- ---------- 5) 5 条共享记录 ----------
-- 设计:覆盖 owner→user 的双向、读写/只读两档
-- sales_chen 被共享:101(读写,by sales_li)、201(只读,by sales_li)
-- sales_li   被共享:102(读写,by sales_chen)、206(只读,by sales_chen)、209(只读,by lead_wang)
INSERT INTO `crm_customer_share` (`id`, `customer_id`, `user_id`, `auth_type`, `create_by`) VALUES
  (1, 101, 5, 2, 'sales_li'),     -- sales_li 把瀚海数据共享给 sales_chen (读写)
  (2, 201, 5, 1, 'sales_li'),     -- sales_li 把大疆共享给 sales_chen (只读)
  (3, 102, 4, 2, 'sales_chen'),   -- sales_chen 把锐驰共享给 sales_li (读写)
  (4, 206, 4, 1, 'sales_chen'),   -- sales_chen 把比亚迪共享给 sales_li (只读)
  (5, 209, 4, 1, 'lead_wang');    -- lead_wang 把招行共享给 sales_li (只读)

-- ---------- 6) 给新客户追加跟进记录(ID 500+) ----------
-- 让时间轴更丰富
INSERT INTO `crm_record` (`id`, `related_type`, `related_id`, `content`, `follow_type`, `next_follow_time`, `create_by`) VALUES
  (501, 'customer', 201, '大疆周总介绍完需求,产品技术下周上门做 POC',           '上门拜访',   '2026-06-30 14:00:00', 'sales_li'),
  (502, 'customer', 201, '客户对 SDK 集成方式有疑问,转给技术同事出方案',         '电话',       '2026-06-28 10:00:00', 'sales_li'),
  (503, 'customer', 202, '蔚来充电桩方案 V1 已提交,等客户内部评估',                '邮件',       '2026-07-01 15:00:00', 'sales_li'),
  (504, 'customer', 203, '小红书广告投放数据回传,讨论第二期续费',                  '微信',       '2026-06-29 11:00:00', 'sales_li'),
  (505, 'customer', 204, '中通业务体量大,需走大客户部流程,正在准备材料',           '上门拜访',   '2026-06-29 16:00:00', 'sales_li'),
  (506, 'customer', 205, '京东物流 WMS POC 已通过,商务谈判启动',                  '上门拜访',   '2026-06-29 10:00:00', 'sales_chen'),
  (507, 'customer', 206, '比亚迪 IoT 升级项目立项,需要技术架构师现场调研',         '上门拜访',   '2026-07-02 14:00:00', 'sales_chen'),
  (508, 'customer', 207, '理想汽车经销商系统演示,客户反馈功能完整,价格偏高',        '电话',       '2026-06-30 11:00:00', 'sales_chen'),
  (509, 'customer', 208, '美的智能制造一期合同签订,启动交付',                      '上门拜访',   '2026-06-30 09:30:00', 'sales_chen'),
  (510, 'customer', 209, '招行私有云需求确认,RFP 撰写中,目标 12 月签约',          '上门拜访',   '2026-07-05 14:00:00', 'lead_wang'),
  (511, 'business', 402, '农业植保机方案 V1 客户反馈选型方向,调整中',               '邮件',       '2026-06-30 16:00:00', 'sales_li'),
  (512, 'business', 404, '京东 WMS 商务条款讨论,价格 + 服务等级',                   '电话',       '2026-06-29 14:00:00', 'sales_chen'),
  (513, 'business', 405, '比亚迪 IoT 立项会纪要:首期 200 个采集点 + 1 个平台',     '上门拜访',   '2026-07-01 10:00:00', 'sales_chen'),
  (514, 'business', 406, '美的智能制造合同已上传法务,等审批',                       '邮件',       '2026-06-29 17:00:00', 'sales_chen'),
  (515, 'business', 407, '招行私有云 RFP 初稿内审中,需要安全合规同事过一遍',       '上门拜访',   '2026-07-01 15:00:00', 'lead_wang');

-- ---------- 7) 给公海客户补一条系统回收记录(可视化"已回收"事件) ----------
INSERT INTO `crm_record` (`id`, `related_type`, `related_id`, `content`, `follow_type`, `next_follow_time`, `create_by`, `create_time`) VALUES
  (601, 'customer', 210, '由于超过 32 天未跟进,系统自动将该客户回收至公海池。',  '系统',  NULL, 'system', '2026-05-26 02:00:00'),
  (602, 'customer', 211, '由于超过 67 天未跟进,系统自动将该客户回收至公海池。',  '系统',  NULL, 'system', '2026-04-21 02:00:00'),
  (603, 'customer', 212, '由于超过 52 天未跟进,系统自动将该客户回收至公海池。',  '系统',  NULL, 'system', '2026-05-06 02:00:00'),
  (604, 'customer', 213, '由于超过 72 天未跟进,系统自动将该客户回收至公海池。',  '系统',  NULL, 'system', '2026-04-16 02:00:00'),
  (605, 'customer', 214, '由于从未跟进,原 owner 离职时移入公海池。',              '系统',  NULL, 'system', '2026-04-01 10:00:00');

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
--  验证(执行后跑一下):
--  -- 数据总览
--  SELECT is_public, COUNT(*) FROM crm_customer WHERE is_deleted=0 GROUP BY is_public;
--  SELECT level, COUNT(*) FROM crm_customer WHERE is_deleted=0 GROUP BY level;
--
--  -- 私海客户(sales_li 视角,只看 owner=4)
--  SELECT id, customer_name, level, last_follow_time
--    FROM crm_customer WHERE owner_user_id=4 AND is_public=0 AND is_deleted=0 ORDER BY id;
--
--  -- 公海池
--  SELECT id, customer_name, level, last_follow_time
--    FROM crm_customer WHERE is_public=1 AND is_deleted=0 ORDER BY last_follow_time;
--
--  -- 共享(sales_chen 被共享给我的)
--  SELECT cs.id, c.customer_name, c.owner_user_id, cs.user_id, cs.auth_type
--    FROM crm_customer_share cs JOIN crm_customer c ON cs.customer_id=c.id
--   WHERE cs.user_id=5;
--
--  -- 3 Tab 期望数据量(sales_chen 登录后):
--  -- 私海:102, 104, 105, 205, 206, 207, 208 + 共享命中 101(读写) + 201(只读) = 8 个
--  -- 共享给我:101 + 201 = 2 个
--  -- 公海池:107, 108, 210, 211, 212, 213, 214 = 7 个
-- =====================================================================

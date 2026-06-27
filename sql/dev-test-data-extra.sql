-- =====================================================================
--  ZenCRM dev 测试数据 - 追加 20 条
--  适用：dev 环境,在 dev-test-data.sql 之后跑,直接追加不动旧数据
--  用法:mysql -u root -p crm_db < dev-test-data-extra.sql
--
--  分布:线索 +5 / 客户 +5 / 联系人 +5 / 跟进记录 +5
--  继续使用 ID 段:111+ / 109+ / 213+ / 409+
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 线索 crm_lead(+5 条,id 111-115) ----------
-- 状态 1/2/3/4 全覆盖,补 1 条王主管(华东)名下跟进中
INSERT INTO `crm_lead` (`id`, `lead_name`, `contact_name`, `phone`, `source`, `status`, `owner_user_id`, `remark`, `create_by`) VALUES
  (111, '天津北辰智能',     '高经理', '13900001011', '百度推广',     1, 4, '今日留资,需快速响应',     'sales_li'),
  (112, '重庆星驰物联',     '邓总',   '13900001012', '客户介绍',     2, 5, '客户主动询价,需方案',     'sales_chen'),
  (113, '长沙宏图医疗',     '罗主任', '13900001013', '线下展会',     3, 4, '已转客户,见 customer 109','sales_li'),
  (114, '青岛海联冷链',     '宋总监', '13900001014', '线上留单',     4, 5, '预算砍半,放弃',           'sales_chen'),
  (115, '合肥科讯半导体',   '韩经理', '13900001015', '客户介绍',     2, 3, '王主管跨部门跟进',         'lead_wang');

-- ---------- 客户 crm_customer(+5 条,id 109-113) ----------
-- 2 条公海(is_public=1)用于测试公海池/阶段四回收
INSERT INTO `crm_customer` (`id`, `customer_name`, `industry`, `level`, `owner_user_id`, `is_public`, `last_follow_time`, `create_by`) VALUES
  (109, '长沙宏图医疗科技有限公司',     '医疗器械',     'A', 4, 0, '2026-06-26 16:00:00', 'sales_li'),
  (110, '东莞精工光电有限公司',         '光电',         'B', 5, 0, '2026-06-24 11:30:00', 'sales_chen'),
  (111, '福州海丝跨境电商有限公司',     '跨境电商',     'B', 4, 0, '2026-06-15 14:20:00', 'sales_li'),
  (112, '济南鲁信智能装备有限公司',     '智能装备',     'A', 3, 0, '2026-05-10 09:00:00', 'lead_wang'),
  (113, '无锡太湖软件园入驻企业',       '软件',         'C', NULL, 1, '2026-03-20 10:00:00', 'system');

-- ---------- 联系人 crm_contact(+5 条,id 213-217) ----------
-- 给新客户 customer 109/110/111/112 配主联系人 + 1 个副联系人
INSERT INTO `crm_contact` (`id`, `customer_id`, `contact_name`, `post`, `phone`, `is_master`, `decision_weight`, `create_by`) VALUES
  (213, 109, '罗致远', '采购主任',     '13900002013', 1, 1, 'sales_li'),
  (214, 110, '段嘉欣', '技术经理',     '13900002014', 1, 2, 'sales_chen'),
  (215, 110, '苏明远', '采购助理',     '13900002015', 0, 3, 'sales_chen'),
  (216, 111, '何子涵', '运营总监',     '13900002016', 1, 1, 'sales_li'),
  (217, 112, '谢安平', '生产副总',     '13900002017', 1, 1, 'lead_wang');

-- ---------- 跟进记录 crm_record(+5 条,id 409-413) ----------
-- 关联已存在的主体,丰富时间轴
INSERT INTO `crm_record` (`id`, `related_type`, `related_id`, `content`, `follow_type`, `next_follow_time`, `create_by`) VALUES
  (409, 'lead',     111, '客户官网留资 5 分钟,需 1 小时内首呼',           '电话',       '2026-06-27 17:00:00', 'sales_li'),
  (410, 'lead',     112, '客户要求提供 SaaS + 私有化两套方案',             '微信',       '2026-06-29 10:30:00', 'sales_chen'),
  (411, 'customer', 109, '医疗设备管理系统 POC 启动,本周交付初版',          '上门拜访',   '2026-06-30 09:00:00', 'sales_li'),
  (412, 'customer', 112, '客户战略调整,二期项目暂缓,保持月度沟通',          '电话',       '2026-07-15 14:00:00', 'lead_wang'),
  (413, 'business', 307, '二期合同法务审核通过,准备签约',                    '邮件',       '2026-06-28 11:00:00', 'sales_li');

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
--  验证:
--  SELECT (SELECT COUNT(*) FROM crm_lead)     AS lead_cnt,
--         (SELECT COUNT(*) FROM crm_customer) AS cust_cnt,
--         (SELECT COUNT(*) FROM crm_contact)   AS contact_cnt,
--         (SELECT COUNT(*) FROM crm_business)  AS biz_cnt,
--         (SELECT COUNT(*) FROM crm_record)    AS rec_cnt;
-- =====================================================================

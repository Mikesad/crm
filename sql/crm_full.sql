-- =============================================================
--  ZenCRM 完整初始化脚本（含种子数据）
--  适用版本：crm-backend v1.0.0
--  用法：mysql -u root -p < crm_full.sql
--
--  包含：18 张表 DDL + 6 个测试账号 + 5 个角色 + 最小权限集
--  密码：所有种子账号统一为 123456（BCrypt 加密值见下方）
--  重新生成 hash：cd backend-tools/crm-tools && mvn exec:java -Dexec.args="123456"
-- =============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ====================== DDL ======================

-- 1. 部门表
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` bigint DEFAULT 0 COMMENT '父部门id',
  `ancestors` varchar(200) DEFAULT '' COMMENT '祖级列表(如: 0,1,3)',
  `dept_name` varchar(50) NOT NULL COMMENT '部门名称',
  `order_num` int DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint DEFAULT 1 COMMENT '部门状态（0停用 1正常）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除（0未删 1已删）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 2. 用户表
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `username` varchar(30) NOT NULL COMMENT '用户账号',
  `nickname` varchar(30) NOT NULL COMMENT '用户昵称',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
  `phone` varchar(11) DEFAULT '' COMMENT '手机号码',
  `sex` tinyint DEFAULT 0 COMMENT '用户性别（0男 1女 2未知）',
  `status` tinyint DEFAULT 1 COMMENT '帐号状态（0停用 1正常）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除（0未删 1已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 角色表
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串(如 admin, sales)',
  `data_scope` tinyint DEFAULT 5 COMMENT '数据范围（1全部 2自定义 3本部门 4本部门及以下 5仅本人）',
  `status` tinyint DEFAULT 1 COMMENT '角色状态（0停用 1正常）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除（0未删 1已删）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 4. 菜单与功能权限表
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `perms` varchar(100) DEFAULT '' COMMENT '权限标识(如 crm:customer:delete)',
  `status` tinyint DEFAULT 1 COMMENT '菜单状态（0隐藏 1显示）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 5. 用户-角色关联表
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- 6. 角色-菜单关联表
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和菜单关联表';

-- 7. 线索表
CREATE TABLE `crm_lead` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '线索ID',
  `lead_name` varchar(100) NOT NULL COMMENT '线索名称/公司暂定名',
  `contact_name` varchar(30) NOT NULL COMMENT '联系人姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '电话/手机',
  `source` varchar(50) DEFAULT NULL COMMENT '线索来源(如:广告/展会/线上留单)',
  `status` tinyint DEFAULT 1 COMMENT '状态（1未跟进 2跟进中 3已转客户 4已死线索）',
  `owner_user_id` bigint DEFAULT NULL COMMENT '负责人ID（逻辑关联sys_user.id）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注描述',
  `dead_reason` varchar(500) DEFAULT NULL COMMENT '死线索原因(可选,阶段五新增)',
  `dead_time` datetime DEFAULT NULL COMMENT '死线索标记时间(阶段五新增)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_owner_user` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线索表';

-- 8. 客户表
CREATE TABLE `crm_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `customer_name` varchar(100) NOT NULL COMMENT '客户公司/主体名称',
  `industry` varchar(50) DEFAULT NULL COMMENT '所属行业',
  `level` char(1) DEFAULT 'C' COMMENT '客户级别(A重要客户 B普通客户 C意向客户)',
  `owner_user_id` bigint DEFAULT NULL COMMENT '归属销售ID（逻辑关联sys_user.id，为NULL且is_public=1则表示在公海）',
  `is_public` tinyint DEFAULT 0 COMMENT '是否为公海客户（0私海 1公海）',
  `last_follow_time` datetime DEFAULT NULL COMMENT '最后跟进时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_owner_user` (`owner_user_id`),
  KEY `idx_customer_name` (`customer_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 9. 联系人表
CREATE TABLE `crm_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人ID',
  `customer_id` bigint NOT NULL COMMENT '所属客户ID（逻辑关联crm_customer.id）',
  `contact_name` varchar(30) NOT NULL COMMENT '联系人姓名',
  `post` varchar(50) DEFAULT NULL COMMENT '职务/职位',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `is_master` tinyint DEFAULT 0 COMMENT '是否为主联系人（0否 1是）',
  `decision_weight` tinyint DEFAULT 3 COMMENT '决策权重(1核心决策者 2弱影响者 3普通职员)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人表';

-- 10. 客户共享表
CREATE TABLE `crm_customer_share` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_id` bigint NOT NULL COMMENT '客户ID（逻辑关联crm_customer.id）',
  `user_id` bigint NOT NULL COMMENT '被共享人ID（逻辑关联sys_user.id）',
  `auth_type` tinyint DEFAULT 1 COMMENT '权限类型（1只读 2读写）',
  `create_by` varchar(64) DEFAULT '' COMMENT '发起人（主销售username）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '共享时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cust_user` (`customer_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户团队协同共享表';

-- 11. 商机表
CREATE TABLE `crm_business` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商机ID',
  `customer_id` bigint NOT NULL COMMENT '关联客户ID',
  `business_name` varchar(100) NOT NULL COMMENT '商机名称/项目名',
  `expected_amount` decimal(12,2) DEFAULT '0.00' COMMENT '预计金额',
  `expected_deal_date` date DEFAULT NULL COMMENT '预计结单日期',
  `stage` varchar(20) DEFAULT '需求分析' COMMENT '商机阶段(需求分析/方案报价/商务谈判/赢单/输单)',
  `owner_user_id` bigint DEFAULT NULL COMMENT '商机负责人ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机/销售机会表';

-- 12. 跟进记录表
CREATE TABLE `crm_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '跟进记录ID',
  `related_type` varchar(20) NOT NULL COMMENT '关联类型(lead-线索 customer-客户 business-商机 contract-合同,阶段五扩展)',
  `related_id` bigint NOT NULL COMMENT '对应的关联主体ID',
  `content` text NOT NULL COMMENT '跟进内容/沟通纪要',
  `follow_type` varchar(20) DEFAULT '电话' COMMENT '跟进方式(电话/微信/上门拜访/邮件/系统)',
  `next_follow_time` datetime DEFAULT NULL COMMENT '下次跟进时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '跟进人（销售昵称/账号）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '跟进时间',
  PRIMARY KEY (`id`),
  KEY `idx_related` (`related_type`,`related_id`),
  KEY `idx_next_follow` (`next_follow_time`),
  KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跟进记录表';

-- 12.1 跟进记录迁移日志表（阶段五新增）
CREATE TABLE `crm_record_migration_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT                COMMENT '主键',
  `record_id`    BIGINT       NOT NULL                               COMMENT '关联跟进记录ID(crm_record.id)',
  `from_type`    VARCHAR(20)  NOT NULL                               COMMENT '原主体类型(lead)',
  `from_id`      BIGINT       NOT NULL                               COMMENT '原主体ID',
  `to_type`      VARCHAR(20)  NOT NULL                               COMMENT '新主体类型(customer)',
  `to_id`        BIGINT       NOT NULL                               COMMENT '新主体ID',
  `operator`     VARCHAR(64)  DEFAULT ''                             COMMENT '操作人(username,不用 user_id 便于用户删除后追溯)',
  `migrate_time` DATETIME     DEFAULT CURRENT_TIMESTAMP               COMMENT '迁移时间',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_from` (`from_type`, `from_id`),
  KEY `idx_to` (`to_type`, `to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跟进记录主体迁移日志(阶段五新增)';

-- 13. 产品分类表
CREATE TABLE `crm_product_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id` bigint DEFAULT 0 COMMENT '父分类ID',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品分类表';

-- 14. 产品表
CREATE TABLE `crm_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '产品ID',
  `category_id` bigint DEFAULT NULL COMMENT '产品分类ID',
  `product_code` varchar(50) NOT NULL COMMENT '产品编码/SKU',
  `product_name` varchar(100) NOT NULL COMMENT '产品名称',
  `spec` varchar(100) DEFAULT NULL COMMENT '规格型号',
  `price` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '标准售价',
  `unit` varchar(10) DEFAULT '个' COMMENT '单位',
  `status` tinyint DEFAULT 1 COMMENT '状态（0下架 1上架）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- 15. 合同表
CREATE TABLE `crm_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `contract_num` varchar(50) NOT NULL COMMENT '合同编号',
  `contract_name` varchar(100) NOT NULL COMMENT '合同名称',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `business_id` bigint DEFAULT NULL COMMENT '商机ID(可为空)',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '合同总金额',
  `start_date` date DEFAULT NULL COMMENT '合同开始日期',
  `end_date` date DEFAULT NULL COMMENT '合同结束日期',
  `status` tinyint DEFAULT 0 COMMENT '状态（0审批中 1执行中 2已结束 3已作废）',
  `owner_user_id` bigint NOT NULL COMMENT '签约人/负责人ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_num` (`contract_num`),
  KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同表';

-- 16. 合同明细表
CREATE TABLE `crm_contract_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `count` int NOT NULL DEFAULT '1' COMMENT '数量',
  `standard_price` decimal(12,2) NOT NULL COMMENT '标准售价',
  `sales_price` decimal(12,2) NOT NULL COMMENT '实际销售成交价',
  `discount` decimal(4,2) DEFAULT '10.00' COMMENT '折扣(如9.5折记为9.50)',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同产品明细表';

-- 17. 回款计划表
CREATE TABLE `crm_receivable_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回款计划ID',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `period` int NOT NULL COMMENT '期数(第几期付款，如: 1, 2, 3)',
  `expected_amount` decimal(12,2) NOT NULL COMMENT '预计回款金额',
  `expected_date` date NOT NULL COMMENT '预计回款日期',
  `status` tinyint DEFAULT 0 COMMENT '状态（0未到期 1催款中 2已回款）',
  `remark` varchar(255) DEFAULT NULL COMMENT '催款说明/备注',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除(阶段三补齐,与 CLAUDE.md 对齐)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回款计划表';

-- 18. 回款记录表
CREATE TABLE `crm_receivable` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回款记录ID',
  `receivable_num` varchar(50) NOT NULL COMMENT '回款编号/流水号',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `plan_id` bigint DEFAULT NULL COMMENT '对应回款计划ID(可为空，允许计划外回款)',
  `actual_amount` decimal(12,2) NOT NULL COMMENT '实际回款金额',
  `return_date` date NOT NULL COMMENT '实际回款日期',
  `payment_method` varchar(30) DEFAULT '银行转账' COMMENT '支付方式(银行转账/微信/支付宝/现金)',
  `create_by` varchar(64) DEFAULT '' COMMENT '财务录入人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receivable_num` (`receivable_num`),
  KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回款记录表';

-- 19. 合同审批表 (阶段三新增)
CREATE TABLE `crm_approval` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批ID',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `applicant_id` bigint NOT NULL COMMENT '申请人(销售)ID',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人(总监)ID',
  `status` tinyint DEFAULT 0 COMMENT '状态(0待审 1通过 2驳回 3撤回)',
  `trigger_reason` varchar(255) DEFAULT NULL COMMENT '触发原因(如:折扣8.4折,低于8.5折审批线)',
  `comment` varchar(500) DEFAULT NULL COMMENT '审批意见/驳回原因',
  `finish_time` datetime DEFAULT NULL COMMENT '审批完成时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同审批表';

-- ====================== 种子数据 ======================

-- 部门（4 个，含层级 ancestors 字段，data_scope=4 的查询会用到）
INSERT INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`) VALUES
  (1, 0, '0',     '总公司',     1),
  (2, 1, '0,1',   '华东销售部', 2),
  (3, 1, '0,1',   '华南销售部', 3),
  (4, 1, '0,1',   '财务部',     4);

-- 角色（5 个，data_scope 字段决定数据权限）
-- 1=全部 2=自定义 3=本部门 4=本部门及以下 5=仅本人
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `data_scope`) VALUES
  (1, '系统管理员', 'admin',          1),
  (2, '销售总监',   'sales_director', 4),
  (3, '销售主管',   'sales_lead',     3),
  (4, '普通销售',   'sales',          5),
  (5, '财务人员',   'finance',        1);

-- 菜单（最小权限集，仅阶段一鉴权用；菜单 UI 待阶段二补齐）
-- menu_type='F' 表示按钮级权限；status=1 + perms 非空 才会被 SysMenuMapper 查出来
-- 阶段二对齐：crm:opportunity:list → crm:business:list（与实际表 crm_business 一致），
--             新增 business:edit / contact:list / contact:edit / record:list / record:add
-- 阶段三新增：crm:contract:approve / crm:receivable:edit / crm:receivable_plan:edit /
--             crm:product:list / crm:product:edit
-- 阶段四新增：crm:customer:share          客户共享(主销售发起/撤销)
--             crm:customer:public_pool    公海池(认领 + 手动回收;回收接口额外校验角色)
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`) VALUES
  (1,  '客户列表',     0, 1,  '', NULL, 'F', 'crm:customer:list',      1),
  (2,  '客户编辑',     0, 2,  '', NULL, 'F', 'crm:customer:edit',      1),
  (3,  '线索列表',     0, 3,  '', NULL, 'F', 'crm:lead:list',          1),
  (4,  '线索编辑',     0, 4,  '', NULL, 'F', 'crm:lead:edit',          1),
  (5,  '商机列表',     0, 5,  '', NULL, 'F', 'crm:business:list',      1),
  (6,  '商机编辑',     0, 6,  '', NULL, 'F', 'crm:business:edit',      1),
  (7,  '联系人列表',   0, 7,  '', NULL, 'F', 'crm:contact:list',       1),
  (8,  '联系人编辑',   0, 8,  '', NULL, 'F', 'crm:contact:edit',       1),
  (9,  '跟进记录列表', 0, 9,  '', NULL, 'F', 'crm:record:list',        1),
  (10, '新增跟进',     0, 10, '', NULL, 'F', 'crm:record:add',         1),
  (11, '合同列表',     0, 11, '', NULL, 'F', 'crm:contract:list',      1),
  (12, '合同编辑',     0, 12, '', NULL, 'F', 'crm:contract:edit',      1),
  (13, '回款列表',     0, 13, '', NULL, 'F', 'crm:receivable:list',    1),
  (14, '合同审批',     0, 14, '', NULL, 'F', 'crm:contract:approve',   1),
  (15, '回款编辑',     0, 15, '', NULL, 'F', 'crm:receivable:edit',    1),
  (16, '回款计划编辑', 0, 16, '', NULL, 'F', 'crm:receivable_plan:edit', 1),
  (17, '产品列表',     0, 17, '', NULL, 'F', 'crm:product:list',       1),
  (18, '产品编辑',     0, 18, '', NULL, 'F', 'crm:product:edit',       1),
  (19, '客户共享',     0, 19, '', NULL, 'F', 'crm:customer:share',     1),
  (20, '公海池',       0, 20, '', NULL, 'F', 'crm:customer:public_pool', 1);

-- 用户（6 个，密码统一 123456，BCrypt hash 由 backend-tools/crm-tools 生成）
-- 当前 hash 是 123456 的一次有效编码；如需更换密码：mvn exec:java -Dexec.args="新密码"
INSERT INTO `sys_user` (`id`, `dept_id`, `username`, `nickname`, `password`, `email`, `phone`, `status`) VALUES
  (1, 1, 'admin',       '超级管理员', '$2a$10$ZtTWH2VV9JXLAv/lCDnMLO5Jqxe5ZN6JLibkW3cAuzU0w7gdYM0nC', 'admin@zencrm.local',       '13800000001', 1),
  (2, 1, 'director',    '张总监',     '$2a$10$ZtTWH2VV9JXLAv/lCDnMLO5Jqxe5ZN6JLibkW3cAuzU0w7gdYM0nC', 'director@zencrm.local',    '13800000002', 1),
  (3, 2, 'lead_wang',   '王主管',     '$2a$10$ZtTWH2VV9JXLAv/lCDnMLO5Jqxe5ZN6JLibkW3cAuzU0w7gdYM0nC', 'lead_wang@zencrm.local',   '13800000003', 1),
  (4, 2, 'sales_li',    '李销售',     '$2a$10$ZtTWH2VV9JXLAv/lCDnMLO5Jqxe5ZN6JLibkW3cAuzU0w7gdYM0nC', 'sales_li@zencrm.local',    '13800000004', 1),
  (5, 3, 'sales_chen',  '陈销售',     '$2a$10$ZtTWH2VV9JXLAv/lCDnMLO5Jqxe5ZN6JLibkW3cAuzU0w7gdYM0nC', 'sales_chen@zencrm.local',  '13800000005', 1),
  (6, 4, 'finance',     '赵财务',     '$2a$10$ZtTWH2VV9JXLAv/lCDnMLO5Jqxe5ZN6JLibkW3cAuzU0w7gdYM0nC', 'finance@zencrm.local',     '13800000006', 1);

-- 用户-角色绑定
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
  (1, 1),  -- admin        -> 系统管理员
  (2, 2),  -- director     -> 销售总监
  (3, 3),  -- lead_wang    -> 销售主管
  (4, 4),  -- sales_li     -> 普通销售
  (5, 4),  -- sales_chen   -> 普通销售
  (6, 5);  -- finance      -> 财务人员

-- 角色-权限绑定（阶段三：含合同审批/回款编辑/回款计划编辑/产品权限）
-- admin (1)        : 全部（含合同审批/回款编辑/回款计划编辑/产品全）
-- sales_director(2): 业务+合同+审批+计划编辑+产品全,无回款编辑
-- sales_lead (3)   : 业务+合同(仅列表)+回款(仅列表)+计划编辑+产品(仅列表)
-- sales       (4)  : 业务+合同(含编辑)+计划编辑+产品全,无回款
-- finance     (5)  : 合同(仅列表)+回款(含编辑)+产品(仅列表)
-- 阶段四新增：crm:customer:share / crm:customer:public_pool
--  - admin / sales_director / sales_lead / sales 全部给共享(19) + 公海池(20)
--    (公海池的「手动回收」接口在 Service 层额外校验 admin/director 角色)
--  - finance 不给(财务不参与客户/公海)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  -- admin (全开 1-23)
  (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18), (1, 19), (1, 20), (1, 21), (1, 22), (1, 23),
  -- sales_director (业务 + 合同 + 审批 + 计划 + 产品 + 共享 + 公海 + 跟进中心 + 标死线索 + 报表中心)
  (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9), (2, 10), (2, 11), (2, 12), (2, 14), (2, 16), (2, 17), (2, 18), (2, 19), (2, 20), (2, 21), (2, 22), (2, 23),
  -- sales_lead (业务 + 合同list + 回款list + 计划edit + 产品list + 共享 + 公海 + 跟进中心 + 标死线索;无报表中心也可以,V1 简化为都给)
  (3, 1), (3, 3), (3, 5), (3, 7), (3, 9), (3, 11), (3, 13), (3, 16), (3, 17), (3, 19), (3, 20), (3, 21), (3, 22), (3, 23),
  -- sales (业务 + 合同全 + 计划edit + 产品全 + 共享 + 公海 + 跟进中心 + 标死线索 + 报表中心)
  (4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (4, 9), (4, 10), (4, 11), (4, 12), (4, 16), (4, 17), (4, 18), (4, 19), (4, 20), (4, 21), (4, 22), (4, 23),
  -- finance (合同list + 回款全 + 产品list + 报表中心;无业务/无共享/无公海/无跟进)
  (5, 11), (5, 13), (5, 15), (5, 17), (5, 23);

-- =====================================================================
--  阶段五(commit 1) · 跟进中心 + commit 2 · 报表中心
--  Schema 增量(全新安装路径同步在此):
--    A. 8 个聚合二级索引(优化报表 SQL):
--         crm_contract   idx_start_date
--         crm_receivable idx_return_date
--         crm_business   idx_stage / idx_expected_deal
--         crm_record     idx_create_time / idx_related
--         crm_customer   idx_industry / idx_last_follow
--    B. 3 个新菜单(id 显式 21/22/23,避免与 phase5-record/phase5-report
--       迁移脚本的 AUTO_INCREMENT 冲突 — 老环境用 24/25/26 起)
--    C. sys_role_menu 5 角色已在上方绑定(本块仅注释说明)
-- =====================================================================

-- 8 个聚合二级索引(直接 CREATE INDEX,新装路径无需存储过程)
-- 注:idx_related(crm_record) 已在建表时定义,此处不重复
CREATE INDEX idx_start_date   ON crm_contract   (start_date);
CREATE INDEX idx_return_date  ON crm_receivable (return_date);
CREATE INDEX idx_stage        ON crm_business   (stage);
CREATE INDEX idx_expected_deal ON crm_business  (expected_deal_date);
CREATE INDEX idx_create_time  ON crm_record     (create_time);
CREATE INDEX idx_industry     ON crm_customer   (industry);
CREATE INDEX idx_last_follow  ON crm_customer   (last_follow_time, is_deleted);

-- 3 个新菜单(阶段五 commit 1 + commit 2)
-- 21: 跟进中心(侧边栏菜单,菜单类型 C)
-- 22: 标为死线索(按钮级权限,菜单类型 F,无 path/component,不显示)
-- 23: 报表中心(侧边栏菜单,菜单类型 C)
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `status`) VALUES
  (21, '跟进中心',   0, 25, 'record/center', 'record/center', 'C', 'crm:record:center', 1),
  (22, '标为死线索', 0, 26, '',              NULL,            'F', 'crm:lead:markDead', 1),
  (23, '报表中心',   0, 30, 'report',        'report/index',  'C', 'crm:report:view',   1);

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
--  验证查询（执行完上面后，可以跑下面这些 SELECT 自检）：
--  SELECT id, username, dept_id, LEFT(password, 7) AS hash_prefix FROM sys_user;
--  SELECT * FROM sys_user_role;
--  SELECT r.role_key, COUNT(rm.menu_id) AS perm_count
--    FROM sys_role r LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
--   GROUP BY r.id, r.role_key;
-- =============================================================
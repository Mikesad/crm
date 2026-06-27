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
                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除',
                            PRIMARY KEY (`id`),
                            KEY `idx_owner_user` (`owner_user_id`) -- 方便查询某销售名下的线索
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
                                KEY `idx_owner_user` (`owner_user_id`),   -- 数据权限过滤核心索引
                                KEY `idx_customer_name` (`customer_name`) -- 方便按客户名搜索
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
                               KEY `idx_customer_id` (`customer_id`) -- 查看客户详情时，关联出所有的联系人
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人表';

-- 10. 团队协作/客户共享表
CREATE TABLE `crm_customer_share` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                      `customer_id` bigint NOT NULL COMMENT '客户ID（逻辑关联crm_customer.id）',
                                      `user_id` bigint NOT NULL COMMENT '被共享人ID（逻辑关联sys_user.id）',
                                      `auth_type` tinyint DEFAULT 1 COMMENT '权限类型（1只读 2读写）',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '共享时间',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_cust_user` (`customer_id`,`user_id`) -- 确保联合唯一，防止重复共享
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
  `related_type` varchar(20) NOT NULL COMMENT '关联类型(lead-线索 customer-客户 business-商机)',
  `related_id` bigint NOT NULL COMMENT '对应的关联主体ID',
  `content` text NOT NULL COMMENT '跟进内容/沟通纪要',
  `follow_type` varchar(20) DEFAULT '电话' COMMENT '跟进方式(电话/微信/上门拜访/邮件)',
  `next_follow_time` datetime DEFAULT NULL COMMENT '下次跟进时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '跟进人（销售昵称/账号）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '跟进时间',
  PRIMARY KEY (`id`),
  KEY `idx_related` (`related_type`,`related_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跟进记录表';

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
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
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

-- 16. 合同明细/产品关联表
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
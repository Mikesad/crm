import request from '@/utils/request'

/**
 * 报表中心接口（阶段五 commit 2）
 * 路径前缀：/crm/report
 *
 * 13 个接口,4 个 Tab 各有 1 个"主接口"返回该 Tab 全部数据,
 * 加上单独 widget 端点(趋势/榜/分布)用于局部刷新。
 *
 * 权限：所有 13 个接口共用 crm:report:view
 * 缓存：5 分钟内存缓存(key = reportKey + ":" + range + ":d<deptId>:u<userId>")
 */

// ================== Tab ① 销售漏斗 + 业绩 ==================

/**
 * Tab ① 主接口:6 KPI + 5 阶段漏斗 + 6 月趋势 + 部门业绩 + 客户来源 + 销售榜
 * @param {{range?: string, startDate?: string, endDate?: string, deptId?: number, userId?: number, topN?: number}} params
 */
export const getFunnel = (params) => request.get('/crm/report/funnel', { params })

/** 趋势(单独刷新用),默认 range=year */
export const getFunnelTrend = (params) => request.get('/crm/report/funnel/trend', { params })

/** 销售个人榜(单独刷新用) */
export const getFunnelPerformer = (params) => request.get('/crm/report/funnel/performer', { params })

// ================== Tab ② 客户分布 ==================

/**
 * Tab ② 主接口:4 KPI + 主分布(按 dim) + 活跃/沉睡/公海 + 等级 + 行业 + 趋势
 * @param {{range?: string, deptId?: number, userId?: number, dim?: string}} params
 */
export const getCustomer = (params) => request.get('/crm/report/customer', { params })

/** 维度分布(单独刷新用),按 dim 切换 */
export const getCustomerDistribution = (params) => request.get('/crm/report/customer/distribution', { params })

// ================== Tab ③ 跟进与转化率 ==================

/**
 * Tab ③ 主接口:4 KPI + 5 阶段转化漏斗 + 跟进方式 + 高频榜 + 团队 vs 全公司 + 6 月趋势
 * @param {{range?: string, deptId?: number, userId?: number, topN?: number}} params
 */
export const getConversion = (params) => request.get('/crm/report/conversion', { params })

/** 阶段转化漏斗(单独刷新用) */
export const getConversionFunnel = (params) => request.get('/crm/report/conversion/funnel', { params })

// ================== Tab ④ 回款 / 财务 ==================

/**
 * Tab ④ 主接口:4 KPI + 3 series 趋势 + 账龄 4 桶 + 回款方式 + 应收 TopN
 * @param {{range?: string, deptId?: number, userId?: number, topN?: number}} params
 */
export const getFinance = (params) => request.get('/crm/report/finance', { params })

/** 账龄分布(单独刷新用) */
export const getFinanceAging = () => request.get('/crm/report/finance/aging')

/** 应收 TopN(单独刷新用) */
export const getFinancePerformer = (params) => request.get('/crm/report/finance/performer', { params })

// ================== 通用:筛选下拉 ==================

/** 部门下拉(返回静态 mock,V2 接 sys_dept) */
export const getFilterDepts = () => request.get('/crm/report/filter/depts')

/**
 * 人员下拉(按 deptId 过滤,V1 返回空)
 * @param {{deptId?: number}} params
 */
export const getFilterUsers = (params) => request.get('/crm/report/filter/users', { params })

// ================== 通用:清缓存 ==================

/** 清空报表缓存(管理员手动刷新) */
export const clearCache = () => request.get('/crm/report/cache/clear')

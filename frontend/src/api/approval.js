import request from '@/utils/request'

/**
 * 合同审批接口（阶段三）
 *
 * 路径前缀：/crm/approval
 * 权限：crm:contract:approve（仅销售总监 / admin）
 *
 * 完整审批流：ContractService.create() 检测折扣 < 8.5 折 → 自动写 crm_approval → 总监在此 approve/reject → 联动 crm_contract.status。
 */

/** 审批单分页（按 status/contractId/applicantId/approverId 过滤） */
export const pageApproval = (params) => request.get('/crm/approval/page', { params })

/** 审批通过（status 0→1，联动 contract.status=1） */
export const approveApproval = (data) => request.post('/crm/approval/approve', data)

/** 审批驳回（status 0→2，comment 必填，联动 contract.status=3） */
export const rejectApproval = (data) => request.post('/crm/approval/reject', data)

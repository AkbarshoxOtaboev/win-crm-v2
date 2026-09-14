import { apiRequest } from './http'
import type { PageResponse, RestApiResponse } from './types'

export type CommissionType = 'PERCENT' | 'FIXED'
export type SalaryEntryType = 'BONUS' | 'DEDUCTION' | 'ADVANCE' | 'COMMISSION' | 'COMMISSION_REVERSAL'

export interface SalaryConfig {
  id: number
  userId: number
  userFullName?: string
  baseSalary: number
  commissionType: CommissionType
  commissionValue: number
  effectiveFrom: string
  effectiveTo?: string | null
  status?: string
  createdAt?: string
  updatedAt?: string
  createdUsername?: string
}

export interface SalaryConfigPayload {
  userId: number
  baseSalary: number
  commissionType: CommissionType
  commissionValue: number
  effectiveFrom: string
}

export interface SalaryTransaction {
  id: number
  userId: number
  saleOrderId?: number
  entryType: SalaryEntryType
  amount: number
  commissionTypeSnapshot?: CommissionType
  rateSnapshot?: number
  baseAmountSnapshot?: number
  earnedAt?: string
  periodYear?: number
  periodMonth?: number
  comment?: string
  status?: string
  createdAt?: string
  createdUsername?: string
}

export interface SalaryAdjustmentPayload {
  userId: number
  entryType: 'BONUS' | 'DEDUCTION' | 'ADVANCE'
  amount: number
  periodYear?: number
  periodMonth?: number
  comment?: string
}

export interface SalarySlip {
  userId: number
  userFullName?: string
  periodYear: number
  periodMonth: number
  baseSalary?: number
  totalCommission?: number
  totalCommissionReversal?: number
  totalBonus?: number
  totalDeduction?: number
  totalAdvance?: number
  netSalary?: number
}

export function fetchSalaryConfigs(page = 0, size = 50) {
  return apiRequest<RestApiResponse<PageResponse<SalaryConfig>>>(
    `/api/salary/configs?page=${page}&size=${size}`,
  )
}

export function createSalaryConfig(payload: SalaryConfigPayload) {
  return apiRequest<RestApiResponse<SalaryConfig>>('/api/salary/configs', {
    method: 'POST',
    body: payload,
  })
}

export function deleteSalaryConfig(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/salary/configs/${id}`, {
    method: 'DELETE',
  })
}

export function addSalaryAdjustment(payload: SalaryAdjustmentPayload) {
  return apiRequest<RestApiResponse<SalaryTransaction>>('/api/salary/transactions/adjustment', {
    method: 'POST',
    body: payload,
  })
}

export function fetchSalaryTransactions(userId: number, page = 0, size = 50) {
  return apiRequest<RestApiResponse<PageResponse<SalaryTransaction>>>(
    `/api/salary/transactions/user/${userId}?page=${page}&size=${size}`,
  )
}

export function deleteSalaryTransaction(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/salary/transactions/${id}`, {
    method: 'DELETE',
  })
}

export function fetchSalarySlip(userId: number, year: number, month: number) {
  return apiRequest<RestApiResponse<SalarySlip>>(
    `/api/salary/slip/user/${userId}?year=${year}&month=${month}`,
  )
}

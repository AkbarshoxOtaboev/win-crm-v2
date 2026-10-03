import { apiRequest } from './http'
import type { RestApiResponse } from './types'
import type { CurrencyCode } from '@/utils/currency'

export interface KpiRate {
  userId: number
  fullName?: string
  username?: string
  roles?: string[]
  filialName?: string
  percent: number | null
}

export interface KpiSummary {
  id: number
  name?: string
  roles?: string[]
  managerName?: string | null
  percent?: number | null
  periodCount: number
  periodAmount: number
  totalAmount: number
}

export interface KpiEntry {
  id: number
  earnedAt?: string
  saleOrderId: number
  clientFullName?: string | null
  /** So'mda. */
  baseAmount?: number
  /** Xorijiy valyutadagi buyurtma: baseAmount = sourceAmount × exchangeRate. */
  sourceCurrency?: CurrencyCode
  sourceAmount?: number | null
  exchangeRate?: number
  percent?: number
  amount: number
}

export function fetchKpiRates() {
  return apiRequest<RestApiResponse<KpiRate[]>>('/api/kpi/rates')
}

export function saveKpiRate(userId: number, percent: number | null) {
  return apiRequest<RestApiResponse<KpiRate>>(`/api/kpi/rates/${userId}`, {
    method: 'PUT',
    body: { percent },
  })
}

function periodQuery(year: number, month: number | null) {
  return month == null ? `year=${year}` : `year=${year}&month=${month}`
}

export function fetchKpiSummary(year: number, month: number) {
  return apiRequest<RestApiResponse<KpiSummary[]>>(`/api/kpi/summary?${periodQuery(year, month)}`)
}

export function fetchKpiUserEntries(userId: number, year: number, month: number | null) {
  return apiRequest<RestApiResponse<KpiEntry[]>>(`/api/kpi/users/${userId}/entries?${periodQuery(year, month)}`)
}

export function fetchWorkshopKpiSummary(year: number, month: number) {
  return apiRequest<RestApiResponse<KpiSummary[]>>(`/api/kpi/workshops/summary?${periodQuery(year, month)}`)
}

export function fetchWorkshopKpiEntries(workshopId: number, year: number, month: number | null) {
  return apiRequest<RestApiResponse<KpiEntry[]>>(
    `/api/kpi/workshops/${workshopId}/entries?${periodQuery(year, month)}`,
  )
}

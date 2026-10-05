import { apiRequest } from './http'
import type { RestApiResponse } from './types'
import type { CurrencyCode } from '@/utils/currency'

export type CashHandoverStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export interface CashHandoverSummary {
  paymentTypeId: number
  paymentTypeName: string
  currency: CurrencyCode
  incoming: number
  outgoing: number
  pending: number
  accepted: number
  /** incoming - outgoing - pending - accepted */
  remaining: number
}

export interface CashHandover {
  id: number
  handoverDate: string
  cashierId: number
  cashierName?: string
  paymentTypeId: number
  paymentTypeName?: string
  currency: CurrencyCode
  expectedAmount: number
  amount: number
  /** amount - expectedAmount: manfiy - kamomad. */
  difference: number
  handoverStatus: CashHandoverStatus
  comment?: string | null
  reviewerId?: number | null
  reviewerName?: string | null
  reviewedAt?: string | null
  reviewComment?: string | null
  createdAt?: string | null
  mine: boolean
}

export interface CashHandoverPayload {
  handoverDate?: string
  comment?: string
  items: { paymentTypeId: number; amount: number }[]
}

function query(params: Record<string, string | number | undefined>) {
  const qs = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== '')
    .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
    .join('&')
  return qs ? `?${qs}` : ''
}

export function fetchCashHandoverSummary(params: { date?: string; userId?: number } = {}) {
  return apiRequest<RestApiResponse<CashHandoverSummary[]>>(`/api/cash-handovers/summary${query(params)}`)
}

export function fetchCashHandovers(
  params: { fromDate?: string; toDate?: string; status?: CashHandoverStatus; userId?: number } = {},
) {
  return apiRequest<RestApiResponse<CashHandover[]>>(`/api/cash-handovers${query(params)}`)
}

export function createCashHandover(body: CashHandoverPayload) {
  return apiRequest<RestApiResponse<CashHandover[]>>('/api/cash-handovers', { method: 'POST', body })
}

export function acceptCashHandover(id: number, comment?: string) {
  return apiRequest<RestApiResponse<CashHandover>>(`/api/cash-handovers/${id}/accept`, {
    method: 'POST',
    body: { comment },
  })
}

export function rejectCashHandover(id: number, comment?: string) {
  return apiRequest<RestApiResponse<CashHandover>>(`/api/cash-handovers/${id}/reject`, {
    method: 'POST',
    body: { comment },
  })
}

export function cancelCashHandover(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/cash-handovers/${id}`, { method: 'DELETE' })
}

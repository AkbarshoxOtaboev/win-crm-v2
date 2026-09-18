import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface ClientBalance {
  id?: number
  clientId?: number
  clientFullName?: string
  totalDebt?: number
  totalPaid?: number
  totalPurchase?: number
  lastUpdated?: string
}

export function fetchClientBalances() {
  return apiRequest<RestApiResponse<ClientBalance[]>>('/api/client-balances')
}

export function fetchClientBalance(
  clientId: number,
  params?: { fromDate?: string; toDate?: string },
) {
  const q = new URLSearchParams()
  if (params?.fromDate) q.set('fromDate', params.fromDate)
  if (params?.toDate) q.set('toDate', params.toDate)
  const qs = q.toString()
  return apiRequest<RestApiResponse<ClientBalance>>(
    `/api/client-balances/${clientId}${qs ? `?${qs}` : ''}`,
  )
}

export function recalculateClientBalance(clientId: number) {
  return apiRequest<RestApiResponse<ClientBalance>>(`/api/client-balances/recalculate/${clientId}`, {
    method: 'PUT',
  })
}

export function adjustClientBalance(
  clientId: number,
  payload: { totalPurchase: number; totalPaid: number },
) {
  return apiRequest<RestApiResponse<ClientBalance>>(`/api/client-balances/adjust/${clientId}`, {
    method: 'PUT',
    body: payload,
  })
}

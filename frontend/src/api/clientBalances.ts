import { apiRequest } from './http'
import type { RestApiResponse } from './types'
import type { CurrencyCode } from '@/utils/currency'

export interface ClientBalance {
  id?: number
  clientId?: number
  clientFullName?: string
  /** Balans valyutasi: har bir valyuta alohida qator, summalar qo'shilmaydi. */
  currency?: CurrencyCode
  totalDebt?: number
  totalPaid?: number
  totalPurchase?: number
  lastUpdated?: string
}

export function fetchClientBalances() {
  return apiRequest<RestApiResponse<ClientBalance[]>>('/api/client-balances')
}

/** Mijozning har bir valyutadagi balansi (so'm qatori doim bor). */
export function fetchClientBalance(
  clientId: number,
  params?: { fromDate?: string; toDate?: string },
) {
  const q = new URLSearchParams()
  if (params?.fromDate) q.set('fromDate', params.fromDate)
  if (params?.toDate) q.set('toDate', params.toDate)
  const qs = q.toString()
  return apiRequest<RestApiResponse<ClientBalance[]>>(
    `/api/client-balances/${clientId}${qs ? `?${qs}` : ''}`,
  )
}

export function recalculateClientBalance(clientId: number) {
  return apiRequest<RestApiResponse<ClientBalance[]>>(`/api/client-balances/recalculate/${clientId}`, {
    method: 'PUT',
  })
}

export function adjustClientBalance(
  clientId: number,
  payload: { totalPurchase: number; totalPaid: number; currency?: CurrencyCode },
) {
  return apiRequest<RestApiResponse<ClientBalance>>(`/api/client-balances/adjust/${clientId}`, {
    method: 'PUT',
    body: payload,
  })
}

/** Berilgan valyutadagi balans qatori (bo'lmasa undefined). */
export function balanceIn(list: ClientBalance[] | null | undefined, currency: CurrencyCode) {
  return (list || []).find((b) => (b.currency || 'UZS') === currency)
}

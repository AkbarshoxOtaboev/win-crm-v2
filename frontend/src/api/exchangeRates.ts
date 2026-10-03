import { apiRequest } from './http'
import type { RestApiResponse } from './types'
import type { CurrencyCode } from '@/utils/currency'

export type ExchangeRateSource = 'MANUAL' | 'CBU'

export interface ExchangeRate {
  id: number
  currency: CurrencyCode
  rateDate: string
  rate: number
  source: ExchangeRateSource
  createdUsername?: string | null
  updatedAt?: string | null
}

export interface CbuRate {
  currency: CurrencyCode
  rateDate: string
  rate: number
  diff?: number | null
}

export interface ExchangeRatePayload {
  currency: CurrencyCode
  rateDate: string
  rate: number
  source?: ExchangeRateSource
}

export function fetchExchangeRates(currency: CurrencyCode = 'USD', from?: string, to?: string) {
  const params = new URLSearchParams({ currency })
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  return apiRequest<RestApiResponse<ExchangeRate[]>>(`/api/exchange-rates?${params}`)
}

export function fetchCurrentRate(currency: CurrencyCode = 'USD') {
  return apiRequest<RestApiResponse<ExchangeRate | null>>(`/api/exchange-rates/current?currency=${currency}`)
}

export function fetchCbuRate(currency: CurrencyCode = 'USD', date?: string) {
  const params = new URLSearchParams({ currency })
  if (date) params.set('date', date)
  return apiRequest<RestApiResponse<CbuRate>>(`/api/exchange-rates/cbu?${params}`)
}

export function saveExchangeRate(payload: ExchangeRatePayload) {
  return apiRequest<RestApiResponse<ExchangeRate>>('/api/exchange-rates', {
    method: 'PUT',
    body: payload,
  })
}

export function deleteExchangeRate(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/exchange-rates/${id}`, {
    method: 'DELETE',
  })
}

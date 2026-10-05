import { apiRequest } from './http'
import type { RestApiResponse } from './types'
import type { CurrencyCode } from '@/utils/currency'

export type ExchangeRateSource = 'MANUAL' | 'CBU'

/** Markaziy bank kursi: 1 birlik valyuta = rate so'm. */
export interface ExchangeRate {
  id: number
  currency: CurrencyCode
  rateDate: string
  rate: number
  /** Oldingi kursga nisbatan o'zgarish (so'm). */
  change?: number | null
  source: ExchangeRateSource
  createdUsername?: string | null
  updatedAt?: string | null
}

/** Eng yangisi birinchi. */
export function fetchExchangeRates(currency: CurrencyCode = 'USD', from?: string, to?: string) {
  const params = new URLSearchParams({ currency })
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  return apiRequest<RestApiResponse<ExchangeRate[]>>(`/api/exchange-rates?${params}`)
}

/** Berilgan kunda (bo'sh - bugun) amal qiladigan kurs: shu sanali hujjatlar aynan shu kursni oladi. */
export function fetchCurrentRate(currency: CurrencyCode = 'USD', date?: string) {
  const params = new URLSearchParams({ currency })
  if (date) params.set('date', date)
  return apiRequest<RestApiResponse<ExchangeRate | null>>(`/api/exchange-rates/current?${params}`)
}

/** Kompaniya o'zi belgilaydigan olish/sotish kursi; belgilanmagan bo'lsa buyRate/sellRate null. */
export interface CompanyFxRate {
  currency: CurrencyCode
  buyRate: number | null
  sellRate: number | null
  updatedUsername?: string | null
  updatedAt?: string | null
  cbuRate?: number | null
  cbuChange?: number | null
  cbuRateDate?: string | null
}

export function fetchCompanyRates() {
  return apiRequest<RestApiResponse<CompanyFxRate[]>>('/api/exchange-rates/company')
}

export function saveCompanyRate(currency: CurrencyCode, buyRate: number, sellRate: number) {
  return apiRequest<RestApiResponse<CompanyFxRate>>(`/api/exchange-rates/company/${currency}`, {
    method: 'PUT',
    body: { buyRate, sellRate },
  })
}

/** Markaziy bankdan oxirgi kursni hozir olish (server buni har soatda o'zi ham qiladi). */
export function syncExchangeRate(currency: CurrencyCode = 'USD') {
  return apiRequest<RestApiResponse<ExchangeRate | null>>(`/api/exchange-rates/sync?currency=${currency}`, {
    method: 'POST',
  })
}

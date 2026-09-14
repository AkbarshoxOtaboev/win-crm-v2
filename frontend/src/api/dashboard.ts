import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface TopGoods {
  goodsId?: number
  goodsName?: string
  totalCount?: number
  totalAmount?: number
  quantity?: number
  amount?: number
}

export interface GoodsGroupSummary {
  goodsGroupId?: number
  goodsGroupName?: string
  totalCount?: number
  totalAmount?: number
  quantity?: number
  amount?: number
}

export interface TopSeller {
  userId?: number
  userName?: string
  fullName?: string
  username?: string
  totalAmount?: number
  amount?: number
  orderCount?: number
}

export interface PaymentTypeSummary {
  paymentTypeId?: number
  paymentTypeName?: string
  totalAmount?: number
  amount?: number
  paymentCount?: number
  count?: number
}

export interface DailyPaymentSummary {
  date?: string
  totalAmount?: number
  amount?: number
  count?: number
}

export interface DailyExpenseReport {
  categoryId?: number
  categoryName?: string
  totalAmount?: number
  amount?: number
}

function dateRange(start: string, end: string) {
  return `startDate=${encodeURIComponent(start)}&endDate=${encodeURIComponent(end)}`
}

function dateTimeRange(start: string, end: string) {
  return `fromDate=${encodeURIComponent(start + 'T00:00:00')}&toDate=${encodeURIComponent(end + 'T23:59:59')}`
}

export function fetchTopGoodsByQuantity(start: string, end: string) {
  return apiRequest<RestApiResponse<TopGoods[]>>(
    `/api/dashboard/top-goods/by-quantity?${dateRange(start, end)}`,
  )
}

export function fetchTopGoodsByAmount(start: string, end: string) {
  return apiRequest<RestApiResponse<TopGoods[]>>(
    `/api/dashboard/top-goods/by-amount?${dateRange(start, end)}`,
  )
}

export function fetchGoodsGroupSummary(start: string, end: string) {
  return apiRequest<RestApiResponse<GoodsGroupSummary[]>>(
    `/api/dashboard/goods-group-summary?${dateRange(start, end)}`,
  )
}

export function fetchTopSellers(start: string, end: string) {
  return apiRequest<RestApiResponse<TopSeller[]>>(
    `/api/dashboard/top-sellers?${dateRange(start, end)}`,
  )
}

export function fetchPaymentsByType(start: string, end: string) {
  return apiRequest<RestApiResponse<PaymentTypeSummary[]>>(
    `/api/dashboard/payments/by-type?${dateTimeRange(start, end)}`,
  )
}

export function fetchDailyPayments(start: string, end: string) {
  return apiRequest<RestApiResponse<DailyPaymentSummary[]>>(
    `/api/dashboard/payments/daily?${dateTimeRange(start, end)}`,
  )
}

export function fetchExpenseInfo(from?: string, to?: string) {
  const q =
    from && to
      ? `?fromDate=${encodeURIComponent(from)}&toDate=${encodeURIComponent(to)}`
      : ''
  return apiRequest<RestApiResponse<DailyExpenseReport[]>>(`/api/dashboard/expense/info${q}`)
}

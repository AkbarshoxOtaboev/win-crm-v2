import { apiRequest } from './http'
import type { RestApiResponse } from './types'
import type { CurrencyCode } from '@/utils/currency'

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
  /** totalAmount valyutasi (kassa valyutasi). */
  currency?: CurrencyCode
}

export interface DailyPaymentTypeAmount {
  paymentTypeId?: number
  paymentTypeName?: string
  amount?: number
  currency?: CurrencyCode
}

export interface DailyPaymentSummary {
  date?: string
  totalAmount?: number
  amount?: number
  count?: number
  byType?: DailyPaymentTypeAmount[]
}

export interface DailyExpenseReport {
  categoryId?: number
  categoryName?: string
  totalAmount?: number
  amount?: number
}

export interface CashBalance {
  paymentTypeId: number
  paymentTypeName: string
  currency: CurrencyCode
  opening: number
  incoming: number
  outgoing: number
  handedOver: number
  /** Kassadan qilingan xarajatlar (yetkazib beruvchi to'lovlarisiz). */
  expenses: number
  closing: number
}

export interface FxDifferenceRow {
  paymentId: number
  paymentDate: string
  saleOrderId: number
  orderDate?: string | null
  clientId?: number | null
  clientFullName?: string | null
  debtCurrency: CurrencyCode
  appliedAmount: number
  paidAmount: number
  paidCurrency: CurrencyCode
  orderRate: number
  paymentRate: number
  orderBase: number
  paymentBase: number
  difference: number
}

export interface FxDifference {
  totalDifference: number
  totalGain: number
  totalLoss: number
  unallocatedCount: number
  rows: FxDifferenceRow[]
}

/** `only` - faqat `currency` valyutasidagi hujjatlar (o'girishsiz). */
function currencyQuery(currency?: CurrencyCode, only?: boolean) {
  if (!currency) return ''
  return `&currency=${currency}${only ? '&only=true' : ''}`
}

function dateRange(start: string, end: string, currency?: CurrencyCode, only?: boolean) {
  return `startDate=${encodeURIComponent(start)}&endDate=${encodeURIComponent(end)}${currencyQuery(currency, only)}`
}

function dateTimeRange(start: string, end: string, currency?: CurrencyCode, only?: boolean) {
  const q = `fromDate=${encodeURIComponent(start + 'T00:00:00')}&toDate=${encodeURIComponent(end + 'T23:59:59')}`
  return `${q}${currencyQuery(currency, only)}`
}

export function fetchTopGoodsByQuantity(start: string, end: string, currency?: CurrencyCode, only?: boolean) {
  return apiRequest<RestApiResponse<TopGoods[]>>(
    `/api/dashboard/top-goods/by-quantity?${dateRange(start, end, currency, only)}`,
  )
}

export function fetchTopGoodsByAmount(start: string, end: string, currency?: CurrencyCode, only?: boolean) {
  return apiRequest<RestApiResponse<TopGoods[]>>(
    `/api/dashboard/top-goods/by-amount?${dateRange(start, end, currency, only)}`,
  )
}

export function fetchGoodsGroupSummary(start: string, end: string, currency?: CurrencyCode, only?: boolean) {
  return apiRequest<RestApiResponse<GoodsGroupSummary[]>>(
    `/api/dashboard/goods-group-summary?${dateRange(start, end, currency, only)}`,
  )
}

export function fetchTopSellers(start: string, end: string, currency?: CurrencyCode, only?: boolean) {
  return apiRequest<RestApiResponse<TopSeller[]>>(
    `/api/dashboard/top-sellers?${dateRange(start, end, currency, only)}`,
  )
}

export function fetchPaymentsByType(start: string, end: string) {
  return apiRequest<RestApiResponse<PaymentTypeSummary[]>>(
    `/api/dashboard/payments/by-type?${dateTimeRange(start, end)}`,
  )
}

export function fetchDailyPayments(start: string, end: string, currency?: CurrencyCode, only?: boolean) {
  return apiRequest<RestApiResponse<DailyPaymentSummary[]>>(
    `/api/dashboard/payments/daily?${dateTimeRange(start, end, currency, only)}`,
  )
}

export function fetchExpenseInfo(from?: string, to?: string, currency?: CurrencyCode, only?: boolean) {
  const params = new URLSearchParams()
  if (from && to) {
    params.set('fromDate', from)
    params.set('toDate', to)
  }
  if (currency) params.set('currency', currency)
  if (currency && only) params.set('only', 'true')
  const q = params.toString()
  return apiRequest<RestApiResponse<DailyExpenseReport[]>>(`/api/dashboard/expense/info${q ? `?${q}` : ''}`)
}

export function fetchCashBalances(from: string, to: string) {
  return apiRequest<RestApiResponse<CashBalance[]>>(
    `/api/dashboard/cash-balances?fromDate=${encodeURIComponent(from)}&toDate=${encodeURIComponent(to)}`,
  )
}

export interface ProfitTotals {
  revenue: number
  cost: number
  profit: number
  marginPercent: number
}

export interface ProfitDay {
  date: string
  revenue: number
  cost: number
  expenses: number
  /** Sof foyda: tushum - tannarx - xarajatlar. */
  profit: number
}

export interface ProfitGoodsRow extends ProfitTotals {
  goodsId: number
  goodsName: string
  goodsType?: 'PRODUCT' | 'WINDOW' | 'SERVICE' | null
  unitName?: string | null
  count: number
}

export interface ProfitSellerRow extends ProfitTotals {
  userId: number
  fullName?: string | null
  orderCount: number
}

/** `profit` va `marginPercent` - sof (xarajatlar ayirilgan); `grossProfit` - tushum - tannarx. */
export interface ProfitReport extends ProfitTotals {
  currency: CurrencyCode
  grossProfit: number
  /** Yetkazib beruvchiga to'lovlarsiz xarajatlar. */
  expenses: number
  discount: number
  deliveryFee: number
  orderCount: number
  itemlessOrderCount: number
  days: ProfitDay[]
  goods: ProfitGoodsRow[]
  sellers: ProfitSellerRow[]
}

export function fetchProfitReport(from: string, to: string, currency: CurrencyCode) {
  return apiRequest<RestApiResponse<ProfitReport>>(
    `/api/dashboard/profit?fromDate=${encodeURIComponent(from)}&toDate=${encodeURIComponent(to)}&currency=${currency}`,
  )
}

export function fetchFxDifference(from: string, to: string) {
  return apiRequest<RestApiResponse<FxDifference>>(
    `/api/dashboard/fx-difference?fromDate=${encodeURIComponent(from)}&toDate=${encodeURIComponent(to)}`,
  )
}

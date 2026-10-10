import { apiRequest } from './http'
import type { RestApiResponse, SpringPage } from './types'
import type { CurrencyCode } from '@/utils/currency'

export interface SaleOrder {
  id: number
  clientId?: number
  clientFullName?: string
  warehouseId?: number
  warehouseName?: string
  userId?: number
  userFullName?: string
  comment?: string
  orderDate?: string
  originalTotalSum?: number
  discountType?: string
  discountValue?: number
  discountAmount?: number
  totalSum?: number
  deliveryType?: DeliveryType | null
  deliveryFee?: number | null
  paidSum?: number
  debtSum?: number
  status?: string
  orderStatus?: string
  /** Buyurtma valyutasi: barcha summalar (narx, jami, to'langan, qarz) shu valyutada. */
  currency?: CurrencyCode
  /** 1 birlik valyuta = necha so'm (so'mda 1). */
  exchangeRate?: number
  totalSumBase?: number
  saleType?: SaleType | null
}

export type DeliveryType = 'DELIVERY' | 'PICKUP'
export type SaleType = 'RETAIL' | 'WHOLESALE'

export interface SaleOrderPayload {
  warehouseId: number
  userId: number
  orderDate: string
  totalSum: number
  clientId?: number | null
  comment?: string
  deliveryType?: DeliveryType
  deliveryFee?: number
  currency?: CurrencyCode
  exchangeRate?: number
  saleType?: SaleType
}

export interface SaleOrderInitialItem {
  goodsId: number
  priceCost: number
  priceSelling: number
  count: number
  width?: number
  height?: number
}

export interface SaleOrderCreatePayload extends SaleOrderPayload {
  items?: SaleOrderInitialItem[]
}

export function fetchSaleOrders(page = 0, size = 50) {
  return apiRequest<RestApiResponse<SpringPage<SaleOrder>>>(
    `/api/sale-orders?page=${page}&size=${size}&sort=id,DESC`,
  )
}

export function createSaleOrder(payload: SaleOrderCreatePayload) {
  return apiRequest<RestApiResponse<SaleOrder>>('/api/sale-orders/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateSaleOrder(id: number, payload: SaleOrderPayload) {
  return apiRequest<RestApiResponse<SaleOrder>>(`/api/sale-orders/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteSaleOrder(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/sale-orders/delete/${id}`, {
    method: 'DELETE',
  })
}

export function changeSaleOrderStatus(id: number, salesOrderStatus: string, comment?: string) {
  const params = new URLSearchParams({ salesOrderStatus })
  if (comment) params.set('comment', comment)
  return apiRequest<RestApiResponse<SaleOrder>>(`/api/sale-orders/${id}/status?${params}`, {
    method: 'PATCH',
  })
}

export function fetchSaleOrder(id: number) {
  return apiRequest<RestApiResponse<SaleOrder>>(`/api/sale-orders/${id}`)
}

export function fetchSaleOrdersByDateRange(startDate: string, endDate: string) {
  return apiRequest<RestApiResponse<SaleOrder[]>>(
    `/api/sale-orders/date-range?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`,
  )
}

export function fetchSaleOrdersByClient(clientId: number, page = 0, size = 50) {
  return apiRequest<RestApiResponse<SpringPage<SaleOrder>>>(
    `/api/sale-orders/client/${clientId}?page=${page}&size=${size}&sort=id,asc`,
  )
}

export interface SellerOrderDebt {
  saleOrderId: number
  orderDate?: string
  totalSum?: number
  paidSum?: number
  debtSum?: number
  currency?: CurrencyCode
  status?: string
}

/** Mijoz + valyuta: bitta mijozning so'm va dollar buyurtmalari alohida qatorda. */
export interface SellerClientDebt {
  clientId?: number | null
  clientFullName?: string | null
  phone?: string | null
  currency?: CurrencyCode
  totalSum?: number
  paidSum?: number
  debt?: number
  orders: SellerOrderDebt[]
}

export interface SellerDebt {
  userId: number
  userFullName?: string
  /** So'mdagi qarz. */
  totalDebt?: number
  debts?: { currency: CurrencyCode; amount: number }[]
  clients: SellerClientDebt[]
}

export function fetchSellerDebts(params: { userId?: number; startDate?: string; endDate?: string } = {}) {
  const q = new URLSearchParams()
  if (params.userId) q.set('userId', String(params.userId))
  if (params.startDate) q.set('startDate', params.startDate)
  if (params.endDate) q.set('endDate', params.endDate)
  const qs = q.toString()
  return apiRequest<RestApiResponse<SellerDebt[]>>(`/api/sale-orders/debts/by-seller${qs ? `?${qs}` : ''}`)
}

export function applyDiscount(id: number, discountType: string, discountValue: number) {
  return apiRequest<RestApiResponse<SaleOrder>>(`/api/sale-orders/${id}/discount`, {
    method: 'PATCH',
    body: { discountType, discountValue },
  })
}

export interface SaleOrderHistory {
  id: number
  fromStatus?: string
  toStatus?: string
  changedAt?: string
  changedByUserId?: number | null
  changedByUserFullName?: string | null
  changedByUsername?: string | null
  comment?: string
}

export interface SaleOrderDiscountHistory {
  id: number
  discountType?: string
  discountValue?: number
  discountAmount?: number
  createdAt?: string
  createdUsername?: string
}

export interface SaleOrderImage {
  id: number
  saleOrderId?: number
  fileName?: string
  originalFileName?: string
  downloadUrl?: string
  contentType?: string
  size?: number
  imageType?: string
  createdAt?: string
}

export function fetchSaleOrderHistory(id: number) {
  return apiRequest<RestApiResponse<SaleOrderHistory[]>>(`/api/sale-orders/${id}/history`)
}

export function fetchDiscountHistory(id: number) {
  return apiRequest<RestApiResponse<SaleOrderDiscountHistory[]>>(
    `/api/sale-orders/${id}/discount-history`,
  )
}

export function fetchSaleOrderImages(saleOrderId: number) {
  return apiRequest<RestApiResponse<SaleOrderImage[]>>(`/api/sale-orders/${saleOrderId}/images`)
}

export function uploadSaleOrderImages(saleOrderId: number, files: File[]) {
  const fd = new FormData()
  files.forEach((f) => fd.append('files', f))
  return apiRequest<RestApiResponse<SaleOrderImage[]>>(`/api/sale-orders/${saleOrderId}/images`, {
    method: 'POST',
    body: fd,
  })
}

export function deleteSaleOrderImage(saleOrderId: number, imageId: number) {
  return apiRequest<RestApiResponse<null>>(`/api/sale-orders/${saleOrderId}/images/${imageId}`, {
    method: 'DELETE',
  })
}

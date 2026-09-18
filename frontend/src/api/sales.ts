import { apiRequest } from './http'
import type { RestApiResponse, SpringPage } from './types'

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
  paidSum?: number
  debtSum?: number
  status?: string
  orderStatus?: string
}

export interface SaleOrderPayload {
  warehouseId: number
  userId: number
  orderDate: string
  totalSum: number
  clientId?: number | null
  comment?: string
}

export function fetchSaleOrders(page = 0, size = 50) {
  return apiRequest<RestApiResponse<SpringPage<SaleOrder>>>(
    `/api/sale-orders?page=${page}&size=${size}&sort=id,DESC`,
  )
}

export function createSaleOrder(payload: SaleOrderPayload) {
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

export function changeSaleOrderStatus(id: number, salesOrderStatus: string) {
  return apiRequest<RestApiResponse<SaleOrder>>(
    `/api/sale-orders/${id}/status?salesOrderStatus=${encodeURIComponent(salesOrderStatus)}`,
    { method: 'PATCH' },
  )
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
  createdAt?: string
  createdUsername?: string
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
  fileName?: string
  url?: string
  type?: string
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

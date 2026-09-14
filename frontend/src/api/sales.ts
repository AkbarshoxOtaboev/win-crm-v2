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

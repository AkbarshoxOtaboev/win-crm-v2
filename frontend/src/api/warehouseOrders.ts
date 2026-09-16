import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface WarehouseOrder {
  id: number
  supplierId?: number
  supplierName?: string
  warehouseId?: number
  warehouseName?: string
  comment?: string
  arrivalDate?: string
  totalSum?: number
  serviceFee?: number
  paidSum?: number
  debtSum?: number
  transferred?: boolean
  orderStatus?: string
  status?: string
}

export interface WarehouseOrderPayload {
  supplierId: number
  warehouseId: number
  arrivalDate: string
  comment?: string
  serviceFee?: number
}

export interface WarehouseOrderItem {
  id: number
  warehouseOrderId?: number
  warehouseId?: number
  supplierId?: number
  goodsId?: number
  goodsName?: string
  priceCost?: number
  priceSelling?: number
  weight?: number
  height?: number
  /** WINDOW: kv.m; boshqa: miqdor */
  count?: number
  /** Miqdor (dona) — bazada alohida */
  pieceCount?: number
  arrivalDate?: string
}

export interface WarehouseOrderItemPayload {
  warehouseId: number
  warehouseOrderId: number
  supplierId: number
  goodsId: number
  priceCost: number
  priceSelling: number
  count: number
  arrivalDate: string
  weight?: number
  height?: number
}

export function fetchWarehouseOrders() {
  return apiRequest<RestApiResponse<WarehouseOrder[]>>('/api/warehouse-orders')
}

export function fetchWarehouseOrdersBySupplier(supplierId: number) {
  return apiRequest<RestApiResponse<WarehouseOrder[]>>(
    `/api/warehouse-orders/by-supplier/${supplierId}`,
  )
}

export function fetchWarehouseOrder(id: number) {
  return apiRequest<RestApiResponse<WarehouseOrder>>(`/api/warehouse-orders/${id}`)
}

export function createWarehouseOrder(payload: WarehouseOrderPayload) {
  return apiRequest<RestApiResponse<WarehouseOrder>>('/api/warehouse-orders/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateWarehouseOrder(id: number, payload: WarehouseOrderPayload) {
  return apiRequest<RestApiResponse<WarehouseOrder>>(`/api/warehouse-orders/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteWarehouseOrder(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/warehouse-orders/delete/${id}`, {
    method: 'DELETE',
  })
}

export function transferWarehouseOrder(id: number) {
  return apiRequest<RestApiResponse<WarehouseOrder>>(`/api/warehouse-orders/${id}/transfer`, {
    method: 'PATCH',
  })
}

export function sendWarehouseOrderSms(orderId: number, message: string) {
  return apiRequest<RestApiResponse<null>>(`/api/warehouse-orders/${orderId}/notify/sms`, {
    method: 'POST',
    body: { message },
  })
}

export function sendWarehouseOrderTelegram(orderId: number, message: string) {
  return apiRequest<RestApiResponse<null>>(`/api/warehouse-orders/${orderId}/notify/telegram`, {
    method: 'POST',
    body: { message },
  })
}

export function fetchWarehouseOrderItems(warehouseOrderId: number) {
  return apiRequest<RestApiResponse<WarehouseOrderItem[]>>(
    `/api/warehouse-order-items/by-order/${warehouseOrderId}`,
  )
}

export function createWarehouseOrderItem(payload: WarehouseOrderItemPayload) {
  return apiRequest<RestApiResponse<WarehouseOrderItem>>('/api/warehouse-order-items/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateWarehouseOrderItem(id: number, payload: WarehouseOrderItemPayload) {
  return apiRequest<RestApiResponse<WarehouseOrderItem>>(
    `/api/warehouse-order-items/update/${id}`,
    { method: 'PUT', body: payload },
  )
}

export function deleteWarehouseOrderItem(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/warehouse-order-items/delete/${id}`, {
    method: 'DELETE',
  })
}

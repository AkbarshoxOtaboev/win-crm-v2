import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface SaleOrderItem {
  id: number
  warehouseId?: number
  warehouseName?: string
  saleOrderId?: number
  clientId?: number
  clientFullName?: string
  goodsId?: number
  goodsName?: string
  priceCost?: number
  priceSelling?: number
  width?: number
  height?: number
  count?: number
  arrivalDate?: string
  status?: string
}

export interface SaleOrderItemPayload {
  warehouseId: number
  saleOrderId: number
  clientId: number
  goodsId: number
  priceCost: number
  priceSelling: number
  count: number
  arrivalDate: string
  width?: number
  height?: number
}

export function fetchSaleOrderItems(saleOrderId: number) {
  return apiRequest<RestApiResponse<SaleOrderItem[]>>(
    `/api/sale-order-items/sale-order/${saleOrderId}`,
  )
}

export function createSaleOrderItem(payload: SaleOrderItemPayload) {
  return apiRequest<RestApiResponse<SaleOrderItem>>('/api/sale-order-items/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateSaleOrderItem(id: number, payload: SaleOrderItemPayload) {
  return apiRequest<RestApiResponse<SaleOrderItem>>(`/api/sale-order-items/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteSaleOrderItem(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/sale-order-items/delete/${id}`, {
    method: 'DELETE',
  })
}

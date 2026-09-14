import { apiRequest } from './http'
import type { RestApiResponse, SpringPage } from './types'

export interface SaleOrderWaste {
  id: number
  saleOrderId?: number
  goodsId?: number
  goodsName?: string
  quantity?: number
  comment?: string
  createdAt?: string
}

export interface SaleOrderWastePayload {
  saleOrderId: number
  goodsId: number
  quantity: number
  comment?: string
}

export function fetchSaleOrderWastes(saleOrderId: number) {
  return apiRequest<RestApiResponse<SaleOrderWaste[]>>(
    `/api/sale-order-wastes/sale-order/${saleOrderId}`,
  )
}

export function fetchSaleOrderWastePage(page = 0, size = 50) {
  return apiRequest<RestApiResponse<SpringPage<SaleOrderWaste>>>(
    `/api/sale-order-wastes?page=${page}&size=${size}`,
  )
}

export function createSaleOrderWaste(payload: SaleOrderWastePayload) {
  return apiRequest<RestApiResponse<SaleOrderWaste>>('/api/sale-order-wastes/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateSaleOrderWaste(id: number, payload: SaleOrderWastePayload) {
  return apiRequest<RestApiResponse<SaleOrderWaste>>(`/api/sale-order-wastes/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteSaleOrderWaste(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/sale-order-wastes/delete/${id}`, {
    method: 'DELETE',
  })
}

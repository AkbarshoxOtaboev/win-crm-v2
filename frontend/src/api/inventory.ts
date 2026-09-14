import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface InventoryCheckItem {
  id: number
  goodsId?: number
  goodsName?: string
  systemCount?: number
  actualCount?: number
  difference?: number
}

export interface InventoryCheck {
  id: number
  warehouseId?: number
  warehouseName?: string
  checkStatus?: string
  comment?: string
  confirmedAt?: string
  confirmedUsername?: string
  items?: InventoryCheckItem[]
  status?: string
  createdAt?: string
  createdUsername?: string
}

export function fetchInventoryChecks() {
  return apiRequest<RestApiResponse<InventoryCheck[]>>('/api/inventory-checks')
}

export function fetchInventoryCheck(id: number) {
  return apiRequest<RestApiResponse<InventoryCheck>>(`/api/inventory-checks/${id}`)
}

export function startInventoryCheck(warehouseId: number, comment?: string) {
  return apiRequest<RestApiResponse<InventoryCheck>>('/api/inventory-checks/start', {
    method: 'POST',
    body: { warehouseId, comment },
  })
}

export function updateInventoryCheckItem(
  inventoryCheckId: number,
  itemId: number,
  actualCount: number,
) {
  return apiRequest<RestApiResponse<InventoryCheck>>(
    `/api/inventory-checks/${inventoryCheckId}/items/${itemId}`,
    { method: 'PUT', body: { actualCount } },
  )
}

export function confirmInventoryCheck(id: number) {
  return apiRequest<RestApiResponse<InventoryCheck>>(`/api/inventory-checks/${id}/confirm`, {
    method: 'POST',
  })
}

export function cancelInventoryCheck(id: number) {
  return apiRequest<RestApiResponse<InventoryCheck>>(`/api/inventory-checks/${id}/cancel`, {
    method: 'POST',
  })
}

export function deleteInventoryCheck(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/inventory-checks/delete/${id}`, {
    method: 'DELETE',
  })
}

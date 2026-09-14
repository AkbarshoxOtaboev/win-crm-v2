import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface Warehouse {
  id: number
  name: string
  status?: string
  createdAt?: string
  updatedAt?: string
  createdUsername?: string
}

export function fetchWarehouses() {
  return apiRequest<RestApiResponse<Warehouse[]>>('/api/warehouses')
}

export function createWarehouse(name: string) {
  return apiRequest<RestApiResponse<Warehouse>>('/api/warehouses/create', {
    method: 'POST',
    body: { name },
  })
}

export function updateWarehouse(id: number, name: string) {
  return apiRequest<RestApiResponse<Warehouse>>(`/api/warehouses/update/${id}`, {
    method: 'PUT',
    body: { name },
  })
}

export function deleteWarehouse(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/warehouses/delete/${id}`, {
    method: 'DELETE',
  })
}

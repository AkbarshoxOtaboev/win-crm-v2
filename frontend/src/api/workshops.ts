import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface Workshop {
  id: number
  name: string
  description?: string
  managerId?: number | null
  managerFullName?: string | null
  status?: string
  createdAt?: string
  updatedAt?: string
  createdUsername?: string
}

export interface WorkshopPayload {
  name: string
  description?: string
  managerId?: number | null
}

export function fetchWorkshops() {
  return apiRequest<RestApiResponse<Workshop[]>>('/api/workshops')
}

export function fetchActiveWorkshops() {
  return apiRequest<RestApiResponse<Workshop[]>>('/api/workshops/active')
}

export function createWorkshop(payload: WorkshopPayload) {
  return apiRequest<RestApiResponse<Workshop>>('/api/workshops/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateWorkshop(id: number, payload: WorkshopPayload) {
  return apiRequest<RestApiResponse<Workshop>>(`/api/workshops/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteWorkshop(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/workshops/delete/${id}`, {
    method: 'DELETE',
  })
}

export function changeWorkshopStatus(id: number) {
  return apiRequest<RestApiResponse<Workshop>>(`/api/workshops/${id}/change-status`, {
    method: 'PUT',
  })
}

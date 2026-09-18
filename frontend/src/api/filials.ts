import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface FilialItem {
  id: number
  name: string
  address?: string | null
  phone?: string | null
  directorId?: number | null
  directorFullName?: string | null
  directorUsername?: string | null
  status?: string
  createdAt?: string
}

export interface FilialPayload {
  name: string
  address?: string
  phone?: string
  directorId?: number | null
}

export function fetchFilials() {
  return apiRequest<RestApiResponse<FilialItem[]>>('/api/filials')
}

export function fetchFilial(id: number) {
  return apiRequest<RestApiResponse<FilialItem>>(`/api/filials/${id}`)
}

export function createFilial(payload: FilialPayload) {
  return apiRequest<RestApiResponse<FilialItem>>('/api/filials', {
    method: 'POST',
    body: payload,
  })
}

export function updateFilial(id: number, payload: FilialPayload) {
  return apiRequest<RestApiResponse<FilialItem>>(`/api/filials/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteFilial(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/filials/${id}`, { method: 'DELETE' })
}

export function assignFilialDirector(id: number, directorId: number) {
  return apiRequest<RestApiResponse<FilialItem>>(`/api/filials/${id}/director`, {
    method: 'PUT',
    body: { directorId },
  })
}

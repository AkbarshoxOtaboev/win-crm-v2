import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface ClientGroup {
  id: number
  name: string
  description?: string
  status?: string
}

export interface ClientGroupPayload {
  name: string
  description?: string
}

export function fetchClientGroups() {
  return apiRequest<RestApiResponse<ClientGroup[]>>('/api/client-groups')
}

export function createClientGroup(payload: ClientGroupPayload) {
  return apiRequest<RestApiResponse<ClientGroup>>('/api/client-groups/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateClientGroup(id: number, payload: ClientGroupPayload) {
  return apiRequest<RestApiResponse<ClientGroup>>(`/api/client-groups/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteClientGroup(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/client-groups/delete/${id}`, {
    method: 'DELETE',
  })
}

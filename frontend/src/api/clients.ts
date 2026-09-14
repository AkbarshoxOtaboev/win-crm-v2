import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface Client {
  id: number
  fullName: string
  inn?: string
  phone: string
  additionalPhone?: string
  address: string
  bankName?: string
  mfo?: string
  accountNumber?: string
  description?: string
  status?: string
  createdAt?: string
  updatedAt?: string
  createdUsername?: string
  clientGroupId?: number
  clientGroupName?: string
}

export interface ClientPayload {
  fullName: string
  phone: string
  address: string
  inn?: string
  additionalPhone?: string
  bankName?: string
  mfo?: string
  accountNumber?: string
  description?: string
  clientGroupId?: number | null
}

export function fetchClients() {
  return apiRequest<RestApiResponse<Client[]>>('/api/clients')
}

export function fetchClient(id: number) {
  return apiRequest<RestApiResponse<Client>>(`/api/clients/${id}`)
}

export function createClient(payload: ClientPayload) {
  return apiRequest<RestApiResponse<Client>>('/api/clients/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateClient(id: number, payload: ClientPayload) {
  return apiRequest<RestApiResponse<Client>>(`/api/clients/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteClient(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/clients/delete/${id}`, {
    method: 'DELETE',
  })
}

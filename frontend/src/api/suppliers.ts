import { apiRequest } from './http'
import type { RestApiResponse, SpringPage } from './types'

export interface Supplier {
  id: number
  name: string
  inn?: string
  phone: string
  additionalPhone?: string
  address?: string
  bankName?: string
  mfo?: string
  accountNumber?: string
  description?: string
  status?: string
  createdUsername?: string
  createdAt?: string
  updatedAt?: string
}

export interface SupplierPayload {
  name: string
  phone: string
  inn?: string
  additionalPhone?: string
  address?: string
  bankName?: string
  mfo?: string
  accountNumber?: string
  description?: string
}

export function fetchSuppliers(page = 0, size = 100) {
  return apiRequest<RestApiResponse<SpringPage<Supplier>>>(
    `/api/suppliers?page=${page}&size=${size}`,
  )
}

export function createSupplier(payload: SupplierPayload) {
  return apiRequest<RestApiResponse<Supplier>>('/api/suppliers/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateSupplier(id: number, payload: SupplierPayload) {
  return apiRequest<RestApiResponse<Supplier>>(`/api/suppliers/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteSupplier(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/suppliers/delete/${id}`, {
    method: 'DELETE',
  })
}

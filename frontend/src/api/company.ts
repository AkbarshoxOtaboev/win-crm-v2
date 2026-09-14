import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface CompanyDetail {
  id: number
  companyName?: string
  inn?: string
  oked?: string
  mfo?: string
  accountNumber?: string
  bankName?: string
  director?: string
  phone?: string
  email?: string
  address?: string
  description?: string
}

export type CompanyDetailPayload = Omit<CompanyDetail, 'id'>

export function fetchCompanyDetails() {
  return apiRequest<RestApiResponse<CompanyDetail[]>>('/api/company-details')
}

export function fetchCurrentCompany() {
  return apiRequest<RestApiResponse<CompanyDetail>>('/api/company-details/current')
}

export function createCompanyDetail(payload: CompanyDetailPayload) {
  return apiRequest<RestApiResponse<CompanyDetail>>('/api/company-details/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateCompanyDetail(id: number, payload: CompanyDetailPayload) {
  return apiRequest<RestApiResponse<CompanyDetail>>(`/api/company-details/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteCompanyDetail(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/company-details/delete/${id}`, {
    method: 'DELETE',
  })
}

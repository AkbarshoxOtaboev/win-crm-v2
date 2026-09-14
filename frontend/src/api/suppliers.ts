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

export function changeSupplierStatus(id: number, status: string) {
  return apiRequest<RestApiResponse<Supplier>>(
    `/api/suppliers/change/status/${id}?status=${encodeURIComponent(status)}`,
    { method: 'PUT' },
  )
}

export interface SupplierBalance {
  id?: number
  supplierId?: number
  supplierName?: string
  totalPurchase?: number
  totalPaid?: number
  totalDebt?: number
  balance?: number
  lastUpdated?: string
  updatedAt?: string
  status?: string
}

export function fetchSupplierBalances(page = 0, size = 50) {
  return apiRequest<RestApiResponse<SpringPage<SupplierBalance>>>(
    `/api/supplier-balances?page=${page}&size=${size}`,
  )
}

export function fetchSupplierBalance(supplierId: number) {
  return apiRequest<RestApiResponse<SupplierBalance>>(`/api/supplier-balances/${supplierId}`)
}

export interface SupplierPayment {
  id: number
  supplierId?: number
  supplierName?: string
  paidSumm?: number
  paidDate?: string
  comment?: string
  paymentTypeId?: number
  paymentTypeName?: string
}

export interface SupplierPaymentPayload {
  supplierId: number
  paidSumm: number
  paidDate: string
  paymentTypeId: number
  comment?: string
}

export function fetchSupplierPayments(page = 0, size = 50) {
  return apiRequest<RestApiResponse<SpringPage<SupplierPayment>>>(
    `/api/supplier-payments?page=${page}&size=${size}`,
  )
}

export function createSupplierPayment(payload: SupplierPaymentPayload) {
  return apiRequest<RestApiResponse<SupplierPayment>>('/api/supplier-payments/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateSupplierPayment(id: number, payload: SupplierPaymentPayload) {
  return apiRequest<RestApiResponse<SupplierPayment>>(`/api/supplier-payments/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteSupplierPayment(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/supplier-payments/delete/${id}`, {
    method: 'DELETE',
  })
}

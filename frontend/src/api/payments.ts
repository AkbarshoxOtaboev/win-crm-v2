import { apiRequest } from './http'
import type { PageResponse, RestApiResponse, SpringPage } from './types'

export interface Payment {
  id: number
  clientId?: number
  clientFullName?: string
  userId?: number
  userFullName?: string
  saleOrderId?: number
  paymentTypeId?: number
  paymentTypeName?: string
  paymentAmount?: number
  paymentDate?: string
  comment?: string
  status?: string
}

export interface PaymentPayload {
  clientId: number
  userId: number
  paymentTypeId: number
  paymentAmount: number
  paymentDate: string
  saleOrderId?: number | null
  comment?: string
}

export interface PaymentType {
  id: number
  name: string
  status?: string
}

export function fetchPayments(page = 0, size = 50) {
  return apiRequest<RestApiResponse<PageResponse<Payment>>>(
    `/api/payments?page=${page}&size=${size}`,
  )
}

export function createPayment(payload: PaymentPayload) {
  return apiRequest<RestApiResponse<Payment>>('/api/payments/create', {
    method: 'POST',
    body: payload,
  })
}

export function updatePayment(id: number, payload: PaymentPayload) {
  return apiRequest<RestApiResponse<Payment>>(`/api/payments/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deletePayment(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/payments/delete/${id}`, {
    method: 'DELETE',
  })
}

export function fetchPaymentTypes() {
  return apiRequest<RestApiResponse<SpringPage<PaymentType>>>(
    '/api/payment-types?page=0&size=100',
  )
}

export function createPaymentType(name: string) {
  return apiRequest<RestApiResponse<PaymentType>>('/api/payment-types/create', {
    method: 'POST',
    body: { name },
  })
}

export function updatePaymentType(id: number, name: string) {
  return apiRequest<RestApiResponse<PaymentType>>(`/api/payment-types/update/${id}`, {
    method: 'PUT',
    body: { name },
  })
}

export function deletePaymentType(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/payment-types/delete/${id}`, {
    method: 'DELETE',
  })
}

export function fetchPaymentsByClient(clientId: number, page = 0, size = 50) {
  return apiRequest<RestApiResponse<PageResponse<Payment>>>(
    `/api/payments/client/${clientId}?page=${page}&size=${size}&sort=id,asc`,
  )
}

export function fetchPaymentsBySaleOrder(saleOrderId: number, page = 0, size = 50) {
  return apiRequest<RestApiResponse<PageResponse<Payment>>>(
    `/api/payments/sale-order/${saleOrderId}?page=${page}&size=${size}`,
  )
}

import { apiRequest } from './http'
import type { RestApiResponse, SpringPage } from './types'

export interface DebtorClient {
  clientId?: number
  clientFullName?: string
  phone?: string
  totalDebt?: number
  debtSum?: number
}

export interface DebtNotificationHistory {
  id: number
  clientId?: number
  clientFullName?: string
  saleOrderId?: number
  status?: string
  message?: string
  createdAt?: string
}

export function fetchDebtors() {
  return apiRequest<RestApiResponse<DebtorClient[]>>('/api/notifications/debt/debtors')
}

export function sendDebtSmsToClient(clientId: number) {
  return apiRequest<RestApiResponse<null>>(`/api/notifications/debt/send/client/${clientId}`, {
    method: 'POST',
  })
}

export function sendDebtSmsToClients(clientIds: number[]) {
  return apiRequest<RestApiResponse<null>>('/api/notifications/debt/send/clients', {
    method: 'POST',
    body: clientIds,
  })
}

export function sendDebtSmsForOrder(saleOrderId: number) {
  return apiRequest<RestApiResponse<null>>(`/api/notifications/debt/send/order/${saleOrderId}`, {
    method: 'POST',
  })
}

export function fetchDebtHistory(page = 0, size = 50) {
  return apiRequest<RestApiResponse<SpringPage<DebtNotificationHistory>>>(
    `/api/notifications/debt/history?page=${page}&size=${size}`,
  )
}

export function fetchDebtHistoryByClient(clientId: number, page = 0, size = 20) {
  return apiRequest<RestApiResponse<SpringPage<DebtNotificationHistory>>>(
    `/api/notifications/debt/history/client/${clientId}?page=${page}&size=${size}`,
  )
}

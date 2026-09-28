import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export const DELIVERY_STATUSES = [
  'PENDING',
  'ACCEPTED',
  'IN_TRANSIT',
  'ARRIVED',
  'CONFIRMED',
  'CANCELLED',
] as const
export type DeliveryStatus = (typeof DELIVERY_STATUSES)[number]

export type WorkerSalaryStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface TransportDriver {
  id: number
  fullName: string
  phone?: string | null
  carModel?: string | null
  carNumber?: string | null
  note?: string | null
  status?: string
  createdAt?: string
}

export interface TransportDriverPayload {
  fullName: string
  phone?: string
  carModel?: string
  carNumber?: string
  note?: string
}

export interface TransportWorker {
  id: number
  fullName: string
  phone?: string | null
  note?: string | null
  status?: string
  createdAt?: string
}

export interface TransportWorkerPayload {
  fullName: string
  phone?: string
  note?: string
}

export interface DeliveryWorkerRef {
  id: number
  fullName: string
  phone?: string | null
}

export interface DeliveryItem {
  id: number
  goodsName?: string | null
  goodsType?: string | null
  count?: number | null
  width?: number | null
  height?: number | null
}

export interface Delivery {
  id: number
  deliveryStatus: DeliveryStatus
  saleOrderId: number
  saleOrderStatus?: string
  orderDate?: string | null
  plannedDeliveryDate?: string | null
  orderTotalSum?: number | null
  sellerFullName?: string | null
  orderComment?: string | null
  clientId?: number | null
  clientFullName?: string | null
  clientPhone?: string | null
  address?: string | null
  driverId?: number | null
  driverFullName?: string | null
  driverPhone?: string | null
  carModel?: string | null
  carNumber?: string | null
  workers: DeliveryWorkerRef[]
  note?: string | null
  sentAt?: string | null
  acceptedAt?: string | null
  acceptedByName?: string | null
  departedAt?: string | null
  arrivedAt?: string | null
  confirmedAt?: string | null
  confirmedByName?: string | null
  cancelledAt?: string | null
  salaryPercent?: number | null
  salaryTotal?: number | null
  items: DeliveryItem[]
}

export interface DeliveryCrewPayload {
  driverId: number
  workerIds: number[]
  address?: string
  note?: string
}

export interface WorkerSalary {
  id: number
  deliveryId?: number
  saleOrderId: number
  clientFullName?: string | null
  workerId: number
  workerFullName?: string | null
  orderTotalSnapshot: number
  percentSnapshot: number
  workersCount: number
  amount: number
  salaryStatus: WorkerSalaryStatus
  earnedAt?: string
  periodYear: number
  periodMonth: number
  decidedAt?: string | null
  decidedByName?: string | null
  comment?: string | null
}

export interface WorkerSalarySummary {
  workerId: number
  workerFullName?: string | null
  deliveriesCount: number
  pendingAmount: number
  approvedAmount: number
}

export interface TransportDashboard {
  pendingCount: number
  acceptedCount: number
  inTransitCount: number
  arrivedCount: number
  confirmedMonthCount: number
  confirmedMonthSum: number
  salaryPendingAmount: number
  salaryApprovedMonthAmount: number
  workerSalaryPercent: number
  activeDrivers: number
  activeWorkers: number
  activeDeliveries: Delivery[]
}

export interface TransportSetting {
  workerSalaryPercent: number
}

export function fetchTransportDashboard() {
  return apiRequest<RestApiResponse<TransportDashboard>>('/api/transport/dashboard')
}

export function fetchDeliveries(statuses: DeliveryStatus[] = []) {
  const query = statuses.length ? `?statuses=${statuses.join(',')}` : ''
  return apiRequest<RestApiResponse<Delivery[]>>(`/api/transport/deliveries${query}`)
}

export function fetchDeliveryBySaleOrder(saleOrderId: number) {
  return apiRequest<RestApiResponse<Delivery | null>>(`/api/transport/deliveries/by-sale-order/${saleOrderId}`)
}

export function acceptDelivery(id: number, payload: DeliveryCrewPayload) {
  return apiRequest<RestApiResponse<Delivery>>(`/api/transport/deliveries/${id}/accept`, {
    method: 'POST',
    body: payload,
  })
}

export function updateDeliveryCrew(id: number, payload: DeliveryCrewPayload) {
  return apiRequest<RestApiResponse<Delivery>>(`/api/transport/deliveries/${id}/crew`, {
    method: 'PUT',
    body: payload,
  })
}

export function departDelivery(id: number) {
  return apiRequest<RestApiResponse<Delivery>>(`/api/transport/deliveries/${id}/depart`, { method: 'POST' })
}

export function arriveDelivery(id: number) {
  return apiRequest<RestApiResponse<Delivery>>(`/api/transport/deliveries/${id}/arrive`, { method: 'POST' })
}

export function fetchDrivers() {
  return apiRequest<RestApiResponse<TransportDriver[]>>('/api/transport/drivers')
}

export function fetchActiveDrivers() {
  return apiRequest<RestApiResponse<TransportDriver[]>>('/api/transport/drivers/active')
}

export function createDriver(payload: TransportDriverPayload) {
  return apiRequest<RestApiResponse<TransportDriver>>('/api/transport/drivers', { method: 'POST', body: payload })
}

export function updateDriver(id: number, payload: TransportDriverPayload) {
  return apiRequest<RestApiResponse<TransportDriver>>(`/api/transport/drivers/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function changeDriverStatus(id: number) {
  return apiRequest<RestApiResponse<TransportDriver>>(`/api/transport/drivers/${id}/change-status`, {
    method: 'PUT',
  })
}

export function deleteDriver(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/transport/drivers/${id}`, { method: 'DELETE' })
}

export function fetchWorkers() {
  return apiRequest<RestApiResponse<TransportWorker[]>>('/api/transport/workers')
}

export function fetchActiveWorkers() {
  return apiRequest<RestApiResponse<TransportWorker[]>>('/api/transport/workers/active')
}

export function createWorker(payload: TransportWorkerPayload) {
  return apiRequest<RestApiResponse<TransportWorker>>('/api/transport/workers', { method: 'POST', body: payload })
}

export function updateWorker(id: number, payload: TransportWorkerPayload) {
  return apiRequest<RestApiResponse<TransportWorker>>(`/api/transport/workers/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function changeWorkerStatus(id: number) {
  return apiRequest<RestApiResponse<TransportWorker>>(`/api/transport/workers/${id}/change-status`, {
    method: 'PUT',
  })
}

export function deleteWorker(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/transport/workers/${id}`, { method: 'DELETE' })
}

export function fetchWorkerSalaries(params: {
  year: number
  month: number
  status?: WorkerSalaryStatus | ''
  workerId?: number | null
}) {
  const q = new URLSearchParams({ year: String(params.year), month: String(params.month) })
  if (params.status) q.set('status', params.status)
  if (params.workerId) q.set('workerId', String(params.workerId))
  return apiRequest<RestApiResponse<WorkerSalary[]>>(`/api/transport/salaries?${q.toString()}`)
}

export function fetchWorkerSalarySummary(year: number, month: number) {
  return apiRequest<RestApiResponse<WorkerSalarySummary[]>>(
    `/api/transport/salaries/summary?year=${year}&month=${month}`,
  )
}

export function approveWorkerSalaries(ids: number[], comment?: string) {
  return apiRequest<RestApiResponse<WorkerSalary[]>>('/api/transport/salaries/approve', {
    method: 'POST',
    body: { ids, comment },
  })
}

export function rejectWorkerSalaries(ids: number[], comment?: string) {
  return apiRequest<RestApiResponse<WorkerSalary[]>>('/api/transport/salaries/reject', {
    method: 'POST',
    body: { ids, comment },
  })
}

export function fetchTransportSetting() {
  return apiRequest<RestApiResponse<TransportSetting>>('/api/transport/settings')
}

export function updateTransportSetting(workerSalaryPercent: number) {
  return apiRequest<RestApiResponse<TransportSetting>>('/api/transport/settings', {
    method: 'PUT',
    body: { workerSalaryPercent },
  })
}

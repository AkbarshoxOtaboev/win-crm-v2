import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface Workshop {
  id: number
  name: string
  description?: string
  managerId?: number | null
  managerFullName?: string | null
  feePercent?: number | null
  status?: string
  createdAt?: string
  updatedAt?: string
  createdUsername?: string
}

export interface WorkshopPayload {
  name: string
  description?: string
  managerId?: number | null
  feePercent?: number | null
}

export interface WorkshopDashboardCompletedWork {
  assignmentId: number
  productionOrderId?: number
  saleOrderId?: number
  clientFullName?: string
  orderTotalSum?: number
  feePercent?: number
  earnedAmount?: number
  acceptedAt?: string
  submittedAt?: string
  sequenceNo?: number
}

export interface WorkshopDashboard {
  workshopId: number
  workshopName: string
  feePercent?: number
  balance?: number
  totalEarned?: number
  queuedCount: number
  queuedSum: number
  inProgressCount: number
  inProgressSum: number
  doneCount: number
  doneSum: number
  completedWorks: WorkshopDashboardCompletedWork[]
}

export function fetchWorkshops() {
  return apiRequest<RestApiResponse<Workshop[]>>('/api/workshops')
}

export function fetchActiveWorkshops() {
  return apiRequest<RestApiResponse<Workshop[]>>('/api/workshops/active')
}

export function fetchWorkshopDashboard(workshopId: number) {
  return apiRequest<RestApiResponse<WorkshopDashboard>>(`/api/workshops/${workshopId}/dashboard`)
}

export function setAssignmentFeePercent(assignmentId: number, feePercent: number) {
  return apiRequest<RestApiResponse<null>>(`/api/workshops/assignments/${assignmentId}/fee-percent`, {
    method: 'PUT',
    body: { feePercent },
  })
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

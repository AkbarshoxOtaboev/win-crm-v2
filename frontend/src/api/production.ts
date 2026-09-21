import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface ProductionOrder {
  id: number
  saleOrderId?: number
  clientFullName?: string
  productionStatus?: string
  currentWorkshopId?: number
  currentWorkshopName?: string
  currentAssignmentStatus?: string
  currentAssignmentId?: number
  startedAt?: string
  doneAt?: string
  note?: string
  createdAt?: string
}

export interface ProductionEvent {
  id: number
  eventType?: string
  fromWorkshopId?: number
  fromWorkshopName?: string
  toWorkshopId?: number
  toWorkshopName?: string
  actorId?: number
  actorName?: string
  occurredAt?: string
  comment?: string
}

export function sendToProduction(payload: {
  saleOrderId: number
  workshopId: number
  note?: string
}) {
  return apiRequest<RestApiResponse<ProductionOrder>>('/api/production-orders/send-to-production', {
    method: 'POST',
    body: payload,
  })
}

export function fetchProductionOrders() {
  return apiRequest<RestApiResponse<ProductionOrder[]>>('/api/production-orders')
}

export function fetchProductionBoard(workshopId: number) {
  return apiRequest<RestApiResponse<ProductionOrder[]>>(
    `/api/production-orders/board?workshopId=${workshopId}`,
  )
}

export function fetchProductionOrder(id: number) {
  return apiRequest<RestApiResponse<ProductionOrder>>(`/api/production-orders/${id}`)
}

export function fetchProductionTimeline(id: number) {
  return apiRequest<RestApiResponse<ProductionEvent[]>>(`/api/production-orders/${id}/timeline`)
}

export function startProduction(id: number) {
  return apiRequest<RestApiResponse<ProductionOrder>>(`/api/production-orders/${id}/start`, {
    method: 'POST',
  })
}

export function redirectProduction(id: number, nextWorkshopId: number, note?: string) {
  return apiRequest<RestApiResponse<ProductionOrder>>(`/api/production-orders/${id}/redirect`, {
    method: 'POST',
    body: { nextWorkshopId, note },
  })
}

export function completeProduction(id: number, note?: string) {
  return apiRequest<RestApiResponse<ProductionOrder>>(`/api/production-orders/${id}/complete`, {
    method: 'POST',
    body: { note },
  })
}

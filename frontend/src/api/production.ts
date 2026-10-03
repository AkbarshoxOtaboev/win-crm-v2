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
  route?: ProductionRouteStep[]
  nextWorkshopId?: number | null
  nextWorkshopName?: string | null
  boardAssignmentId?: number
  boardAssignmentStatus?: 'PENDING' | 'ACTIVE' | 'DONE' | 'REDIRECTED' | string
  acceptedAt?: string
  submittedAt?: string
  orderDate?: string
  plannedReadyDate?: string
  saleOrderComment?: string
  items?: ProductionOrderItem[]
  images?: ProductionOrderImage[]
}

export interface ProductionOrderItem {
  id: number
  goodsId?: number
  goodsName?: string
  width?: number
  height?: number
  count?: number
  pieces?: number
}

export interface ProductionOrderImage {
  id: number
  url: string
  originalFileName?: string
}

export interface ProductionRouteStep {
  stepNo: number
  workshopId: number
  workshopName?: string
  state: 'DONE' | 'CURRENT' | 'PLANNED'
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
  workshopIds: number[]
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

export function fetchProductionBoard(workshopId: number, fromDate?: string, toDate?: string) {
  const params = new URLSearchParams({ workshopId: String(workshopId) })
  if (fromDate) params.set('fromDate', fromDate)
  if (toDate) params.set('toDate', toDate)
  return apiRequest<RestApiResponse<ProductionOrder[]>>(`/api/production-orders/board?${params}`)
}

export function fetchProductionBySaleOrder(saleOrderId: number) {
  return apiRequest<RestApiResponse<ProductionOrder | null>>(
    `/api/production-orders/by-sale-order/${saleOrderId}`,
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

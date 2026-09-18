import { apiRequest } from './http'
import type { PageResponse, RestApiResponse } from './types'

export interface AuditLog {
  id: number
  username?: string
  action?: string
  entity?: string
  httpMethod?: string
  requestUrl?: string
  description?: string
  createdAt?: string
  ipAddress?: string
}

export interface AuditLogParams {
  page?: number
  size?: number
  username?: string
  fromDate?: string
  toDate?: string
}

export function fetchAuditLogs(params: AuditLogParams = {}) {
  const q = new URLSearchParams()
  q.set('page', String(params.page ?? 0))
  q.set('size', String(params.size ?? 50))
  q.set('sort', 'createdAt,DESC')
  if (params.username) q.set('username', params.username)
  if (params.fromDate) q.set('fromDate', params.fromDate)
  if (params.toDate) q.set('toDate', params.toDate)
  return apiRequest<RestApiResponse<PageResponse<AuditLog>>>(`/api/audit/logs?${q.toString()}`)
}

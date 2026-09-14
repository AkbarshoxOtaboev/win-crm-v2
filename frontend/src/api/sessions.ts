import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface SessionItem {
  id: number
  username?: string
  status?: string
  ipAddress?: string
  userAgent?: string
  createdAt?: string
  lastSeenAt?: string
}

export interface SessionSummary {
  total?: number
  active?: number
  revoked?: number
}

export function fetchSessions() {
  return apiRequest<RestApiResponse<SessionItem[]>>('/api/sessions')
}

export function fetchSessionSummary() {
  return apiRequest<RestApiResponse<SessionSummary>>('/api/sessions/summary')
}

export function heartbeat() {
  return apiRequest<RestApiResponse<null>>('/api/sessions/heartbeat', { method: 'POST' })
}

export function revokeSession(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/sessions/${id}`, { method: 'DELETE' })
}

export function revokeOtherSessions() {
  return apiRequest<RestApiResponse<null>>('/api/sessions', { method: 'DELETE' })
}

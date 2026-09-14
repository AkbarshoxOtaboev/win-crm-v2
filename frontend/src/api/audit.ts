import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface AuditLog {
  id: number
  username?: string
  action?: string
  entity?: string
  entityId?: number
  details?: string
  createdAt?: string
}

export function fetchAuditLogs() {
  return apiRequest<RestApiResponse<AuditLog[]>>('/api/audit/logs')
}

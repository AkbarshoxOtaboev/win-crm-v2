import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface EskizSettings {
  configured?: boolean
  email?: string
  hasToken?: boolean
}

export function fetchEskizSettings() {
  return apiRequest<RestApiResponse<EskizSettings>>('/api/eskiz-settings')
}

export function saveEskizSettings(email: string, password: string) {
  return apiRequest<RestApiResponse<EskizSettings>>('/api/eskiz-settings', {
    method: 'POST',
    body: { email, password },
  })
}

export function fetchEskizToken() {
  return apiRequest<RestApiResponse<string>>('/api/eskiz-settings/token')
}

import { apiRequest } from './http'

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  sessionId: number
  roles?: string[]
  superAdmin?: boolean
  filialId?: number | null
  filialName?: string | null
}

export interface LoginPayload {
  username: string
  password: string
}

export function login(payload: LoginPayload) {
  return apiRequest<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: payload,
    auth: false,
  })
}

export function refresh(refreshToken: string) {
  return apiRequest<AuthResponse>('/api/auth/refresh', {
    method: 'POST',
    body: { refreshToken },
    auth: false,
  })
}

export function fetchMe() {
  return apiRequest<AuthResponse>('/api/auth/me')
}

export function logout() {
  return apiRequest<string>('/api/auth/logout', {
    method: 'POST',
  })
}

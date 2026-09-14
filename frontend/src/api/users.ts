import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface UserItem {
  id: number
  username: string
  fullName?: string
  status?: string
}

export function fetchUsers() {
  return apiRequest<RestApiResponse<UserItem[]>>('/api/users')
}

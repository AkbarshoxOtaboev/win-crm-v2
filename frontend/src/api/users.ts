import { apiRequest } from './http'
import type { RestApiResponse } from './types'
import type { RoleItem } from './roles'

export interface UserItem {
  id: number
  username: string
  fullName?: string
  phone?: string
  status?: string
  photoLink?: string
  role?: RoleItem[]
  createdAt?: string
}

export interface UserStat {
  orderCount?: number
  paymentSum?: number
  debtSum?: number
}

export function fetchUsers() {
  return apiRequest<RestApiResponse<UserItem[]>>('/api/users')
}

export function fetchUser(id: number) {
  return apiRequest<RestApiResponse<UserItem>>(`/api/users/${id}`)
}

export function fetchUserStats(id: number) {
  return apiRequest<RestApiResponse<UserStat>>(`/api/users/${id}/stats`)
}

export function createUser(form: FormData) {
  return apiRequest<RestApiResponse<UserItem>>('/api/users/create', {
    method: 'POST',
    body: form,
  })
}

export function updateUser(id: number, form: FormData) {
  return apiRequest<RestApiResponse<UserItem>>(`/api/users/update/${id}`, {
    method: 'PUT',
    body: form,
  })
}

export function deleteUser(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/users/delete/${id}`, { method: 'DELETE' })
}

export function changeUserStatus(id: number) {
  return apiRequest<RestApiResponse<UserItem>>(`/api/users/change/status/${id}`, {
    method: 'PUT',
  })
}
